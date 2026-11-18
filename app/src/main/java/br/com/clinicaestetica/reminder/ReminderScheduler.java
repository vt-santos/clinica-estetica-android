package br.com.clinicaestetica.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import br.com.clinicaestetica.data.Appointment;

public final class ReminderScheduler {
    private ReminderScheduler() {
    }

    public static void schedule(Context context, Appointment appointment) {
        long triggerAt = appointment.startsAt - appointment.reminderMinutes * 60_000L;
        if (appointment.done || appointment.reminderMinutes < 0
                || triggerAt <= System.currentTimeMillis()) {
            cancel(context, appointment.id);
            return;
        }
        alarms(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt,
                pendingIntent(context, appointment.id));
    }

    public static void cancel(Context context, long appointmentId) {
        alarms(context).cancel(pendingIntent(context, appointmentId));
    }

    private static AlarmManager alarms(Context context) {
        return context.getSystemService(AlarmManager.class);
    }

    private static PendingIntent pendingIntent(Context context, long appointmentId) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_APPOINTMENT_ID, appointmentId);
        return PendingIntent.getBroadcast(context, (int) appointmentId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
