package br.com.clinicaestetica.ui;

import android.os.Bundle;
import android.text.InputType;
import android.widget.LinearLayout;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.data.FormCatalog;
import br.com.clinicaestetica.data.Patient;
import br.com.clinicaestetica.data.Payment;

public class PaymentFormActivity extends ScreenActivity {
    static final String EXTRA_PAYMENT_ID = "payment_id";
    static final String EXTRA_PATIENT_ID = "patient_id";

    private static final int MONEY = InputType.TYPE_CLASS_NUMBER
            | InputType.TYPE_NUMBER_FLAG_DECIMAL;

    private final long[] date = new long[1];
    private Payment payment;
    private TextInputEditText amount, description;
    private MaterialAutoCompleteTextView method;
    private MaterialSwitch paid;

    private static class FormData {
        Payment payment;
        Patient patient;
        List<Patient> patients;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long id = getIntent().getLongExtra(EXTRA_PAYMENT_ID, 0);
        long patientId = getIntent().getLongExtra(EXTRA_PATIENT_ID, 0);
        toolbar.setTitle(id == 0 ? "Novo pagamento" : "Pagamento");
        addAction("Salvar", this::save);
        if (id != 0) {
            addAction("Excluir", () -> Ui.confirm(this, "Excluir este pagamento?",
                    () -> Async.run(() -> dao.delete(payment), this::finish)));
        }

        Async.load(() -> {
            FormData data = new FormData();
            data.payment = id == 0 ? newPayment(patientId) : dao.payment(id);
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
        date[0] = payment.date;

        if (data.patient != null) {
            Ui.section(content, data.patient.name);
        } else {
            Ui.picker(content, "Paciente *", data.patients,
                    (Patient patient) -> payment.patientId = patient.id);
        }
        LinearLayout line = Ui.row(content);
        amount = Ui.field(line, "Valor (R$) *",
                payment.amountCents == 0 ? "" : Fmt.moneyInput(payment.amountCents), MONEY);
        Ui.dateField(line, "Data", date);
        method = Ui.dropdown(line, "Forma de pagamento", FormCatalog.PAYMENT_METHODS, payment.method);
        description = Ui.field(content, "Descrição", payment.description);
        paid = new MaterialSwitch(this);
        paid.setText("Pago");
        paid.setChecked(payment.paid);
        content.addView(paid);
    }

    private void save() {
        if (payment == null || paid == null) {
            return;
        }
        if (payment.patientId == 0) {
            Ui.toast(this, "Escolha a paciente na lista.");
            return;
        }
        long cents = Fmt.parseCents(Ui.value(amount));
        if (cents <= 0) {
            Ui.toast(this, "Informe um valor válido.");
            return;
        }
        payment.amountCents = cents;
        payment.date = date[0];
        payment.method = method.getText().toString();
        payment.description = Ui.value(description);
        payment.paid = paid.isChecked();
        Async.run(() -> {
            if (payment.id == 0) {
                dao.insert(payment);
            } else {
                dao.update(payment);
            }
        }, this::finish);
    }
}
