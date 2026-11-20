package br.com.clinicaestetica.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.List;

import br.com.clinicaestetica.data.AppDao;
import br.com.clinicaestetica.data.AppDatabase;
import br.com.clinicaestetica.data.Appointment;
import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.data.FormCatalog;

/**
 * Agenda dois alarmes por consulta: o lembrete antes dela e, depois de concluída,
 * o aviso de retorno de {@link FormCatalog#FOLLOW_UP_DAYS} dias.
 */
public final class ReminderScheduler {
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private ReminderScheduler() {
    }

    public static void schedule(Context context, Appointment appointment) {
        long triggerAt = appointment.startsAt - appointment.reminderMinutes * 60_000L;
        if (appointment.done || appointment.reminderMinutes < 0
                || triggerAt <= System.currentTimeMillis()) {
            alarms(context).cancel(reminderIntent(context, appointment.id));
            return;
        }
        alarms(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt,
                reminderIntent(context, appointment.id));
    }

    /** Cancela tudo da consulta: lembrete e aviso de retorno (ao excluir). */
    public static void cancel(Context context, long appointmentId) {
        alarms(context).cancel(reminderIntent(context, appointmentId));
        alarms(context).cancel(followUpIntent(context, appointmentId));
    }

    /** Momento do aviso de retorno: N dias depois da consulta, às 9h. */
    public static long followUpAt(long startsAt) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startsAt);
        calendar.add(Calendar.DAY_OF_MONTH, FormCatalog.FOLLOW_UP_DAYS);
        calendar.set(Calendar.HOUR_OF_DAY, FormCatalog.FOLLOW_UP_HOUR);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /** Agenda o aviso de retorno de uma consulta concluída (ou o cancela, se não couber mais). */
    public static void scheduleFollowUp(Context context, Appointment appointment) {
        long triggerAt = followUpAt(appointment.startsAt);
        PendingIntent intent = followUpIntent(context, appointment.id);
        if (!appointment.done || triggerAt <= System.currentTimeMillis()) {
            alarms(context).cancel(intent);
            return;
        }
        alarms(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent);
    }

    /**
     * Recria todos os alarmes a partir do banco. O Android apaga os alarmes quando o tablet
     * reinicia, então isso roda na inicialização do tablet e sempre que o app abre.
     */
    public static void restoreAll(Context context, Runnable onDone) {
        Context app = context.getApplicationContext();
        AppDao dao = AppDatabase.dao(app);
        long now = System.currentTimeMillis();
        Async.load(() -> new Object[]{dao.pendingAppointments(now),
                dao.doneAppointmentsSince(now - (FormCatalog.FOLLOW_UP_DAYS + 1) * DAY_MS)},
                result -> {
                    @SuppressWarnings("unchecked")
                    List<Appointment> upcoming = (List<Appointment>) result[0];
                    @SuppressWarnings("unchecked")
                    List<Appointment> done = (List<Appointment>) result[1];
                    for (Appointment appointment : upcoming) {
                        schedule(app, appointment);
                    }
                    for (Appointment appointment : done) {
                        scheduleFollowUp(app, appointment);
                    }
                    if (onDone != null) {
                        onDone.run();
                    }
                });
    }

    private static AlarmManager alarms(Context context) {
        return context.getSystemService(AlarmManager.class);
    }

    private static PendingIntent reminderIntent(Context context, long appointmentId) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_APPOINTMENT_ID, appointmentId);
        return PendingIntent.getBroadcast(context, (int) appointmentId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent followUpIntent(Context context, long appointmentId) {
        Intent intent = new Intent(context, FollowUpReceiver.class);
        intent.putExtra(FollowUpReceiver.EXTRA_APPOINTMENT_ID, appointmentId);
        return PendingIntent.getBroadcast(context, (int) appointmentId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
