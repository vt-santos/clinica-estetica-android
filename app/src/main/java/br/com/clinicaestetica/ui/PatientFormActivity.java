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
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
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
import br.com.clinicaestetica.data.ConsentContent;
import br.com.clinicaestetica.data.Cpf;
import br.com.clinicaestetica.data.FormCatalog;
import br.com.clinicaestetica.data.Json;
import br.com.clinicaestetica.data.Patient;

/**
 * Cadastro com todos os campos da Ficha de Anamnese Facial. A ficha só é gravada junto com
 * um termo assinado para o texto exato que está na tela.
 */
public class PatientFormActivity extends ScreenActivity {
    static final String EXTRA_PATIENT_ID = "patient_id";

    private static final String[] YES_NO = {"Sim", "Não"};
    private static final int PHONE = InputType.TYPE_CLASS_PHONE;
    private static final int EMAIL = InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
    private static final int NAME = InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_WORDS;
    private static final int SIGNATURE_HEIGHT_DP = 240;
    private static final String BLANK = "________";

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

    private TextView ageView;
    private LinearLayout guardianSection;
    private LinearLayout signing;
    private View signAgain;
    private TextView termView;
    private TextView signatureLabel;
    private MaterialCheckBox accepted;
    private TextInputEditText place;
    private SignatureView signature;

    private TextInputEditText name, sex, address, neighborhood, city, cep, cellPhone, homePhone,
            workPhone, email, ethnicity, maritalStatus, profession, referral, visitReason,
            emergencyName, emergencyPhone, doctorName, doctorPhone, healthPlan, healthPlanCard,
            hospital, guardianName, guardianCpf, guardianPhone;
    private MaterialAutoCompleteTextView guardianRelationship;

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
        Ui.dateField(line, "Data de nascimento *", birthDate, this::onBirthDateChanged);
        sex = Ui.field(line, "Sexo", patient.sex);
        maritalStatus = Ui.field(line, "Estado civil", patient.maritalStatus);
        ageView = Ui.muted(content, "");
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

        buildGuardian();

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

