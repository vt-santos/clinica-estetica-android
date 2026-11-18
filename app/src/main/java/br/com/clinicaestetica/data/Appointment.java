package br.com.clinicaestetica.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Consulta ou retorno agendado. */
@Entity(tableName = "appointments",
        foreignKeys = @ForeignKey(entity = Patient.class, parentColumns = "id",
                childColumns = "patientId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("patientId"), @Index("startsAt")})
public class Appointment {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long patientId;
    public long startsAt;
    public String type;
    public String notes;
    public boolean done;

    /** Minutos de antecedência do lembrete; negativo significa sem lembrete. */
    public int reminderMinutes = -1;
}
