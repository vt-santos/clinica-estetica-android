package br.com.clinicaestetica.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Avaliação da Pele. */
@Entity(tableName = "skin_assessments",
        foreignKeys = @ForeignKey(entity = Patient.class, parentColumns = "id",
                childColumns = "patientId", onDelete = ForeignKey.CASCADE),
        indices = @Index("patientId"))
public class SkinAssessment {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long patientId;
    public long date;

    /** Itens marcados por grupo: {"Sequelas": ["Cicatriz"]}. */
    public String selectionsJson;
    public String notes;
}
