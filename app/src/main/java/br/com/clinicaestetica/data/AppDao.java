package br.com.clinicaestetica.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AppDao {
    String APPOINTMENT_ROW = "SELECT a.*, p.name AS patientName, p.cellPhone AS cellPhone "
            + "FROM appointments a JOIN patients p ON p.id = a.patientId ";
    String PAYMENT_ROW = "SELECT pay.*, p.name AS patientName "
            + "FROM payments pay JOIN patients p ON p.id = pay.patientId ";

    // Pacientes

    @Insert
    long insert(Patient patient);

    @Update
    void update(Patient patient);

    @Delete
    void delete(Patient patient);

    @Query("SELECT * FROM patients WHERE id = :id")
    Patient patient(long id);

    @Query("SELECT * FROM patients ORDER BY name COLLATE NOCASE")
    List<Patient> allPatients();

    @Query("SELECT * FROM patients WHERE name LIKE '%' || :term || '%' "
            + "OR cellPhone LIKE '%' || :term || '%' ORDER BY name COLLATE NOCASE")
    List<Patient> searchPatients(String term);

    @Query("SELECT COUNT(*) FROM patients")
    int patientCount();

    // Termos assinados

    @Insert
    long insert(Consent consent);

    @Query("SELECT * FROM consents WHERE patientId = :patientId ORDER BY signedAt DESC LIMIT 1")
    Consent latestConsent(long patientId);

    @Query("SELECT * FROM consents WHERE patientId = :patientId ORDER BY signedAt DESC")
    List<Consent> consents(long patientId);

    /** Salva a ficha e o termo juntos, para nunca existir um sem o outro. */
    @Transaction
    default void savePatient(Patient patient, Consent consent) {
        if (patient.id == 0) {
            patient.id = insert(patient);
        } else {
            update(patient);
        }
        consent.patientId = patient.id;
        consent.sealedPackage = ConsentVault.seal(consent.content.toJson(), patient.id);
        insert(consent);
    }

    // Avaliações

    @Insert
    long insert(SkinAssessment assessment);

    @Update
    void update(SkinAssessment assessment);

    @Delete
    void delete(SkinAssessment assessment);

    @Query("SELECT * FROM skin_assessments WHERE id = :id")
    SkinAssessment assessment(long id);

    @Query("SELECT * FROM skin_assessments WHERE patientId = :patientId ORDER BY date DESC")
    List<SkinAssessment> assessments(long patientId);

    // Sessões

    @Insert
    long insert(TreatmentSession session);

    @Update
    void update(TreatmentSession session);

    @Delete
    void delete(TreatmentSession session);

    @Query("SELECT * FROM treatment_sessions WHERE id = :id")
    TreatmentSession session(long id);

    @Query("SELECT * FROM treatment_sessions WHERE patientId = :patientId ORDER BY date DESC")
    List<TreatmentSession> sessions(long patientId);

    // Agenda

    @Insert
    long insert(Appointment appointment);

    @Update
    void update(Appointment appointment);

    @Delete
    void delete(Appointment appointment);

    @Query("SELECT * FROM appointments WHERE id = :id")
    Appointment appointment(long id);

    @Query("SELECT * FROM appointments WHERE startsAt >= :from AND done = 0")
    List<Appointment> pendingAppointments(long from);

    /** Consultas concluídas a partir de uma data; usadas para recriar os avisos de retorno. */
    @Query("SELECT * FROM appointments WHERE done = 1 AND startsAt >= :from")
    List<Appointment> doneAppointmentsSince(long from);

    /** Concluir a consulta e registrar o pagamento acontecem juntos, ou nenhum dos dois. */
    @Transaction
    default void completeAppointment(Appointment appointment, Payment payment) {
        payment.id = insert(payment);
        appointment.done = true;
        update(appointment);
    }

    @Query(APPOINTMENT_ROW + "WHERE a.id = :id")
    AppointmentRow appointmentRow(long id);

    @Query(APPOINTMENT_ROW + "WHERE a.startsAt >= :from AND a.startsAt < :to ORDER BY a.startsAt")
    List<AppointmentRow> appointmentsBetween(long from, long to);

    @Query(APPOINTMENT_ROW + "WHERE a.patientId = :patientId ORDER BY a.startsAt DESC")
    List<AppointmentRow> appointmentsOf(long patientId);

    @Query("SELECT COUNT(*) FROM appointments WHERE type = 'Retorno' AND done = 0 "
            + "AND startsAt >= :from AND startsAt < :to")
    int returnCount(long from, long to);

    // Pagamentos

    @Insert
    long insert(Payment payment);

    @Update
    void update(Payment payment);

    @Delete
    void delete(Payment payment);

    @Query("SELECT * FROM payments WHERE id = :id")
    Payment payment(long id);

    @Query(PAYMENT_ROW + "ORDER BY pay.date DESC")
    List<PaymentRow> allPayments();

    @Query(PAYMENT_ROW + "WHERE pay.patientId = :patientId ORDER BY pay.date DESC")
    List<PaymentRow> paymentsOf(long patientId);

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM payments "
            + "WHERE paid = 1 AND date >= :from AND date < :to")
    long receivedCents(long from, long to);
}
