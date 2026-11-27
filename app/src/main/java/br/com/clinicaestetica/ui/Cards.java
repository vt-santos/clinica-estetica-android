package br.com.clinicaestetica.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.CalendarContract;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.json.JSONException;

import java.security.GeneralSecurityException;

import br.com.clinicaestetica.data.Appointment;
import br.com.clinicaestetica.data.AppointmentRow;
import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.data.Consent;
import br.com.clinicaestetica.data.ConsentContent;
import br.com.clinicaestetica.data.ConsentVault;
import br.com.clinicaestetica.data.Payment;
import br.com.clinicaestetica.data.PaymentRow;
import br.com.clinicaestetica.reminder.WhatsApp;

/** Cartões de consulta e pagamento, compartilhados entre as telas. */
final class Cards {
    private static final long APPOINTMENT_DURATION_MS = 60 * 60 * 1000;
    private static final int SIGNATURE_PREVIEW_DP = 160;

    private Cards() {
    }

    static void appointment(ViewGroup parent, AppointmentRow row) {
        Context context = parent.getContext();
        Appointment appointment = row.appointment;
        String status;
        if(appointment.done) {
            status = " . concluída e paga";
        } else if (appointment.startsAt < System.currentTimeMillis()) {
            status = " . sem pagamento";
        } else {
            status = "";
        }
        String title = Fmt.dateTime(appointment.startsAt) + " · " + appointment.type + status;
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
        Ui.action(actions, "Pôr no calendário", view -> addToCalendar(context, row));
        if (!empty(row.cellPhone)) {
            Ui.action(actions, "Lembrar no WhatsApp", view -> openWhatsApp(context, row));
        }
        // Concluir exige registrar o pagamento: a consulta só fica concluída quando ele é salvo.
        Ui.action(actions, "Concluir e receber", view -> {
            Intent intent = new Intent(context, PaymentFormActivity.class);
            intent.putExtra(PaymentFormActivity.EXTRA_APPOINTMENT_ID, appointment.id);
            context.startActivity(intent);
        });
    }

    /** Termo aberto e imagem já decodificada, preparados fora da thread principal. */
    private static final class OpenedConsent {
        ConsentContent content;
        Bitmap signature;
    }

    /** Abre o pacote criptografado do termo e mostra texto, quem assinou e a assinatura. */
    static void consent(ViewGroup parent, Consent consent) {
        Context context = parent.getContext();
        LinearLayout box = Ui.column(context);
        Ui.addWeighted(parent, box);
        Async.load(() -> open(consent), opened -> {
            if (opened == null) {
                Ui.muted(box, "Termo de " + Fmt.dateTime(consent.signedAt)
                        + " indisponível: a chave de criptografia não está neste aparelho.");
                return;
            }
            ConsentContent content = opened.content;
            String place = empty(content.place) ? "" : content.place + ", ";
            Ui.text(box, content.termText, 15);
            Ui.muted(box, "Assinado por " + content.signerName + " (" + content.signerRole
                    + ") em " + place + Fmt.dateTime(consent.signedAt));
            if (opened.signature == null) {
                Ui.muted(box, "Imagem da assinatura não disponível.");
                return;
            }
            ImageView signature = new ImageView(context);
            signature.setAdjustViewBounds(true);
            signature.setScaleType(ImageView.ScaleType.FIT_START);
            signature.setMaxHeight(Ui.dp(context, SIGNATURE_PREVIEW_DP));
            signature.setImageBitmap(opened.signature);
            signature.setContentDescription("Assinatura de " + content.signerName);
            Ui.addWeighted(box, signature);
        });
    }

    private static OpenedConsent open(Consent consent) {
        try {
            OpenedConsent opened = new OpenedConsent();
            opened.content = ConsentContent.fromJson(
                    ConsentVault.open(consent.sealedPackage, consent.patientId));
            byte[] png = opened.content.signaturePng;
            opened.signature = png == null ? null : BitmapFactory.decodeByteArray(png, 0, png.length);
            return opened;
        } catch (GeneralSecurityException | JSONException e) {
            return null;
        }
    }

    /** Todo pagamento registrado já está pago: o app não trabalha com fiado. */
    static void payment(ViewGroup parent, PaymentRow row) {
        Context context = parent.getContext();
        Payment payment = row.payment;
        String title = Fmt.money(payment.amountCents) + " · " + row.patientName;
        String subtitle = Fmt.date(payment.date) + " · " + payment.method
                + (empty(payment.description) ? "" : "\n" + payment.description);

        Ui.card(parent, title, subtitle, view -> {
            Intent intent = new Intent(context, PaymentFormActivity.class);
            intent.putExtra(PaymentFormActivity.EXTRA_PAYMENT_ID, payment.id);
            context.startActivity(intent);
        });
    }

    private static void addToCalendar(Context context, AppointmentRow row) {
        Appointment appointment = row.appointment;
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE,
                        appointment.type + " - " + shortName(row.patientName))
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, appointment.startsAt)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                        appointment.startsAt + APPOINTMENT_DURATION_MS);
        start(context, intent, "Nenhum aplicativo de calendário encontrado.");
    }

    private static void openWhatsApp(Context context, AppointmentRow row) {
        String message = "Olá, " + WhatsApp.firstName(row.patientName) + "! Lembrete da sua "
                + row.appointment.type.toLowerCase() + " em "
                + Fmt.dateTime(row.appointment.startsAt) + ".";
        start(context, new Intent(Intent.ACTION_VIEW, WhatsApp.link(row.cellPhone, message)),
                "WhatsApp não encontrado.");
    }

    private static void start(Context context, Intent intent, String errorMessage) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Ui.toast(context, errorMessage);
        }
    }

    /**
     * "Maria S.": o calendário costuma sincronizar com a nuvem (conta Google), então o evento
     * leva só o necessário para a equipe reconhecer a paciente (LGPD, minimização).
     */
    static String shortName(String name) {
        String trimmed = name == null ? "" : name.trim();
        String first = WhatsApp.firstName(trimmed);
        int last = trimmed.lastIndexOf(' ');
        return last < 0 ? first : first + " " + trimmed.charAt(last + 1) + ".";
    }

    private static boolean empty(String text) {
        return text == null || text.isEmpty();
    }
}
