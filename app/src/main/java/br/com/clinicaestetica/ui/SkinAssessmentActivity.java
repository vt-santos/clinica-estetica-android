package br.com.clinicaestetica.ui;

import android.os.Bundle;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.data.FormCatalog;
import br.com.clinicaestetica.data.Json;
import br.com.clinicaestetica.data.SkinAssessment;

public class SkinAssessmentActivity extends ScreenActivity {
    static final String EXTRA_PATIENT_ID = "patient_id";
    static final String EXTRA_ASSESSMENT_ID = "assessment_id";

    private final Map<String, ChipGroup> groups = new LinkedHashMap<>();
    private final long[] date = new long[1];
    private SkinAssessment assessment;
    private TextInputEditText notes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long id = getIntent().getLongExtra(EXTRA_ASSESSMENT_ID, 0);
        toolbar.setTitle("Avaliação da pele");
        addAction("Salvar", this::save);

        if (id == 0) {
            assessment = new SkinAssessment();
            assessment.patientId = getIntent().getLongExtra(EXTRA_PATIENT_ID, 0);
            assessment.date = System.currentTimeMillis();
            buildForm();
        } else {
            addAction("Excluir", () -> Ui.confirm(this, "Excluir esta avaliação?",
                    () -> Async.run(() -> dao.delete(assessment), this::finish)));
            Async.load(() -> dao.assessment(id), loaded -> {
                assessment = loaded;
                buildForm();
            });
        }
    }

    private void buildForm() {
        date[0] = assessment.date;
        Ui.dateField(content, "Data", date);
        JSONObject saved = Json.object(assessment.selectionsJson);
        for (FormCatalog.SkinGroup group : FormCatalog.SKIN_GROUPS) {
            Ui.section(content, group.title);
            groups.put(group.title, Ui.chips(content, group.options,
                    Json.stringSet(saved.optJSONArray(group.title)), group.single));
        }
        Ui.section(content, "Relatório");
        notes = Ui.multiline(content, "Observações", assessment.notes);
    }

    private void save() {
        if (assessment == null || notes == null) {
            return;
        }
        assessment.date = date[0];
        assessment.notes = Ui.value(notes);
        JSONObject selections = new JSONObject();
        try {
            for (Map.Entry<String, ChipGroup> entry : groups.entrySet()) {
                List<String> checked = Ui.checked(entry.getValue());
                if (!checked.isEmpty()) {
                    selections.put(entry.getKey(), new JSONArray(checked));
                }
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        assessment.selectionsJson = selections.toString();
        Async.run(() -> {
            if (assessment.id == 0) {
                dao.insert(assessment);
            } else {
                dao.update(assessment);
            }
        }, this::finish);
    }
}
