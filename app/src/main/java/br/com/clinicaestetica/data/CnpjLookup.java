package br.com.clinicaestetica.data;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Confere na Receita Federal, pela BrasilAPI (gratuita), se um CNPJ existe e qual é a situação
 * cadastral. É o único acesso do app à internet: só o número do CNPJ sai do tablet, nunca dados
 * de paciente. CPF não é consultado (não há serviço público gratuito e é dado pessoal).
 */
public final class CnpjLookup {
    /** Único endereço que o app acessa. Ver docs/CONFORMIDADE.md. */
    public static final String BASE_URL = "https://brasilapi.com.br/api/cnpj/v1/";

    private static final int TIMEOUT_MS = 8000;
    private static final int MAX_BYTES = 256 * 1024;
    private static final ExecutorService NETWORK = Executors.newSingleThreadExecutor();

    /** Os testes trocam por uma resposta pronta, para não depender da internet. */
    public static volatile Function<String, Result> source = CnpjLookup::fetch;
    public static volatile Executor executor = NETWORK;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public enum Status { FOUND, NOT_FOUND, OFFLINE, ERROR }

    public static final class Result {
        public final Status status;
        public final String companyName;
        public final String tradeName;
        public final String situation;

        public Result(Status status, String companyName, String tradeName, String situation) {
            this.status = status;
            this.companyName = companyName;
            this.tradeName = tradeName;
            this.situation = situation;
        }

        public static Result of(Status status) {
            return new Result(status, "", "", "");
        }

        public boolean isActive() {
            return status == Status.FOUND && "ATIVA".equalsIgnoreCase(situation);
        }

        /** Frase para mostrar embaixo do campo. */
        public String describe() {
            switch (status) {
                case FOUND:
                    String name = tradeName.isEmpty() ? companyName
                            : companyName + " (" + tradeName + ")";
                    return "Receita: " + name + (situation.isEmpty() ? ""
                            : " · situação " + situation.toLowerCase(java.util.Locale.ROOT));
                case NOT_FOUND:
                    return "CNPJ não encontrado na Receita Federal";
                case OFFLINE:
                    return "CNPJ válido. Sem internet para consultar a Receita agora";
                default:
                    return "CNPJ válido. A consulta à Receita não respondeu, tente mais tarde";
            }
        }
    }

    private CnpjLookup() {
    }

    /** Consulta fora da thread principal e entrega o resultado nela. */
    public static void lookup(String cnpj, Consumer<Result> done) {
        String id = TaxId.clean(cnpj);
        executor.execute(() -> {
            Result result = source.apply(id);
            MAIN.post(() -> done.accept(result));
        });
    }

    static Result fetch(String cnpj) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(BASE_URL + cnpj).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_NOT_FOUND
                    || code == HttpURLConnection.HTTP_BAD_REQUEST) {
                return Result.of(Status.NOT_FOUND);
            }
            if (code != HttpURLConnection.HTTP_OK) {
                return Result.of(Status.ERROR);
            }
            try (InputStream in = connection.getInputStream()) {
                return parse(read(in));
            }
        } catch (java.net.UnknownHostException | java.net.SocketTimeoutException
                 | java.net.ConnectException e) {
            return Result.of(Status.OFFLINE);
        } catch (IOException | RuntimeException e) {
            return Result.of(Status.ERROR);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Lê a resposta da BrasilAPI (campos razao_social, nome_fantasia, situação). */
    public static Result parse(String body) {
        try {
            JSONObject json = new JSONObject(body);
            String company = json.optString("razao_social").trim();
            if (company.isEmpty()) {
                return Result.of(Status.NOT_FOUND);
            }
            return new Result(Status.FOUND, company, json.optString("nome_fantasia").trim(),
                    json.optString("descricao_situacao_cadastral").trim());
        } catch (JSONException e) {
            return Result.of(Status.ERROR);
        }
    }

    private static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            if (out.size() > MAX_BYTES) {
                throw new IOException("Resposta grande demais");
            }
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }
}
