package br.com.clinicaestetica.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Registro de pagamento; o app não processa cobrança. */
@Entity(tableName = "payments",
        foreignKeys = @ForeignKey(entity = Patient.class, parentColumns = "id",
                childColumns = "patientId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("patientId"), @Index("date")})
public class Payment {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long patientId;
    public long date;
    public long amountCents;
    public String method;
    public String description;
    public boolean paid;
}
