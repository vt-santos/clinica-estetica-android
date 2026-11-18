package br.com.clinicaestetica.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

final class Fmt {
    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private Fmt() {
    }

    static String date(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", BR).format(new Date(millis));
    }

    static String time(long millis) {
        return new SimpleDateFormat("HH:mm", BR).format(new Date(millis));
    }

    static String dateTime(long millis) {
        return date(millis) + " às " + time(millis);
    }

    static long startOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    static long plusDays(long millis, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTimeInMillis();
    }

    static int age(long birthDate) {
        LocalDate birth = Instant.ofEpochMilli(birthDate).atZone(ZoneId.systemDefault()).toLocalDate();
        return Period.between(birth, LocalDate.now()).getYears();
    }

    static String money(long cents) {
        return NumberFormat.getCurrencyInstance(BR).format(cents / 100.0);
    }

    static String moneyInput(long cents) {
        return String.format(BR, "%.2f", cents / 100.0);
    }

    /** Aceita "150", "150,50" e "1.500,50"; retorna -1 se o valor for inválido. */
    static long parseCents(String text) {
        String normalized = text.trim().replace("R$", "").trim();
        if (normalized.contains(",")) {
            normalized = normalized.replace(".", "").replace(',', '.');
        }
        try {
            return new BigDecimal(normalized).movePointRight(2).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            return -1;
        }
    }
}
