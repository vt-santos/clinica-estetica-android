package br.com.clinicaestetica.ui;

import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

import br.com.clinicaestetica.R;
import br.com.clinicaestetica.data.AppointmentRow;
import br.com.clinicaestetica.data.Async;

public class DashboardFragment extends ScreenFragment {
    private static final int RETURN_WINDOW_DAYS = 7;

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
        Ui.title(column, "Início");
        stats = Ui.row(column);
        Ui.section(column, "Consultas de hoje");
        today = Ui.column(requireContext());
        column.addView(today);
    }

    @Override
    protected void refresh() {
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

    private void render(Summary summary) {
        if (!isAdded()) {
            return;
        }
        stats.removeAllViews();
        stat("Pacientes", String.valueOf(summary.patients), R.color.peach);
        stat("Consultas hoje", String.valueOf(summary.today.size()), R.color.sand);
        stat("Retornos em 7 dias", String.valueOf(summary.returns), R.color.cream);
        stat("A receber", Fmt.money(summary.pendingCents), R.color.sage);

        today.removeAllViews();
        if (summary.today.isEmpty()) {
            Ui.text(today, "Nenhuma consulta para hoje.", 14);
        }
        for (AppointmentRow row : summary.today) {
            Cards.appointment(today, row, this::refresh);
        }
    }

    private void stat(String label, String value, int color) {
        MaterialCardView card = new MaterialCardView(requireContext(), null,
                com.google.android.material.R.attr.materialCardViewOutlinedStyle);
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), color));
        card.setStrokeWidth(0);
        LinearLayout body = Ui.column(requireContext());
        int padding = Ui.dp(requireContext(), 16);
        body.setPadding(padding, padding, padding, 0);
        card.addView(body);
        Ui.text(body, label, 13);
        TextView number = Ui.text(body, value, 24);
        number.setTypeface(Typeface.DEFAULT_BOLD);
        Ui.addWeighted(stats, card);
    }
}
