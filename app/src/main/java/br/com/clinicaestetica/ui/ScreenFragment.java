package br.com.clinicaestetica.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import br.com.clinicaestetica.data.AppDao;
import br.com.clinicaestetica.data.AppDatabase;

/** Seção principal do app; recarrega os dados sempre que volta a aparecer. */
abstract class ScreenFragment extends Fragment {
    protected LinearLayout column;
    protected AppDao dao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        dao = AppDatabase.dao(requireContext());
        ScrollView scroll = new ScrollView(requireContext());
        column = Ui.page(requireContext());
        scroll.addView(column);
        build();
        return scroll;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    protected abstract void build();

    protected abstract void refresh();
}
