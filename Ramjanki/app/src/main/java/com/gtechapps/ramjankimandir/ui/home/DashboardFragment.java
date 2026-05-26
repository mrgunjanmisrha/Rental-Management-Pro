package com.gtechapps.ramjankimandir.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.data.RentalRepository;
import com.gtechapps.ramjankimandir.data.RepositoryCallback;
import com.gtechapps.ramjankimandir.model.Garage;
import com.gtechapps.ramjankimandir.model.GaragePayment;
import com.gtechapps.ramjankimandir.model.Room;
import com.gtechapps.ramjankimandir.model.RoomPayment;
import com.gtechapps.ramjankimandir.ui.common.MonthManagementActivity;
import com.gtechapps.ramjankimandir.ui.common.RecordsActivity;
import com.gtechapps.ramjankimandir.util.MonthUtils;

import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private final RentalRepository repository = new RentalRepository();

    private TextView currentMonthView;
    private TextView roomTotalView;
    private TextView roomOccupiedView;
    private TextView roomEmptyView;
    private TextView garageTotalView;
    private TextView garageOccupiedView;
    private TextView garageEmptyView;
    private TextView paymentTotalView;
    private TextView paymentReceivedView;
    private TextView paymentPendingView;
    private SwipeRefreshLayout refreshLayout;

    private String currentMonthId = MonthUtils.currentMonthId();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        currentMonthView = view.findViewById(R.id.summaryCurrentMonth);
        roomTotalView = bindStat(view, R.id.roomTotalCard, "Total rooms");
        roomOccupiedView = bindStat(view, R.id.roomOccupiedCard, "Occupied rooms");
        roomEmptyView = bindStat(view, R.id.roomEmptyCard, "Empty rooms");
        garageTotalView = bindStat(view, R.id.garageTotalCard, "Total garage");
        garageOccupiedView = bindStat(view, R.id.garageOccupiedCard, "Occupied garage");
        garageEmptyView = bindStat(view, R.id.garageEmptyCard, "Empty garage");
        paymentTotalView = bindStat(view, R.id.paymentTotalCard, "Total");
        paymentReceivedView = bindStat(view, R.id.paymentReceivedCard, "Received");
        paymentPendingView = bindStat(view, R.id.paymentPendingCard, "Pending");

        refreshLayout = view.findViewById(R.id.dashboardRefresh);
        refreshLayout.setOnRefreshListener(this::loadSummary);

        view.findViewById(R.id.openRecordsButton).setOnClickListener(v -> {
            Context context = getContext();
            if (context != null) {
                startActivity(new Intent(context, RecordsActivity.class));
            }
        });
        view.findViewById(R.id.openMonthsButton).setOnClickListener(v -> {
            Context context = getContext();
            if (context != null) {
                startActivity(new Intent(context, MonthManagementActivity.class));
            }
        });

        loadSummary();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSummary();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        currentMonthView = null;
        roomTotalView = null;
        roomOccupiedView = null;
        roomEmptyView = null;
        garageTotalView = null;
        garageOccupiedView = null;
        garageEmptyView = null;
        paymentTotalView = null;
        paymentReceivedView = null;
        paymentPendingView = null;
        refreshLayout = null;
    }

    private TextView bindStat(View root, int cardId, String label) {
        View card = root.findViewById(cardId);
        ((TextView) card.findViewById(R.id.statLabel)).setText(label);
        return card.findViewById(R.id.statValue);
    }

    private void loadSummary() {
        if (!isUiReady()) {
            return;
        }
        currentMonthId = MonthUtils.currentMonthId();
        currentMonthView.setText("Current month: " + MonthUtils.displayName(currentMonthId));
        refreshLayout.setRefreshing(true);
        repository.loadRooms(new RepositoryCallback<List<Room>>() {
            @Override
            public void onSuccess(List<Room> rooms) {
                if (!isUiReady()) {
                    return;
                }
                bindRoomSummary(rooms);
                loadGarageSummary();
            }

            @Override
            public void onError(String message) {
                if (!isUiReady()) {
                    return;
                }
                bindRoomSummary(java.util.Collections.emptyList());
                loadGarageSummary();
            }
        });
    }

    private void bindRoomSummary(List<Room> rooms) {
        int occupiedCount = 0;
        int emptyCount = 0;
        for (Room room : rooms) {
            if (!room.room_active) {
                continue;
            }
            if (isBlank(room.rental_id)) {
                emptyCount++;
            } else {
                occupiedCount++;
            }
        }
        roomTotalView.setText(String.valueOf(rooms.size()));
        roomOccupiedView.setText(String.valueOf(occupiedCount));
        roomEmptyView.setText(String.valueOf(emptyCount));
    }

    private void loadGarageSummary() {
        repository.loadGarages(new RepositoryCallback<List<Garage>>() {
            @Override
            public void onSuccess(List<Garage> garages) {
                if (!isUiReady()) {
                    return;
                }
                bindGarageSummary(garages);
                loadCurrentMonthPayments();
            }

            @Override
            public void onError(String message) {
                if (!isUiReady()) {
                    return;
                }
                bindGarageSummary(java.util.Collections.emptyList());
                loadCurrentMonthPayments();
            }
        });
    }

    private void bindGarageSummary(List<Garage> garages) {
        int occupiedCount = 0;
        int emptyCount = 0;
        for (Garage garage : garages) {
            if (!garage.is_active) {
                continue;
            }
            if (isBlank(garage.vehicle_owner_id)) {
                emptyCount++;
            } else {
                occupiedCount++;
            }
        }
        garageTotalView.setText(String.valueOf(garages.size()));
        garageOccupiedView.setText(String.valueOf(occupiedCount));
        garageEmptyView.setText(String.valueOf(emptyCount));
    }

    private void loadCurrentMonthPayments() {
        repository.loadRoomPayments(currentMonthId, new RepositoryCallback<List<RoomPayment>>() {
            @Override
            public void onSuccess(List<RoomPayment> roomPayments) {
                if (!isUiReady()) {
                    return;
                }
                CollectionSummary summary = new CollectionSummary();
                addRoomPayments(summary, roomPayments);
                loadGaragePayments(summary);
            }

            @Override
            public void onError(String message) {
                if (!isUiReady()) {
                    return;
                }
                loadGaragePayments(new CollectionSummary());
            }
        });
    }

    private void loadGaragePayments(CollectionSummary summary) {
        repository.loadGaragePayments(currentMonthId, new RepositoryCallback<List<GaragePayment>>() {
            @Override
            public void onSuccess(List<GaragePayment> garagePayments) {
                if (!isUiReady()) {
                    return;
                }
                addGaragePayments(summary, garagePayments);
                bindPaymentSummary(summary);
                stopRefreshing();
            }

            @Override
            public void onError(String message) {
                if (!isUiReady()) {
                    return;
                }
                bindPaymentSummary(summary);
                stopRefreshing();
            }
        });
    }

    private void addRoomPayments(CollectionSummary summary, List<RoomPayment> payments) {
        for (RoomPayment payment : payments) {
            double roomPart = payment.room_rent + payment.other_charge;
            double powerPart = payment.bijlibill;
            summary.total += roomPart + powerPart;
            if (payment.rent_received) {
                summary.received += roomPart;
            }
            if (payment.bijlibill_received) {
                summary.received += powerPart;
            }
        }
    }

    private void addGaragePayments(CollectionSummary summary, List<GaragePayment> payments) {
        for (GaragePayment payment : payments) {
            summary.total += payment.charge;
            if (payment.charge_received) {
                summary.received += payment.charge;
            }
        }
    }

    private void bindPaymentSummary(CollectionSummary summary) {
        double pending = Math.max(0D, summary.total - summary.received);
        paymentTotalView.setText(formatCurrency(summary.total));
        paymentReceivedView.setText(formatCurrency(summary.received));
        paymentPendingView.setText(formatCurrency(pending));
    }

    private String formatCurrency(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.US, "Rs %.0f", value);
        }
        return String.format(Locale.US, "Rs %.2f", value);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void stopRefreshing() {
        if (refreshLayout != null) {
            refreshLayout.setRefreshing(false);
        }
    }

    private boolean isUiReady() {
        return isAdded()
                && getView() != null
                && currentMonthView != null
                && roomTotalView != null
                && roomOccupiedView != null
                && roomEmptyView != null
                && garageTotalView != null
                && garageOccupiedView != null
                && garageEmptyView != null
                && paymentTotalView != null
                && paymentReceivedView != null
                && paymentPendingView != null
                && refreshLayout != null;
    }

    private static class CollectionSummary {
        double total;
        double received;
    }
}
