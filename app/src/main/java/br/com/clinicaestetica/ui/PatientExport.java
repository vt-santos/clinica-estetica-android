package br.com.clinicaestetica.ui;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import br.com.clinicaestetica.data.AppDao;
import br.com.clinicaestetica.data.AppointmentRow;
import br.com.clinicaestetica.data.Consent;
import br.com.clinicaestetica.data.ConsentContent;
import br.com.clinicaestetica.data.ConsentVault;
import br.com.clinicaestetica.data.Cpf;
import br.com.clinicaestetica.data.FormCatalog;
import br.com.clinicaestetica.data.Json;
import br.com.clinicaestetica.data.Patient;
import br.com.clinicaestetica.data.TaxId;
import br.com.clinicaestetica.data.PaymentRow;
import br.com.clinicaestetica.data.SkinAssessment;
import br.com.clinicaestetica.data.TreatmentSession;

/**
 * Cópia legível de todos os dados de uma paciente, para atender aos pedidos de acesso e
 * portabilidade (LGPD art. 18, II e V, e art. 19). Roda fora da thread principal.
 */
final class PatientExport {
    private PatientExport() {
    }

    static String text(AppDao dao, long patientId, String clinic) {
        Patient patient = dao.patient(patientId);
        if (patient == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        out.append("DADOS PESSOAIS DE ").append(patient.name.toUpperCase()).append('\n')
                .append("Emitido por ").append(clinic).append(" em ")
                .append(Fmt.dateTime(System.currentTimeMillis())).append('\n')
                .append("Documento com dados de saúde: entregue somente à titular ou ao "
                        + "responsável legal.\n");

        section(out, "Dados pessoais");
        line(out, "Nome", patient.name);
        line(out, "Nascimento", patient.birthDate == 0 ? null : Fmt.date(patient.birthDate));
        line(out, "CPF/CNPJ", TaxId.format(patient.taxId));
        line(out, "Sexo", patient.sex);
        line(out, "Estado civil", patient.maritalStatus);
        line(out, "Etnia", patient.ethnicity);
        line(out, "Profissão", patient.profession);
        line(out, "Endereço", join(patient.address, patient.neighborhood, patient.city,
                patient.cep));
        line(out, "Celular", patient.cellPhone);
        line(out, "Telefone residencial", patient.homePhone);
        line(out, "Telefone comercial", patient.workPhone);
        line(out, "E-mail", patient.email);
        line(out, "Indicação", patient.referral);
        line(out, "Motivo da visita", patient.visitReason);
        line(out, "Cadastro em", Fmt.date(patient.createdAt));

        if (patient.guardianName != null && !patient.guardianName.isEmpty()) {
            section(out, "Responsável legal");
            line(out, "Nome", patient.guardianName);
            line(out, "CPF", Cpf.format(patient.guardianCpf));
            line(out, "Parentesco", patient.guardianRelationship);
            line(out, "Telefone", patient.guardianPhone);
        }

        section(out, "Em caso de emergência");
        line(out, "Avisar", join(patient.emergencyName, patient.emergencyPhone));
        line(out, "Médico", join(patient.doctorName, patient.doctorPhone));
        line(out, "Convênio", join(patient.healthPlan, patient.healthPlanCard));
        line(out, "Hospital", patient.hospital);

        section(out, "Histórico de saúde");
        JSONObject history = Json.object(patient.historyJson);
        for (FormCatalog.Question question : FormCatalog.HISTORY) {
            line(out, question.label, PatientChanges.answer(history, question.key));
        }

        section(out, "Avaliações da pele");
        for (SkinAssessment assessment : dao.assessments(patientId)) {
            JSONObject selections = Json.object(assessment.selectionsJson);
            List<String> items = new ArrayList<>();
            for (FormCatalog.SkinGroup group : FormCatalog.SKIN_GROUPS) {
                JSONArray selected = selections.optJSONArray(group.title);
                items.addAll(Json.stringSet(selected));
            }
            line(out, Fmt.date(assessment.date), join(String.join(", ", items), assessment.notes));
        }

        section(out, "Sessões");
        for (TreatmentSession session : dao.sessions(patientId)) {
            List<String> equipment = new ArrayList<>(Json.stringSet(session.equipmentJson));
            if (session.otherEquipment != null && !session.otherEquipment.isEmpty()) {
                equipment.add(session.otherEquipment);
            }
            line(out, Fmt.date(session.date), join(session.protocol,
                    String.join(", ", equipment), session.supervision, session.report));
        }

        section(out, "Consultas");
        for (AppointmentRow row : dao.appointmentsOf(patientId)) {
            line(out, Fmt.dateTime(row.appointment.startsAt), join(row.appointment.type,
                    row.appointment.done ? "concluída" : "agendada", row.appointment.notes));
        }

        section(out, "Pagamentos");
        for (PaymentRow row : dao.paymentsOf(patientId)) {
            line(out, Fmt.date(row.payment.date), join(Fmt.money(row.payment.amountCents),
                    row.payment.method, row.payment.description));
        }

        section(out, "Termos assinados");
        for (Consent consent : dao.consents(patientId)) {
            out.append("\n").append(Fmt.dateTime(consent.signedAt)).append('\n');
            try {
                ConsentContent content = ConsentContent.fromJson(
                        ConsentVault.open(consent.sealedPackage, consent.patientId));
                out.append("Assinado por ").append(content.signerName).append(" (")
                        .append(content.signerRole).append(")")
                        .append(content.place == null || content.place.isEmpty() ? ""
                                : ", " + content.place).append('\n')
                        .append(content.termText).append('\n');
            } catch (Exception e) {
                out.append("Termo indisponível: a chave de criptografia não está neste "
                        + "aparelho.\n");
            }
        }
        return out.toString();
    }

    private static void section(StringBuilder out, String title) {
        out.append("\n== ").append(title).append(" ==\n");
    }

    private static void line(StringBuilder out, String label, String value) {
        if (value != null && !value.isEmpty()) {
            out.append(label).append(": ").append(value).append('\n');
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
}
