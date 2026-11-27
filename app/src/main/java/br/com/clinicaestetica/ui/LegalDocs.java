package br.com.clinicaestetica.ui;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.clinicaestetica.R;

/**
 * Textos legais do app (res/raw/legal_*.txt). Os textos usam marcadores como {{CNPJ}}, que são
 * trocados pelos dados da empresa de res/values/company.xml. Assim o advogado revisa um texto
 * só e os dados da clínica ficam em um lugar.
 *
 * Formato dos arquivos: a primeira linha é o título; linhas que começam com "# " são seções;
 * as demais são parágrafos.
 */
final class LegalDocs {
    static final String PRIVACY = "privacidade";
    static final String TERMS = "termos";
    static final String COOKIES = "cookies";
    static final String REFUND = "reembolso";
    static final String ACCESSIBILITY = "acessibilidade";

    /** Ordem em que os documentos aparecem na tela Sobre. */
    static final String[] ALL = {PRIVACY, TERMS, REFUND, COOKIES, ACCESSIBILITY};

    private static final Pattern TOKEN = Pattern.compile("\\{\\{([A-Z_]+)\\}\\}");
    private static final Pattern PENDING = Pattern.compile("\\[[^\\]]+\\]");

    private LegalDocs() {
    }

    static int raw(String key) {
        switch (key) {
            case PRIVACY:
                return R.raw.legal_privacidade;
            case TERMS:
                return R.raw.legal_termos;
            case COOKIES:
                return R.raw.legal_cookies;
            case REFUND:
                return R.raw.legal_reembolso;
            case ACCESSIBILITY:
                return R.raw.legal_acessibilidade;
            default:
                throw new IllegalArgumentException(key);
        }
    }

    /** Texto do documento já com os dados da empresa. */
    static String load(Context context, String key) {
        String text;
        try (InputStream in = context.getResources().openRawResource(raw(key))) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int read; (read = in.read(buffer)) != -1; ) {
                bytes.write(buffer, 0, read);
            }
            text = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Map<String, String> values = values(context);
        Matcher matcher = TOKEN.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            if (value == null) {
                throw new IllegalStateException("Marcador desconhecido: " + matcher.group());
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString().trim();
    }

    static String title(Context context, String key) {
        String text = load(context, key);
        int end = text.indexOf('\n');
        return end < 0 ? text : text.substring(0, end).trim();
    }

    /** Dados da empresa na ordem em que aparecem na tela Sobre. */
    static Map<String, String> company(Context context) {
        Map<String, String> company = new LinkedHashMap<>();
        company.put("Nome fantasia", context.getString(R.string.company_trade_name));
        company.put("Razão social", context.getString(R.string.company_legal_name));
        company.put("CNPJ", context.getString(R.string.company_cnpj));
        company.put("Endereço", context.getString(R.string.company_address));
        company.put("Telefone", context.getString(R.string.company_phone));
        company.put("E-mail", context.getString(R.string.company_email));
        company.put("Atendimento", context.getString(R.string.company_hours));
        company.put("Responsável técnico", context.getString(R.string.company_technical_lead));
        company.put("Encarregado de dados (LGPD)", context.getString(R.string.company_dpo)
                + " · " + context.getString(R.string.company_dpo_email));
        return company;
    }

    /** true enquanto algum dado da empresa ou regra das políticas ainda for um [campo pendente]. */
    static boolean hasPending(Context context) {
        for (String value : values(context).values()) {
            if (PENDING.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }

    static String version(Context context) {
        return context.getString(R.string.legal_version);
    }

    private static Map<String, String> values(Context context) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("NOME_FANTASIA", context.getString(R.string.company_trade_name));
        values.put("RAZAO_SOCIAL", context.getString(R.string.company_legal_name));
        values.put("CNPJ", context.getString(R.string.company_cnpj));
        values.put("ENDERECO", context.getString(R.string.company_address));
        values.put("TELEFONE", context.getString(R.string.company_phone));
        values.put("EMAIL", context.getString(R.string.company_email));
        values.put("HORARIO", context.getString(R.string.company_hours));
        values.put("ENCARREGADO", context.getString(R.string.company_dpo));
        values.put("EMAIL_ENCARREGADO", context.getString(R.string.company_dpo_email));
        values.put("CIDADE_FORO", context.getString(R.string.company_city));
        values.put("PRAZO_GUARDA", context.getString(R.string.policy_retention));
        values.put("ANTECEDENCIA", context.getString(R.string.policy_cancel_hours));
        values.put("REGRA_FALTA", context.getString(R.string.policy_no_show));
        values.put("PRAZO_REEMBOLSO", context.getString(R.string.policy_refund_days));
        values.put("VERSAO", context.getString(R.string.legal_version));
        values.put("ATUALIZADO", context.getString(R.string.legal_updated));
        return values;
    }
}
