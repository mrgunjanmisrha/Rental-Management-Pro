package com.gtechapps.ramjankimandir.ui.common;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.data.RentalRepository;
import com.gtechapps.ramjankimandir.data.RepositoryCallback;
import com.gtechapps.ramjankimandir.model.Garage;
import com.gtechapps.ramjankimandir.model.GaragePayment;
import com.gtechapps.ramjankimandir.model.RentalPerson;
import com.gtechapps.ramjankimandir.model.Room;
import com.gtechapps.ramjankimandir.model.RoomPayment;
import com.gtechapps.ramjankimandir.model.VehicleEntry;
import com.gtechapps.ramjankimandir.util.MonthUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MonthlyDetailActivity extends AppCompatActivity {

    private static final String EXTRA_TYPE = "type";
    private static final String EXTRA_MONTH_ID = "month_id";
    private static final String EXTRA_RECORD_ID = "record_id";

    public static Intent createIntent(Context context, String type, String monthId, String recordId) {
        Intent intent = new Intent(context, MonthlyDetailActivity.class);
        intent.putExtra(EXTRA_TYPE, type);
        intent.putExtra(EXTRA_MONTH_ID, monthId);
        intent.putExtra(EXTRA_RECORD_ID, recordId);
        return intent;
    }

    private final RentalRepository repository = new RentalRepository();

    private TextView headingView;
    private TextView subheadingView;
    private TextView identityTitleView;
    private TextView primaryValueView;
    private TextView nameValueView;
    private TextView phoneValueView;
    private TextView addressValueView;
    private TextView entryDateValueView;
    private TextView monthValueView;
    private TextView currentDateValueView;
    private TextView monthlyRentValueView;
    private TextView electricityChargeValueView;
    private TextView otherChargeValueView;
    private TextView totalChargeValueView;
    private TextView startReadingValueView;
    private TextView currentReadingValueView;
    private TextView unitValueView;
    private TextView unitChargeValueView;
    private MaterialCardView electricitySectionView;
    private ExtendedFloatingActionButton whatsappFab;

    private String sharePhone = "";
    private String shareMessage = "";
    private String monthId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_detail);

        MaterialToolbar toolbar = findViewById(R.id.detailToolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        whatsappFab.setOnClickListener(v -> shareOnWhatsApp());

        String type = getIntent().getStringExtra(EXTRA_TYPE);
        monthId = safe(getIntent().getStringExtra(EXTRA_MONTH_ID));
        String recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);
        if (isBlank(type) || isBlank(monthId) || isBlank(recordId)) {
            renderError(getString(R.string.form_invalid_state));
            return;
        }
        monthValueView.setText("Month: " + MonthUtils.displayName(monthId));
        currentDateValueView.setText("Date: " + todayDisplay());
        if (RentalRepository.TYPE_ROOM.equals(type)) {
            loadRoomDetail(monthId, recordId);
        } else {
            loadGarageDetail(monthId, recordId);
        }
    }

    private void bindViews() {
        headingView = findViewById(R.id.detailHeading);
        subheadingView = findViewById(R.id.detailSubheading);
        identityTitleView = findViewById(R.id.identityTitle);
        primaryValueView = findViewById(R.id.detailPrimaryValue);
        nameValueView = findViewById(R.id.detailNameValue);
        phoneValueView = findViewById(R.id.detailPhoneValue);
        addressValueView = findViewById(R.id.detailAddressValue);
        entryDateValueView = findViewById(R.id.detailEntryDateValue);
        monthValueView = findViewById(R.id.detailMonthValue);
        currentDateValueView = findViewById(R.id.detailCurrentDateValue);
        monthlyRentValueView = findViewById(R.id.detailMonthlyRentValue);
        electricityChargeValueView = findViewById(R.id.detailElectricityChargeValue);
        otherChargeValueView = findViewById(R.id.detailOtherChargeValue);
        totalChargeValueView = findViewById(R.id.detailTotalChargeValue);
        startReadingValueView = findViewById(R.id.detailStartReadingValue);
        currentReadingValueView = findViewById(R.id.detailCurrentReadingValue);
        unitValueView = findViewById(R.id.detailUnitValue);
        unitChargeValueView = findViewById(R.id.detailUnitChargeValue);
        electricitySectionView = findViewById(R.id.electricitySection);
        whatsappFab = findViewById(R.id.whatsappFab);
    }

    private void loadRoomDetail(String monthId, String roomId) {
        repository.loadRoomPayment(monthId, roomId, new RepositoryCallback<RoomPayment>() {
            @Override
            public void onSuccess(RoomPayment payment) {
                repository.loadRoom(payment.room_id, new RepositoryCallback<Room>() {
                    @Override
                    public void onSuccess(Room room) {
                        if (isBlank(payment.rental_id)) {
                            renderRoomDetail(payment, room, null);
                            return;
                        }
                        repository.loadRental(payment.rental_id, new RepositoryCallback<RentalPerson>() {
                            @Override
                            public void onSuccess(RentalPerson rental) {
                                renderRoomDetail(payment, room, rental);
                            }

                            @Override
                            public void onError(String message) {
                                renderRoomDetail(payment, room, null);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        Room fallbackRoom = new Room();
                        fallbackRoom.room_number = safeRoomNumber(payment.room_id);
                        fallbackRoom.rental_id = payment.rental_id;
                        renderRoomDetail(payment, fallbackRoom, null);
                    }
                });
            }

            @Override
            public void onError(String message) {
                renderError(message);
            }
        });
    }

    private void renderRoomDetail(RoomPayment payment, Room room, RentalPerson rental) {
        String roomLabel = !isBlank(payment.room_label) ? payment.room_label : "Room " + room.room_number;
        String rentalName = firstValue(payment.rental_name, rental == null ? "" : rental.name);
        String rentalPhone = firstValue(payment.rental_phone_number, rental == null ? "" : rental.phone_number);
        String rentalAddress = firstValue(payment.rental_address, rental == null ? "" : rental.address);
        String entryDate = firstValue(payment.rental_entry_date, rental == null ? "" : rental.entry_date);
        double totalCharge = payment.total_charge > 0D
                ? payment.total_charge
                : payment.room_rent + payment.bijlibill + payment.other_charge;
        boolean hasCurrent = !isBlank(payment.bill_current_unit);
        double currentReading = hasCurrent ? parseDouble(payment.bill_current_unit) : 0D;
        double usedUnit = hasCurrent ? Math.max(0D, currentReading - payment.bill_start_unit) : 0D;

        headingView.setText(roomLabel);
        subheadingView.setText(rentalName + " | " + rentalPhone);
        identityTitleView.setText("Room Details");
        primaryValueView.setText("Room number: " + roomLabel);
        nameValueView.setText("Rental name: " + rentalName);
        phoneValueView.setText("Phone number: " + rentalPhone);
        addressValueView.setText("Address: " + rentalAddress);
        entryDateValueView.setText("Entry date: " + entryDate);
        monthlyRentValueView.setText("Monthly rent: " + formatCurrency(payment.room_rent));
        electricityChargeValueView.setText("Electricity charge: " + formatCurrency(payment.bijlibill));
        otherChargeValueView.setText("Other charge: " + formatCurrency(payment.other_charge));
        totalChargeValueView.setText("Total charge: " + formatCurrency(totalCharge));
        electricitySectionView.setVisibility(View.VISIBLE);
        startReadingValueView.setText("Start reading: " + formatNumber(payment.bill_start_unit));
        currentReadingValueView.setText("Current reading: " + (hasCurrent ? formatNumber(currentReading) : "--"));
        unitValueView.setText("Unit: " + (hasCurrent ? formatNumber(usedUnit) : "--"));
        unitChargeValueView.setText("Unit charge: " + formatCurrency(payment.bijli_charge));

        sharePhone = rentalPhone;
        shareMessage = "Room payment detail\n"
                + roomLabel + "\n"
                + "Rental name: " + rentalName + "\n"
                + "Phone: " + rentalPhone + "\n"
                + "Address: " + rentalAddress + "\n"
                + "Entry date: " + entryDate + "\n"
                + "Month: " + MonthUtils.displayName(monthId) + "\n"
                + "Date: " + todayDisplay() + "\n"
                + "Monthly rent: " + formatCurrency(payment.room_rent) + "\n"
                + "Electricity charge: " + formatCurrency(payment.bijlibill) + "\n"
                + "Other charge: " + formatCurrency(payment.other_charge) + "\n"
                + "Total charge: " + formatCurrency(totalCharge) + "\n"
                + "Start reading: " + formatNumber(payment.bill_start_unit) + "\n"
                + "Current reading: " + (hasCurrent ? formatNumber(currentReading) : "--") + "\n"
                + "Unit: " + (hasCurrent ? formatNumber(usedUnit) : "--") + "\n"
                + "Unit charge: " + formatCurrency(payment.bijli_charge);
        whatsappFab.setEnabled(true);
    }

    private void loadGarageDetail(String monthId, String garageId) {
        repository.loadGaragePayment(monthId, garageId, new RepositoryCallback<GaragePayment>() {
            @Override
            public void onSuccess(GaragePayment payment) {
                repository.loadGarage(payment.garage_id, new RepositoryCallback<Garage>() {
                    @Override
                    public void onSuccess(Garage garage) {
                        if (isBlank(payment.vehicle_owner_id)) {
                            renderGarageDetail(payment, garage, null);
                            return;
                        }
                        repository.loadVehicle(payment.vehicle_owner_id, new RepositoryCallback<VehicleEntry>() {
                            @Override
                            public void onSuccess(VehicleEntry vehicle) {
                                renderGarageDetail(payment, garage, vehicle);
                            }

                            @Override
                            public void onError(String message) {
                                renderGarageDetail(payment, garage, null);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        Garage fallbackGarage = new Garage();
                        fallbackGarage.area_number = payment.garage_id;
                        fallbackGarage.area_mark = payment.garage_mark;
                        renderGarageDetail(payment, fallbackGarage, null);
                    }
                });
            }

            @Override
            public void onError(String message) {
                renderError(message);
            }
        });
    }

    private void renderGarageDetail(GaragePayment payment, Garage garage, VehicleEntry vehicle) {
        String garageLabel = "Garage " + payment.garage_id;
        String ownerName = firstValue(payment.owner_name, vehicle == null ? "" : vehicle.owner_name);
        String ownerNumber = firstValue(payment.owner_number, vehicle == null ? "" : vehicle.owner_number);
        String ownerAddress = firstValue(payment.owner_address, vehicle == null ? "" : vehicle.address);
        String entryDate = firstValue(payment.vehicle_entry_date, vehicle == null ? "" : vehicle.entry_date);
        String vehicleNumber = firstValue(payment.vehicle_number, vehicle == null ? "" : vehicle.vehicle_number);
        String vehicleName = firstValue(payment.vehicle_name, vehicle == null ? "" : vehicle.vehicle_name);
        double receivedAmount = payment.charge_received ? payment.charge : 0D;
        double pendingAmount = Math.max(0D, payment.charge - receivedAmount);

        headingView.setText(garageLabel);
        subheadingView.setText(ownerName + " | " + vehicleNumber);
        identityTitleView.setText("Garage and Vehicle Details");
        primaryValueView.setText("Garage number: " + payment.garage_id);
        nameValueView.setText("Owner name: " + ownerName);
        phoneValueView.setText("Owner phone: " + ownerNumber);
        addressValueView.setText("Address: " + ownerAddress);
        entryDateValueView.setText("Entry date: " + entryDate);
        monthlyRentValueView.setText("Garage charge: " + formatCurrency(payment.charge));
        electricityChargeValueView.setText("Received payment: " + formatCurrency(receivedAmount));
        otherChargeValueView.setText("Pending payment: " + formatCurrency(pendingAmount));
        totalChargeValueView.setText("Total charge: " + formatCurrency(payment.charge));
        electricitySectionView.setVisibility(View.GONE);

        sharePhone = ownerNumber;
        shareMessage = "Garage payment detail\n"
                + garageLabel + "\n"
                + "Owner name: " + ownerName + "\n"
                + "Owner phone: " + ownerNumber + "\n"
                + "Address: " + ownerAddress + "\n"
                + "Vehicle number: " + vehicleNumber + "\n"
                + "Vehicle name: " + vehicleName + "\n"
                + "Month: " + MonthUtils.displayName(monthId) + "\n"
                + "Date: " + todayDisplay() + "\n"
                + "Garage charge: " + formatCurrency(payment.charge) + "\n"
                + "Received payment: " + formatCurrency(receivedAmount) + "\n"
                + "Pending payment: " + formatCurrency(pendingAmount);
        whatsappFab.setEnabled(true);
    }

    private void shareOnWhatsApp() {
        if (isBlank(shareMessage)) {
            Toast.makeText(this, R.string.form_invalid_state, Toast.LENGTH_SHORT).show();
            return;
        }
        String normalizedPhone = normalizePhone(sharePhone);
        Intent intent;
        if (!isBlank(normalizedPhone)) {
            intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/" + normalizedPhone + "?text=" + urlEncode(shareMessage)));
        } else {
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        }
        intent.setPackage("com.whatsapp");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Intent fallback = new Intent(Intent.ACTION_SEND);
            fallback.setType("text/plain");
            fallback.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(fallback, getString(R.string.send_whatsapp)));
        }
    }

    private void renderError(String message) {
        headingView.setText(getString(R.string.detail_screen_title));
        subheadingView.setText(message);
        whatsappFab.setEnabled(false);
    }

    private String firstValue(String primary, String fallback) {
        if (!isBlank(primary)) {
            return primary;
        }
        return isBlank(fallback) ? "-" : fallback;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private int safeRoomNumber(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
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

    private String todayDisplay() {
        return new SimpleDateFormat("dd MMM yyyy", Locale.US).format(new Date());
    }

    private String normalizePhone(String value) {
        String digits = safe(value).replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "91" + digits;
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return "91" + digits.substring(1);
        }
        return digits.length() > 10 ? digits : "";
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException exception) {
            return value;
        }
    }
}
