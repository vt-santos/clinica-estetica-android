package br.com.clinicaestetica.ui;

import android.os.Bundle;
import android.text.InputType;
import android.widget.LinearLayout;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import br.com.clinicaestetica.data.Appointment;
import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.data.FormCatalog;
import br.com.clinicaestetica.data.Patient;
import br.com.clinicaestetica.data.Payment;
import br.com.clinicaestetica.reminder.ReminderScheduler;

/**
 * Registro de pagamento. Todo pagamento já entra como pago: o app não trabalha com fiado.
 * Aberta pelo botão Concluir de uma consulta, a consulta só fica concluída quando o
 * pagamento é salvo; voltar sem salvar deixa a consulta em aberto.
 */
public class PaymentFormActivity extends ScreenActivity {
    static final String EXTRA_PAYMENT_ID = "payment_id";
    static final String EXTRA_PATIENT_ID = "patient_id";
    static final String EXTRA_APPOINTMENT_ID = "appointment_id";

    private static final int MONEY = InputType.TYPE_CLASS_NUMBER
            | InputType.TYPE_NUMBER_FLAG_DECIMAL;

    private final long[] date = new long[1];
    private Payment payment;
    private Appointment appointment;
    private TextInputEditText amount, description;
    private MaterialAutoCompleteTextView method;

    private static class FormData {
        Payment payment;
        Appointment appointment;
        Patient patient;
        List<Patient> patients;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long id = getIntent().getLongExtra(EXTRA_PAYMENT_ID, 0);
        long patientId = getIntent().getLongExtra(EXTRA_PATIENT_ID, 0);
        long appointmentId = getIntent().getLongExtra(EXTRA_APPOINTMENT_ID, 0);
        toolbar.setTitle(appointmentId != 0 ? "Concluir consulta"
                : id == 0 ? "Novo pagamento" : "Pagamento");
        addAction("Salvar", this::save);
        if (id != 0) {
            addAction("Excluir", () -> Ui.confirm(this, "Excluir este pagamento?",
                    () -> Async.run(() -> dao.delete(payment), this::finish)));
        }

        Async.load(() -> {
            FormData data = new FormData();
            if (appointmentId != 0) {
                data.appointment = dao.appointment(appointmentId);
                data.payment = newPayment(data.appointment.patientId);
                data.payment.description = data.appointment.type;
            } else {
                data.payment = id == 0 ? newPayment(patientId) : dao.payment(id);
            }
            data.patient = dao.patient(data.payment.patientId);
            data.patients = dao.allPatients();
            return data;
        }, this::buildForm);
    }

    private static Payment newPayment(long patientId) {
        Payment created = new Payment();
        created.patientId = patientId;
        created.date = System.currentTimeMillis();
        created.method = FormCatalog.PAYMENT_METHODS[0];
        created.paid = true;
        return created;
    }

    private void buildForm(FormData data) {
        payment = data.payment;
        appointment = data.appointment;
        date[0] = payment.date;

        if (data.patient != null) {
            Ui.section(content, data.patient.name);
        } else {
            Ui.picker(content, "Paciente *", data.patients,
                    (Patient patient) -> payment.patientId = patient.id);
        }
        if (appointment != null) {
            Ui.muted(content, appointment.type + " de " + Fmt.dateTime(appointment.startsAt)
                    + ". Registre o pagamento para concluir a consulta.");
        }
        LinearLayout line = Ui.row(content);
        amount = Ui.field(line, "Valor (R$) *",
                payment.amountCents == 0 ? "" : Fmt.moneyInput(payment.amountCents), MONEY);
        Ui.dateField(line, "Data", date);
        method = Ui.dropdown(line, "Forma de pagamento", FormCatalog.PAYMENT_METHODS, payment.method);
        description = Ui.field(content, "Descrição", payment.description);
    }

    private void save() {
        if (payment == null || amount == null) {
            return;
        }
        if (payment.patientId == 0) {
            Ui.toast(this, "Escolha a paciente na lista.");
            return;
        }
        long cents = Fmt.parseCents(Ui.value(amount));
        if (cents <= 0) {
            Ui.toast(this, "Informe o valor pago.");
            return;
        }
        payment.amountCents = cents;
        payment.date = date[0];
        payment.method = method.getText().toString();
        payment.description = Ui.value(description);
        payment.paid = true;
        Async.run(() -> {
            if (appointment != null) {
                dao.completeAppointment(appointment, payment);
            } else if (payment.id == 0) {
                dao.insert(payment);
            } else {
                dao.update(payment);
            }
        }, () -> {
            if (appointment != null) {
                ReminderScheduler.schedule(this, appointment);
                ReminderScheduler.scheduleFollowUp(this, appointment);
                Ui.toast(this, "Consulta concluída e pagamento registrado.");
            }
            finish();
        });
    }
}
