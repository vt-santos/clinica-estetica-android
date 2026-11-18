package br.com.clinicaestetica.data;

import androidx.room.Embedded;

public class PaymentRow {
    @Embedded
    public Payment payment;

    public String patientName;
}
