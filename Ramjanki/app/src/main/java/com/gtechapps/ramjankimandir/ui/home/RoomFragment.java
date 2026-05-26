package com.gtechapps.ramjankimandir.ui.home;

import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.data.OperationCallback;
import com.gtechapps.ramjankimandir.data.RentalRepository;
import com.gtechapps.ramjankimandir.data.RepositoryCallback;
import com.gtechapps.ramjankimandir.model.BillingMonth;
import com.gtechapps.ramjankimandir.model.BillingStatus;
import com.gtechapps.ramjankimandir.model.RentalPerson;
import com.gtechapps.ramjankimandir.model.Room;
import com.gtechapps.ramjankimandir.model.RoomPayment;
import com.gtechapps.ramjankimandir.ui.common.MonthlyDetailActivity;
import com.gtechapps.ramjankimandir.ui.common.PaymentSetupActivity;
import com.gtechapps.ramjankimandir.ui.common.PaymentSummaryActivity;
import com.gtechapps.ramjankimandir.ui.common.RoomPaymentAdapter;
import com.gtechapps.ramjankimandir.util.BillingCalculator;
import com.gtechapps.ramjankimandir.util.MonthUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomFragment extends Fragment implements RoomPaymentAdapter.RoomPaymentListener {

    private final RentalRepository repository = new RentalRepository();

    private final Map<String, Room> roomMap = new HashMap<>();
    private final Map<String, RentalPerson> rentalMap = new HashMap<>();

    private ChipGroup chipGroup;
    private SwipeRefreshLayout refreshLayout;
    private TextView emptyState;
    private RoomPaymentAdapter adapter;
    private String selectedMonthId = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collection, container, false);
        MaterialButton buttonPrimary = view.findViewById(R.id.buttonPrimary);
        buttonPrimary.setText(R.string.setup_payment);
        buttonPrimary.setOnClickListener(v -> {
            Context context = getContext();
            if (context != null) {
                startActivity(new Intent(context, PaymentSetupActivity.class));
            }
        });
        MaterialButton buttonSecondary = view.findViewById(R.id.buttonSecondary);
        buttonSecondary.setText(R.string.view_payment_details);
        buttonSecondary.setOnClickListener(v -> {
            Context context = getContext();
            if (context != null) {
                startActivity(PaymentSummaryActivity.createIntent(context, RentalRepository.TYPE_ROOM));
            }
        });

        chipGroup = view.findViewById(R.id.chipGroupMonths);
        refreshLayout = view.findViewById(R.id.swipeRefresh);
        emptyState = view.findViewById(R.id.emptyState);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerRecords);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RoomPaymentAdapter(this);
        recyclerView.setAdapter(adapter);
        refreshLayout.setOnRefreshListener(this::loadMonthsAndPayments);
        loadMonthsAndPayments();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMonthsAndPayments();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        chipGroup = null;
        refreshLayout = null;
        emptyState = null;
        adapter = null;
    }

    private void loadMonthsAndPayments() {
        if (!isUiReady()) {
            return;
        }
        refreshLayout.setRefreshing(true);
        repository.loadMonths(new RepositoryCallback<List<BillingMonth>>() {
            @Override
            public void onSuccess(List<BillingMonth> months) {
                if (!isUiReady()) {
                    return;
                }
                if (months.isEmpty()) {
                    refreshLayout.setRefreshing(false);
                    chipGroup.removeAllViews();
                    emptyState.setVisibility(View.VISIBLE);
                    emptyState.setText("Create a month and run payment setup to see room billing.");
                    adapter.submit(java.util.Collections.emptyList(), roomMap, rentalMap);
                    return;
                }
                if (TextUtils.isEmpty(selectedMonthId)) {
                    selectedMonthId = months.get(months.size() - 1).month_id;
                } else {
                    boolean found = false;
                    for (BillingMonth month : months) {
                        if (selectedMonthId.equals(month.month_id)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        selectedMonthId = months.get(months.size() - 1).month_id;
                    }
                }
                renderMonthChips(months);
                loadReferenceData();
            }

            @Override
            public void onError(String message) {
                if (!isUiReady()) {
                    return;
                }
                refreshLayout.setRefreshing(false);
                emptyState.setVisibility(View.VISIBLE);
                emptyState.setText(message);
            }
        });
    }

    private void renderMonthChips(List<BillingMonth> months) {
        Context context = getContext();
        if (context == null || chipGroup == null) {
            return;
        }
        chipGroup.removeAllViews();
        for (BillingMonth month : months) {
            Chip chip = new Chip(context);
            chip.setId(View.generateViewId());
            chip.setText(month.month_name);
            chip.setCheckable(true);
            chip.setChecked(month.month_id.equals(selectedMonthId));
            chip.setOnClickListener(v -> {
                if (!isUiReady()) {
                    return;
                }
                selectedMonthId = month.month_id;
                loadReferenceData();
            });
            chipGroup.addView(chip);
        }
    }

    private void loadReferenceData() {
        if (!isUiReady()) {
            return;
        }
        if (TextUtils.isEmpty(selectedMonthId)) {
            refreshLayout.setRefreshing(false);
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("Create a month and run payment setup to see room billing.");
            adapter.submit(java.util.Collections.emptyList(), roomMap, rentalMap);
            return;
        }
        repository.loadRooms(new RepositoryCallback<List<Room>>() {
            @Override
            public void onSuccess(List<Room> rooms) {
                if (!isUiReady()) {
                    return;
                }
                roomMap.clear();
                for (Room room : rooms) {
                    roomMap.put(String.valueOf(room.room_number), room);
                }
                repository.loadRentals(new RepositoryCallback<List<RentalPerson>>() {
                    @Override
                    public void onSuccess(List<RentalPerson> rentals) {
                        if (!isUiReady()) {
                            return;
                        }
                        rentalMap.clear();
                        for (RentalPerson rental : rentals) {
                            rentalMap.put(rental.id, rental);
                        }
                        repository.loadRoomPayments(selectedMonthId, new RepositoryCallback<List<RoomPayment>>() {
                            @Override
                            public void onSuccess(List<RoomPayment> payments) {
                                if (!isUiReady()) {
                                    return;
                                }
                                refreshLayout.setRefreshing(false);
                                emptyState.setVisibility(payments.isEmpty() ? View.VISIBLE : View.GONE);
                                emptyState.setText(payments.isEmpty()
                                        ? "No room payments found for " + MonthUtils.displayName(selectedMonthId)
                                        : "");
                                adapter.submit(payments, roomMap, rentalMap);
                            }

                            @Override
                            public void onError(String message) {
                                if (!isUiReady()) {
                                    return;
                                }
                                refreshLayout.setRefreshing(false);
                                emptyState.setVisibility(View.VISIBLE);
                                emptyState.setText(message);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        if (!isUiReady()) {
                            return;
                        }
                        refreshLayout.setRefreshing(false);
                        emptyState.setVisibility(View.VISIBLE);
                        emptyState.setText(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (!isUiReady()) {
                    return;
                }
                refreshLayout.setRefreshing(false);
                emptyState.setVisibility(View.VISIBLE);
                emptyState.setText(message);
            }
        });
    }

    @Override
    public void onSaveCurrentUnit(RoomPayment payment, String currentUnit) {
        if (TextUtils.isEmpty(currentUnit)) {
            showMessage("Current unit is required.");
            return;
        }
        try {
            double unit = Double.parseDouble(currentUnit);
            if (unit < payment.bill_start_unit) {
                showMessage("Current unit cannot be less than start unit.");
                return;
            }
            Room room = roomMap.get(payment.room_id);
            double unitCharge = room == null ? 0D : room.bijli_charge;
            payment.bijli_charge = unitCharge;
            payment.bill_current_unit = currentUnit;
            payment.bijlibill = BillingCalculator.calculateElectricityAmount(payment.bill_start_unit, unit, unitCharge, 0D);
            payment.bijlibill_status = BillingStatus.CALCULATED;
            repository.updateRoomPayment(selectedMonthId, payment, reloadCallback());
        } catch (NumberFormatException exception) {
            showMessage("Current unit must be numeric.");
        }
    }

    @Override
    public void onRentReceived(RoomPayment payment) {
        payment.rent_received = true;
        payment.rent_status = BillingStatus.RECEIVED;
        markCompleteWhenRoomCollectionDone(payment);
        repository.updateRoomPayment(selectedMonthId, payment, reloadCallback());
    }

    @Override
    public void onElectricityReceived(RoomPayment payment) {
        payment.bijlibill_received = true;
        payment.bijlibill_status = BillingStatus.RECEIVED;
        markCompleteWhenRoomCollectionDone(payment);
        repository.updateRoomPayment(selectedMonthId, payment, reloadCallback());
    }

    @Override
    public void onOpenDetail(RoomPayment payment) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        startActivity(MonthlyDetailActivity.createIntent(context, RentalRepository.TYPE_ROOM, selectedMonthId, payment.room_id));
    }

    private OperationCallback reloadCallback() {
        return new OperationCallback() {
            @Override
            public void onComplete(boolean success, String message) {
                showMessage(message);
                if (success && isUiReady()) {
                    loadReferenceData();
                }
            }
        };
    }

    private void markCompleteWhenRoomCollectionDone(RoomPayment payment) {
        if (payment.rent_received
                && payment.bijlibill_received
                && !TextUtils.isEmpty(payment.bill_current_unit)) {
            payment.status = BillingStatus.COMPLETE;
        }
    }

    private boolean isUiReady() {
        return isAdded() && getView() != null && chipGroup != null && refreshLayout != null && emptyState != null && adapter != null;
    }

    private void showMessage(String message) {
        View view = getView();
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}
