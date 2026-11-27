package br.com.clinicaestetica.ui;

import android.content.Intent;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import br.com.clinicaestetica.data.AppointmentRow;
import br.com.clinicaestetica.data.Appointment;
import br.com.clinicaestetica.data.Async;

import com.google.android.material.chip.ChipGroup;

public class AgendaFragment extends ScreenFragment {
    private long selectedDay;
    private TextView dayTitle;
    private LinearLayout list;
    private boolean monthMode;


    @Override
    protected void build() {
        selectedDay = Fmt.startOfDay(System.currentTimeMillis());
        Ui.title(column, "Agenda");
        Ui.button(column, "Nova consulta", view -> {
            Intent intent = new Intent(requireContext(), AppointmentFormActivity.class);
            intent.putExtra(AppointmentFormActivity.EXTRA_DAY, selectedDay);
            startActivity(intent);
        });
        
        ChipGroup mode = Ui.chips(column, new String[]{"Dia", "Mês inteiro"}, 
            Collections.singleton("Dia"), true);
        mode.setSelectionRequired(true);
        mode.setOnCheckedStateChangeListener((group, ids) -> {
            monthMode = Ui.checked(group).contains("Mês inteiro");
            refresh();
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
        long from;
        long to;
        if (monthMode) {
            from = Fmt.startOfMonth(selectedDay);
            to = Fmt.plusMonths(from, 1);
        } else {
            from = selectedDay;
            to = Fmt.plusDays(selectedDay, 1);
        }    
        Async.load(() -> dao.appointmentsBetween(from, to), this::render);
    }
            

    private void render(List<AppointmentRow> rows) {
        if (!isAdded()) {
            return;
        }
        list.removeAllViews();
        if (monthMode) {
            dayTitle.setText(Fmt.month(selectedDay));
            showMonthSummary(rows);
        } else {
            dayTitle.setText(Fmt.date(selectedDay));
        }
        if (rows.isEmpty()) {
            Ui.muted(list, monthMode ? "Nenhuma consulta neste mês" : "Nenhuma consulta neste dia.");
        }
        for (AppointmentRow row : rows) {
            Cards.appointment(list, row);
        }
    }

    private void showMonthSummary(List<AppointmentRow> rows) {
        long now = System.currentTimeMillis();
        int paid = 0;
        int unpaid = 0;
        int upcoming = 0;
        for (AppointmentRow row : rows) {
            Appointment appointment = row.appointment;
            if (appointment.done) {
                paid++;
            } else if (appointment.startsAt < now) {
                unpaid++;
            } else {
                upcoming++;
            }
        }

        Ui.strong(list, rows.size() + " Consultas no mês", 18);
        Ui.text(list, "Realizadas e pagas: " + paid, 16);
        Ui.text(list, "Já realizadas e não pagas: " + unpaid, 16);
        Ui.text(list, "Ainda por realizar: " + upcoming, 16);
    }
}
