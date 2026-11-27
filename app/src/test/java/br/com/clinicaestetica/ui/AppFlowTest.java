package br.com.clinicaestetica.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Application;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.view.menu.MenuItemImpl;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.navigationrail.NavigationRailView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.spec.SecretKeySpec;

import br.com.clinicaestetica.R;
import br.com.clinicaestetica.data.AppDao;
import br.com.clinicaestetica.data.AppDatabase;
import br.com.clinicaestetica.data.Appointment;
import br.com.clinicaestetica.data.Consent;
import br.com.clinicaestetica.data.ConsentContent;
import br.com.clinicaestetica.data.ConsentVault;
import br.com.clinicaestetica.data.Patient;
import br.com.clinicaestetica.data.Payment;
import br.com.clinicaestetica.data.SkinAssessment;
import br.com.clinicaestetica.data.TreatmentSession;

/** Percorre o app de ponta a ponta com as telas reais, sem aparelho. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, qualifiers = "w1280dp-h800dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AppFlowTest {
    private static final int SCREEN_WIDTH = 1280;
    private static final int SCREEN_HEIGHT = 800;

    private final Application app = RuntimeEnvironment.getApplication();
    private final AppDao dao = AppDatabase.dao(app);

    @Before
    public void useSoftwareKey() {
        ConsentVault.useTestKey(new SecretKeySpec(new byte[32], "AES"));
    }

    @Test
    public void parsesMoney() {
        assertEquals(15000, Fmt.parseCents("150"));
        assertEquals(15050, Fmt.parseCents("150,50"));
        assertEquals(150050, Fmt.parseCents("1.500,50"));
        assertEquals(-1, Fmt.parseCents("abc"));
    }

    @Test
    public void fullFlow() throws Exception {
        Patient patient = registersPatient();
        registersAssessment(patient.id);
        registersSession(patient.id);
        long appointmentId = schedulesAppointment(patient.id);
        firesReminder(appointmentId);
        concludesAppointmentWithPayment(appointmentId);
        registersPayment(patient.id);
        showsPatientRecord(patient.id);
        showsMainSections();
        registersMinor();
    }

    /** Nada de internet: sem analytics, anúncios, cookies ou conteúdo de terceiros. */
    @Test
    public void appCannotReachTheInternet() throws Exception {
        String[] permissions = app.getPackageManager().getPackageInfo(app.getPackageName(),
                android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions;
        for (String permission : permissions == null ? new String[0] : permissions) {
            assertFalse(permission, permission.equals(android.Manifest.permission.INTERNET));
        }
    }

    @Test
    public void legalDocumentsOpen() {
        for (String key : LegalDocs.ALL) {
            String text = LegalDocs.load(app, key);
            assertFalse(key + " sem marcadores soltos", text.contains("{{"));
            assertTrue(key, text.contains(app.getString(R.string.legal_version)));
            LegalActivity screen = open(LegalActivity.class, LegalActivity.intent(app, key));
            assertEquals(LegalDocs.title(app, key), screen.toolbar.getTitle().toString());
            assertTrue(hasText(root(screen), "revisão por advogado"));
        }
        assertTrue("dados da empresa ainda são modelo", LegalDocs.hasPending(app));
        assertTrue(LegalDocs.load(app, LegalDocs.PRIVACY)
                .contains(app.getString(R.string.company_cnpj)));
        screenshot(open(LegalActivity.class, LegalActivity.intent(app, LegalDocs.PRIVACY)),
                "aviso-privacidade");
    }

    /** Datas, listas e cartões funcionam com teclado físico; ícones têm descrição. */
    @Test
    public void worksWithKeyboardAndScreenReader() {
        PatientFormActivity form = open(PatientFormActivity.class, new Intent());
        assertEquals("Voltar", form.toolbar.getNavigationContentDescription().toString());
        android.widget.EditText date = field(root(form), "Data de nascimento *").getEditText();
        assertTrue("data recebe foco pelo teclado", date.isFocusable());
        date.requestFocus();
        date.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
        date.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        settle();
        assertTrue("Enter abre o seletor de data",
                ShadowDialog.getLatestDialog() instanceof DatePickerDialog);
        ShadowDialog.getLatestDialog().dismiss();

        SignatureView signature = first(root(form), SignatureView.class);
        assertTrue(signature.getContentDescription().toString().contains("vazia"));
        stroke(signature, MotionEvent.TOOL_TYPE_STYLUS);
        assertTrue(signature.getContentDescription().toString().contains("assinada"));

        MainActivity main = open(MainActivity.class, new Intent());
        View card = clickableAncestor(main.findViewById(R.id.container), "Pacientes");
        assertNotNull(card);
        assertTrue("cartão do Início recebe foco", card.isFocusable());
        assertTrue(card.getContentDescription().toString().startsWith("Pacientes: "));
    }

    private Patient registersPatient() throws Exception {
        PatientFormActivity form = open(PatientFormActivity.class, new Intent());
        assertEquals("Nova paciente", form.toolbar.getTitle().toString());

        save(form);
        assertFalse("nome é obrigatório", form.isFinishing());

        setField(form, "Nome *", "Maria Silva");
        save(form);
        assertFalse("data de nascimento é obrigatória", form.isFinishing());
        pickDate(form, "Data de nascimento *", 1990, Calendar.MARCH, 15);
        assertFalse("adulta não pede responsável", field(root(form), "Nome do responsável *").isShown());
        setField(form, "Celular / WhatsApp", "(11) 98888-7777");
        setField(form, "Motivo da visita", "Manchas no rosto");
        setField(form, "Hospital", "Santa Casa");
        chip(form, "Sim", 1).setChecked(true);
        setField(form, "Quais?", "Dipirona");
        screenshot(form, "cadastro");

        save(form);
        assertFalse("termo é obrigatório", form.isFinishing());
        assertTrue("termo traz o consentimento LGPD", hasText(root(form), "LGPD"));
        checkBoxes(root(form)).get(0).setChecked(true);
        save(form);
        assertFalse("autorização de dados (LGPD) é obrigatória", form.isFinishing());
        checkBoxes(root(form)).get(1).setChecked(true);
        save(form);
        assertFalse("assinatura é obrigatória", form.isFinishing());

        SignatureView signature = first(root(form), SignatureView.class);
        stroke(signature, MotionEvent.TOOL_TYPE_STYLUS);
        assertFalse(signature.isEmpty());
        signature.clear();
        stroke(signature, MotionEvent.TOOL_TYPE_FINGER);
        assertTrue("palma é ignorada depois da caneta", signature.isEmpty());
        stroke(signature, MotionEvent.TOOL_TYPE_STYLUS);
        setField(form, "Local", "Brasília");
        save(form);
        assertTrue(form.isFinishing());

        Patient saved = db(() -> dao.allPatients().get(0));
        Consent consent = db(() -> dao.latestConsent(saved.id));
        assertNull("conteúdo aberto não é gravado", consent.content);
        assertFalse("termo não fica legível no banco", consent.sealedPackage.contains("Brasília"));
        Base64.getDecoder().decode(consent.sealedPackage);
        ConsentContent signed = opened(consent);
        assertEquals("Brasília", signed.place);
        assertTrue(signed.accepted);
        assertEquals("Maria Silva", signed.signerName);
        assertEquals("Paciente", signed.signerRole);
        assertTrue(signed.termText.startsWith("Estou ciente e de acordo"));
        assertTrue(signed.termText.contains("TRATAMENTO DE DADOS PESSOAIS (LGPD)"));
        assertTrue(signed.privacyAccepted);
        assertFalse(signed.privacyVersion.isEmpty());
        assertEquals("imagem guardada junto ao aceite", (byte) 0x89, signed.signaturePng[0]);
        assertThrows("outra paciente não abre", GeneralSecurityException.class,
                () -> ConsentVault.open(consent.sealedPackage, saved.id + 1));
        assertEquals("Maria Silva", saved.name);
        assertEquals("Santa Casa", saved.hospital);
        JSONObject allergies = new JSONObject(saved.historyJson).getJSONObject("allergies");
        assertEquals("S", allergies.getString("a"));
        assertEquals("Dipirona", allergies.getString("d"));

        Intent edit = new Intent().putExtra(PatientFormActivity.EXTRA_PATIENT_ID, saved.id);
        PatientFormActivity reopened = open(PatientFormActivity.class, edit);
        assertTrue(chip(reopened, "Sim", 1).isChecked());
        save(reopened);
        assertTrue("ficha assinada salva sem nova assinatura", reopened.isFinishing());
        assertEquals("Dipirona", new JSONObject(db(() -> dao.patient(saved.id)).historyJson)
                .getJSONObject("allergies").getString("d"));
        assertEquals(consent.id, db(() -> dao.latestConsent(saved.id)).id);

        requiresNewSignatureWhenChanged(edit, saved.id);
        return saved;
    }

    private void requiresNewSignatureWhenChanged(Intent edit, long patientId) throws Exception {
        PatientFormActivity form = open(PatientFormActivity.class, edit);
        setField(form, "Profissão", "Dentista");
        save(form);
        assertFalse("alteração sem assinatura não salva", form.isFinishing());
        assertEquals("", db(() -> dao.patient(patientId)).profession);
        assertTrue(hasText(root(form), "esta ficha foi alterada"));
        assertTrue(hasText(root(form), "Profissão: (vazio) → Dentista"));

        screenshot(form, "termo-alteracao");
        acceptTerms(form);
        SignatureView signature = first(root(form), SignatureView.class);
        stroke(signature, MotionEvent.TOOL_TYPE_STYLUS);

        setField(form, "Profissão", "Médica");
        save(form);
        assertFalse("mudar depois de assinar invalida a assinatura", form.isFinishing());
        assertTrue(signature.isEmpty());
        assertTrue(hasText(root(form), "Profissão: (vazio) → Médica"));

        acceptTerms(form);
        stroke(signature, MotionEvent.TOOL_TYPE_STYLUS);
        save(form);
        assertTrue(form.isFinishing());
        assertEquals("Médica", db(() -> dao.patient(patientId)).profession);
        assertEquals(2, (int) db(() -> dao.consents(patientId).size()));
        assertTrue(opened(db(() -> dao.latestConsent(patientId))).termText
                .contains("Profissão: (vazio) → Médica"));
    }

    private void registersMinor() throws Exception {
        PatientFormActivity form = open(PatientFormActivity.class, new Intent());
        setField(form, "Nome *", "Ana Souza");
        pickDate(form, "Data de nascimento *", Calendar.getInstance().get(Calendar.YEAR) - 10,
                Calendar.JANUARY, 10);
        assertTrue(hasText(root(form), "Menor de idade"));
        assertTrue(field(root(form), "Nome do responsável *").isShown());
        assertTrue(hasText(root(form), "Assinatura do(a) responsável legal"));
        save(form);
        assertFalse("menor sem responsável não salva", form.isFinishing());

        setField(form, "Nome do responsável *", "Carla Souza");
        setField(form, "CPF do responsável *", "111.111.111-11");
        setField(form, "Parentesco *", "Mãe");
        assertTrue(hasText(root(form), "Carla Souza"));
        assertTrue(hasText(root(form), "mãe e responsável legal por Ana Souza, menor de idade"));
        acceptTerms(form);
        SignatureView signature = first(root(form), SignatureView.class);
        stroke(signature, MotionEvent.TOOL_TYPE_STYLUS);
        save(form);
        assertFalse("CPF inválido não salva", form.isFinishing());

        setField(form, "CPF do responsável *", "529.982.247-25");
        assertTrue("termo mudou, assinatura antiga é descartada", signature.isEmpty());
        for (MaterialCheckBox box : checkBoxes(root(form))) {
            assertFalse("aceites são desmarcados quando o termo muda", box.isChecked());
        }
        screenshot(form, "termo-menor");
        acceptTerms(form);
        stroke(signature, MotionEvent.TOOL_TYPE_STYLUS);
        save(form);
        assertTrue(form.isFinishing());

        Patient minor = db(() -> dao.searchPatients("Ana Souza").get(0));
        assertEquals("52998224725", minor.guardianCpf);
        assertEquals("Mãe", minor.guardianRelationship);
        ConsentContent signed = opened(db(() -> dao.latestConsent(minor.id)));
        assertEquals("Carla Souza", signed.signerName);
        assertEquals("Responsável legal (Mãe)", signed.signerRole);
        assertEquals("52998224725", signed.signerCpf);
        assertTrue(signed.termText.contains("CPF 529.982.247-25"));
        assertTrue(signed.termText.contains("como responsável legal, autorizo"));
    }

    private static ConsentContent opened(Consent consent) throws Exception {
        return ConsentContent.fromJson(ConsentVault.open(consent.sealedPackage, consent.patientId));
    }

    private void pickDate(Activity activity, String label, int year, int month, int day) {
        field(root(activity), label).getEditText().performClick();
        settle();
        DatePickerDialog dialog = (DatePickerDialog) ShadowDialog.getLatestDialog();
        dialog.updateDate(year, month, day);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        settle();
    }

    private static void stroke(SignatureView view, int toolType) {
        view.dispatchTouchEvent(touch(MotionEvent.ACTION_DOWN, toolType, 100, 100));
        view.dispatchTouchEvent(touch(MotionEvent.ACTION_MOVE, toolType, 200, 130));
        view.dispatchTouchEvent(touch(MotionEvent.ACTION_UP, toolType, 200, 130));
    }

    private static MotionEvent touch(int action, int toolType, float x, float y) {
        MotionEvent.PointerProperties properties = new MotionEvent.PointerProperties();
        properties.toolType = toolType;
        MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
        coords.x = x;
        coords.y = y;
        coords.pressure = 1;
        coords.size = 1;
        return MotionEvent.obtain(0, 0, action, 1,
                new MotionEvent.PointerProperties[]{properties},
                new MotionEvent.PointerCoords[]{coords},
                0, 0, 1, 1, 0, 0, InputDevice.SOURCE_STYLUS, 0);
    }

    private void registersAssessment(long patientId) {
        Intent intent = new Intent().putExtra(SkinAssessmentActivity.EXTRA_PATIENT_ID, patientId);
        SkinAssessmentActivity form = open(SkinAssessmentActivity.class, intent);
        chip(form, "Cloasma", 0).setChecked(true);
        chip(form, "Seborreica", 0).setChecked(true);
        setField(form, "Observações", "Pele oleosa");
        screenshot(form, "avaliacao");
        save(form);

        SkinAssessment saved = db(() -> dao.assessments(patientId).get(0));
        assertTrue(saved.selectionsJson.contains("Cloasma"));
        intent.putExtra(SkinAssessmentActivity.EXTRA_ASSESSMENT_ID, saved.id);
        assertTrue(chip(open(SkinAssessmentActivity.class, intent), "Cloasma", 0).isChecked());
    }

    private void registersSession(long patientId) {
        Intent intent = new Intent().putExtra(SessionFormActivity.EXTRA_PATIENT_ID, patientId);
        SessionFormActivity form = open(SessionFormActivity.class, intent);
        setField(form, "Protocolo e produtos usados", "Limpeza de pele");
        chip(form, "Vaporizador", 0).setChecked(true);
        setField(form, "Relatório", "Boa resposta");
        screenshot(form, "sessao");
        save(form);

        TreatmentSession saved = db(() -> dao.sessions(patientId).get(0));
        assertEquals("Limpeza de pele", saved.protocol);
        assertTrue(saved.equipmentJson.contains("Vaporizador"));
    }

    private long schedulesAppointment(long patientId) {
        AppointmentFormActivity withoutPatient = open(AppointmentFormActivity.class, new Intent());
        save(withoutPatient);
        assertFalse("paciente é obrigatória", withoutPatient.isFinishing());

        long tomorrow = Fmt.plusDays(Fmt.startOfDay(System.currentTimeMillis()), 1);
        Intent intent = new Intent()
                .putExtra(AppointmentFormActivity.EXTRA_PATIENT_ID, patientId)
                .putExtra(AppointmentFormActivity.EXTRA_DAY, tomorrow);
        AppointmentFormActivity form = open(AppointmentFormActivity.class, intent);
        screenshot(form, "consulta");
        save(form);
        assertTrue(form.isFinishing());

        Appointment saved = db(() -> dao.appointmentsOf(patientId).get(0)).appointment;
        assertEquals("Consulta", saved.type);
        assertEquals(60, saved.reminderMinutes);
        AlarmManager alarms = app.getSystemService(AlarmManager.class);
        assertEquals(1, shadowOf(alarms).getScheduledAlarms().size());
        return saved.id;
    }

    private void firesReminder(long appointmentId) {
        Intent alarm = new Intent(app, br.com.clinicaestetica.reminder.ReminderReceiver.class)
                .putExtra("appointment_id", appointmentId);
        app.sendBroadcast(alarm);
        settle();
        NotificationManager notifications = app.getSystemService(NotificationManager.class);
        assertEquals(1, shadowOf(notifications).size());
    }

    /** Concluir abre o pagamento; sem pagamento salvo a consulta continua em aberto. */
    private void concludesAppointmentWithPayment(long appointmentId) {
        Intent intent = new Intent()
                .putExtra(PaymentFormActivity.EXTRA_APPOINTMENT_ID, appointmentId);
        PaymentFormActivity form = open(PaymentFormActivity.class, intent);
        assertEquals("Concluir consulta", form.toolbar.getTitle().toString());
        assertEquals("Consulta", field(root(form), "Descrição").getEditText().getText().toString());
        save(form);
        assertFalse("pagamento é obrigatório", form.isFinishing());
        assertFalse(db(() -> dao.appointment(appointmentId)).done);

        setField(form, "Valor (R$) *", "200");
        screenshot(form, "concluir");
        save(form);
        assertTrue(form.isFinishing());
        Appointment done = db(() -> dao.appointment(appointmentId));
        assertTrue(done.done);
        Payment paid = db(() -> dao.allPayments().get(0)).payment;
        assertEquals(20000, paid.amountCents);
        assertTrue(paid.paid);
        assertEquals("Consulta", paid.description);

        // O lembrete da consulta sai e entra o aviso de retorno, 28 dias depois às 9h.
        AlarmManager alarms = app.getSystemService(AlarmManager.class);
        assertEquals(1, shadowOf(alarms).getScheduledAlarms().size());
        long followUp = br.com.clinicaestetica.reminder.ReminderScheduler.followUpAt(done.startsAt);
        assertEquals(followUp, shadowOf(alarms).getScheduledAlarms().get(0).getTriggerAtMs());
        Calendar expected = Calendar.getInstance();
        expected.setTimeInMillis(done.startsAt);
        expected.add(Calendar.DAY_OF_MONTH, 28);
        Calendar actual = Calendar.getInstance();
        actual.setTimeInMillis(followUp);
        assertEquals(expected.get(Calendar.DAY_OF_YEAR), actual.get(Calendar.DAY_OF_YEAR));
        assertEquals(9, actual.get(Calendar.HOUR_OF_DAY));

        app.sendBroadcast(new Intent(app, br.com.clinicaestetica.reminder.FollowUpReceiver.class)
                .putExtra("appointment_id", appointmentId));
        settle();
        NotificationManager notifications = app.getSystemService(NotificationManager.class);
        android.app.Notification notice = null;
        for (android.app.Notification each : shadowOf(notifications).getAllNotifications()) {
            if (String.valueOf(each.extras.getCharSequence(android.app.Notification.EXTRA_TITLE))
                    .contains("Enviar mensagem")) {
                notice = each;
            }
        }
        assertNotNull("aviso de retorno", notice);
        assertEquals("Enviar mensagem para Maria Silva",
                notice.extras.getCharSequence(android.app.Notification.EXTRA_TITLE).toString());
        assertTrue(notice.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString()
                .startsWith("Consulta de "));
        assertEquals(2, notice.actions.length);
    }

    /** Pagamento avulso: entra sempre como pago, sem opção de fiado. */
    private void registersPayment(long patientId) {
        Intent intent = new Intent().putExtra(PaymentFormActivity.EXTRA_PATIENT_ID, patientId);
        PaymentFormActivity form = open(PaymentFormActivity.class, intent);
        assertNull(first(root(form),
                com.google.android.material.materialswitch.MaterialSwitch.class));
        setField(form, "Valor (R$) *", "1.500,50");
        screenshot(form, "pagamento");
        save(form);

        Payment saved = db(() -> dao.allPayments().get(0)).payment;
        assertEquals(150050, saved.amountCents);
        assertTrue(saved.paid);
        long month = Fmt.startOfMonth(System.currentTimeMillis());
        assertEquals(170050L, (long) db(() -> dao.receivedCents(month, Fmt.plusMonths(month, 1))));
    }

    private void showsPatientRecord(long patientId) {
        Intent intent = new Intent().putExtra(PatientDetailActivity.EXTRA_PATIENT_ID, patientId);
        PatientDetailActivity detail = open(PatientDetailActivity.class, intent);
        assertTrue(hasText(root(detail), "Dipirona"));
        screenshot(detail, "prontuario");

        TabLayout tabs = first(root(detail), TabLayout.class);
        String[] expected = {"Dipirona", "Cloasma", "Limpeza de pele", "Maria Silva", "1.500,50"};
        for (int i = 0; i < expected.length; i++) {
            tabs.selectTab(tabs.getTabAt(i));
            settle();
            assertTrue(expected[i], hasText(root(detail), expected[i]));
        }

        // O formato de moeda pt-BR usa espaço não separável depois de "R$".
        String export = db(() -> PatientExport.text(dao, patientId, "Clínica"))
                .replace('\u00A0', ' ');
        for (String text : new String[]{"Maria Silva", "Dipirona", "Cloasma", "Limpeza de pele",
                "R$ 1.500,50", "Assinado por Maria Silva", "LGPD"}) {
            assertTrue("exportação tem " + text, export.contains(text));
        }
        invoke(detail, "Exportar dados");
        Intent chooser = lastStarted(detail);
        assertEquals(Intent.ACTION_CHOOSER, chooser.getAction());
        Intent shared = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
        assertTrue(shared.getStringExtra(Intent.EXTRA_TEXT).contains("Dipirona"));
    }

    private void showsMainSections() {
        MainActivity main = open(MainActivity.class, new Intent());
        View window = root(main);
        assertTrue(hasText(window, "R$"));
        screenshot(main, "inicio");

        // Os cartões do Início levam à seção certa.
        NavigationRailView rail = main.findViewById(R.id.rail);
        assertTrue(clickCard(main.findViewById(R.id.container), "Recebido no mês"));
        settle();
        assertEquals(R.id.nav_payments, rail.getSelectedItemId());
        assertTrue(hasText(window, "Recebido no mês: "));
        rail.setSelectedItemId(R.id.nav_home);
        settle();
        assertTrue(clickCard(main.findViewById(R.id.container), "Consultas hoje"));
        settle();
        assertEquals(R.id.nav_agenda, rail.getSelectedItemId());
        rail.setSelectedItemId(R.id.nav_home);
        settle();
        assertTrue(clickCard(main.findViewById(R.id.container), "Pacientes"));
        settle();
        assertEquals(R.id.nav_patients, rail.getSelectedItemId());
        assertTrue(hasText(window, "Maria Silva"));
        setField(main, "Pesquisar por nome ou celular", "ninguem");
        settle();
        assertFalse(hasText(window, "Maria Silva"));
        setField(main, "Pesquisar por nome ou celular", "mar");
        settle();
        assertTrue(hasText(window, "Maria Silva"));
        screenshot(main, "pacientes");

        rail.setSelectedItemId(R.id.nav_agenda);
        settle();
        assertTrue(hasText(window, "Nenhuma consulta neste dia."));
        screenshot(main, "agenda");

        rail.setSelectedItemId(R.id.nav_payments);
        settle();
        screenshot(main, "pagamentos");

        rail.setSelectedItemId(R.id.nav_about);
        settle();
        assertTrue(hasText(window, "CNPJ"));
        assertTrue(hasText(window, "Encarregado de dados (LGPD)"));
        assertTrue(hasText(window, "Política de Cancelamento e Reembolso"));
        screenshot(main, "sobre");
        assertTrue(clickCard(main.findViewById(R.id.container), "Aviso de Privacidade"));
        // A MainActivity também abre o pedido de permissão de notificações; procura a política.
        Intent legal = null;
        for (Intent next; (next = shadowOf(main).getNextStartedActivity()) != null; ) {
            if (next.getComponent() != null && LegalActivity.class.getName()
                    .equals(next.getComponent().getClassName())) {
                legal = next;
            }
        }
        assertNotNull("tela do Aviso de Privacidade", legal);
        assertEquals(LegalDocs.PRIVACY, legal.getStringExtra(LegalActivity.EXTRA_DOCUMENT));
        assertFalse(hasText(window, "Marcar como pago"));
    }

    private static <T> T db(Callable<T> query) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            return executor.submit(query).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        } finally {
            executor.shutdown();
        }
    }

    private <T extends Activity> T open(Class<T> type, Intent intent) {
        T activity = Robolectric.buildActivity(type, intent).setup().get();
        settle();
        return activity;
    }

    private void save(ScreenActivity activity) {
        invoke(activity, "Salvar");
    }

    private void invoke(ScreenActivity activity, String action) {
        Menu menu = activity.toolbar.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            if (action.contentEquals(menu.getItem(i).getTitle())) {
                ((MenuItemImpl) menu.getItem(i)).invoke();
            }
        }
        settle();
    }

    private static Intent lastStarted(Activity activity) {
        Intent last = null;
        for (Intent next; (next = shadowOf(activity).getNextStartedActivity()) != null; ) {
            last = next;
        }
        assertNotNull("nenhuma tela foi aberta", last);
        return last;
    }

    /** Marca o aceite do termo e a autorização de tratamento de dados (LGPD). */
    private static void acceptTerms(Activity activity) {
        for (MaterialCheckBox box : checkBoxes(root(activity))) {
            box.setChecked(true);
        }
    }

    private static java.util.List<MaterialCheckBox> checkBoxes(View view) {
        java.util.List<MaterialCheckBox> found = new java.util.ArrayList<>();
        collectCheckBoxes(view, found);
        return found;
    }

    private static void collectCheckBoxes(View view, java.util.List<MaterialCheckBox> found) {
        if (view instanceof MaterialCheckBox && view.isShown()) {
            found.add((MaterialCheckBox) view);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectCheckBoxes(group.getChildAt(i), found);
            }
        }
    }

    private static View clickableAncestor(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) {
            for (View parent = view; parent != null; parent = (View) parent.getParent()) {
                if (parent.isClickable()) {
                    return parent;
                }
                if (!(parent.getParent() instanceof View)) {
                    break;
                }
            }
            return null;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = clickableAncestor(group.getChildAt(i), text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void settle() {
        for (int i = 0; i < 20; i++) {
            shadowOf(Looper.getMainLooper()).idle();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static View root(Activity activity) {
        return activity.getWindow().getDecorView();
    }

    private void setField(Activity activity, String label, String value) {
        TextInputLayout layout = field(root(activity), label);
        assertNotNull(label, layout);
        layout.getEditText().setText(value);
    }

    private static TextInputLayout field(View view, String label) {
        if (view instanceof TextInputLayout
                && label.contentEquals(((TextInputLayout) view).getHint())) {
            return (TextInputLayout) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextInputLayout found = field(group.getChildAt(i), label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Chip chip(Activity activity, String text, int skip) {
        int[] remaining = {skip};
        Chip found = findChip(root(activity), text, remaining);
        assertNotNull(text, found);
        return found;
    }

    private static Chip findChip(View view, String text, int[] remaining) {
        if (view instanceof Chip && text.contentEquals(((Chip) view).getText())) {
            return remaining[0]-- == 0 ? (Chip) view : null;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Chip found = findChip(group.getChildAt(i), text, remaining);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends View> T first(View view, Class<T> type) {
        if (type.isInstance(view)) {
            return (T) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                T found = first(group.getChildAt(i), type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean hasText(View view, String text) {
        if (view.getVisibility() != View.VISIBLE) {
            return false;
        }
        if (view instanceof TextView && ((TextView) view).getText().toString().contains(text)) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (hasText(group.getChildAt(i), text)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Toca no cartão (o primeiro ancestral clicável) que contém o texto. */
    private static boolean clickCard(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) {
            for (View parent = view; parent != null; parent = (View) parent.getParent()) {
                if (parent.isClickable()) {
                    return parent.performClick();
                }
                if (!(parent.getParent() instanceof View)) {
                    break;
                }
            }
            return false;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (clickCard(group.getChildAt(i), text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void screenshot(Activity activity, String name) {
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));
        View view = root(activity);
        view.measure(View.MeasureSpec.makeMeasureSpec(SCREEN_WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(SCREEN_HEIGHT, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        Bitmap bitmap = Bitmap.createBitmap(SCREEN_WIDTH, SCREEN_HEIGHT, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        File folder = new File("build/screens");
        folder.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(folder, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
