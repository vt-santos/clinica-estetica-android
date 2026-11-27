package br.com.clinicaestetica.ui;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import br.com.clinicaestetica.data.AppointmentRow;
import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.data.Consent;
import br.com.clinicaestetica.data.Cpf;
import br.com.clinicaestetica.data.FormCatalog;
import br.com.clinicaestetica.data.Json;
import br.com.clinicaestetica.data.Patient;
import br.com.clinicaestetica.data.PaymentRow;
import br.com.clinicaestetica.data.SkinAssessment;
import br.com.clinicaestetica.data.TreatmentSession;

/** Prontuário da paciente: ficha, avaliações, sessões, consultas e pagamentos. */
public class PatientDetailActivity extends ScreenActivity {
    public static final String EXTRA_PATIENT_ID = "patient_id";

    private static final int TAB_RECORD = 0;
    private static final int TAB_ASSESSMENTS = 1;
    private static final int TAB_SESSIONS = 2;
    private static final int TAB_APPOINTMENTS = 3;
    private static final int TAB_PAYMENTS = 4;

    private long patientId;
    private Patient patient;
    private TabLayout tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        patientId = getIntent().getLongExtra(EXTRA_PATIENT_ID, 0);

        addAction("Editar", () -> {
            Intent intent = new Intent(this, PatientFormActivity.class);
            intent.putExtra(PatientFormActivity.EXTRA_PATIENT_ID, patientId);
            startActivity(intent);
        });
        addAction("Excluir", () -> Ui.confirm(this,
                "Excluir esta paciente e todo o histórico dela?",
                () -> Async.run(() -> dao.delete(patient), this::finish)));

        tabs = new TabLayout(this);
        tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
        for (String label : new String[]{"Ficha", "Avaliações", "Sessões", "Consultas", "Pagamentos"}) {
            tabs.addTab(tabs.newTab().setText(label));
        }
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                refresh();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        root.addView(tabs, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        Async.load(() -> dao.patient(patientId), loaded -> {
            if (loaded == null) {
                finish();
                return;
            }
            patient = loaded;
            toolbar.setTitle(patient.name);
            switch (tabs.getSelectedTabPosition()) {
                case TAB_ASSESSMENTS:
                    Async.load(() -> dao.assessments(patientId), this::renderAssessments);
                    break;
                case TAB_SESSIONS:
                    Async.load(() -> dao.sessions(patientId), this::renderSessions);
                    break;
                case TAB_APPOINTMENTS:
                    Async.load(() -> dao.appointmentsOf(patientId), this::renderAppointments);
                    break;
                case TAB_PAYMENTS:
                    Async.load(() -> dao.paymentsOf(patientId), this::renderPayments);
                    break;
                case TAB_RECORD:
                default:
                    Async.load(() -> dao.consents(patientId), this::renderRecord);
            }
        });
    }

    private void renderRecord(List<Consent> consents) {
        content.removeAllViews();
        Ui.section(content, "Dados pessoais");
        info("Nascimento", patient.birthDate == 0 ? null
                : Fmt.date(patient.birthDate) + " (" + Fmt.age(patient.birthDate) + " anos)");
        info("Sexo", patient.sex);
        info("Estado civil", patient.maritalStatus);
        info("Etnia", patient.ethnicity);
        info("Profissão", patient.profession);
        info("Endereço", join(patient.address, patient.neighborhood, patient.city, patient.cep));
        info("Celular", patient.cellPhone);
        info("Residencial", patient.homePhone);
        info("Comercial", patient.workPhone);
        info("E-mail", patient.email);
        info("Indicação", patient.referral);
        info("Motivo da visita", patient.visitReason);

        if (patient.guardianName != null && !patient.guardianName.isEmpty()) {
            Ui.section(content, "Responsável legal");
            info("Nome", patient.guardianName);
            info("CPF", Cpf.format(patient.guardianCpf));
            info("Parentesco", patient.guardianRelationship);
            info("Telefone", patient.guardianPhone);
        }

        Ui.section(content, "Em caso de emergência");
        info("Avisar", join(patient.emergencyName, patient.emergencyPhone));
        info("Médico", join(patient.doctorName, patient.doctorPhone));
        info("Convênio", join(patient.healthPlan, patient.healthPlanCard));
        info("Hospital", patient.hospital);

        Ui.section(content, "Histórico");
        JSONObject history = Json.object(patient.historyJson);
        for (FormCatalog.QuestionGroup group : FormCatalog.HISTORY_GROUPS) {
            boolean titled = false;
            for (FormCatalog.Question question : group.questions) {
                String answer = PatientChanges.answer(history, question.key);
                if (answer.isEmpty()) {
                    continue;
                }
                if (!titled) {
                    Ui.subsection(content, group.title);
                    titled = true;
                }
                info(question.label, answer);
            }
        }

        Ui.section(content, "Termo de responsabilidade e ciência do procedimento");
        if (consents.isEmpty()) {
            Ui.muted(content, "Termo ainda não assinado. Toque em Editar para colher a assinatura.");
            return;
        }
        Cards.consent(content, consents.get(0));
        if (consents.size() > 1) {
            Ui.section(content, "Assinaturas anteriores");
            for (Consent previous : consents.subList(1, consents.size())) {
                Cards.consent(content, previous);
            }
        }
    }

