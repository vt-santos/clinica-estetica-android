package br.com.clinicaestetica.ui;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.LinearLayout;

import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.data.Patient;

public class PatientsFragment extends ScreenFragment {
    private TextInputEditText search;
    private LinearLayout list;

    @Override
    protected void build() {
        Ui.title(column, "Pacientes");
        Ui.button(column, "Nova paciente", view ->
                startActivity(new Intent(requireContext(), PatientFormActivity.class)));
        search = Ui.field(column, "Pesquisar por nome ou celular", "");
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                refresh();
            }
        });
        list = Ui.column(requireContext());
        column.addView(list);
    }

    @Override
    protected void refresh() {
        String term = Ui.value(search);
        Async.load(() -> term.isEmpty() ? dao.allPatients() : dao.searchPatients(term), this::render);
    }

    private void render(List<Patient> patients) {
        if (!isAdded()) {
            return;
        }
        list.removeAllViews();
        if (patients.isEmpty()) {
            Ui.muted(list, "Nenhuma paciente encontrada.");
        }
        for (Patient patient : patients) {
            Ui.card(list, patient.name, patient.cellPhone, view -> {
                Intent intent = new Intent(requireContext(), PatientDetailActivity.class);
                intent.putExtra(PatientDetailActivity.EXTRA_PATIENT_ID, patient.id);
                startActivity(intent);
            });
        }
    }
}
