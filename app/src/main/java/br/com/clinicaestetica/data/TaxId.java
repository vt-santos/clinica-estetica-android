package br.com.clinicaestetica.data;

/**
 * CPF ou CNPJ num campo só: limpeza, máscara e validação pelos dígitos verificadores, tudo
 * offline. O CNPJ pode ser numérico ou alfanumérico (formato novo da Receita, desde julho de
 * 2026): as 12 primeiras posições aceitam letras e os 2 dígitos verificadores são sempre
 * números.
 */
public final class TaxId {
    public enum Kind { NONE, CPF, CNPJ }

    public static final int CPF_LENGTH = 11;
    public static final int CNPJ_LENGTH = 14;

    private static final int[] CNPJ_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private TaxId() {
    }

    /** Só letras maiúsculas e números, no máximo 14: "12.abc.345/01de-35" vira "12ABC34501DE35". */
    public static String clean(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.toUpperCase(java.util.Locale.ROOT).replaceAll("[^0-9A-Z]", "");
        return cleaned.length() > CNPJ_LENGTH ? cleaned.substring(0, CNPJ_LENGTH) : cleaned;
    }

    /** Pelo tamanho e pelas letras: até 11 números é CPF; com letra ou mais que 11, CNPJ. */
    public static Kind kind(String text) {
        String id = clean(text);
        if (id.isEmpty()) {
            return Kind.NONE;
        }
        return id.length() <= CPF_LENGTH && id.matches("\\d+") ? Kind.CPF : Kind.CNPJ;
    }

    public static boolean isValid(String text) {
        String id = clean(text);
        switch (kind(id)) {
            case CPF:
                return Cpf.isValid(id);
            case CNPJ:
                return isValidCnpj(id);
            default:
                return false;
        }
    }

    public static boolean isComplete(String text) {
        int length = clean(text).length();
        return kind(text) == Kind.CPF ? length == CPF_LENGTH : length == CNPJ_LENGTH;
    }

    /** Máscara parcial, para aplicar enquanto a pessoa digita. */
    public static String format(String text) {
        String id = clean(text);
        if (kind(id) == Kind.CPF) {
            return mask(id, "###.###.###-##");
        }
        return mask(id, "##.###.###/####-##");
    }

    /** Frase curta para mostrar embaixo do campo. Vazio quando não há nada digitado. */
    public static String describe(String text) {
        Kind kind = kind(text);
        if (kind == Kind.NONE) {
            return "";
        }
        String name = kind == Kind.CPF ? "CPF" : "CNPJ";
        if (!isComplete(text)) {
            return kind == Kind.CPF ? "Continue digitando: CPF tem 11 números, CNPJ tem 14"
                    : "CNPJ incompleto: são 14 caracteres";
        }
        if (isValid(text)) {
            return name + " válido";
        }
        return kind == Kind.CPF ? "CPF inválido: confira os números (se for CNPJ, continue)"
                : "CNPJ inválido: confira os números";
    }

    private static boolean isValidCnpj(String cnpj) {
        if (cnpj.length() != CNPJ_LENGTH || !cnpj.substring(12).matches("\\d{2}")
                || cnpj.chars().distinct().count() == 1) {
            return false;
        }
        return cnpjDigit(cnpj, 12) == cnpj.charAt(12) - '0'
                && cnpjDigit(cnpj, 13) == cnpj.charAt(13) - '0';
    }

    /** Cada caractere vale o código ASCII menos 48: '0'..'9' valem 0..9, 'A' vale 17. */
    private static int cnpjDigit(String cnpj, int count) {
        int sum = 0;
        int offset = CNPJ_WEIGHTS.length - count;
        for (int i = 0; i < count; i++) {
            sum += (cnpj.charAt(i) - '0') * CNPJ_WEIGHTS[offset + i];
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }

    private static String mask(String id, String pattern) {
        StringBuilder out = new StringBuilder();
        int next = 0;
        for (int i = 0; i < pattern.length() && next < id.length(); i++) {
            char slot = pattern.charAt(i);
            out.append(slot == '#' ? id.charAt(next++) : slot);
        }
        return out.toString();
    }
}
