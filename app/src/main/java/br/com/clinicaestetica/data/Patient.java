package br.com.clinicaestetica.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Ficha de Anamnese Facial: dados pessoais, emergência e histórico. */
@Entity(tableName = "patients", indices = @Index("name"))
public class Patient {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long createdAt;
    public String name;
    public String sex;
    public long birthDate;
    public String address;
    public String neighborhood;
    public String city;
    public String cep;
    public String cellPhone;
    public String homePhone;
    public String workPhone;
    public String email;
    public String ethnicity;
    public String maritalStatus;
    public String profession;
    public String referral;
    public String visitReason;

    public String emergencyName;
    public String emergencyPhone;
    public String doctorName;
    public String doctorPhone;
    public String healthPlan;
    public String healthPlanCard;
    public String hospital;

    /** Respostas do histórico: {"chave": {"a": "S" ou "N", "d": "detalhe"}}. */
    public String historyJson;

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
