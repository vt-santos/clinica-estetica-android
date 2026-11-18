package br.com.clinicaestetica.ui;

import android.os.Bundle;
import android.widget.LinearLayout;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.List;

import br.com.clinicaestetica.data.Appointment;
import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.data.FormCatalog;
import br.com.clinicaestetica.data.Patient;
import br.com.clinicaestetica.reminder.ReminderScheduler;

public class AppointmentFormActivity extends ScreenActivity {
    static final String EXTRA_APPOINTMENT_ID = "appointment_id";
    static final String EXTRA_PATIENT_ID = "patient_id";
    static final String EXTRA_DAY = "day";

    private static final int DEFAULT_HOUR = 9;
    private static final int DEFAULT_REMINDER = 2;

    private final long[] startsAt = new long[1];
    private Appointment appointment;
    private MaterialAutoCompleteTextView type, reminder;
    private TextInputEditText notes;

    private static class FormData {
        Appointment appointment;
        Patient patient;
        List<Patient> patients;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long id = getIntent().getLongExtra(EXTRA_APPOINTMENT_ID, 0);
        long patientId = getIntent().getLongExtra(EXTRA_PATIENT_ID, 0);
        toolbar.setTitle(id == 0 ? "Nova consulta" : "Consulta");
        addAction("Salvar", this::save);
        if (id != 0) {
            addAction("Excluir", () -> Ui.confirm(this, "Excluir esta consulta?", () -> {
                ReminderScheduler.cancel(this, id);
                Async.run(() -> dao.delete(appointment), this::finish);
            }));
        }

        Async.load(() -> {
            FormData data = new FormData();
            data.appointment = id == 0 ? newAppointment(patientId) : dao.appointment(id);
            data.patient = dao.patient(data.appointment.patientId);
            data.patients = dao.allPatients();
            return data;
        }, this::buildForm);
    }

    private Appointment newAppointment(long patientId) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getIntent().getLongExtra(EXTRA_DAY, System.currentTimeMillis()));
        calendar.set(Calendar.HOUR_OF_DAY, DEFAULT_HOUR);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Appointment created = new Appointment();
        created.patientId = patientId;
        created.startsAt = calendar.getTimeInMillis();
        created.type = FormCatalog.APPOINTMENT_TYPES[0];
        created.reminderMinutes = FormCatalog.REMINDER_MINUTES[DEFAULT_REMINDER];
        return created;
    }

    private void buildForm(FormData data) {
        appointment = data.appointment;
        startsAt[0] = appointment.startsAt;

        if (data.patient != null) {
            Ui.section(content, data.patient.name);
        } else {
            Ui.picker(content, "Paciente *", data.patients,
                    (Patient patient) -> appointment.patientId = patient.id);
        }
        LinearLayout line = Ui.row(content);
        Ui.dateField(line, "Data", startsAt);
        Ui.timeField(line, "Horário", startsAt);
        line = Ui.row(content);
        type = Ui.dropdown(line, "Tipo", FormCatalog.APPOINTMENT_TYPES, appointment.type);
        reminder = Ui.dropdown(line, "Lembrete", FormCatalog.REMINDER_LABELS,
                reminderLabel(appointment.reminderMinutes));
        notes = Ui.multiline(content, "Observações", appointment.notes);
    }

    private static String reminderLabel(int minutes) {
        for (int i = 0; i < FormCatalog.REMINDER_MINUTES.length; i++) {
            if (FormCatalog.REMINDER_MINUTES[i] == minutes) {
                return FormCatalog.REMINDER_LABELS[i];
            }
        }
        return FormCatalog.REMINDER_LABELS[0];
    }

    private static int reminderMinutes(String label) {
        for (int i = 0; i < FormCatalog.REMINDER_LABELS.length; i++) {
            if (FormCatalog.REMINDER_LABELS[i].equals(label)) {
                return FormCatalog.REMINDER_MINUTES[i];
            }
        }
        return -1;
    }

    private void save() {
        if (appointment == null || notes == null) {
            return;
        }
        if (appointment.patientId == 0) {
            Ui.toast(this, "Escolha a paciente na lista.");
            return;
        }
        appointment.startsAt = startsAt[0];
        appointment.type = type.getText().toString();
        appointment.reminderMinutes = reminderMinutes(reminder.getText().toString());
        appointment.notes = Ui.value(notes);
        Async.run(() -> {
            if (appointment.id == 0) {
                appointment.id = dao.insert(appointment);
            } else {
                dao.update(appointment);
            }
        }, () -> {
            ReminderScheduler.schedule(this, appointment);
            finish();
        });
    }
}
