package br.com.clinicaestetica.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Os alarmes do Android são apagados ao desligar o tablet; este receiver os recria. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        PendingResult result = goAsync();
        ReminderScheduler.restoreAll(context, result::finish);
    }
}
