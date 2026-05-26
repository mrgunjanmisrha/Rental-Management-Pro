package com.gtechapps.ramjankimandir.ui.common;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.model.BillingStatus;
import com.gtechapps.ramjankimandir.model.RentalPerson;
import com.gtechapps.ramjankimandir.model.Room;
import com.gtechapps.ramjankimandir.model.RoomPayment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RoomPaymentAdapter extends RecyclerView.Adapter<RoomPaymentAdapter.RoomPaymentViewHolder> {

    public interface RoomPaymentListener {
        void onSaveCurrentUnit(RoomPayment payment, String currentUnit);

        void onRentReceived(RoomPayment payment);

        void onElectricityReceived(RoomPayment payment);

        void onOpenDetail(RoomPayment payment);
    }

    private final List<RoomPayment> items = new ArrayList<>();
    private final Map<String, Room> roomMap = new HashMap<>();
    private final Map<String, RentalPerson> rentalMap = new HashMap<>();
    private final RoomPaymentListener listener;

    public RoomPaymentAdapter(RoomPaymentListener listener) {
        this.listener = listener;
    }

    public void submit(List<RoomPayment> payments, Map<String, Room> rooms, Map<String, RentalPerson> rentals) {
        items.clear();
        items.addAll(payments);
        roomMap.clear();
        roomMap.putAll(rooms);
        rentalMap.clear();
        rentalMap.putAll(rentals);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoomPaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RoomPaymentViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room_payment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RoomPaymentViewHolder holder, int position) {
        RoomPayment payment = items.get(position);
        Room room = roomMap.get(payment.room_id);
        RentalPerson rentalPerson = rentalMap.get(payment.rental_id);
        holder.titleView.setText(TextUtils.isEmpty(payment.room_label) ? "Room " + payment.room_id : payment.room_label);
        String rentalName = !TextUtils.isEmpty(payment.rental_name)
                ? payment.rental_name
                : rentalPerson == null ? "No rental linked" : rentalPerson.name;
        String rentalPhone = !TextUtils.isEmpty(payment.rental_phone_number)
                ? payment.rental_phone_number
                : rentalPerson == null ? "-" : rentalPerson.phone_number;
        holder.subtitleView.setText(rentalName + " | " + rentalPhone);
        holder.dateView.setText("Created: " + safeDisplay(payment.createdAt));

        double totalCharge = payment.total_charge > 0D
                ? payment.total_charge
                : payment.room_rent + payment.bijlibill + payment.other_charge;
        holder.roomRentView.setText(formatCurrency(payment.room_rent));
        holder.electricityBillView.setText(formatCurrency(payment.bijlibill));
        holder.totalPaymentView.setText(formatCurrency(totalCharge));

        boolean hasCurrentReading = !TextUtils.isEmpty(payment.bill_current_unit);
        double currentReading = hasCurrentReading ? parseDouble(payment.bill_current_unit) : 0D;
        double consumedUnit = hasCurrentReading ? Math.max(0D, currentReading - payment.bill_start_unit) : 0D;
        holder.electricityMetaView.setText(
                "Previous reading: " + formatNumber(payment.bill_start_unit)
                        + "\nCurrent reading: " + (hasCurrentReading ? formatNumber(currentReading) : "--")
                        + "\nCurrent bijli unit: " + (hasCurrentReading ? formatNumber(consumedUnit) : "--")
                        + "\nUnit charge: " + formatCurrency(resolveUnitCharge(payment, room))
        );
        holder.currentUnitEditText.setText("");
        boolean locked = BillingStatus.COMPLETE.equals(payment.status) || BillingStatus.TRANSFER.equals(payment.status);
        boolean complete = payment.rent_received && payment.bijlibill_received;

        holder.cardView.setStrokeColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                complete ? R.color.status_paid : R.color.line_gold_soft
        ));
        holder.cardView.setStrokeWidth(complete ? 3 : 1);
        holder.statusView.setText((complete ? "Status " : "")
                + (payment.rent_received ? "Rent received" : "Rent pending")
                + " | "
                + (payment.bijlibill_received ? "Power received" : "Power pending"));
        if (payment.rent_received){
            holder.tvRentStatus.setText("Rent ✔");
            holder.tvRentStatus.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_paid));
        }else {
            holder.tvRentStatus.setText("Rent ☒");
            holder.tvRentStatus.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_pending));
        }
        if (payment.bijlibill_received){
            holder.tvPowerStatus.setText("Power ✔");
            holder.tvPowerStatus.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_paid));
        }else {
            holder.tvPowerStatus.setText("Power ☒");
            holder.tvPowerStatus.setBackgroundTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.status_pending));
        }

        int readingInputVisibility = (!locked && !hasCurrentReading) ? View.VISIBLE : View.GONE;
        holder.currentUnitLayout.setVisibility(readingInputVisibility);
        holder.saveUnitButton.setVisibility(readingInputVisibility);
        holder.rentReceivedButton.setVisibility((!locked && !payment.rent_received) ? View.VISIBLE : View.GONE);
        holder.electricityReceivedButton.setVisibility((!locked && !payment.bijlibill_received) ? View.VISIBLE : View.GONE);
        holder.actionRow.setVisibility(
                holder.rentReceivedButton.getVisibility() == View.GONE
                        && holder.electricityReceivedButton.getVisibility() == View.GONE
                        ? View.GONE
                        : View.VISIBLE
        );
        holder.saveUnitButton.setOnClickListener(v -> listener.onSaveCurrentUnit(payment, holder.currentUnitValue()));
        holder.rentReceivedButton.setOnClickListener(v -> listener.onRentReceived(payment));
        holder.electricityReceivedButton.setOnClickListener(v -> listener.onElectricityReceived(payment));
        holder.detailView.setOnClickListener(v -> listener.onOpenDetail(payment));
        holder.itemView.setOnClickListener(v -> listener.onOpenDetail(payment));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safeText(String primary, String fallback) {
        return TextUtils.isEmpty(primary) ? fallback : primary;
    }

    private String safeDisplay(String value) {
        return TextUtils.isEmpty(value) ? "-" : value;
    }

    private double resolveUnitCharge(RoomPayment payment, Room room) {
        return payment.bijli_charge <= 0D && room != null ? room.bijli_charge : payment.bijli_charge;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            return 0D;
        }
    }

    private String formatCurrency(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.US, "Rs %.0f", value);
        }
        return String.format(Locale.US, "Rs %.2f", value);
    }

    private String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    static class RoomPaymentViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView titleView;
        private final TextView subtitleView;
        private final TextView dateView;
        private final TextView roomRentView;
        private final TextView electricityBillView;
        private final TextView totalPaymentView;
        private final TextView electricityMetaView;
        private final TextView statusView;
        private final TextView detailView;
        private final TextView tvRentStatus;
        private final TextView tvPowerStatus;
        private final TextInputLayout currentUnitLayout;
        private final TextInputEditText currentUnitEditText;
        private final MaterialButton saveUnitButton;
        private final MaterialButton rentReceivedButton;
        private final MaterialButton electricityReceivedButton;
        private final View actionRow;

        RoomPaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.paymentCard);
            titleView = itemView.findViewById(R.id.paymentTitle);
            subtitleView = itemView.findViewById(R.id.paymentSubtitle);
            dateView = itemView.findViewById(R.id.paymentDate);
            roomRentView = itemView.findViewById(R.id.roomRentValue);
            electricityBillView = itemView.findViewById(R.id.electricityBillValue);
            totalPaymentView = itemView.findViewById(R.id.totalPaymentValue);
            electricityMetaView = itemView.findViewById(R.id.electricityMeta);
            statusView = itemView.findViewById(R.id.paymentStatus);
            detailView = itemView.findViewById(R.id.detailButton);
            currentUnitLayout = itemView.findViewById(R.id.currentUnitLayout);
            currentUnitEditText = itemView.findViewById(R.id.currentUnitEditText);
            saveUnitButton = itemView.findViewById(R.id.saveUnitButton);
            rentReceivedButton = itemView.findViewById(R.id.rentReceivedButton);
            electricityReceivedButton = itemView.findViewById(R.id.electricityReceivedButton);
            tvRentStatus = itemView.findViewById(R.id.tvRentStatus);
            tvPowerStatus = itemView.findViewById(R.id.tvPowerStatus);
            actionRow = itemView.findViewById(R.id.actionRow);
        }

        String currentUnitValue() {
            return currentUnitEditText.getText() == null ? "" : currentUnitEditText.getText().toString().trim();
        }
    }
}
