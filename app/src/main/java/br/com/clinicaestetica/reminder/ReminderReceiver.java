package br.com.clinicaestetica.reminder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import br.com.clinicaestetica.R;
import br.com.clinicaestetica.data.AppDatabase;
import br.com.clinicaestetica.data.AppointmentRow;
import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.ui.MainActivity;

public class ReminderReceiver extends BroadcastReceiver {
    static final String EXTRA_APPOINTMENT_ID = "appointment_id";

    private static final String CHANNEL_ID = "appointments";

    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(EXTRA_APPOINTMENT_ID, 0);
        PendingResult result = goAsync();
        Async.load(() -> AppDatabase.dao(context).appointmentRow(id), row -> {
            if (row != null && !row.appointment.done) {
                notify(context, row);
            }
            result.finish();
        });
    }

    private void notify(Context context, AppointmentRow row) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Consultas",
                NotificationManager.IMPORTANCE_HIGH));

        PendingIntent open = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        String time = new SimpleDateFormat("dd/MM 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
                .format(new Date(row.appointment.startsAt));

        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle(row.appointment.type + ": " + row.patientName)
                .setContentText(time)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build();
        manager.notify((int) row.appointment.id, notification);
    }
}
