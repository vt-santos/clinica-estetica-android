package br.com.clinicaestetica.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.clinicaestetica.R;
import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.data.Consent;
import br.com.clinicaestetica.data.FormCatalog;
import br.com.clinicaestetica.data.Json;
import br.com.clinicaestetica.data.Patient;

/** Cadastro com todos os campos da Ficha de Anamnese Facial. */
public class PatientFormActivity extends ScreenActivity {
    static final String EXTRA_PATIENT_ID = "patient_id";

    private static final String[] YES_NO = {"Sim", "Não"};
    private static final int PHONE = InputType.TYPE_CLASS_PHONE;
    private static final int EMAIL = InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
    private static final int NAME = InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_WORDS;
    private static final int SIGNATURE_HEIGHT_DP = 240;

    private static final class Answer {
        final ChipGroup yesNo;
        final TextInputEditText detail;

        Answer(ChipGroup yesNo, TextInputEditText detail) {
            this.yesNo = yesNo;
            this.detail = detail;
        }
    }

    private final Map<String, Answer> answers = new LinkedHashMap<>();
    private final long[] birthDate = new long[1];
    private Patient patient;
    private Patient original;
    private Consent consent;
    private LinearLayout signing;
    private View signAgain;
    private TextView termView;
    private MaterialCheckBox accepted;
    private TextInputEditText place;
    private SignatureView signature;

    private TextInputEditText name, sex, address, neighborhood, city, cep, cellPhone, homePhone,
            workPhone, email, ethnicity, maritalStatus, profession, referral, visitReason,
            emergencyName, emergencyPhone, doctorName, doctorPhone, healthPlan, healthPlanCard,
            hospital;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long id = getIntent().getLongExtra(EXTRA_PATIENT_ID, 0);
        toolbar.setTitle(id == 0 ? "Nova paciente" : "Editar ficha");
        addAction("Salvar", this::save);

