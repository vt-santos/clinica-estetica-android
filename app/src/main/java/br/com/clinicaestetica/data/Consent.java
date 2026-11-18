package br.com.clinicaestetica.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Termo assinado; guarda o texto exato que a paciente aceitou. */
@Entity(tableName = "consents",
        foreignKeys = @ForeignKey(entity = Patient.class, parentColumns = "id",
                childColumns = "patientId", onDelete = ForeignKey.CASCADE),
        indices = @Index("patientId"))
public class Consent {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long patientId;
    public long signedAt;
    public String place;
    public String termText;

    /** Imagem criptografada pelo {@link SignatureVault}. */
    public byte[] signatureEncrypted;

    /** Imagem original; só existe em memória até a ficha ser salva. */
    @Ignore
    public byte[] signaturePng;
}
