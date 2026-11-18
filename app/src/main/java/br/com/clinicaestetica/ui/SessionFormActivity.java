package br.com.clinicaestetica.ui;

import android.os.Bundle;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.data.FormCatalog;
import br.com.clinicaestetica.data.Json;
import br.com.clinicaestetica.data.TreatmentSession;

/** Registro do protocolo aplicado em uma sessão. */
public class SessionFormActivity extends ScreenActivity {
    static final String EXTRA_PATIENT_ID = "patient_id";
    static final String EXTRA_SESSION_ID = "session_id";

    private final long[] date = new long[1];
    private TreatmentSession session;
    private ChipGroup equipment;
    private TextInputEditText protocol, otherEquipment, supervision, report;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long id = getIntent().getLongExtra(EXTRA_SESSION_ID, 0);
        toolbar.setTitle("Sessão de tratamento");
        addAction("Salvar", this::save);

        if (id == 0) {
            session = new TreatmentSession();
            session.patientId = getIntent().getLongExtra(EXTRA_PATIENT_ID, 0);
            session.date = System.currentTimeMillis();
            buildForm();
        } else {
            addAction("Excluir", () -> Ui.confirm(this, "Excluir esta sessão?",
                    () -> Async.run(() -> dao.delete(session), this::finish)));
            Async.load(() -> dao.session(id), loaded -> {
                session = loaded;
                buildForm();
            });
        }
    }

    private void buildForm() {
        date[0] = session.date;
        Ui.dateField(content, "Data", date);
        protocol = Ui.multiline(content, "Protocolo e produtos usados", session.protocol);
        Ui.section(content, "Equipamentos");
        equipment = Ui.chips(content, FormCatalog.EQUIPMENT,
                Json.stringSet(session.equipmentJson), false);
        otherEquipment = Ui.field(content, "Outro equipamento", session.otherEquipment);
        supervision = Ui.field(content, "Supervisão", session.supervision);
        report = Ui.multiline(content, "Relatório", session.report);
    }

    private void save() {
        if (session == null || report == null) {
            return;
        }
        session.date = date[0];
        session.protocol = Ui.value(protocol);
        session.equipmentJson = Json.array(Ui.checked(equipment));
        session.otherEquipment = Ui.value(otherEquipment);
        session.supervision = Ui.value(supervision);
        session.report = Ui.value(report);
        Async.run(() -> {
            if (session.id == 0) {
                dao.insert(session);
            } else {
                dao.update(session);
            }
        }, this::finish);
    }
}
