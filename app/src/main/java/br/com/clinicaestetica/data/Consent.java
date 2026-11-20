package br.com.clinicaestetica.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Termo assinado. Fora a data, que serve para ordenar o histórico, tudo fica dentro do
 * pacote criptografado: texto aceito, quem assinou e a imagem da assinatura.
 */
@Entity(tableName = "consents",
        foreignKeys = @ForeignKey(entity = Patient.class, parentColumns = "id",
                childColumns = "patientId", onDelete = ForeignKey.CASCADE),
        indices = @Index("patientId"))
public class Consent {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long patientId;
    public long signedAt;

    /** {@link ConsentContent} criptografado pelo {@link ConsentVault}, em Base64. */
    public String sealedPackage;

    /** Conteúdo aberto; só existe em memória até a ficha ser salva. */
    @Ignore
    public ConsentContent content;
}
