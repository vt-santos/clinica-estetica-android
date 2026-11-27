package br.com.clinicaestetica.ui;

import java.util.Map;

/** Dados da empresa e acesso às políticas: privacidade, termos, reembolso, cookies e acessibilidade. */
public class AboutFragment extends ScreenFragment {
    @Override
    protected void build() {
        Ui.title(column, "Sobre e políticas");
        if (LegalDocs.hasPending(requireContext())) {
            Ui.text(column, "Atenção: ainda há dados da empresa ou regras das políticas entre "
                    + "colchetes. Eles precisam ser preenchidos antes de usar o app com "
                    + "pacientes.", 15);
        }

        Ui.section(column, "Dados da empresa");
        for (Map.Entry<String, String> entry : LegalDocs.company(requireContext()).entrySet()) {
            Ui.info(column, entry.getKey(), entry.getValue());
        }

        Ui.section(column, "Políticas e termos");
        for (String key : LegalDocs.ALL) {
            Ui.card(column, LegalDocs.title(requireContext(), key), null,
                    view -> startActivity(LegalActivity.intent(requireContext(), key)));
        }
        Ui.muted(column, "Versão dos textos: " + LegalDocs.version(requireContext()));
    }

    @Override
    protected void refresh() {
    }
}
