package br.com.clinicaestetica.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Ficha de Anamnese Facial: dados pessoais, emergência e histórico. */
@Entity(tableName = "patients", indices = {@Index("name"), @Index("taxId")})
public class Patient {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long createdAt;
    public String name;
    /** CPF ou CNPJ só com números e letras, sem pontuação (ver {@link TaxId}). */
    public String taxId;
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

    // Responsável legal, obrigatório quando a paciente é menor de idade.
    public String guardianName;
    public String guardianCpf;
    public String guardianRelationship;
    public String guardianPhone;

    /** Respostas do histórico: {"chave": {"a": "S" ou "N", "d": "detalhe"}}. */
    public String historyJson;

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
