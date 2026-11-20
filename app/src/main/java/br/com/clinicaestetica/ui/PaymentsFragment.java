package br.com.clinicaestetica.ui;

import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.data.PaymentRow;

public class PaymentsFragment extends ScreenFragment {
    private TextView received;
    private LinearLayout list;

    @Override
    protected void build() {
        Ui.title(column, "Pagamentos");
        received = Ui.text(column, "", 16);
        Ui.button(column, "Novo pagamento", view ->
                startActivity(new Intent(requireContext(), PaymentFormActivity.class)));
        list = Ui.column(requireContext());
        column.addView(list);
    }

    @Override
    protected void refresh() {
        Async.load(() -> {
            long month = Fmt.startOfMonth(System.currentTimeMillis());
            Object[] result = {dao.receivedCents(month, Fmt.plusMonths(month, 1)),
                    dao.allPayments()};
            return result;
        }, result -> {
            @SuppressWarnings("unchecked")
            List<PaymentRow> rows = (List<PaymentRow>) result[1];
            render((long) result[0], rows);
        });
    }

    private void render(long receivedCents, List<PaymentRow> rows) {
        if (!isAdded()) {
            return;
        }
        received.setText("Recebido no mês: " + Fmt.money(receivedCents));
        list.removeAllViews();
        if (rows.isEmpty()) {
            Ui.muted(list, "Nenhum pagamento registrado.");
        }
        for (PaymentRow row : rows) {
            Cards.payment(list, row);
        }
    }
}
