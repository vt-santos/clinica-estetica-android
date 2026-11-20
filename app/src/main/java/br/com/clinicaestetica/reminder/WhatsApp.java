package br.com.clinicaestetica.reminder;

import android.net.Uri;

/** Link do WhatsApp com a mensagem já escrita; quem envia é sempre a proprietária. */
public final class WhatsApp {
    private WhatsApp() {
    }

    public static boolean hasPhone(String phone) {
        return phone != null && !phone.replaceAll("\\D", "").isEmpty();
    }

    public static Uri link(String phone, String message) {
        String digits = phone.replaceAll("\\D", "");
        String withCountry = digits.startsWith("55") && digits.length() >= 12 ? digits : "55" + digits;
        return Uri.parse("https://wa.me/" + withCountry + "?text=" + Uri.encode(message));
    }

    /** Primeiro nome, para a mensagem soar pessoal. */
    public static String firstName(String name) {
        String trimmed = name == null ? "" : name.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }
}
