package com.gtechapps.ramjankimandir.ui.common;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.data.OperationCallback;
import com.gtechapps.ramjankimandir.data.RentalRepository;
import com.gtechapps.ramjankimandir.data.RepositoryCallback;
import com.gtechapps.ramjankimandir.model.BillingMonth;
import com.gtechapps.ramjankimandir.util.MonthUtils;

import java.util.List;

public class PaymentSetupActivity extends AppCompatActivity {

    private final RentalRepository repository = new RentalRepository();
    private ChipGroup chipGroup;
    private TextView statusView;
    private MaterialButton setupRoomButton;
    private MaterialButton setupGarageButton;
    private String selectedMonthId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_setup);

        MaterialToolbar toolbar = findViewById(R.id.paymentSetupToolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        chipGroup = findViewById(R.id.paymentMonthChips);
        statusView = findViewById(R.id.paymentSetupStatus);
        setupRoomButton = findViewById(R.id.setupRoomButton);
        setupGarageButton = findViewById(R.id.setupGarageButton);

        setupRoomButton.setOnClickListener(v -> runSetup(true));
        setupGarageButton.setOnClickListener(v -> runSetup(false));

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
                chipGroup.removeAllViews();
                if (data.isEmpty()) {
                    selectedMonthId = "";
                    statusView.setText("No billing month created yet.");
                    setSetupButtonsEnabled(false);
                    return;
                }
                setSetupButtonsEnabled(true);
                selectedMonthId = resolveDefaultMonth(data);
                for (BillingMonth month : data) {
                    Chip chip = new Chip(PaymentSetupActivity.this);
                    chip.setId(View.generateViewId());
                    chip.setText(month.month_name);
                    chip.setCheckable(true);
                    chip.setChecked(month.month_id.equals(selectedMonthId));
                    chip.setOnClickListener(v -> {
                        selectedMonthId = month.month_id;
                        statusView.setText("Selected month: " + month.month_name);
                    });
                    chipGroup.addView(chip);
                }
                statusView.setText("Selected month: " + MonthUtils.displayName(selectedMonthId));
            }

            @Override
            public void onError(String message) {
                statusView.setText(message);
                setSetupButtonsEnabled(false);
            }
        });
    }

    private String resolveDefaultMonth(List<BillingMonth> months) {
        String current = MonthUtils.currentMonthId();
        for (BillingMonth month : months) {
            if (current.equals(month.month_id)) {
                return month.month_id;
            }
        }
        return months.get(months.size() - 1).month_id;
    }

    private void runSetup(boolean roomSetup) {
        if (selectedMonthId.isEmpty()) {
            Snackbar.make(chipGroup, "Please select a month first.", Snackbar.LENGTH_LONG).show();
            return;
        }
        setSetupButtonsEnabled(false);
        OperationCallback callback = new OperationCallback() {
            @Override
            public void onComplete(boolean success, String message) {
                setSetupButtonsEnabled(true);
                statusView.setText(message);
                Snackbar.make(chipGroup, message, Snackbar.LENGTH_LONG).show();
            }
        };
        if (roomSetup) {
            repository.setupRoomPayments(selectedMonthId, callback);
        } else {
            repository.setupGaragePayments(selectedMonthId, callback);
        }
    }

    private void setSetupButtonsEnabled(boolean enabled) {
        if (setupRoomButton != null) {
            setupRoomButton.setEnabled(enabled);
        }
        if (setupGarageButton != null) {
            setupGarageButton.setEnabled(enabled);
        }
    }
}
