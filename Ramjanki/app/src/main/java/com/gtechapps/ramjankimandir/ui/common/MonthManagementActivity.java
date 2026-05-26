package com.gtechapps.ramjankimandir.ui.common;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.data.OperationCallback;
import com.gtechapps.ramjankimandir.data.RentalRepository;
import com.gtechapps.ramjankimandir.data.RepositoryCallback;
import com.gtechapps.ramjankimandir.model.BillingMonth;
import com.gtechapps.ramjankimandir.model.RecordItem;

import java.util.ArrayList;
import java.util.List;

public class MonthManagementActivity extends AppCompatActivity {

    private final RentalRepository repository = new RentalRepository();
    private final RecordAdapter adapter = new RecordAdapter(null, "");
    private TextView emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_month_management);

        MaterialToolbar toolbar = findViewById(R.id.monthToolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        emptyState = findViewById(R.id.monthEmptyState);
        RecyclerView recyclerView = findViewById(R.id.monthRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        findViewById(R.id.createMonthButton).setOnClickListener(v ->
                repository.createNextMonth(new OperationCallback() {
                    @Override
                    public void onComplete(boolean success, String message) {
                        Snackbar.make(recyclerView, message, Snackbar.LENGTH_LONG).show();
                        loadMonths();
                    }
                }));
        loadMonths();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMonths();
    }

    private void loadMonths() {
        repository.loadMonths(new RepositoryCallback<List<BillingMonth>>() {
            @Override
            public void onSuccess(List<BillingMonth> data) {
                List<RecordItem> items = new ArrayList<>();
                for (BillingMonth month : data) {
                    items.add(new RecordItem(month.month_id, month.month_name, month.month_id, month.createAt, ""));
                }
                adapter.submitItems(items);
                emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                emptyState.setVisibility(View.VISIBLE);
                emptyState.setText(message);
                adapter.submitItems(java.util.Collections.emptyList());
            }
        });
    }
}
