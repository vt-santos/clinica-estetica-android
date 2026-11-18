package br.com.clinicaestetica.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Sessão de tratamento: protocolo aplicado na pele da paciente. */
@Entity(tableName = "treatment_sessions",
        foreignKeys = @ForeignKey(entity = Patient.class, parentColumns = "id",
                childColumns = "patientId", onDelete = ForeignKey.CASCADE),
        indices = @Index("patientId"))
public class TreatmentSession {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long patientId;
    public long date;

    /** Equipamentos usados: ["Vaporizador", "Ionizador"]. */
    public String equipmentJson;
    public String otherEquipment;
    public String protocol;
    public String supervision;
    public String report;
}
