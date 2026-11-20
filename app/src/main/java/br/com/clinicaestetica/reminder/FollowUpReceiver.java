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
import br.com.clinicaestetica.data.FormCatalog;
import br.com.clinicaestetica.ui.PatientDetailActivity;

/**
 * Avisa a proprietária, {@link FormCatalog#FOLLOW_UP_DAYS} dias depois de uma consulta concluída,
 * que é hora de mandar mensagem para a cliente. Tocar no aviso abre o WhatsApp com o texto pronto.
 */
public class FollowUpReceiver extends BroadcastReceiver {
    static final String EXTRA_APPOINTMENT_ID = "appointment_id";
    static final String NOTIFICATION_TAG = "follow_up";

    private static final String CHANNEL_ID = "follow_ups";

    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(EXTRA_APPOINTMENT_ID, 0);
        PendingResult result = goAsync();
        Async.load(() -> AppDatabase.dao(context).appointmentRow(id), row -> {
            if (row != null && row.appointment.done) {
                notify(context, row);
            }
            result.finish();
        });
    }

    private void notify(Context context, AppointmentRow row) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                "Retorno das clientes", NotificationManager.IMPORTANCE_HIGH));

        int code = (int) row.appointment.id;
        String day = new SimpleDateFormat("dd/MM", Locale.forLanguageTag("pt-BR"))
                .format(new Date(row.appointment.startsAt));
        String text = row.appointment.type + " de " + day + " · " + FormCatalog.FOLLOW_UP_DAYS
                + " dias atrás. Envie a mensagem de retorno.";

        Intent record = new Intent(context, PatientDetailActivity.class)
                .putExtra(PatientDetailActivity.EXTRA_PATIENT_ID, row.appointment.patientId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openRecord = PendingIntent.getActivity(context, code, record,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle("Enviar mensagem para " + row.patientName)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true);

        if (WhatsApp.hasPhone(row.cellPhone)) {
            Intent whatsApp = new Intent(Intent.ACTION_VIEW,
                    WhatsApp.link(row.cellPhone, message(row)))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent send = PendingIntent.getActivity(context, code, whatsApp,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.setContentIntent(send)
                    .addAction(new Notification.Action.Builder(null, "WhatsApp", send).build())
                    .addAction(new Notification.Action.Builder(null, "Ver ficha", openRecord).build());
        } else {
            builder.setContentIntent(openRecord);
        }
        manager.notify(NOTIFICATION_TAG, code, builder.build());
    }

    /** Texto sugerido para a cliente; a proprietária pode mudar antes de enviar. */
    static String message(AppointmentRow row) {
        return "Olá, " + WhatsApp.firstName(row.patientName) + "! Já faz "
                + FormCatalog.FOLLOW_UP_DAYS + " dias da sua " + row.appointment.type.toLowerCase()
                + " aqui na clínica. Vamos agendar o seu retorno?";
    }
}