    private void info(String label, String value) {
        if (value != null && !value.isEmpty()) {
            Ui.info(content, label, value);
        }
    }

    private static String join(String... parts) {
        List<String> filled = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                filled.add(part);
            }
        }
        return String.join(" · ", filled);
    }

    private void renderAssessments(List<SkinAssessment> assessments) {
        content.removeAllViews();
        Ui.button(content, "Nova avaliação", view -> {
            Intent intent = new Intent(this, SkinAssessmentActivity.class);
            intent.putExtra(SkinAssessmentActivity.EXTRA_PATIENT_ID, patientId);
            startActivity(intent);
        });
        for (SkinAssessment assessment : assessments) {
            Ui.card(content, Fmt.date(assessment.date), summary(assessment), view -> {
                Intent intent = new Intent(this, SkinAssessmentActivity.class);
                intent.putExtra(SkinAssessmentActivity.EXTRA_PATIENT_ID, patientId);
                intent.putExtra(SkinAssessmentActivity.EXTRA_ASSESSMENT_ID, assessment.id);
                startActivity(intent);
            });
        }
    }

    private static String summary(SkinAssessment assessment) {
        JSONObject selections = Json.object(assessment.selectionsJson);
        List<String> items = new ArrayList<>();
        for (FormCatalog.SkinGroup group : FormCatalog.SKIN_GROUPS) {
            JSONArray selected = selections.optJSONArray(group.title);
            items.addAll(Json.stringSet(selected));
        }
        return String.join(", ", items);
    }

    private void renderSessions(List<TreatmentSession> sessions) {
        content.removeAllViews();
        Ui.button(content, "Nova sessão", view -> {
            Intent intent = new Intent(this, SessionFormActivity.class);
            intent.putExtra(SessionFormActivity.EXTRA_PATIENT_ID, patientId);
            startActivity(intent);
        });
        int number = sessions.size();
        for (TreatmentSession session : sessions) {
            List<String> equipment = new ArrayList<>(Json.stringSet(session.equipmentJson));
            if (session.otherEquipment != null && !session.otherEquipment.isEmpty()) {
                equipment.add(session.otherEquipment);
            }
            String subtitle = join(session.protocol, String.join(", ", equipment), session.report);
            Ui.card(content, number + "ª sessão · " + Fmt.date(session.date), subtitle, view -> {
                Intent intent = new Intent(this, SessionFormActivity.class);
                intent.putExtra(SessionFormActivity.EXTRA_PATIENT_ID, patientId);
                intent.putExtra(SessionFormActivity.EXTRA_SESSION_ID, session.id);
                startActivity(intent);
            });
            number--;
        }
    }

    private void renderAppointments(List<AppointmentRow> rows) {
        content.removeAllViews();
        Ui.button(content, "Nova consulta", view -> {
            Intent intent = new Intent(this, AppointmentFormActivity.class);
            intent.putExtra(AppointmentFormActivity.EXTRA_PATIENT_ID, patientId);
            startActivity(intent);
        });
        for (AppointmentRow row : rows) {
            Cards.appointment(content, row);
        }
    }

    private void renderPayments(List<PaymentRow> rows) {
        content.removeAllViews();
        Ui.button(content, "Novo pagamento", view -> {
            Intent intent = new Intent(this, PaymentFormActivity.class);
            intent.putExtra(PaymentFormActivity.EXTRA_PATIENT_ID, patientId);
            startActivity(intent);
        });
        for (PaymentRow row : rows) {
            Cards.payment(content, row);
        }
    }
}
