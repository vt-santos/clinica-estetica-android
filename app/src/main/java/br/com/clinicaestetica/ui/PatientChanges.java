package br.com.clinicaestetica.ui;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import br.com.clinicaestetica.data.FormCatalog;
import br.com.clinicaestetica.data.Json;
import br.com.clinicaestetica.data.Patient;

/** Compara duas versões da ficha e descreve o que mudou, para constar no termo. */
final class PatientChanges {
    private PatientChanges() {
    }

    static List<String> between(Patient before, Patient after) {
        List<String> changes = new ArrayList<>();
        compare(changes, "Nome", before.name, after.name);
        compare(changes, "Data de nascimento", date(before.birthDate), date(after.birthDate));
        compare(changes, "Sexo", before.sex, after.sex);
        compare(changes, "Estado civil", before.maritalStatus, after.maritalStatus);
        compare(changes, "Etnia", before.ethnicity, after.ethnicity);
        compare(changes, "Profissão", before.profession, after.profession);
        compare(changes, "Endereço", before.address, after.address);
        compare(changes, "Bairro", before.neighborhood, after.neighborhood);
        compare(changes, "Cidade", before.city, after.city);
        compare(changes, "CEP", before.cep, after.cep);
        compare(changes, "Celular", before.cellPhone, after.cellPhone);
        compare(changes, "Telefone residencial", before.homePhone, after.homePhone);
        compare(changes, "Telefone comercial", before.workPhone, after.workPhone);
        compare(changes, "E-mail", before.email, after.email);
        compare(changes, "Indicação", before.referral, after.referral);
        compare(changes, "Motivo da visita", before.visitReason, after.visitReason);
        compare(changes, "Contato de emergência", before.emergencyName, after.emergencyName);
        compare(changes, "Telefone de emergência", before.emergencyPhone, after.emergencyPhone);
        compare(changes, "Médico", before.doctorName, after.doctorName);
        compare(changes, "Telefone do médico", before.doctorPhone, after.doctorPhone);
        compare(changes, "Convênio médico", before.healthPlan, after.healthPlan);
        compare(changes, "Carteirinha", before.healthPlanCard, after.healthPlanCard);
        compare(changes, "Hospital", before.hospital, after.hospital);

        JSONObject oldHistory = Json.object(before.historyJson);
        JSONObject newHistory = Json.object(after.historyJson);
        for (FormCatalog.Question question : FormCatalog.HISTORY) {
            compare(changes, question.label, answer(oldHistory, question.key),
                    answer(newHistory, question.key));
        }
        return changes;
    }

    /** Resposta do histórico em texto, por exemplo "Sim · Dipirona". */
    static String answer(JSONObject history, String key) {
        JSONObject saved = history.optJSONObject(key);
        if (saved == null) {
            return "";
        }
        String yesNo = "S".equals(saved.optString("a")) ? "Sim"
                : "N".equals(saved.optString("a")) ? "Não" : "";
        String detail = saved.optString("d");
        return yesNo.isEmpty() || detail.isEmpty() ? yesNo + detail : yesNo + " · " + detail;
    }

    private static String date(long millis) {
        return millis == 0 ? "" : Fmt.date(millis);
    }

    private static void compare(List<String> changes, String label, String before, String after) {
        String oldValue = before == null ? "" : before;
        String newValue = after == null ? "" : after;
        if (!oldValue.equals(newValue)) {
            changes.add(label + ": " + shown(oldValue) + " → " + shown(newValue));
        }
    }

    private static String shown(String value) {
        return value.isEmpty() ? "(vazio)" : value;
    }
}
