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

@Database(entities = {Patient.class, SkinAssessment.class, TreatmentSession.class,
        Appointment.class, Payment.class, Consent.class}, version = 3, exportSchema = false)
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
                            SignatureVault.encrypt(row.getBlob(5), row.getLong(1)));
                    db.insert("consents_new", SQLiteDatabase.CONFLICT_ABORT, values);
                }
            }
            db.execSQL("DROP TABLE `consents`");
            db.execSQL("ALTER TABLE `consents_new` RENAME TO `consents`");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_consents_patientId` "
                    + "ON `consents` (`patientId`)");
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
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build();
                }
            }
        }
        return instance.dao();
    }
}
