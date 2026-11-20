package br.com.clinicaestetica.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import br.com.clinicaestetica.R;

/** Blocos de interface usados por todas as telas. */
final class Ui {
    private static final int TEXT_FIELD = InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
    private static final int DEFAULT_BIRTH_YEAR = 1990;
    private static final int PAGE_MAX_WIDTH_DP = 960;
    private static final int PAGE_MARGIN_DP = 32;
    private static final Typeface SERIF = Typeface.create("serif", Typeface.NORMAL);
    private static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    private Ui() {
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static LinearLayout column(Context context) {
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        return column;
    }

    /**
     * Coluna principal das telas. Em telas largas o conteúdo fica centralizado com largura
     * máxima, para a leitura não se espalhar pela tela inteira do tablet.
     */
    static LinearLayout page(Context context) {
        LinearLayout page = new LinearLayout(context) {
            @Override
            protected void onMeasure(int widthSpec, int heightSpec) {
                int width = MeasureSpec.getSize(widthSpec);
                int side = Math.max(dp(context, PAGE_MARGIN_DP),
                        (width - dp(context, PAGE_MAX_WIDTH_DP)) / 2);
                if (side != getPaddingLeft()) {
                    setPadding(side, getPaddingTop(), side, getPaddingBottom());
                }
                super.onMeasure(widthSpec, heightSpec);
            }
        };
        page.setOrientation(LinearLayout.VERTICAL);
        int vertical = dp(context, 28);
        page.setPadding(0, vertical, 0, vertical * 2);
        return page;
    }

    /** Linha que vira coluna em telas estreitas. */
    static LinearLayout row(ViewGroup parent) {
        Context context = parent.getContext();
        LinearLayout row = column(context);
        if (context.getResources().getConfiguration().screenWidthDp >= 600) {
            row.setOrientation(LinearLayout.HORIZONTAL);
        }
        parent.addView(row);
        return row;
    }

    private static void addFilling(ViewGroup parent, View view) {
        Context context = parent.getContext();
        LinearLayout.LayoutParams params;
        if (parent instanceof LinearLayout
                && ((LinearLayout) parent).getOrientation() == LinearLayout.HORIZONTAL) {
            params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            if (parent.getChildCount() > 0) {
                params.leftMargin = dp(context, 12);
            }
        } else {
            params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        params.bottomMargin = dp(context, 12);
        parent.addView(view, params);
    }

    static void addWeighted(ViewGroup parent, View view) {
        addFilling(parent, view);
    }

    /** Título da tela, com serifa, como um cabeçalho de página. */
    static TextView title(ViewGroup parent, String text) {
        TextView view = text(parent, text, 32);
        view.setTypeface(SERIF);
        ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).bottomMargin =
                dp(parent.getContext(), 20);
        return view;
    }

    static TextView section(ViewGroup parent, String text) {
        TextView view = text(parent, text, 19);
        view.setTypeface(SERIF);
        view.setPadding(0, dp(parent.getContext(), 20), 0, 0);
        return view;
    }

    static TextView text(ViewGroup parent, String text, int sizeSp) {
        TextView view = new TextView(parent.getContext());
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.ink));
        view.setLineSpacing(0, 1.2f);
        addFilling(parent, view);
        return view;
    }

    /** Texto de apoio, em cinza: avisos, listas vazias, detalhes. */
    static TextView muted(ViewGroup parent, String text) {
        TextView view = text(parent, text, 14);
        view.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.muted));
        return view;
    }

    static TextView strong(ViewGroup parent, String text, int sizeSp) {
        TextView view = text(parent, text, sizeSp);
        view.setTypeface(MEDIUM);
        return view;
    }

    /** Linha "rótulo  valor" do prontuário: rótulo em cinza à esquerda, valor à direita. */
    static void info(ViewGroup parent, String label, String value) {
        Context context = parent.getContext();
        LinearLayout line = new LinearLayout(context);
        line.setOrientation(LinearLayout.HORIZONTAL);
        TextView labelView = muted(line, label);
        labelView.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 200),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView valueView = text(line, value, 15);
        valueView.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(context, 10);
        parent.addView(line, params);
    }

    static Typeface serif() {
        return SERIF;
    }

    static TextInputEditText field(ViewGroup parent, String label, String value) {
        return field(parent, label, value, TEXT_FIELD);
    }

    static TextInputEditText multiline(ViewGroup parent, String label, String value) {
        TextInputEditText edit = field(parent, label, value,
                TEXT_FIELD | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        edit.setMinLines(3);
        return edit;
    }

    static TextInputEditText field(ViewGroup parent, String label, String value, int inputType) {
        TextInputLayout layout = new TextInputLayout(parent.getContext(), null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        layout.setHint(label);
        TextInputEditText edit = new TextInputEditText(layout.getContext());
        edit.setInputType(inputType);
        edit.setText(value);
        layout.addView(edit);
        addFilling(parent, layout);
        return edit;
    }

    /** Chama action sempre que o texto do campo mudar. */
    static void onChange(TextView view, Runnable action) {
        view.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                action.run();
            }
        });
    }

    static String value(TextView view) {
        Editable text = view.getEditableText();
        return text == null ? view.getText().toString().trim() : text.toString().trim();
    }

    static TextInputEditText dateField(ViewGroup parent, String label, long[] millis) {
        return dateField(parent, label, millis, null);
    }

    /** Campo de data; o valor fica em millis[0], preservando a hora. */
    static TextInputEditText dateField(ViewGroup parent, String label, long[] millis,
                                       Runnable onChange) {
        TextInputEditText edit = pickerField(parent, label,
                millis[0] == 0 ? "" : Fmt.date(millis[0]));
        edit.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();
            if (millis[0] != 0) {
                calendar.setTimeInMillis(millis[0]);
            } else {
                calendar.set(DEFAULT_BIRTH_YEAR, Calendar.JANUARY, 1);
            }
            show(new DatePickerDialog(parent.getContext(), (picker, year, month, day) -> {
                calendar.set(year, month, day);
                millis[0] = calendar.getTimeInMillis();
                edit.setText(Fmt.date(millis[0]));
                if (onChange != null) {
                    onChange.run();
                }
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)));
        });
        return edit;
    }

    /** Campo de hora; o valor fica em millis[0], preservando a data. */
    static TextInputEditText timeField(ViewGroup parent, String label, long[] millis) {
        TextInputEditText edit = pickerField(parent, label, Fmt.time(millis[0]));
        edit.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(millis[0]);
            show(new TimePickerDialog(parent.getContext(), (picker, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                millis[0] = calendar.getTimeInMillis();
                edit.setText(Fmt.time(millis[0]));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true));
        });
        return edit;
    }

    /**
     * Mostra o seletor do sistema com os botões OK e Cancelar na cor do texto. Alguns tablets
     * (Samsung) pintam esses botões de branco, e eles somem no fundo branco.
     */
    private static void show(AlertDialog dialog) {
        dialog.show();
        int ink = ContextCompat.getColor(dialog.getContext(), R.color.ink);
        for (int which : new int[]{DialogInterface.BUTTON_POSITIVE,
                DialogInterface.BUTTON_NEGATIVE}) {
            Button button = dialog.getButton(which);
            if (button != null) {
                button.setTextColor(ink);
            }
        }
    }

    private static TextInputEditText pickerField(ViewGroup parent, String label, String value) {
        TextInputEditText edit = field(parent, label, value, InputType.TYPE_NULL);
        edit.setFocusable(false);
        edit.setCursorVisible(false);
        return edit;
    }

    static MaterialAutoCompleteTextView dropdown(ViewGroup parent, String label, String[] options,
                                                 String value) {
        MaterialAutoCompleteTextView view = autoComplete(parent, label);
        view.setInputType(InputType.TYPE_NULL);
        view.setKeyListener(null);
        view.setAdapter(new ArrayAdapter<>(parent.getContext(),
                android.R.layout.simple_list_item_1, options));
        view.setText(value, false);
        return view;
    }

    /** Lista pesquisável: digite parte do nome e escolha um item. */
    @SuppressWarnings("unchecked")
    static <T> MaterialAutoCompleteTextView picker(ViewGroup parent, String label, List<T> items,
                                                   Consumer<T> onPick) {
        MaterialAutoCompleteTextView view = autoComplete(parent, label);
        view.setThreshold(1);
        view.setAdapter(new ArrayAdapter<>(parent.getContext(),
                android.R.layout.simple_list_item_1, items));
        view.setOnItemClickListener((list, item, position, id) ->
                onPick.accept((T) list.getItemAtPosition(position)));
        return view;
    }

    private static MaterialAutoCompleteTextView autoComplete(ViewGroup parent, String label) {
        TextInputLayout layout = new TextInputLayout(parent.getContext(), null,
                com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle);
        layout.setHint(label);
        MaterialAutoCompleteTextView view = new MaterialAutoCompleteTextView(layout.getContext());
        layout.addView(view);
        addFilling(parent, layout);
        return view;
    }

    static ChipGroup chips(ViewGroup parent, String[] options, Collection<String> selected,
                           boolean single) {
        ChipGroup group = new ChipGroup(parent.getContext());
        group.setSingleSelection(single);
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        for (String option : options) {
            Chip chip = (Chip) inflater.inflate(R.layout.chip_filter, group, false);
            chip.setId(View.generateViewId());
            chip.setText(option);
            chip.setChecked(selected.contains(option));
            group.addView(chip);
        }
        addFilling(parent, group);
        return group;
    }

    static List<String> checked(ChipGroup group) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip chip = (Chip) group.getChildAt(i);
            if (chip.isChecked()) {
                values.add(chip.getText().toString());
            }
        }
        return values;
    }

    static MaterialButton button(ViewGroup parent, String text, View.OnClickListener listener) {
        MaterialButton button = new MaterialButton(parent.getContext());
        button.setText(text);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(parent.getContext(), 8);
        parent.addView(button, params);
        return button;
    }

    static MaterialButton action(ViewGroup parent, String text, View.OnClickListener listener) {
        MaterialButton button = new MaterialButton(parent.getContext(), null,
                androidx.appcompat.R.attr.borderlessButtonStyle);
        button.setText(text);
        button.setOnClickListener(listener);
        parent.addView(button);
        return button;
    }

    /** Cartão de lista; devolve a linha onde as ações do cartão são adicionadas. */
    static LinearLayout card(ViewGroup parent, String title, String subtitle,
                             View.OnClickListener onClick) {
        Context context = parent.getContext();
        MaterialCardView card = new MaterialCardView(context, null,
                com.google.android.material.R.attr.materialCardViewOutlinedStyle);
        LinearLayout body = column(context);
        int padding = dp(context, 20);
        body.setPadding(padding, padding, padding, padding / 2);
        card.addView(body);

        TextView titleView = strong(body, title, 16);
        ((ViewGroup.MarginLayoutParams) titleView.getLayoutParams()).bottomMargin = dp(context, 4);
        if (subtitle != null && !subtitle.isEmpty()) {
            muted(body, subtitle);
        }
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        body.addView(actions);

        if (onClick != null) {
            card.setOnClickListener(onClick);
        }
        addFilling(parent, card);
        return actions;
    }

    static void confirm(Context context, String message, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setMessage(message)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> onConfirm.run())
                .show();
    }

    static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
