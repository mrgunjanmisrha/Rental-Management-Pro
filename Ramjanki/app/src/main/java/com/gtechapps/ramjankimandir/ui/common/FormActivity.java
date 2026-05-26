package com.gtechapps.ramjankimandir.ui.common;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.gtechapps.ramjankimandir.R;
import com.gtechapps.ramjankimandir.data.OperationCallback;
import com.gtechapps.ramjankimandir.data.RentalRepository;
import com.gtechapps.ramjankimandir.data.RepositoryCallback;
import com.gtechapps.ramjankimandir.model.Garage;
import com.gtechapps.ramjankimandir.model.RentalPerson;
import com.gtechapps.ramjankimandir.model.Room;
import com.gtechapps.ramjankimandir.model.VehicleEntry;
import com.gtechapps.ramjankimandir.util.TimeUtils;
import com.gtechapps.ramjankimandir.util.ValidationUtils;

import java.util.HashMap;
import java.util.Map;

public class FormActivity extends AppCompatActivity {

    private static final String EXTRA_TYPE = "type";
    private static final String EXTRA_RECORD_ID = "record_id";

    public static Intent createCreateIntent(Context context, String type) {
        Intent intent = new Intent(context, FormActivity.class);
        intent.putExtra(EXTRA_TYPE, type);
        return intent;
    }

    public static Intent createEditIntent(Context context, String type, String recordId) {
        Intent intent = createCreateIntent(context, type);
        intent.putExtra(EXTRA_RECORD_ID, recordId);
        return intent;
    }

    private final RentalRepository repository = new RentalRepository();
    private final Map<String, View> fieldViews = new HashMap<>();

