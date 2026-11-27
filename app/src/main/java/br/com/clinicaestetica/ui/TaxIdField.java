package br.com.clinicaestetica.ui;

import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.ViewGroup;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import br.com.clinicaestetica.data.AppDao;
import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.data.CnpjLookup;
import br.com.clinicaestetica.data.Patient;
import br.com.clinicaestetica.data.TaxId;

/**
 * Campo "CPF ou CNPJ": põe a máscara enquanto a pessoa digita e, quando o número fica completo,
 * verifica sozinho os dígitos, se já existe outra ficha com ele e, no caso de CNPJ, a situação
 * na Receita Federal. O resultado aparece embaixo do campo.
 */
final class TaxIdField {
    static final String LABEL = "CPF ou CNPJ";

    private static final int INPUT = InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
    /** "00.000.000/0000-00" tem 18 caracteres. */
    private static final int MAX_LENGTH = 18;

    final TextInputEditText edit;
    private final TextInputLayout layout;
    private final AppDao dao;
    private final long patientId;

    private boolean formatting;
    private String checked = "";
    private Patient duplicate;
    private CnpjLookup.Result receita;

    TaxIdField(ViewGroup parent, AppDao dao, long patientId, String value) {
        this.dao = dao;
        this.patientId = patientId;
        edit = Ui.field(parent, LABEL, TaxId.format(value), INPUT);
        edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_LENGTH)});
        layout = Ui.layoutOf(edit);
        layout.setHelperText("Opcional. A verificação é automática.");
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                if (!formatting) {
                    applyMask(text);
                    refresh();
                }
            }
        });
        refresh();
    }

    /** O número sem pontuação, como é gravado. */
    String value() {
        return TaxId.clean(Ui.value(edit));
    }

    /** Ficha que já usa este número, ou null (só vale depois que a verificação terminou). */
    Patient duplicate() {
        return value().equals(checked) ? duplicate : null;
    }

    /** Resultado da Receita para o CNPJ atual, ou null se não foi consultado ainda. */
    CnpjLookup.Result receita() {
        return value().equals(checked) ? receita : null;
    }

    /** Reaplica a máscara mantendo o cursor depois do mesmo caractere digitado. */
    private void applyMask(Editable text) {
        String current = text.toString();
        String masked = TaxId.format(current);
        if (masked.equals(current)) {
            return;
        }
        int cursor = Math.max(0, edit.getSelectionEnd());
        int typedBefore = TaxId.clean(current.substring(0, Math.min(cursor, current.length())))
                .length();
        formatting = true;
        text.replace(0, text.length(), masked);
        formatting = false;
        int position = 0;
        for (int seen = 0; position < masked.length() && seen < typedBefore; position++) {
            if (Character.isLetterOrDigit(masked.charAt(position))) {
                seen++;
            }
        }
        edit.setSelection(Math.min(position, edit.length()));
    }

    private void refresh() {
        String id = value();
        duplicate = null;
        receita = null;
        checked = "";
        if (id.isEmpty()) {
            show("Opcional. A verificação é automática.", false);
            return;
        }
        if (!TaxId.isComplete(id) || !TaxId.isValid(id)) {
            show(TaxId.describe(id), TaxId.isComplete(id));
            return;
        }
        show(TaxId.describe(id) + ". Verificando…", false);
        Async.load(() -> dao.patientWithTaxId(id, patientId), other -> {
            if (!id.equals(value())) {
                return;
            }
            duplicate = other;
            if (TaxId.kind(id) == TaxId.Kind.CPF) {
                checked = id;
                showResult();
                return;
            }
            show("CNPJ válido. Consultando a Receita Federal…", false);
            CnpjLookup.lookup(id, result -> {
                if (!id.equals(value())) {
                    return;
                }
                receita = result;
                checked = id;
                showResult();
            });
        });
    }

    private void showResult() {
        String id = value();
        String name = TaxId.kind(id) == TaxId.Kind.CPF ? "CPF" : "CNPJ";
        if (duplicate != null) {
            show(name + " já cadastrado na ficha de " + duplicate.name, true);
            return;
        }
        if (receita == null) {
            show(name + " válido ✓", false);
            return;
        }
        boolean problem = receita.status == CnpjLookup.Status.NOT_FOUND
                || receita.status == CnpjLookup.Status.FOUND && !receita.isActive();
        show(receita.isActive() ? receita.describe() + " ✓" : receita.describe(), problem);
    }

    private void show(String message, boolean error) {
        if (error) {
            layout.setError(message);
        } else {
            layout.setError(null);
            layout.setHelperText(message);
        }
    }
}
