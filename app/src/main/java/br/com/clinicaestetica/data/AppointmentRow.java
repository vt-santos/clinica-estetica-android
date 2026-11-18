package br.com.clinicaestetica.data;

import androidx.room.Embedded;

public class AppointmentRow {
    @Embedded
    public Appointment appointment;

    public String patientName;
    public String cellPhone;
}