    private TextView titleView;
    private TextView subtitleView;
    private View formFieldContainer;
    private String type;
    private String recordId;
    private String existingCreatedAt = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        type = getIntent().getStringExtra(EXTRA_TYPE);
        recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);

        MaterialToolbar toolbar = findViewById(R.id.formToolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        titleView = findViewById(R.id.formTitle);
        subtitleView = findViewById(R.id.formSubtitle);
        formFieldContainer = findViewById(R.id.formFieldContainer);

        MaterialButton saveButton = findViewById(R.id.saveRecordButton);
        saveButton.setOnClickListener(v -> saveRecord());
        saveButton.setText(TextUtils.isEmpty(recordId) ? R.string.save_record : R.string.update_record);

        if (!isSupportedType(type)) {
            showMessage(getString(R.string.form_invalid_state));
            finish();
            return;
        }

        try {
            renderForm();
            if (!TextUtils.isEmpty(recordId)) {
                loadRecord();
            }
        } catch (RuntimeException exception) {
            showMessage("Unable to open this form right now.");
            finish();
        }
    }

    private boolean isSupportedType(String value) {
        return RentalRepository.TYPE_ROOM.equals(value)
                || RentalRepository.TYPE_GARAGE.equals(value)
                || RentalRepository.TYPE_RENTAL.equals(value)
                || RentalRepository.TYPE_VEHICLE.equals(value);
    }

    private void renderForm() {
        fieldViews.clear();
        ((android.widget.LinearLayout) formFieldContainer).removeAllViews();

        if (RentalRepository.TYPE_ROOM.equals(type)) {
            inflateContent(R.layout.form_content_room);
            registerView("room_number", R.id.roomNumberEditText);
            registerView("room_charge", R.id.roomChargeEditText);
            registerView("bijli_unit_start", R.id.roomStartUnitEditText);
            registerView("bijli_charge", R.id.roomBijliChargeEditText);
            registerView("extra_charge", R.id.roomExtraChargeEditText);
            registerView("room_active", R.id.roomActiveSwitch);
            registerView("rental_id", R.id.roomRentalIdEditText);
            titleView.setText(TextUtils.isEmpty(recordId) ? R.string.create_room : R.string.update_record);
            subtitleView.setText("Create and maintain room masters with unique room number and billing defaults.");
            setText("extra_charge", "0");
            setChecked("room_active", true);
            lockField("rental_id");
            return;
        }

        if (RentalRepository.TYPE_GARAGE.equals(type)) {
            inflateContent(R.layout.form_content_garage);
            registerView("area_number", R.id.garageAreaNumberEditText);
            registerView("area_mark", R.id.garageAreaMarkEditText);
            registerView("vehicle_owner_id", R.id.garageVehicleOwnerIdEditText);
            registerView("charge", R.id.garageChargeEditText);
            registerView("is_active", R.id.garageActiveSwitch);
            titleView.setText(TextUtils.isEmpty(recordId) ? R.string.create_garage : R.string.update_record);
            subtitleView.setText("Create and maintain garage masters with unique area number and charge.");
            setChecked("is_active", true);
            lockField("vehicle_owner_id");
            return;
        }

        if (RentalRepository.TYPE_RENTAL.equals(type)) {
            inflateContent(R.layout.form_content_rental);
            registerView("name", R.id.rentalNameEditText);
            registerView("phone_number", R.id.rentalPhoneEditText);
            registerView("adhar_number", R.id.rentalAdharEditText);
            registerView("address", R.id.rentalAddressEditText);
            registerView("entry_date", R.id.rentalEntryDateEditText);
            registerView("start_r_date", R.id.rentalStartDateEditText);
            registerView("status", R.id.rentalStatusDropdown);
            registerView("room_number", R.id.rentalRoomNumberEditText);
            setupDropdown(R.id.rentalStatusDropdown, R.array.room_status_options);
            titleView.setText(TextUtils.isEmpty(recordId) ? R.string.rental_entry : R.string.update_record);
            subtitleView.setText("Pending can stay without room, active links a room, reject keeps room blank.");
            setText("status", "pending");
            setupAssignmentStatusSync("status", "room_number");
            return;
        }

        inflateContent(R.layout.form_content_vehicle);
        registerView("owner_name", R.id.vehicleOwnerNameEditText);
        registerView("owner_number", R.id.vehicleOwnerNumberEditText);
        registerView("owner_adhar_number", R.id.vehicleOwnerAdharEditText);
        registerView("address", R.id.vehicleAddressEditText);
        registerView("vehicle_number", R.id.vehicleNumberEditText);
        registerView("vehicle_name", R.id.vehicleNameEditText);
        registerView("other_vehicle_details", R.id.vehicleOtherDetailsEditText);
        registerView("garage_number", R.id.vehicleGarageNumberEditText);
        registerView("entry_date", R.id.vehicleEntryDateEditText);
        registerView("start_rent_date", R.id.vehicleStartRentDateEditText);
        registerView("status", R.id.vehicleStatusDropdown);
        setupDropdown(R.id.vehicleStatusDropdown, R.array.garage_status_options);
        titleView.setText(TextUtils.isEmpty(recordId) ? R.string.vehicle_entry : R.string.update_record);
        subtitleView.setText("Pending can stay without garage, active links a garage, reject keeps garage blank.");
        setText("status", "pending");
        setupAssignmentStatusSync("status", "garage_number");
    }

    private void inflateContent(int layoutRes) {
        LayoutInflater.from(this).inflate(layoutRes, (android.widget.LinearLayout) formFieldContainer, true);
    }

    private void registerView(String key, int viewId) {
        View view = findViewById(viewId);
        if (view != null) {
            fieldViews.put(key, view);
        }
    }

    private void setupDropdown(int viewId, int arrayRes) {
        MaterialAutoCompleteTextView dropdown = findViewById(viewId);
        if (dropdown == null) {
            return;
        }
        dropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(arrayRes)));
        dropdown.setKeyListener(null);
        dropdown.setCursorVisible(false);
        dropdown.setOnClickListener(v -> dropdown.showDropDown());
    }

    private void setupAssignmentStatusSync(String statusKey, String assignmentKey) {
        View statusView = fieldViews.get(statusKey);
        View assignmentView = fieldViews.get(assignmentKey);
        if (!(statusView instanceof MaterialAutoCompleteTextView) || !(assignmentView instanceof TextInputEditText)) {
            return;
        }
        MaterialAutoCompleteTextView statusDropdown = (MaterialAutoCompleteTextView) statusView;
        TextInputEditText assignmentEditText = (TextInputEditText) assignmentView;
        assignmentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!"reject".equalsIgnoreCase(getText(statusKey))) {
                    statusDropdown.setText(TextUtils.isEmpty(editable) ? "pending" : "active", false);
                }
            }
        });
        statusDropdown.setOnItemClickListener((parent, view, position, id) -> syncAssignmentStatus(statusKey, assignmentKey));
        syncAssignmentStatus(statusKey, assignmentKey);
    }

    private void syncAssignmentStatus(String statusKey, String assignmentKey) {
        String status = getText(statusKey).trim();
        String assignment = getText(assignmentKey).trim();
        if ("reject".equalsIgnoreCase(status)) {
            setText(assignmentKey, "");
            setAssignmentFieldEnabled(assignmentKey, false);
            return;
        }
        setAssignmentFieldEnabled(assignmentKey, true);
        setText(statusKey, TextUtils.isEmpty(assignment) ? "pending" : "active");
    }

    private String resolveAssignmentStatus(String status, String assignment) {
        if ("reject".equalsIgnoreCase(status)) {
            return "reject";
        }
        return TextUtils.isEmpty(assignment) ? "pending" : "active";
    }

    private void setAssignmentFieldEnabled(String key, boolean enabled) {
        View view = fieldViews.get(key);
        if (view instanceof TextInputEditText) {
            view.setEnabled(enabled);
            view.setFocusable(enabled);
            view.setFocusableInTouchMode(enabled);
        } else if (view != null) {
            view.setEnabled(enabled);
        }
    }

    private void loadRecord() {
        if (RentalRepository.TYPE_ROOM.equals(type)) {
            repository.loadRoom(recordId, new RepositoryCallback<Room>() {
                @Override
                public void onSuccess(Room room) {
                    existingCreatedAt = room.createAt;
                    setText("room_number", String.valueOf(room.room_number));
                    setText("room_charge", trimDouble(room.room_charge));
                    setText("bijli_unit_start", trimDouble(room.bijli_unit_start));
                    setText("bijli_charge", trimDouble(room.bijli_charge));
                    setText("extra_charge", trimDouble(room.extra_charge));
                    setChecked("room_active", room.room_active);
                    setText("rental_id", room.rental_id);
                    lockField("room_number");
                }

                @Override
                public void onError(String message) {
                    showMessage(message);
                }
            });
            return;
        }

        if (RentalRepository.TYPE_GARAGE.equals(type)) {
            repository.loadGarage(recordId, new RepositoryCallback<Garage>() {
                @Override
                public void onSuccess(Garage garage) {
                    existingCreatedAt = garage.create_at;
                    setText("area_number", garage.area_number);
                    setText("area_mark", garage.area_mark);
                    setText("vehicle_owner_id", garage.vehicle_owner_id);
                    setText("charge", trimDouble(garage.charge));
                    setChecked("is_active", garage.is_active);
                    lockField("area_number");
                }

                @Override
                public void onError(String message) {
                    showMessage(message);
                }
            });
            return;
        }

        if (RentalRepository.TYPE_RENTAL.equals(type)) {
            repository.loadRental(recordId, new RepositoryCallback<RentalPerson>() {
                @Override
                public void onSuccess(RentalPerson rentalPerson) {
                    existingCreatedAt = rentalPerson.createAt;
                    setText("name", rentalPerson.name);
                    setText("phone_number", rentalPerson.phone_number);
                    setText("adhar_number", rentalPerson.adhar_number);
                    setText("address", rentalPerson.address);
                    setText("entry_date", rentalPerson.entry_date);
                    setText("start_r_date", rentalPerson.start_r_date);
                    setText("status", rentalPerson.status);
                    setText("room_number", rentalPerson.room_number);
                    syncAssignmentStatus("status", "room_number");
                }

                @Override
                public void onError(String message) {
                    showMessage(message);
                }
            });
            return;
        }

        repository.loadVehicle(recordId, new RepositoryCallback<VehicleEntry>() {
            @Override
            public void onSuccess(VehicleEntry vehicleEntry) {
                existingCreatedAt = vehicleEntry.createAt;
                setText("owner_name", vehicleEntry.owner_name);
                setText("owner_number", vehicleEntry.owner_number);
                setText("owner_adhar_number", vehicleEntry.owner_adhar_number);
                setText("address", vehicleEntry.address);
                setText("vehicle_number", vehicleEntry.vehicle_number);
                setText("vehicle_name", vehicleEntry.vehicle_name);
                setText("other_vehicle_details", vehicleEntry.other_vehicle_details);
                setText("garage_number", vehicleEntry.garage_number);
                setText("entry_date", vehicleEntry.entry_date);
                setText("start_rent_date", vehicleEntry.start_rent_date);
                setText("status", vehicleEntry.status);
                syncAssignmentStatus("status", "garage_number");
            }

            @Override
            public void onError(String message) {
                showMessage(message);
            }
        });
    }

    private void saveRecord() {
        if (RentalRepository.TYPE_ROOM.equals(type)) {
            Integer roomNumber = parseRequiredInt("room_number", "Room number");
            Double roomCharge = parseRequiredDouble("room_charge", "Room charge");
            Double startUnit = parseRequiredDouble("bijli_unit_start", "Bijli unit start");
            Double bijliCharge = parseRequiredDouble("bijli_charge", "Bijli charge");
            Double extraCharge = parseOptionalDouble("extra_charge", 0D, "Extra charge");
            if (roomNumber == null || roomCharge == null || startUnit == null || bijliCharge == null || extraCharge == null) {
                return;
            }
            Room room = new Room();
            room.room_number = roomNumber;
            room.room_charge = roomCharge;
            room.bijli_unit_start = startUnit;
            room.bijli_charge = bijliCharge;
            room.extra_charge = extraCharge;
            room.room_active = isChecked("room_active");
            room.rental_id = getText("rental_id").trim();
            room.createAt = TextUtils.isEmpty(existingCreatedAt) ? TimeUtils.nowIsoTimestamp() : existingCreatedAt;
            repository.saveRoom(room, !TextUtils.isEmpty(recordId), operationCallback());
            return;
        }

        if (RentalRepository.TYPE_GARAGE.equals(type)) {
            String areaNumber = requireText("area_number", "Area number");
            String areaMark = requireText("area_mark", "Area mark");
            Double charge = parseRequiredDouble("charge", "Monthly charge");
            if (areaNumber == null || areaMark == null || charge == null) {
                return;
            }
            Garage garage = new Garage();
            garage.area_number = areaNumber;
            garage.area_mark = areaMark;
            garage.vehicle_owner_id = getText("vehicle_owner_id").trim();
            garage.charge = charge;
            garage.is_active = isChecked("is_active");
            garage.create_at = TextUtils.isEmpty(existingCreatedAt) ? TimeUtils.nowIsoTimestamp() : existingCreatedAt;
            repository.saveGarage(garage, !TextUtils.isEmpty(recordId), operationCallback());
            return;
        }

        if (RentalRepository.TYPE_RENTAL.equals(type)) {
            String name = requireText("name", "Name");
            String phone = requireText("phone_number", "Phone number");
            String entryDate = normalizeDate("entry_date", "Entry date");
            String startDate = normalizeDate("start_r_date", "Start rent date");
            String roomNumber = getText("room_number").trim();
            String status = resolveAssignmentStatus(getText("status"), roomNumber);
            if (name == null || phone == null || entryDate == null || startDate == null) {
                return;
            }
            RentalPerson rentalPerson = new RentalPerson();
            rentalPerson.id = recordId;
            rentalPerson.name = name;
            rentalPerson.phone_number = phone;
            rentalPerson.adhar_number = getText("adhar_number").trim();
            rentalPerson.address = getText("address").trim();
            rentalPerson.entry_date = entryDate;
            rentalPerson.start_r_date = startDate;
            rentalPerson.status = status;
            rentalPerson.room_number = "reject".equalsIgnoreCase(status) ? "" : roomNumber;
            rentalPerson.createAt = TextUtils.isEmpty(existingCreatedAt) ? TimeUtils.nowIsoTimestamp() : existingCreatedAt;
            repository.saveRental(rentalPerson, operationCallback());
            return;
        }

        String ownerName = requireText("owner_name", "Owner name");
        String ownerNumber = requireText("owner_number", "Owner number");
        String vehicleNumber = requireText("vehicle_number", "Vehicle number");
        String vehicleName = requireText("vehicle_name", "Vehicle name");
        String garageNumber = getText("garage_number").trim();
        String entryDate = normalizeDate("entry_date", "Entry date");
        String startRentDate = normalizeDate("start_rent_date", "Start rent date");
        String status = resolveAssignmentStatus(getText("status"), garageNumber);
        if (ownerName == null || ownerNumber == null || vehicleNumber == null || vehicleName == null
                || entryDate == null || startRentDate == null) {
            return;
        }
        VehicleEntry vehicleEntry = new VehicleEntry();
        vehicleEntry.id = recordId;
        vehicleEntry.owner_name = ownerName;
        vehicleEntry.owner_number = ownerNumber;
        vehicleEntry.owner_adhar_number = getText("owner_adhar_number").trim();
        vehicleEntry.address = getText("address").trim();
        vehicleEntry.vehicle_number = vehicleNumber;
        vehicleEntry.vehicle_name = vehicleName;
        vehicleEntry.other_vehicle_details = getText("other_vehicle_details").trim();
        vehicleEntry.garage_number = "reject".equalsIgnoreCase(status) ? "" : garageNumber;
        vehicleEntry.entry_date = entryDate;
        vehicleEntry.start_rent_date = startRentDate;
        vehicleEntry.status = status;
        vehicleEntry.createAt = TextUtils.isEmpty(existingCreatedAt) ? TimeUtils.nowIsoTimestamp() : existingCreatedAt;
        repository.saveVehicle(vehicleEntry, operationCallback());
    }

    private OperationCallback operationCallback() {
        return (success, message) -> {
            if (success) {
                finish();
            } else {
                showMessage(message);
            }
        };
    }

    private String requireText(String key, String label) {
        String value = getText(key).trim();
        if (ValidationUtils.isBlank(value)) {
            showMessage(label + " is required.");
            return null;
        }
        return value;
    }

    private String normalizeDate(String key, String label) {
        try {
            return ValidationUtils.normalizeDate(getText(key), label);
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
            return null;
        }
    }

    private Integer parseRequiredInt(String key, String label) {
        String value = requireText(key, label);
        if (value == null) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                showMessage(label + " must be greater than zero.");
                return null;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            showMessage(label + " must be numeric.");
            return null;
        }
    }

    private Double parseRequiredDouble(String key, String label) {
        String value = requireText(key, label);
        if (value == null) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(value);
            ValidationUtils.requireNonNegative(parsed, label);
            return parsed;
        } catch (NumberFormatException exception) {
            showMessage(label + " must be numeric.");
            return null;
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
            return null;
        }
    }

    private Double parseOptionalDouble(String key, double defaultValue, String label) {
        String value = getText(key).trim();
        if (ValidationUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            double parsed = Double.parseDouble(value);
            ValidationUtils.requireNonNegative(parsed, label);
            return parsed;
        } catch (NumberFormatException exception) {
            showMessage(label + " must be numeric.");
            return null;
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
            return null;
        }
    }

    private String trimDouble(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private String getText(String key) {
        View view = fieldViews.get(key);
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            return text == null ? "" : text.toString();
        }
        return "";
    }

    private void setText(String key, String value) {
        View view = fieldViews.get(key);
        if (view instanceof TextView) {
            ((TextView) view).setText(value);
        }
    }

    private boolean isChecked(String key) {
        View view = fieldViews.get(key);
        if (view instanceof SwitchCompat) {
            return ((SwitchCompat) view).isChecked();
        }
        return false;
    }

    private void setChecked(String key, boolean checked) {
        View view = fieldViews.get(key);
        if (view instanceof SwitchCompat) {
            ((SwitchCompat) view).setChecked(checked);
        }
    }

    private void lockField(String key) {
        View view = fieldViews.get(key);
        if (view instanceof TextView) {
            view.setEnabled(false);
            if (view instanceof TextInputEditText) {
                ((TextInputEditText) view).setFocusable(false);
                ((TextInputEditText) view).setFocusableInTouchMode(false);
            }
        } else if (view != null) {
            view.setEnabled(false);
        }
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
}
