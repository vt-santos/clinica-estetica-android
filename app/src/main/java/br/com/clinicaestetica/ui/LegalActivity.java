package br.com.clinicaestetica.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/** Mostra um texto legal (privacidade, termos, reembolso, cookies, acessibilidade). */
public class LegalActivity extends ScreenActivity {
    static final String EXTRA_DOCUMENT = "document";

    static Intent intent(Context context, String document) {
        return new Intent(context, LegalActivity.class).putExtra(EXTRA_DOCUMENT, document);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String key = getIntent().getStringExtra(EXTRA_DOCUMENT);
        if (key == null) {
            key = LegalDocs.PRIVACY;
        }
        String[] lines = LegalDocs.load(this, key).split("\n");
        toolbar.setTitle(lines[0].trim());
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("# ")) {
                Ui.section(content, line.substring(2));
            } else {
                Ui.text(content, line, 16);
            }
        }
        Ui.muted(content, "Modelo de texto. Precisa de revisão por advogado antes do uso oficial.");
    }
}
