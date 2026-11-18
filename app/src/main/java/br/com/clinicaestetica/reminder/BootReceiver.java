package br.com.clinicaestetica.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

import br.com.clinicaestetica.data.AppDatabase;
import br.com.clinicaestetica.data.Appointment;
import br.com.clinicaestetica.data.Async;

/** Os alarmes do Android são apagados ao desligar o tablet; este receiver os recria. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        PendingResult result = goAsync();
        Async.load(() -> AppDatabase.dao(context).pendingAppointments(System.currentTimeMillis()),
                (List<Appointment> appointments) -> {
                    for (Appointment appointment : appointments) {
                        ReminderScheduler.schedule(context, appointment);
                    }
                    result.finish();
                });
    }
}
