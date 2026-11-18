package br.com.clinicaestetica.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.CalendarContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.security.GeneralSecurityException;

import br.com.clinicaestetica.data.AppDao;
import br.com.clinicaestetica.data.AppDatabase;
import br.com.clinicaestetica.data.Appointment;
import br.com.clinicaestetica.data.AppointmentRow;
import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.data.Consent;
import br.com.clinicaestetica.data.Payment;
import br.com.clinicaestetica.data.PaymentRow;
import br.com.clinicaestetica.data.SignatureVault;
import br.com.clinicaestetica.reminder.ReminderScheduler;

/** Cartões de consulta e pagamento, compartilhados entre as telas. */
final class Cards {
    private static final long APPOINTMENT_DURATION_MS = 60 * 60 * 1000;
    private static final int SIGNATURE_PREVIEW_DP = 160;

    private Cards() {
    }

    static void appointment(ViewGroup parent, AppointmentRow row, Runnable onChange) {
        Context context = parent.getContext();
        Appointment appointment = row.appointment;
        String title = Fmt.dateTime(appointment.startsAt) + " · " + appointment.type
                + (appointment.done ? " · concluída" : "");
        String subtitle = row.patientName
                + (empty(appointment.notes) ? "" : "\n" + appointment.notes);

        LinearLayout actions = Ui.card(parent, title, subtitle, view -> {
            Intent intent = new Intent(context, AppointmentFormActivity.class);
            intent.putExtra(AppointmentFormActivity.EXTRA_APPOINTMENT_ID, appointment.id);
            context.startActivity(intent);
        });
        if (appointment.done) {
            return;
        }
        Ui.action(actions, "Calendário", view -> addToCalendar(context, row));
        if (!empty(row.cellPhone)) {
            Ui.action(actions, "WhatsApp", view -> openWhatsApp(context, row));
        }
        Ui.action(actions, "Concluir", view -> {
            appointment.done = true;
            AppDao dao = AppDatabase.dao(context);
            ReminderScheduler.cancel(context, appointment.id);
            Async.run(() -> dao.update(appointment), onChange);
        });
    }

    static void consent(ViewGroup parent, Consent consent) {
        Context context = parent.getContext();
        LinearLayout box = Ui.column(context);
        Ui.addWeighted(parent, box);
        String place = empty(consent.place) ? "" : consent.place + ", ";
        Ui.text(box, consent.termText, 15);
        Ui.text(box, "Assinado em " + place + Fmt.dateTime(consent.signedAt), 14);

        ImageView signature = new ImageView(context);
        signature.setAdjustViewBounds(true);
        signature.setScaleType(ImageView.ScaleType.FIT_START);
        signature.setMaxHeight(Ui.dp(context, SIGNATURE_PREVIEW_DP));
        Ui.addWeighted(box, signature);
        Async.load(() -> decodeSignature(consent), bitmap -> {
            if (bitmap != null) {
                signature.setImageBitmap(bitmap);
            } else {
                signature.setVisibility(View.GONE);
                Ui.text(box, "Assinatura indisponível: a chave não está neste aparelho.", 14);
            }
        });
    }

    private static Bitmap decodeSignature(Consent consent) {
        try {
            byte[] png = SignatureVault.decrypt(consent.signatureEncrypted, consent.patientId);
            return BitmapFactory.decodeByteArray(png, 0, png.length);
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

    static void payment(ViewGroup parent, PaymentRow row, Runnable onChange) {
        Context context = parent.getContext();
        Payment payment = row.payment;
        String title = Fmt.money(payment.amountCents) + " · " + row.patientName;
        String subtitle = Fmt.date(payment.date) + " · " + payment.method
                + " · " + (payment.paid ? "pago" : "pendente")
                + (empty(payment.description) ? "" : "\n" + payment.description);

        LinearLayout actions = Ui.card(parent, title, subtitle, view -> {
            Intent intent = new Intent(context, PaymentFormActivity.class);
            intent.putExtra(PaymentFormActivity.EXTRA_PAYMENT_ID, payment.id);
            context.startActivity(intent);
        });
        if (!payment.paid) {
            Ui.action(actions, "Marcar como pago", view -> {
                payment.paid = true;
                AppDao dao = AppDatabase.dao(context);
                Async.run(() -> dao.update(payment), onChange);
            });
        }
    }

    private static void addToCalendar(Context context, AppointmentRow row) {
        Appointment appointment = row.appointment;
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, appointment.type + " - " + row.patientName)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, appointment.startsAt)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                        appointment.startsAt + APPOINTMENT_DURATION_MS);
        start(context, intent, "Nenhum aplicativo de calendário encontrado.");
    }

    private static void openWhatsApp(Context context, AppointmentRow row) {
        String digits = row.cellPhone.replaceAll("\\D", "");
        String phone = digits.startsWith("55") && digits.length() >= 12 ? digits : "55" + digits;
        String message = "Olá, " + row.patientName + "! Lembrete da sua "
                + row.appointment.type.toLowerCase() + " em "
                + Fmt.dateTime(row.appointment.startsAt) + ".";
        Uri uri = Uri.parse("https://wa.me/" + phone + "?text=" + Uri.encode(message));
        start(context, new Intent(Intent.ACTION_VIEW, uri), "WhatsApp não encontrado.");
    }

    private static void start(Context context, Intent intent, String errorMessage) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Ui.toast(context, errorMessage);
        }
    }

    private static boolean empty(String text) {
        return text == null || text.isEmpty();
    }
}
