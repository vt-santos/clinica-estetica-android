package br.com.clinicaestetica.ui;

import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;
import java.util.List;

import br.com.clinicaestetica.R;
import br.com.clinicaestetica.data.AppointmentRow;
import br.com.clinicaestetica.data.Async;

public class DashboardFragment extends ScreenFragment {
    private static final int RETURN_WINDOW_DAYS = 7;
    private static final int DOT_DP = 10;

    private TextView greeting;
    private LinearLayout stats;
    private LinearLayout today;

    private static class Summary {
        int patients;
        int returns;
        long pendingCents;
        List<AppointmentRow> today;
    }

    @Override
    protected void build() {
        greeting = Ui.title(column, "");
        stats = Ui.row(column);
        Ui.section(column, "Consultas de hoje");
        today = Ui.column(requireContext());
        column.addView(today);
    }

    @Override
    protected void refresh() {
        greeting.setText(greeting());
        Async.load(() -> {
            long start = Fmt.startOfDay(System.currentTimeMillis());
            Summary summary = new Summary();
            summary.patients = dao.patientCount();
            summary.returns = dao.returnCount(start, Fmt.plusDays(start, RETURN_WINDOW_DAYS));
            summary.pendingCents = dao.pendingCents();
            summary.today = dao.appointmentsBetween(start, Fmt.plusDays(start, 1));
            return summary;
        }, this::render);
    }

    private static String greeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour < 12 ? "Bom dia" : hour < 18 ? "Boa tarde" : "Boa noite";
    }

    private void render(Summary summary) {
        if (!isAdded()) {
            return;
        }
        stats.removeAllViews();
        stat("Pacientes", String.valueOf(summary.patients), R.color.peach);
        stat("Consultas hoje", String.valueOf(summary.today.size()), R.color.sand);
        stat("Retornos em 7 dias", String.valueOf(summary.returns), R.color.sage);
        stat("A receber", Fmt.money(summary.pendingCents), R.color.slate);

        today.removeAllViews();
        if (summary.today.isEmpty()) {
            Ui.muted(today, "Nenhuma consulta para hoje.");
        }
        for (AppointmentRow row : summary.today) {
            Cards.appointment(today, row, this::refresh);
        }
    }

    /** Cartão branco com um ponto da paleta, o rótulo e o número em destaque. */
    private void stat(String label, String value, int color) {
        MaterialCardView card = new MaterialCardView(requireContext(), null,
                com.google.android.material.R.attr.materialCardViewOutlinedStyle);
        LinearLayout body = Ui.column(requireContext());
        int padding = Ui.dp(requireContext(), 20);
        body.setPadding(padding, padding, padding, padding / 2);
        card.addView(body);

        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        View dot = new View(requireContext());
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(ContextCompat.getColor(requireContext(), color));
        dot.setBackground(circle);
        int size = Ui.dp(requireContext(), DOT_DP);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(size, size);
        dotParams.rightMargin = Ui.dp(requireContext(), 8);
        header.addView(dot, dotParams);
        TextView labelView = Ui.muted(header, label);
        ((ViewGroup.MarginLayoutParams) labelView.getLayoutParams()).bottomMargin = 0;
        body.addView(header);

        TextView number = Ui.text(body, value, 30);
        number.setTypeface(Ui.serif());
        number.setPadding(0, Ui.dp(requireContext(), 8), 0, 0);
        Ui.addWeighted(stats, card);
    }
}