        if (id == 0) {
            patient = new Patient();
            patient.createdAt = System.currentTimeMillis();
            buildForm();
        } else {
            Async.load(() -> {
                consent = dao.latestConsent(id);
                original = dao.patient(id);
                return dao.patient(id);
            }, loaded -> {
                patient = loaded;
                buildForm();
            });
        }
    }

    private void buildForm() {
        birthDate[0] = patient.birthDate;

        Ui.section(content, "Dados pessoais");
        name = Ui.field(content, "Nome *", patient.name, NAME);
        LinearLayout line = Ui.row(content);
        Ui.dateField(line, "Data de nascimento", birthDate);
        sex = Ui.field(line, "Sexo", patient.sex);
        maritalStatus = Ui.field(line, "Estado civil", patient.maritalStatus);
        line = Ui.row(content);
        ethnicity = Ui.field(line, "Etnia", patient.ethnicity);
        profession = Ui.field(line, "Profissão", patient.profession);
        address = Ui.field(content, "Endereço", patient.address);
        line = Ui.row(content);
        neighborhood = Ui.field(line, "Bairro", patient.neighborhood);
        city = Ui.field(line, "Cidade", patient.city);
        cep = Ui.field(line, "CEP", patient.cep, InputType.TYPE_CLASS_NUMBER);
        line = Ui.row(content);
        cellPhone = Ui.field(line, "Celular / WhatsApp", patient.cellPhone, PHONE);
        homePhone = Ui.field(line, "Telefone residencial", patient.homePhone, PHONE);
        workPhone = Ui.field(line, "Telefone comercial", patient.workPhone, PHONE);
        email = Ui.field(content, "E-mail", patient.email, EMAIL);
        referral = Ui.field(content, "Indicação", patient.referral);
        visitReason = Ui.field(content, "Motivo da visita", patient.visitReason);

        Ui.section(content, "Em caso de emergência");
        line = Ui.row(content);
        emergencyName = Ui.field(line, "Avisar (nome)", patient.emergencyName, NAME);
        emergencyPhone = Ui.field(line, "Telefone", patient.emergencyPhone, PHONE);
        line = Ui.row(content);
        doctorName = Ui.field(line, "Médico", patient.doctorName, NAME);
        doctorPhone = Ui.field(line, "Telefone do médico", patient.doctorPhone, PHONE);
        line = Ui.row(content);
        healthPlan = Ui.field(line, "Convênio médico", patient.healthPlan);
        healthPlanCard = Ui.field(line, "Carteirinha", patient.healthPlanCard);
        hospital = Ui.field(line, "Hospital", patient.hospital);

        Ui.section(content, "Histórico");
        JSONObject saved = Json.object(patient.historyJson);
        for (FormCatalog.Question question : FormCatalog.HISTORY) {
            addQuestion(question, saved.optJSONObject(question.key));
        }
        buildConsent();
    }

    private void buildConsent() {
        Ui.section(content, "Termo de responsabilidade e ciência do procedimento");
        signing = Ui.column(this);
        if (consent != null) {
            Cards.consent(content, consent);
            signing.setVisibility(View.GONE);
            signAgain = Ui.action(content, "Assinar novamente", view -> {
                view.setVisibility(View.GONE);
                signing.setVisibility(View.VISIBLE);
            });
        }
        content.addView(signing);

        termView = Ui.text(signing, getString(R.string.term_text), 15);
        accepted = new MaterialCheckBox(this);
        accepted.setText("Li e estou de acordo com o termo");
        signing.addView(accepted);
        place = Ui.field(signing, "Local", patient.city);

        Ui.text(signing, "Assinatura da paciente", 15);
        MaterialCardView pad = new MaterialCardView(this, null,
                com.google.android.material.R.attr.materialCardViewOutlinedStyle);
        signature = new SignatureView(this);
        pad.addView(signature, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Ui.dp(this, SIGNATURE_HEIGHT_DP)));
        Ui.addWeighted(signing, pad);
        Ui.action(signing, "Limpar assinatura", view -> signature.clear());
    }

    private void addQuestion(FormCatalog.Question question, JSONObject saved) {
        Ui.text(content, question.label, 15);
        String answer = saved == null ? "" : saved.optString("a");
        String selected = "S".equals(answer) ? YES_NO[0] : "N".equals(answer) ? YES_NO[1] : "";
        LinearLayout line = Ui.row(content);
        ChipGroup yesNo = Ui.chips(line, YES_NO, Collections.singleton(selected), true);
        TextInputEditText detail = question.detailLabel == null ? null
                : Ui.field(line, question.detailLabel, saved == null ? "" : saved.optString("d"));
        answers.put(question.key, new Answer(yesNo, detail));
    }

    private void save() {
        if (patient == null || name == null) {
            return;
        }
        String patientName = Ui.value(name);
        if (patientName.isEmpty()) {
            Ui.toast(this, "Informe o nome da paciente.");
            return;
        }
        patient.name = patientName;
        patient.sex = Ui.value(sex);
        patient.birthDate = birthDate[0];
        patient.address = Ui.value(address);
        patient.neighborhood = Ui.value(neighborhood);
        patient.city = Ui.value(city);
        patient.cep = Ui.value(cep);
        patient.cellPhone = Ui.value(cellPhone);
        patient.homePhone = Ui.value(homePhone);
        patient.workPhone = Ui.value(workPhone);
        patient.email = Ui.value(email);
        patient.ethnicity = Ui.value(ethnicity);
        patient.maritalStatus = Ui.value(maritalStatus);
        patient.profession = Ui.value(profession);
        patient.referral = Ui.value(referral);
        patient.visitReason = Ui.value(visitReason);
        patient.emergencyName = Ui.value(emergencyName);
        patient.emergencyPhone = Ui.value(emergencyPhone);
        patient.doctorName = Ui.value(doctorName);
        patient.doctorPhone = Ui.value(doctorPhone);
        patient.healthPlan = Ui.value(healthPlan);
        patient.healthPlanCard = Ui.value(healthPlanCard);
        patient.hospital = Ui.value(hospital);
        patient.historyJson = historyJson();

        List<String> changes = original == null ? Collections.emptyList()
                : PatientChanges.between(original, patient);
        boolean signingOpen = signing.getVisibility() == View.VISIBLE;
        if (!signingOpen && changes.isEmpty()) {
            finish();
            return;
        }
        String term = termFor(changes);
        if (!signingOpen || !term.equals(termView.getText().toString())) {
            requestSignature(term);
            return;
        }
        if (!accepted.isChecked()) {
            Ui.toast(this, "Marque que a paciente leu e concorda com o termo.");
            return;
        }
        if (signature.isEmpty()) {
            Ui.toast(this, "Colha a assinatura da paciente.");
            return;
        }
        Consent signed = newConsent(term);
        Async.run(() -> dao.savePatient(patient, signed), this::finish);
    }

    /** Sem assinatura válida para o texto atual, a ficha alterada não é salva. */
    private void requestSignature(String term) {
        termView.setText(term);
        accepted.setChecked(false);
        signature.clear();
        signing.setVisibility(View.VISIBLE);
        if (signAgain != null) {
            signAgain.setVisibility(View.GONE);
        }
        Ui.toast(this, "A ficha foi alterada. Peça uma nova assinatura da paciente.");
        scroll.post(() -> scroll.smoothScrollTo(0, signing.getTop()));
    }

    private String termFor(List<String> changes) {
        String term = getString(R.string.term_text);
        if (consent == null || changes.isEmpty()) {
            return term;
        }
        String notice = getString(R.string.term_change_notice, Fmt.dateTime(consent.signedAt),
                "• " + String.join("\n• ", changes));
        return notice + "\n\n" + term;
    }

    private Consent newConsent(String term) {
        Consent created = new Consent();
        created.signedAt = System.currentTimeMillis();
        created.place = Ui.value(place);
        created.termText = term;
        created.signaturePng = signature.toPng();
        return created;
    }

    private String historyJson() {
        JSONObject history = new JSONObject();
        try {
            for (Map.Entry<String, Answer> entry : answers.entrySet()) {
                List<String> checked = Ui.checked(entry.getValue().yesNo);
                String detail = entry.getValue().detail == null ? ""
                        : Ui.value(entry.getValue().detail);
                if (checked.isEmpty() && detail.isEmpty()) {
                    continue;
                }
                JSONObject answer = new JSONObject();
                answer.put("a", checked.isEmpty() ? "" : YES_NO[0].equals(checked.get(0)) ? "S" : "N");
                answer.put("d", detail);
                history.put(entry.getKey(), answer);
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return history.toString();
    }
}
