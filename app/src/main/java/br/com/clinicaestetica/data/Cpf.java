package br.com.clinicaestetica.data;

/** Validação de CPF pelos dígitos verificadores. */
public final class Cpf {
    private static final int LENGTH = 11;

    private Cpf() {
    }

    public static String digits(String text) {
        return text == null ? "" : text.replaceAll("\\D", "");
    }

    public static boolean isValid(String text) {
        String cpf = digits(text);
        if (cpf.length() != LENGTH || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(cpf, 9) == cpf.charAt(9) - '0'
                && checkDigit(cpf, 10) == cpf.charAt(10) - '0';
    }

    /** "52998224725" vira "529.982.247-25"; outro tamanho volta como veio. */
    public static String format(String text) {
        String cpf = digits(text);
        if (cpf.length() != LENGTH) {
            return text == null ? "" : text;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9)
                + "-" + cpf.substring(9);
    }

    private static int checkDigit(String cpf, int count) {
        int sum = 0;
        for (int i = 0; i < count; i++) {
            sum += (cpf.charAt(i) - '0') * (count + 1 - i);
        }
        int rest = sum % LENGTH;
        return rest < 2 ? 0 : LENGTH - rest;
    }
}
