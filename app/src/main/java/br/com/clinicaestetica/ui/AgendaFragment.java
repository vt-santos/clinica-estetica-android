package br.com.clinicaestetica.ui;

import android.content.Intent;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

import br.com.clinicaestetica.data.AppointmentRow;
import br.com.clinicaestetica.data.Async;

public class AgendaFragment extends ScreenFragment {
    private long selectedDay;
    private TextView dayTitle;
    private LinearLayout list;

    @Override
    protected void build() {
        selectedDay = Fmt.startOfDay(System.currentTimeMillis());
        Ui.title(column, "Agenda");
        Ui.button(column, "Nova consulta", view -> {
            Intent intent = new Intent(requireContext(), AppointmentFormActivity.class);
            intent.putExtra(AppointmentFormActivity.EXTRA_DAY, selectedDay);
            startActivity(intent);
        });

        LinearLayout row = Ui.row(column);
        CalendarView calendar = new CalendarView(requireContext());
        calendar.setDate(selectedDay);
        calendar.setOnDateChangeListener((view, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, day);
            selectedDay = Fmt.startOfDay(picked.getTimeInMillis());
            refresh();
        });
        Ui.addWeighted(row, calendar);

        LinearLayout side = Ui.column(requireContext());
        Ui.addWeighted(row, side);
        dayTitle = Ui.section(side, "");
        list = Ui.column(requireContext());
        side.addView(list);
    }

    @Override
    protected void refresh() {
        long day = selectedDay;
        Async.load(() -> dao.appointmentsBetween(day, Fmt.plusDays(day, 1)), this::render);
    }

    private void render(List<AppointmentRow> rows) {
        if (!isAdded()) {
            return;
        }
        dayTitle.setText(Fmt.date(selectedDay));
        list.removeAllViews();
        if (rows.isEmpty()) {
            Ui.muted(list, "Nenhuma consulta neste dia.");
        }
        for (AppointmentRow row : rows) {
            Cards.appointment(list, row, this::refresh);
        }
    }
}
