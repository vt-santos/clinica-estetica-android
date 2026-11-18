package br.com.clinicaestetica.ui;

import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import br.com.clinicaestetica.data.Async;
import br.com.clinicaestetica.data.PaymentRow;

public class PaymentsFragment extends ScreenFragment {
    private TextView pending;
    private LinearLayout list;

    @Override
    protected void build() {
        Ui.title(column, "Pagamentos");
        pending = Ui.text(column, "", 16);
        Ui.button(column, "Novo pagamento", view ->
                startActivity(new Intent(requireContext(), PaymentFormActivity.class)));
        Ui.text(column, "", 8);
        list = Ui.column(requireContext());
        column.addView(list);
    }

    @Override
    protected void refresh() {
        Async.load(() -> {
            Object[] result = {dao.pendingCents(), dao.allPayments()};
            return result;
        }, result -> {
            @SuppressWarnings("unchecked")
            List<PaymentRow> rows = (List<PaymentRow>) result[1];
            render((long) result[0], rows);
        });
    }

    private void render(long pendingCents, List<PaymentRow> rows) {
        if (!isAdded()) {
            return;
        }
        pending.setText("A receber: " + Fmt.money(pendingCents));
        list.removeAllViews();
        if (rows.isEmpty()) {
            Ui.text(list, "Nenhum pagamento registrado.", 14);
        }
        for (PaymentRow row : rows) {
            Cards.payment(list, row, this::refresh);
        }
    }
}
