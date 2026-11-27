package br.com.clinicaestetica.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import br.com.clinicaestetica.R;
import br.com.clinicaestetica.data.AppDao;
import br.com.clinicaestetica.data.AppDatabase;

/** Tela com barra superior e conteúdo rolável. */
abstract class ScreenActivity extends AppCompatActivity {
    protected LinearLayout root;
    protected LinearLayout content;
    protected ScrollView scroll;
    protected MaterialToolbar toolbar;
    protected AppDao dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dao = AppDatabase.dao(this);

        root = Ui.column(this);
        root.setFitsSystemWindows(true);

        toolbar = new MaterialToolbar(this);
        toolbar.setBackgroundResource(R.color.paper);
        toolbar.setTitleTextAppearance(this, R.style.TextAppearance_Clinica_ToolbarTitle);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationContentDescription(R.string.back);
        toolbar.setNavigationOnClickListener(view -> finish());
        root.addView(toolbar);

        scroll = new ScrollView(this);
        content = Ui.page(this);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1));
        setContentView(root);
    }

    protected void addAction(String title, Runnable action) {
        MenuItem item = toolbar.getMenu().add(title);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setOnMenuItemClickListener(menuItem -> {
            action.run();
            return true;
        });
    }
}
