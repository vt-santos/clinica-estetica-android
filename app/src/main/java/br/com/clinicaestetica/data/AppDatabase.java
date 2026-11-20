package br.com.clinicaestetica.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.security.GeneralSecurityException;

@Database(entities = {Patient.class, SkinAssessment.class, TreatmentSession.class,
        Appointment.class, Payment.class, Consent.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `consents` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`patientId` INTEGER NOT NULL, "
                    + "`signedAt` INTEGER NOT NULL, "
                    + "`place` TEXT, "
                    + "`termText` TEXT, "
                    + "`signaturePng` BLOB, "
                    + "FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_consents_patientId` "
                    + "ON `consents` (`patientId`)");
        }
    };

    /** Recria a tabela com a coluna criptografada e cifra as assinaturas já salvas. */
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE `consents_new` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`patientId` INTEGER NOT NULL, "
                    + "`signedAt` INTEGER NOT NULL, "
                    + "`place` TEXT, "
                    + "`termText` TEXT, "
                    + "`signatureEncrypted` BLOB, "
                    + "FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            try (Cursor row = db.query("SELECT id, patientId, signedAt, place, termText, "
                    + "signaturePng FROM consents")) {
                while (row.moveToNext()) {
                    ContentValues values = new ContentValues();
                    values.put("id", row.getLong(0));
                    values.put("patientId", row.getLong(1));
                    values.put("signedAt", row.getLong(2));
                    values.put("place", row.getString(3));
                    values.put("termText", row.getString(4));
                    values.put("signatureEncrypted",
                            ConsentVault.encrypt(row.getBlob(5), row.getLong(1)));
                    db.insert("consents_new", SQLiteDatabase.CONFLICT_ABORT, values);
                }
            }
            db.execSQL("DROP TABLE `consents`");
            db.execSQL("ALTER TABLE `consents_new` RENAME TO `consents`");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_consents_patientId` "
                    + "ON `consents` (`patientId`)");
        }
    };

    /**
     * Junta texto, local, assinante e imagem de cada termo num único pacote criptografado em
     * Base64, e cria as colunas do responsável legal.
     */
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            for (String column : new String[]{"guardianName", "guardianCpf",
                    "guardianRelationship", "guardianPhone"}) {
                db.execSQL("ALTER TABLE `patients` ADD COLUMN `" + column + "` TEXT");
            }
            db.execSQL("CREATE TABLE `consents_new` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`patientId` INTEGER NOT NULL, "
                    + "`signedAt` INTEGER NOT NULL, "
                    + "`sealedPackage` TEXT, "
                    + "FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            try (Cursor row = db.query("SELECT c.id, c.patientId, c.signedAt, c.place, "
                    + "c.termText, c.signatureEncrypted, p.name FROM consents c "
                    + "JOIN patients p ON p.id = c.patientId")) {
                while (row.moveToNext()) {
                    long patientId = row.getLong(1);
                    ConsentContent content = new ConsentContent();
                    content.place = row.getString(3);
                    content.termText = row.getString(4);
                    content.accepted = true;
                    content.signerName = row.getString(6);
                    content.signerRole = "Paciente";
                    content.signaturePng = previousSignature(row.getBlob(5), patientId);

                    ContentValues values = new ContentValues();
                    values.put("id", row.getLong(0));
                    values.put("patientId", patientId);
                    values.put("signedAt", row.getLong(2));
                    values.put("sealedPackage", ConsentVault.seal(content.toJson(), patientId));
                    db.insert("consents_new", SQLiteDatabase.CONFLICT_ABORT, values);
                }
            }
            db.execSQL("DROP TABLE `consents`");
            db.execSQL("ALTER TABLE `consents_new` RENAME TO `consents`");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_consents_patientId` "
                    + "ON `consents` (`patientId`)");
        }

        /** Sem a chave antiga, o termo continua guardado, só que sem a imagem. */
        private byte[] previousSignature(byte[] encrypted, long patientId) {
            try {
                return ConsentVault.decrypt(encrypted, patientId);
            } catch (GeneralSecurityException e) {
                return null;
            }
        }
    };

    private static volatile AppDatabase instance;

    public abstract AppDao dao();

    public static AppDao dao(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "clinica.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .build();
                }
            }
        }
        return instance.dao();
    }
}
