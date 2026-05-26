package com.gtechapps.ramjankimandir.ui.common;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.data.RentalRepository;
import com.gtechapps.ramjankimandir.data.RepositoryCallback;
import com.gtechapps.ramjankimandir.model.BillingMonth;
import com.gtechapps.ramjankimandir.model.GaragePayment;
import com.gtechapps.ramjankimandir.model.RoomPayment;
import com.gtechapps.ramjankimandir.util.MonthUtils;

import java.util.List;
import java.util.Locale;

public class PaymentSummaryActivity extends AppCompatActivity {

    private static final String EXTRA_TYPE = "type";

    public static Intent createIntent(Context context, String type) {
        Intent intent = new Intent(context, PaymentSummaryActivity.class);
        intent.putExtra(EXTRA_TYPE, type);
        return intent;
    }

    private final RentalRepository repository = new RentalRepository();

    private ChipGroup chipGroup;
    private TextView headingView;
    private TextView subtitleView;
    private TextView emptyStateView;
    private View summaryContentView;
    private TextView totalPaymentView;
    private TextView receivedPaymentView;
    private TextView pendingPaymentView;
    private LinearLayout roomBreakdownSection;
    private TextView roomBreakdownValuesView;

    private String type = RentalRepository.TYPE_ROOM;
    private String selectedMonthId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_summary);

        MaterialToolbar toolbar = findViewById(R.id.paymentSummaryToolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        type = getIntent().getStringExtra(EXTRA_TYPE);
        if (!RentalRepository.TYPE_GARAGE.equals(type)) {
            type = RentalRepository.TYPE_ROOM;
        }

        bindViews();
        headingView.setText(RentalRepository.TYPE_ROOM.equals(type)
                ? "Room Payment Details"
                : "Garage Payment Details");
        loadMonths();
    }

    private void bindViews() {
        chipGroup = findViewById(R.id.chipGroupMonths);
        headingView = findViewById(R.id.summaryHeading);
        subtitleView = findViewById(R.id.summarySubtitle);
        emptyStateView = findViewById(R.id.emptyState);
        summaryContentView = findViewById(R.id.summaryContent);
        totalPaymentView = findViewById(R.id.totalPaymentValue);
        receivedPaymentView = findViewById(R.id.receivedPaymentValue);
        pendingPaymentView = findViewById(R.id.pendingPaymentValue);
        roomBreakdownSection = findViewById(R.id.roomBreakdownSection);
        roomBreakdownValuesView = findViewById(R.id.roomBreakdownValues);
    }

    private void loadMonths() {
        repository.loadMonths(new RepositoryCallback<List<BillingMonth>>() {
            @Override
            public void onSuccess(List<BillingMonth> months) {
                if (months.isEmpty()) {
                    showEmpty("Create a month first to view payment details.");
                    return;
                }
                selectedMonthId = resolveDefaultMonth(months);
                renderMonthChips(months);
                loadSelectedSummary();
            }

            @Override
            public void onError(String message) {
                showEmpty(message);
            }
        });
    }

    private String resolveDefaultMonth(List<BillingMonth> months) {
        String currentMonthId = MonthUtils.currentMonthId();
        for (BillingMonth month : months) {
            if (currentMonthId.equals(month.month_id)) {
                return currentMonthId;
            }
        }
        return months.get(months.size() - 1).month_id;
    }

    private void renderMonthChips(List<BillingMonth> months) {
        chipGroup.removeAllViews();
        for (BillingMonth month : months) {
            Chip chip = new Chip(this);
            chip.setId(View.generateViewId());
            chip.setText(month.month_name);
            chip.setCheckable(true);
            chip.setChecked(month.month_id.equals(selectedMonthId));
            chip.setOnClickListener(v -> {
                selectedMonthId = month.month_id;
                loadSelectedSummary();
            });
            chipGroup.addView(chip);
        }
    }

    private void loadSelectedSummary() {
        if (TextUtils.isEmpty(selectedMonthId)) {
            showEmpty("Select a month to view payment details.");
            return;
        }
        subtitleView.setText(MonthUtils.displayName(selectedMonthId));
        if (RentalRepository.TYPE_ROOM.equals(type)) {
            loadRoomSummary();
        } else {
            loadGarageSummary();
        }
    }

    private void loadRoomSummary() {
        repository.loadRoomPayments(selectedMonthId, new RepositoryCallback<List<RoomPayment>>() {
            @Override
            public void onSuccess(List<RoomPayment> payments) {
                RoomSummary summary = calculateRoomSummary(payments);
                showContent();
                roomBreakdownSection.setVisibility(View.VISIBLE);
                totalPaymentView.setText(formatCurrency(summary.totalPayment));
                receivedPaymentView.setText(formatCurrency(summary.receivedPayment));
                pendingPaymentView.setText(formatCurrency(summary.pendingPayment));
                roomBreakdownValuesView.setText(
                        "Total room payment: " + formatCurrency(summary.totalRoomPayment)
                                + "\nRoom received payment: " + formatCurrency(summary.roomReceivedPayment)
                                + "\nRoom pending payment: " + formatCurrency(summary.roomPendingPayment)
                                + "\n\nTotal power payment: " + formatCurrency(summary.totalPowerPayment)
                                + "\nPower received payment: " + formatCurrency(summary.powerReceivedPayment)
                                + "\nPower pending payment: " + formatCurrency(summary.powerPendingPayment)
                );
            }

            @Override
            public void onError(String message) {
                showEmpty(message);
            }
        });
    }

    private void loadGarageSummary() {
        repository.loadGaragePayments(selectedMonthId, new RepositoryCallback<List<GaragePayment>>() {
            @Override
            public void onSuccess(List<GaragePayment> payments) {
                GarageSummary summary = calculateGarageSummary(payments);
                showContent();
                roomBreakdownSection.setVisibility(View.GONE);
                totalPaymentView.setText(formatCurrency(summary.totalPayment));
                receivedPaymentView.setText(formatCurrency(summary.receivedPayment));
                pendingPaymentView.setText(formatCurrency(summary.pendingPayment));
            }

            @Override
            public void onError(String message) {
                showEmpty(message);
            }
        });
    }

    private RoomSummary calculateRoomSummary(List<RoomPayment> payments) {
        RoomSummary summary = new RoomSummary();
        for (RoomPayment payment : payments) {
            double roomPart = payment.room_rent + payment.other_charge;
            double powerPart = payment.bijlibill;
            summary.totalRoomPayment += roomPart;
            summary.totalPowerPayment += powerPart;
            if (payment.rent_received) {
                summary.roomReceivedPayment += roomPart;
            }
            if (payment.bijlibill_received) {
                summary.powerReceivedPayment += powerPart;
            }
        }
        summary.roomPendingPayment = Math.max(0D, summary.totalRoomPayment - summary.roomReceivedPayment);
        summary.powerPendingPayment = Math.max(0D, summary.totalPowerPayment - summary.powerReceivedPayment);
        summary.totalPayment = summary.totalRoomPayment + summary.totalPowerPayment;
        summary.receivedPayment = summary.roomReceivedPayment + summary.powerReceivedPayment;
        summary.pendingPayment = Math.max(0D, summary.totalPayment - summary.receivedPayment);
        return summary;
    }

    private GarageSummary calculateGarageSummary(List<GaragePayment> payments) {
        GarageSummary summary = new GarageSummary();
        for (GaragePayment payment : payments) {
            summary.totalPayment += payment.charge;
            if (payment.charge_received) {
                summary.receivedPayment += payment.charge;
            }
        }
        summary.pendingPayment = Math.max(0D, summary.totalPayment - summary.receivedPayment);
        return summary;
    }

    private void showContent() {
        emptyStateView.setVisibility(View.GONE);
        summaryContentView.setVisibility(View.VISIBLE);
    }

    private void showEmpty(String message) {
        emptyStateView.setText(message);
        emptyStateView.setVisibility(View.VISIBLE);
        summaryContentView.setVisibility(View.GONE);
    }

    private String formatCurrency(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.US, "Rs %.0f", value);
        }
        return String.format(Locale.US, "Rs %.2f", value);
    }

    private static class RoomSummary {
        double totalPayment;
        double receivedPayment;
        double pendingPayment;
        double totalRoomPayment;
        double roomReceivedPayment;
        double roomPendingPayment;
        double totalPowerPayment;
        double powerReceivedPayment;
        double powerPendingPayment;
    }

    private static class GarageSummary {
        double totalPayment;
        double receivedPayment;
        double pendingPayment;
    }
}
