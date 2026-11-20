package br.com.clinicaestetica.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigationrail.NavigationRailView;

import br.com.clinicaestetica.R;
import br.com.clinicaestetica.reminder.ReminderScheduler;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_NOTIFICATIONS = 1;

    private NavigationRailView rail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rail = findViewById(R.id.rail);
        rail.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_patients) {
                show(new PatientsFragment());
            } else if (id == R.id.nav_agenda) {
                show(new AgendaFragment());
            } else if (id == R.id.nav_payments) {
                show(new PaymentsFragment());
            } else {
                show(new DashboardFragment());
            }
            return true;
        });
        if (savedInstanceState == null) {
            show(new DashboardFragment());
        }
        requestNotificationPermission();
        ReminderScheduler.restoreAll(this, null);
    }

    /** Abre uma seção do menu lateral, como se ela tivesse sido tocada. */
    void open(int section) {
        rail.setSelectedItemId(section);
    }

    private void show(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
        }
    }
}
