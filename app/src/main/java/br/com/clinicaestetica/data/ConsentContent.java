package br.com.clinicaestetica.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Conteúdo de um termo assinado: o texto aceito, quem assinou e a imagem da assinatura.
 * Tudo isso vira um único JSON, que é criptografado inteiro pelo {@link ConsentVault}.
 */
public class ConsentContent {
    private static final int FORMAT_VERSION = 1;

    public String termText;
    public String place;
    public boolean accepted;
    public String signerName;
    public String signerRole;
    public String signerCpf;
    public byte[] signaturePng;

    public byte[] toJson() {
        try {
            JSONObject json = new JSONObject()
                    .put("v", FORMAT_VERSION)
                    .put("termText", termText)
                    .put("place", place)
                    .put("accepted", accepted)
                    .put("signerName", signerName)
                    .put("signerRole", signerRole)
                    .put("signerCpf", signerCpf);
            if (signaturePng != null) {
                json.put("signaturePng", Base64.getEncoder().encodeToString(signaturePng));
            }
            return json.toString().getBytes(StandardCharsets.UTF_8);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ConsentContent fromJson(byte[] bytes) throws JSONException {
        JSONObject json = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        ConsentContent content = new ConsentContent();
        content.termText = json.optString("termText");
        content.place = json.optString("place");
        content.accepted = json.optBoolean("accepted");
        content.signerName = json.optString("signerName");
        content.signerRole = json.optString("signerRole");
        content.signerCpf = json.optString("signerCpf");
        String png = json.optString("signaturePng");
        content.signaturePng = png.isEmpty() ? null : Base64.getDecoder().decode(png);
        return content;
    }
}
