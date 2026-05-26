package com.gtechapps.ramjankimandir.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.model.BillingStatus;
import com.gtechapps.ramjankimandir.model.Garage;
import com.gtechapps.ramjankimandir.model.GaragePayment;
import com.gtechapps.ramjankimandir.model.VehicleEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GaragePaymentAdapter extends RecyclerView.Adapter<GaragePaymentAdapter.GaragePaymentViewHolder> {

    public interface GaragePaymentListener {
        void onChargeReceived(GaragePayment payment);

        void onOpenDetail(GaragePayment payment);
    }

    private final List<GaragePayment> items = new ArrayList<>();
    private final Map<String, Garage> garageMap = new HashMap<>();
    private final Map<String, VehicleEntry> vehicleMap = new HashMap<>();
    private final GaragePaymentListener listener;

    public GaragePaymentAdapter(GaragePaymentListener listener) {
        this.listener = listener;
    }

    public void submit(List<GaragePayment> payments, Map<String, Garage> garages, Map<String, VehicleEntry> vehicles) {
        items.clear();
        items.addAll(payments);
        garageMap.clear();
        garageMap.putAll(garages);
        vehicleMap.clear();
        vehicleMap.putAll(vehicles);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GaragePaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GaragePaymentViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_garage_payment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull GaragePaymentViewHolder holder, int position) {
        GaragePayment payment = items.get(position);
        Garage garage = garageMap.get(payment.garage_id);
        VehicleEntry vehicle = vehicleMap.get(payment.vehicle_owner_id);
        holder.titleView.setText("Garage " + payment.garage_id);
        String ownerName = emptyFallback(payment.owner_name, vehicle == null ? "No vehicle linked" : vehicle.owner_name);
        String ownerNumber = emptyFallback(payment.owner_number, vehicle == null ? "-" : vehicle.owner_number);
        String vehicleNumber = emptyFallback(payment.vehicle_number, vehicle == null ? "-" : vehicle.vehicle_number);
        holder.subtitleView.setText(ownerName + " | " + ownerNumber);
        holder.dateView.setText("Created: " + emptyFallback(payment.createdAt, "-"));
        double receivedAmount = payment.charge_received ? payment.charge : 0D;
        double pendingAmount = Math.max(0D, payment.charge - receivedAmount);
        holder.metaView.setText(
                "Vehicle number: " + vehicleNumber
                        + "\nVehicle name: " + emptyFallback(payment.vehicle_name, vehicle == null ? "-" : vehicle.vehicle_name)
                        + "\nGarage mark: " + emptyFallback(payment.garage_mark, garage == null ? "-" : garage.area_mark)
                        + "\nTotal payment: " + formatCurrency(payment.charge)
                        + "\nReceived payment: " + formatCurrency(receivedAmount)
                        + "\nPending payment: " + formatCurrency(pendingAmount)
        );
        boolean locked = BillingStatus.COMPLETE.equals(payment.status) || BillingStatus.TRANSFER.equals(payment.status);
        holder.cardView.setStrokeColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                payment.charge_received ? R.color.status_paid : R.color.line_gold_soft
        ));
        holder.cardView.setStrokeWidth(payment.charge_received ? 3 : 1);
        holder.statusView.setText(payment.charge_received ? "Garage charge received" : "Garage charge pending");
        if (payment.charge_received){
            holder.tvRentStatus.setText("Rent resived");
            holder.tvRentStatus.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_paid));
        }else {
            holder.tvRentStatus.setText("Rent pending");
            holder.tvRentStatus.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_pending));
        }
        holder.chargeReceivedButton.setVisibility((!locked && !payment.charge_received) ? View.VISIBLE : View.GONE);
        holder.chargeReceivedButton.setOnClickListener(v -> listener.onChargeReceived(payment));
        holder.detailView.setOnClickListener(v -> listener.onOpenDetail(payment));
        holder.itemView.setOnClickListener(v -> listener.onOpenDetail(payment));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String emptyFallback(String primary, String fallback) {
        return primary == null || primary.trim().isEmpty() ? fallback : primary;
    }

    private String formatCurrency(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.US, "Rs %.0f", value);
        }
        return String.format(Locale.US, "Rs %.2f", value);
    }

    static class GaragePaymentViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView titleView;
        private final TextView subtitleView;
        private final TextView dateView;
        private final TextView metaView;
        private final TextView statusView;
        private final TextView detailView;
        private final TextView tvRentStatus;
        private final MaterialButton chargeReceivedButton;

        GaragePaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.paymentCard);
            titleView = itemView.findViewById(R.id.paymentTitle);
            subtitleView = itemView.findViewById(R.id.paymentSubtitle);
            dateView = itemView.findViewById(R.id.paymentDate);
            metaView = itemView.findViewById(R.id.paymentMeta);
            statusView = itemView.findViewById(R.id.paymentStatus);
            tvRentStatus = itemView.findViewById(R.id.tvRentStatus);
            detailView = itemView.findViewById(R.id.detailButton);
            chargeReceivedButton = itemView.findViewById(R.id.chargeReceivedButton);
        }
    }
}