        for (TextView field : new TextView[]{name, guardianName, guardianCpf,
                guardianRelationship}) {
            Ui.onChange(field, this::refreshTerm);
        }
        onBirthDateChanged();
    }

    /** Aparece só para menores de idade; quem assina passa a ser o responsável. */
    private void buildGuardian() {
        guardianSection = Ui.column(this);
        content.addView(guardianSection);
        Ui.section(guardianSection, "Responsável legal");
        Ui.text(guardianSection, "A paciente é menor de idade. O termo será assinado pelo "
                + "responsável legal.", 14);
        LinearLayout line = Ui.row(guardianSection);
        guardianName = Ui.field(line, "Nome do responsável *", patient.guardianName, NAME);
        guardianCpf = Ui.field(line, "CPF do responsável *", Cpf.format(patient.guardianCpf),
                InputType.TYPE_CLASS_NUMBER);
        line = Ui.row(guardianSection);
        guardianRelationship = Ui.dropdown(line, "Parentesco *",
                FormCatalog.GUARDIAN_RELATIONSHIPS, patient.guardianRelationship);
        guardianPhone = Ui.field(line, "Telefone do responsável", patient.guardianPhone, PHONE);
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
                refreshTerm();
            });
        }
        content.addView(signing);

        termView = Ui.text(signing, getString(R.string.term_text), 15);
        accepted = new MaterialCheckBox(this);
        accepted.setText("Li e estou de acordo com o termo");
        signing.addView(accepted);
        place = Ui.field(signing, "Local", patient.city);

        signatureLabel = Ui.text(signing, "", 15);
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

    private void onBirthDateChanged() {
        boolean minor = isMinor(birthDate[0]);
        if (birthDate[0] == 0) {
            ageView.setText("Informe a data de nascimento para verificar a idade.");
        } else if (birthDate[0] > System.currentTimeMillis()) {
            ageView.setText("A data de nascimento está no futuro.");
        } else {
            int age = Fmt.age(birthDate[0]);
            ageView.setText(age + (age == 1 ? " ano" : " anos") + (minor ? " · Menor de idade" : ""));
        }
        guardianSection.setVisibility(minor ? View.VISIBLE : View.GONE);
        signatureLabel.setText(minor ? "Assinatura do(a) responsável legal" : "Assinatura da paciente");
        refreshTerm();
    }

    /**
     * Mantém o termo da tela igual ao que vai ser gravado. Se ele mudar depois da assinatura,
     * a assinatura é descartada, porque foi feita para outro texto.
     */
    private void refreshTerm() {
        if (signing == null || signing.getVisibility() != View.VISIBLE) {
            return;
        }
        Patient edited = fromForm();
        String term = termFor(edited, changesIn(edited));
        if (term.equals(termView.getText().toString())) {
            return;
        }
        termView.setText(term);
        if (accepted.isChecked() || !signature.isEmpty()) {
            accepted.setChecked(false);
            signature.clear();
            Ui.toast(this, "O termo mudou. Colha a assinatura novamente.");
        }
    }

    private void save() {
        if (patient == null || name == null) {
            return;
        }
        Patient edited = fromForm();
        String problem = validate(edited);
        if (problem != null) {
            Ui.toast(this, problem);
            return;
        }

        List<String> changes = changesIn(edited);
        boolean signingOpen = signing.getVisibility() == View.VISIBLE;
        if (!signingOpen && changes.isEmpty()) {
            finish();
            return;
        }
        String term = termFor(edited, changes);
        if (!signingOpen || !term.equals(termView.getText().toString())) {
            requestSignature(term);
            return;
        }
        if (!accepted.isChecked()) {
            Ui.toast(this, "Marque a caixa de aceite do termo.");
            return;
        }
        if (signature.isEmpty()) {
            Ui.toast(this, "Colha a assinatura com a caneta.");
            return;
        }
        Consent signed = newConsent(edited, term);
        Async.run(() -> dao.savePatient(edited, signed), this::finish);
    }

    /** Devolve a mensagem do primeiro problema da ficha, ou null se estiver tudo certo. */
    private String validate(Patient edited) {
        if (edited.name.isEmpty()) {
            return "Informe o nome da paciente.";
        }
        if (edited.birthDate == 0) {
            return "Informe a data de nascimento para verificar a idade.";
        }
        if (edited.birthDate > System.currentTimeMillis()) {
            return "A data de nascimento está no futuro.";
        }
        if (!isMinor(edited.birthDate)) {
            return null;
        }
        if (edited.guardianName.isEmpty()) {
            return "Paciente menor de idade: informe o nome do responsável legal.";
        }
        if (!Cpf.isValid(edited.guardianCpf)) {
            return "CPF do responsável inválido.";
        }
        if (edited.guardianRelationship.isEmpty()) {
            return "Informe o parentesco do responsável.";
        }
        return null;
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
        Ui.toast(this, consent == null ? "Confira o termo e colha a assinatura."
                : "A ficha foi alterada. Peça uma nova assinatura.");
        scroll.post(() -> scroll.smoothScrollTo(0, signing.getTop()));
    }

    private List<String> changesIn(Patient edited) {
        return original == null ? Collections.emptyList()
                : PatientChanges.between(original, edited);
    }

    private String termFor(Patient edited, List<String> changes) {
        String term = isMinor(edited.birthDate)
                ? getString(R.string.term_minor_text, orBlank(edited.guardianName),
                orBlank(Cpf.format(edited.guardianCpf)),
                orBlank(edited.guardianRelationship).toLowerCase(), orBlank(edited.name))
                : getString(R.string.term_text);
        if (consent == null || changes.isEmpty()) {
            return term;
        }
        String notice = getString(R.string.term_change_notice, Fmt.dateTime(consent.signedAt),
                "• " + String.join("\n• ", changes));
        return notice + "\n\n" + term;
    }

    private Consent newConsent(Patient edited, String term) {
        ConsentContent signed = new ConsentContent();
        signed.termText = term;
        signed.place = Ui.value(place);
        signed.accepted = accepted.isChecked();
        if (isMinor(edited.birthDate)) {
            signed.signerName = edited.guardianName;
            signed.signerRole = "Responsável legal (" + edited.guardianRelationship + ")";
            signed.signerCpf = edited.guardianCpf;
        } else {
            signed.signerName = edited.name;
            signed.signerRole = "Paciente";
        }
        signed.signaturePng = signature.toPng();

        Consent created = new Consent();
        created.signedAt = System.currentTimeMillis();
        created.content = signed;
        return created;
    }

    /** Monta uma cópia da paciente com o que está na tela, sem alterar a original. */
    private Patient fromForm() {
        Patient edited = new Patient();
        edited.id = patient.id;
        edited.createdAt = patient.createdAt;
        edited.name = Ui.value(name);
        edited.sex = Ui.value(sex);
        edited.birthDate = birthDate[0];
        edited.address = Ui.value(address);
        edited.neighborhood = Ui.value(neighborhood);
        edited.city = Ui.value(city);
        edited.cep = Ui.value(cep);
        edited.cellPhone = Ui.value(cellPhone);
        edited.homePhone = Ui.value(homePhone);
        edited.workPhone = Ui.value(workPhone);
        edited.email = Ui.value(email);
        edited.ethnicity = Ui.value(ethnicity);
        edited.maritalStatus = Ui.value(maritalStatus);
        edited.profession = Ui.value(profession);
        edited.referral = Ui.value(referral);
        edited.visitReason = Ui.value(visitReason);
        edited.emergencyName = Ui.value(emergencyName);
        edited.emergencyPhone = Ui.value(emergencyPhone);
        edited.doctorName = Ui.value(doctorName);
        edited.doctorPhone = Ui.value(doctorPhone);
        edited.healthPlan = Ui.value(healthPlan);
        edited.healthPlanCard = Ui.value(healthPlanCard);
        edited.hospital = Ui.value(hospital);
        edited.historyJson = historyJson();

        boolean minor = isMinor(edited.birthDate);
        edited.guardianName = minor ? Ui.value(guardianName) : "";
        edited.guardianCpf = minor ? Cpf.digits(Ui.value(guardianCpf)) : "";
        edited.guardianRelationship = minor ? Ui.value(guardianRelationship) : "";
        edited.guardianPhone = minor ? Ui.value(guardianPhone) : "";
        return edited;
    }

    private static boolean isMinor(long birthDate) {
        return birthDate != 0 && birthDate <= System.currentTimeMillis()
                && Fmt.age(birthDate) < FormCatalog.ADULT_AGE;
    }

    private static String orBlank(String value) {
        return value == null || value.isEmpty() ? BLANK : value;
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
