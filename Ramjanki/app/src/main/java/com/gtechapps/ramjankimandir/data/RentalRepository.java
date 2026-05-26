package com.gtechapps.ramjankimandir.data;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.gtechapps.ramjankimandir.model.BillingMonth;
import com.gtechapps.ramjankimandir.model.BillingStatus;
import com.gtechapps.ramjankimandir.model.Garage;
import com.gtechapps.ramjankimandir.model.GaragePayment;
import com.gtechapps.ramjankimandir.model.RecordItem;
import com.gtechapps.ramjankimandir.model.RentalPerson;
import com.gtechapps.ramjankimandir.model.Room;
import com.gtechapps.ramjankimandir.model.RoomPayment;
import com.gtechapps.ramjankimandir.model.VehicleEntry;
import com.gtechapps.ramjankimandir.util.BillingCalculator;
import com.gtechapps.ramjankimandir.util.BillingRules;
import com.gtechapps.ramjankimandir.util.MonthUtils;
import com.gtechapps.ramjankimandir.util.TimeUtils;
import com.gtechapps.ramjankimandir.util.ValidationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RentalRepository {

    public static final String TYPE_ROOM = "room";
    public static final String TYPE_GARAGE = "garage";
    public static final String TYPE_RENTAL = "rental";
    public static final String TYPE_VEHICLE = "vehicle";

    private static final String NODE_ROOMS = "rooms";
    private static final String NODE_GARAGES = "garages";
    private static final String NODE_RENTALS = "rentals";
    private static final String NODE_VEHICLES = "vehicles";
    private static final String NODE_MONTHS = "months";
    private static final String NODE_ROOM_PAYMENTS = "room_payments";
    private static final String NODE_GARAGE_PAYMENTS = "garage_payments";

    private final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

    public void saveRoom(Room room, boolean editMode, OperationCallback callback) {
        try {
            validateRoom(room);
        } catch (IllegalArgumentException exception) {
            callback.onComplete(false, exception.getMessage());
            return;
        }
        DatabaseReference target = rootRef.child(NODE_ROOMS).child(String.valueOf(room.room_number));
        if (editMode) {
            target.setValue(room)
                    .addOnSuccessListener(unused -> callback.onComplete(true, "Room saved."))
                    .addOnFailureListener(error -> callback.onComplete(false, error.getMessage()));
            return;
        }
        target.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    callback.onComplete(false, "Room number already exists.");
                    return;
                }
                target.setValue(room)
                        .addOnSuccessListener(unused -> callback.onComplete(true, "Room created."))
                        .addOnFailureListener(error -> callback.onComplete(false, error.getMessage()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(false, error.getMessage());
            }
        });
    }

    public void saveGarage(Garage garage, boolean editMode, OperationCallback callback) {
        try {
            validateGarage(garage);
        } catch (IllegalArgumentException exception) {
            callback.onComplete(false, exception.getMessage());
            return;
        }
        String key = garage.area_number.trim();
        DatabaseReference target = rootRef.child(NODE_GARAGES).child(key);
        if (editMode) {
            target.setValue(garage)
                    .addOnSuccessListener(unused -> callback.onComplete(true, "Garage saved."))
                    .addOnFailureListener(error -> callback.onComplete(false, error.getMessage()));
            return;
        }
        target.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    callback.onComplete(false, "Garage number already exists.");
                    return;
                }
                target.setValue(garage)
                        .addOnSuccessListener(unused -> callback.onComplete(true, "Garage created."))
                        .addOnFailureListener(error -> callback.onComplete(false, error.getMessage()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(false, error.getMessage());
            }
        });
    }

    public void saveRental(RentalPerson rentalPerson, OperationCallback callback) {
        normalizeRentalAssignment(rentalPerson);
        if (ValidationUtils.isBlank(rentalPerson.room_number)) {
            loadExistingRental(rentalPerson.id, new RepositoryCallback<RentalPerson>() {
                @Override
                public void onSuccess(RentalPerson existingRental) {
                    persistRental(rentalPerson, existingRental, null, callback);
                }

                @Override
                public void onError(String message) {
                    persistRental(rentalPerson, null, null, callback);
                }
            });
            return;
        }
        rootRef.child(NODE_ROOMS).child(rentalPerson.room_number.trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Room targetRoom = snapshot.getValue(Room.class);
                        if (targetRoom == null) {
                            callback.onComplete(false, "Selected room does not exist.");
                            return;
                        }
                        if (!targetRoom.room_active && shouldLinkRental(rentalPerson.status)) {
                            callback.onComplete(false, "Inactive room cannot have an active rental assignment.");
                            return;
                        }
                        loadExistingRental(rentalPerson.id, new RepositoryCallback<RentalPerson>() {
                            @Override
                            public void onSuccess(RentalPerson existingRental) {
                                persistRental(rentalPerson, existingRental, targetRoom, callback);
                            }

                            @Override
                            public void onError(String message) {
                                persistRental(rentalPerson, null, targetRoom, callback);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(false, error.getMessage());
                    }
                });
    }

    public void saveVehicle(VehicleEntry vehicleEntry, OperationCallback callback) {
        normalizeVehicleAssignment(vehicleEntry);
        if (ValidationUtils.isBlank(vehicleEntry.garage_number)) {
            validateVehicleNumberAndPersist(vehicleEntry, null, callback);
            return;
        }
        rootRef.child(NODE_GARAGES).child(vehicleEntry.garage_number.trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Garage targetGarage = snapshot.getValue(Garage.class);
                        if (targetGarage == null) {
                            callback.onComplete(false, "Selected garage does not exist.");
                            return;
                        }
                        if (!targetGarage.is_active && shouldLinkVehicle(vehicleEntry.status)) {
                            callback.onComplete(false, "Inactive garage cannot have an active vehicle assignment.");
                            return;
                        }
                        validateVehicleNumberAndPersist(vehicleEntry, targetGarage, callback);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(false, error.getMessage());
                    }
                });
    }

    public void loadRooms(RepositoryCallback<List<Room>> callback) {
        rootRef.child(NODE_ROOMS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Room> rooms = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Room room = child.getValue(Room.class);
                    if (room != null) {
                        rooms.add(room);
                    }
                }
                Collections.sort(rooms, Comparator.comparingInt(room -> room.room_number));
                callback.onSuccess(rooms);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void loadGarages(RepositoryCallback<List<Garage>> callback) {
        rootRef.child(NODE_GARAGES).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Garage> garages = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Garage garage = child.getValue(Garage.class);
                    if (garage != null) {
                        garages.add(garage);
                    }
                }
                Collections.sort(garages, Comparator.comparing(garage -> garage.area_number));
                callback.onSuccess(garages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void loadRentals(RepositoryCallback<List<RentalPerson>> callback) {
        rootRef.child(NODE_RENTALS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<RentalPerson> rentals = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    RentalPerson rentalPerson = child.getValue(RentalPerson.class);
                    if (rentalPerson != null) {
                        rentals.add(rentalPerson);
                    }
                }
                Collections.sort(rentals, Comparator.comparing(rental -> safe(rental.name)));
                callback.onSuccess(rentals);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void loadVehicles(RepositoryCallback<List<VehicleEntry>> callback) {
        rootRef.child(NODE_VEHICLES).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<VehicleEntry> vehicles = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    VehicleEntry vehicleEntry = child.getValue(VehicleEntry.class);
                    if (vehicleEntry != null) {
                        vehicles.add(vehicleEntry);
                    }
                }
                Collections.sort(vehicles, Comparator.comparing(vehicle -> safe(vehicle.owner_name)));
                callback.onSuccess(vehicles);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void loadRoom(String roomId, RepositoryCallback<Room> callback) {
        rootRef.child(NODE_ROOMS).child(roomId).addListenerForSingleValueEvent(singleValue(Room.class, callback));
    }

    public void loadGarage(String garageId, RepositoryCallback<Garage> callback) {
        rootRef.child(NODE_GARAGES).child(garageId).addListenerForSingleValueEvent(singleValue(Garage.class, callback));
    }

    public void loadRental(String rentalId, RepositoryCallback<RentalPerson> callback) {
        rootRef.child(NODE_RENTALS).child(rentalId).addListenerForSingleValueEvent(singleValue(RentalPerson.class, callback));
    }

    public void loadVehicle(String vehicleId, RepositoryCallback<VehicleEntry> callback) {
        rootRef.child(NODE_VEHICLES).child(vehicleId).addListenerForSingleValueEvent(singleValue(VehicleEntry.class, callback));
    }

    public void loadMonths(RepositoryCallback<List<BillingMonth>> callback) {
        rootRef.child(NODE_MONTHS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<BillingMonth> months = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    BillingMonth month = child.getValue(BillingMonth.class);
                    if (month != null) {
                        if (ValidationUtils.isBlank(month.month_name)) {
                            month.month_name = MonthUtils.displayName(month.month_id);
                        }
                        months.add(month);
                    }
                }
                callback.onSuccess(MonthUtils.sortMonths(months));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void createNextMonth(OperationCallback callback) {
        loadMonths(new RepositoryCallback<List<BillingMonth>>() {
            @Override
            public void onSuccess(List<BillingMonth> months) {
                BillingMonth target = MonthUtils.nextMonth(months);
                rootRef.child(NODE_MONTHS).child(target.month_id).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            callback.onComplete(false, "Month already exists.");
                            return;
                        }
                        rootRef.child(NODE_MONTHS).child(target.month_id).setValue(target)
                                .addOnSuccessListener(unused -> callback.onComplete(true, target.month_name + " created."))
                                .addOnFailureListener(error -> callback.onComplete(false, error.getMessage()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(false, error.getMessage());
                    }
                });
            }

            @Override
            public void onError(String message) {
                callback.onComplete(false, message);
            }
        });
    }

    public void setupRoomPayments(String monthId, OperationCallback callback) {
        loadRooms(new RepositoryCallback<List<Room>>() {
            @Override
            public void onSuccess(List<Room> rooms) {
                loadRentals(new RepositoryCallback<List<RentalPerson>>() {
                    @Override
                    public void onSuccess(List<RentalPerson> rentals) {
                        Map<String, RentalPerson> rentalMap = new HashMap<>();
                        for (RentalPerson rental : rentals) {
                            rentalMap.put(rental.id, rental);
                        }
                        Map<String, Object> updates = new HashMap<>();
                        for (Room room : rooms) {
                            if (!room.room_active || ValidationUtils.isBlank(room.rental_id)) {
                                continue;
                            }
                            RentalPerson rental = rentalMap.get(room.rental_id);
                            if (rental == null || !shouldLinkRental(rental.status)) {
                                continue;
                            }
                            RoomPayment roomPayment = buildRoomPayment(room, rental);
                            updates.put(NODE_ROOM_PAYMENTS + "/" + monthId + "/" + roomPayment.room_id, roomPayment);
                        }
                        if (updates.isEmpty()) {
                            callback.onComplete(false, "No active room assignments found for setup.");
                            return;
                        }
                        mergeIfAbsent(NODE_ROOM_PAYMENTS, monthId, updates, "Room payment setup completed.", callback);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onComplete(false, message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                callback.onComplete(false, message);
            }
        });
    }

    public void setupGaragePayments(String monthId, OperationCallback callback) {
        loadGarages(new RepositoryCallback<List<Garage>>() {
            @Override
            public void onSuccess(List<Garage> garages) {
                loadVehicles(new RepositoryCallback<List<VehicleEntry>>() {
                    @Override
                    public void onSuccess(List<VehicleEntry> vehicles) {
                        Map<String, VehicleEntry> vehicleMap = new HashMap<>();
                        for (VehicleEntry vehicle : vehicles) {
                            vehicleMap.put(vehicle.id, vehicle);
                        }
                        Map<String, Object> updates = new HashMap<>();
                        for (Garage garage : garages) {
                            if (!garage.is_active || ValidationUtils.isBlank(garage.vehicle_owner_id)) {
                                continue;
                            }
                            VehicleEntry vehicle = vehicleMap.get(garage.vehicle_owner_id);
                            if (vehicle == null || !shouldLinkVehicle(vehicle.status)) {
                                continue;
                            }
                            GaragePayment garagePayment = buildGaragePayment(garage, vehicle);
                            updates.put(NODE_GARAGE_PAYMENTS + "/" + monthId + "/" + garagePayment.garage_id, garagePayment);
                        }
                        if (updates.isEmpty()) {
                            callback.onComplete(false, "No active garage assignments found for setup.");
                            return;
                        }
                        mergeIfAbsent(NODE_GARAGE_PAYMENTS, monthId, updates, "Garage payment setup completed.", callback);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onComplete(false, message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                callback.onComplete(false, message);
            }
        });
    }

    public void loadRoomPayments(String monthId, RepositoryCallback<List<RoomPayment>> callback) {
        rootRef.child(NODE_ROOM_PAYMENTS).child(monthId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<RoomPayment> payments = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    RoomPayment payment = child.getValue(RoomPayment.class);
                    if (payment != null) {
                        BillingRules.syncRoomDerivedStatuses(payment);
                        payments.add(payment);
                    }
                }
                Collections.sort(payments, Comparator.comparingInt(payment -> parseRoomId(payment.room_id)));
                callback.onSuccess(payments);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void loadGaragePayments(String monthId, RepositoryCallback<List<GaragePayment>> callback) {
        rootRef.child(NODE_GARAGE_PAYMENTS).child(monthId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<GaragePayment> payments = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    GaragePayment payment = child.getValue(GaragePayment.class);
                    if (payment != null) {
                        payments.add(payment);
                    }
                }
                Collections.sort(payments, Comparator.comparing(payment -> safe(payment.garage_id)));
                callback.onSuccess(payments);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void loadRoomPayment(String monthId, String roomId, RepositoryCallback<RoomPayment> callback) {
        rootRef.child(NODE_ROOM_PAYMENTS).child(monthId).child(roomId)
                .addListenerForSingleValueEvent(singleValue(RoomPayment.class, callback));
    }

    public void loadGaragePayment(String monthId, String garageId, RepositoryCallback<GaragePayment> callback) {
        rootRef.child(NODE_GARAGE_PAYMENTS).child(monthId).child(garageId)
                .addListenerForSingleValueEvent(singleValue(GaragePayment.class, callback));
    }

    public void updateRoomPayment(String monthId, RoomPayment roomPayment, OperationCallback callback) {
        BillingRules.syncRoomDerivedStatuses(roomPayment);
        rootRef.child(NODE_ROOM_PAYMENTS).child(monthId).child(roomPayment.room_id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        RoomPayment existing = snapshot.getValue(RoomPayment.class);
                        try {
                            BillingRules.validateRoomPaymentUpdate(existing, roomPayment);
                        } catch (IllegalArgumentException | IllegalStateException exception) {
                            callback.onComplete(false, exception.getMessage());
                            return;
                        }
                        roomPayment.total_charge = BillingCalculator.calculateRoomTotal(
                                roomPayment.room_rent,
                                roomPayment.bijlibill,
                                roomPayment.other_charge
                        );
                        Map<String, Object> updates = new HashMap<>();
                        updates.put(NODE_ROOM_PAYMENTS + "/" + monthId + "/" + roomPayment.room_id, roomPayment);
                        if (!ValidationUtils.isBlank(roomPayment.bill_current_unit)) {
                            updates.put(NODE_ROOMS + "/" + roomPayment.room_id + "/bijli_unit_start",
                                    parseDouble(roomPayment.bill_current_unit));
                        }
                        rootRef.updateChildren(updates)
                                .addOnSuccessListener(unused -> callback.onComplete(true, "Room payment updated."))
                                .addOnFailureListener(error -> callback.onComplete(false, error.getMessage()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(false, error.getMessage());
                    }
                });
    }

    public void updateGaragePayment(String monthId, GaragePayment garagePayment, OperationCallback callback) {
        rootRef.child(NODE_GARAGE_PAYMENTS).child(monthId).child(garagePayment.garage_id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        GaragePayment existing = snapshot.getValue(GaragePayment.class);
                        try {
                            BillingRules.validateGaragePaymentUpdate(existing, garagePayment);
                        } catch (IllegalArgumentException | IllegalStateException exception) {
                            callback.onComplete(false, exception.getMessage());
                            return;
                        }
                        rootRef.child(NODE_GARAGE_PAYMENTS).child(monthId).child(garagePayment.garage_id)
                                .setValue(garagePayment)
                                .addOnSuccessListener(unused -> callback.onComplete(true, "Garage payment updated."))
                                .addOnFailureListener(error -> callback.onComplete(false, error.getMessage()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(false, error.getMessage());
                    }
                });
    }

    public List<RecordItem> toRoomItems(List<Room> rooms) {
        List<RecordItem> items = new ArrayList<>();
        for (Room room : rooms) {
            items.add(new RecordItem(
                    String.valueOf(room.room_number),
                    "Room " + room.room_number,
                    "Rent Rs " + trimDouble(room.room_charge)
                            + " | Bijli rate Rs " + trimDouble(room.bijli_charge)
                            + " | Start unit " + trimDouble(room.bijli_unit_start),
                    "Status: " + (
                            room.rental_id != null && !room.rental_id.trim().isEmpty()
                                    ? "Booked ✅"
                                    : "Available"
                            )
                            +"Extra Rs " + trimDouble(room.extra_charge)
                            + "\nRental id: " + safeFallback(room.rental_id, "blank")
                            + "\nCreated: " + safeFallback(room.createAt, "-"),
                    room.room_active ? "active" : "inactive"
            ));
        }
        return items;
    }

    public List<RecordItem> toGarageItems(List<Garage> garages) {
        List<RecordItem> items = new ArrayList<>();
        for (Garage garage : garages) {
            items.add(new RecordItem(
                    garage.area_number,
                    "Garage " + garage.area_number,
                    "Mark: " + safeFallback(garage.area_mark, "-")
                            + " | Charge Rs " + trimDouble(garage.charge),
                    "Vehicle owner id: " + safeFallback(garage.vehicle_owner_id, "blank")
                            + "\nCreated: " + safeFallback(garage.create_at, "-"),
                    garage.is_active ? "active" : "inactive"
            ));
        }
        return items;
    }

    public List<RecordItem> toRentalItems(List<RentalPerson> rentals) {
        List<RecordItem> items = new ArrayList<>();
        for (RentalPerson rental : rentals) {
            items.add(new RecordItem(
                    rental.id,
                    safeFallback(rental.name, "Rental person"),
                    "Phone: " + safeFallback(rental.phone_number, "-")
                            + " | Room " + safeFallback(rental.room_number, "-"),
                    "Entry: " + safeFallback(rental.entry_date, "-")
                            + "\nStart rent: " + safeFallback(rental.start_r_date, "-")
                            + "\nAddress: " + safeFallback(rental.address, "-"),
                    safeFallback(rental.status, "pending")
            ));
        }
        return items;
    }

    public List<RecordItem> toVehicleItems(List<VehicleEntry> vehicles) {
        List<RecordItem> items = new ArrayList<>();
        for (VehicleEntry vehicle : vehicles) {
            items.add(new RecordItem(
                    vehicle.id,
                    safeFallback(vehicle.owner_name, "Vehicle owner"),
                    safeFallback(vehicle.vehicle_number, "-")
                            + " | Garage " + safeFallback(vehicle.garage_number, "-"),
                    "Vehicle: " + safeFallback(vehicle.vehicle_name, "-")
                            + "\nEntry: " + safeFallback(vehicle.entry_date, "-")
                            + "\nOwner number: " + safeFallback(vehicle.owner_number, "-"),
                    safeFallback(vehicle.status, "pending")
            ));
        }
        return items;
    }

    private void validateRoom(Room room) {
        if (room == null || room.room_number <= 0) {
            throw new IllegalArgumentException("Room number is required.");
        }
        ValidationUtils.requireNonNegative(room.room_charge, "Room charge");
        ValidationUtils.requireNonNegative(room.bijli_unit_start, "Bijli unit start");
        ValidationUtils.requireNonNegative(room.bijli_charge, "Bijli charge");
        ValidationUtils.requireNonNegative(room.extra_charge, "Extra charge");
        room.rental_id = safe(room.rental_id);
        if (ValidationUtils.isBlank(room.createAt)) {
            room.createAt = TimeUtils.nowIsoTimestamp();
        }
    }

    private void validateGarage(Garage garage) {
        if (garage == null || ValidationUtils.isBlank(garage.area_number)) {
            throw new IllegalArgumentException("Garage area number is required.");
        }
        ValidationUtils.requireNonNegative(garage.charge, "Garage charge");
        garage.vehicle_owner_id = safe(garage.vehicle_owner_id);
        if (ValidationUtils.isBlank(garage.create_at)) {
            garage.create_at = TimeUtils.nowIsoTimestamp();
        }
    }

    private void normalizeRentalAssignment(RentalPerson rentalPerson) {
        if (rentalPerson == null) {
            return;
        }
        String roomNumber = safe(rentalPerson.room_number);
        if ("reject".equalsIgnoreCase(rentalPerson.status)) {
            rentalPerson.status = "reject";
            rentalPerson.room_number = "";
            return;
        }
        rentalPerson.room_number = roomNumber;
        rentalPerson.status = ValidationUtils.isBlank(roomNumber) ? "pending" : "active";
    }

    private void normalizeVehicleAssignment(VehicleEntry vehicleEntry) {
        if (vehicleEntry == null) {
            return;
        }
        String garageNumber = safe(vehicleEntry.garage_number);
        if ("reject".equalsIgnoreCase(vehicleEntry.status)) {
            vehicleEntry.status = "reject";
            vehicleEntry.garage_number = "";
            return;
        }
        vehicleEntry.garage_number = garageNumber;
        vehicleEntry.status = ValidationUtils.isBlank(garageNumber) ? "pending" : "active";
    }

    private void validateVehicleNumberAndPersist(VehicleEntry vehicleEntry,
                                                 Garage targetGarage,
                                                 OperationCallback callback) {
        rootRef.child(NODE_VEHICLES).orderByChild("vehicle_number")
                .equalTo(vehicleEntry.vehicle_number)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot vehicleSnapshot) {
                        if (hasConflictingVehicleNumber(vehicleEntry.id, vehicleSnapshot)) {
                            callback.onComplete(false, "Vehicle number already exists.");
                            return;
                        }
                        loadExistingVehicle(vehicleEntry.id, new RepositoryCallback<VehicleEntry>() {
                            @Override
                            public void onSuccess(VehicleEntry existingVehicle) {
                                persistVehicle(vehicleEntry, existingVehicle, targetGarage, callback);
                            }

                            @Override
                            public void onError(String message) {
                                persistVehicle(vehicleEntry, null, targetGarage, callback);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(false, error.getMessage());
                    }
                });
    }

    private void loadExistingRental(String rentalId, RepositoryCallback<RentalPerson> callback) {
        if (ValidationUtils.isBlank(rentalId)) {
            callback.onError("No existing rental.");
            return;
        }
        loadRental(rentalId, callback);
    }

    private void loadExistingVehicle(String vehicleId, RepositoryCallback<VehicleEntry> callback) {
        if (ValidationUtils.isBlank(vehicleId)) {
            callback.onError("No existing vehicle.");
            return;
        }
        loadVehicle(vehicleId, callback);
    }

    private void persistRental(RentalPerson rentalPerson,
                               RentalPerson existingRental,
                               Room targetRoom,
                               OperationCallback callback) {
        String rentalId = ValidationUtils.isBlank(rentalPerson.id)
                ? rootRef.child(NODE_RENTALS).push().getKey()
                : rentalPerson.id;
        if (rentalId == null) {
            callback.onComplete(false, "Unable to create rental id.");
            return;
        }
        rentalPerson.id = rentalId;
        String previousRoomNumber = existingRental == null ? "" : safe(existingRental.room_number);
        String targetRoomNumber = safe(rentalPerson.room_number);
        if (shouldLinkRental(rentalPerson.status)
                && targetRoom != null
                && !ValidationUtils.isBlank(targetRoom.rental_id)
                && !targetRoom.rental_id.equals(rentalId)) {
            callback.onComplete(false, "This room already has an active rental assignment.");
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put(NODE_RENTALS + "/" + rentalId, rentalPerson);
        if (!ValidationUtils.isBlank(previousRoomNumber)
                && !previousRoomNumber.equals(targetRoomNumber)) {
            updates.put(NODE_ROOMS + "/" + previousRoomNumber + "/rental_id", "");
        }
        if (shouldLinkRental(rentalPerson.status) && targetRoom != null) {
            updates.put(NODE_ROOMS + "/" + targetRoomNumber + "/rental_id", rentalId);
        } else if (!ValidationUtils.isBlank(previousRoomNumber)) {
            updates.put(NODE_ROOMS + "/" + previousRoomNumber + "/rental_id", "");
        }
        rootRef.updateChildren(updates)
                .addOnSuccessListener(unused -> callback.onComplete(true, "Rental entry saved."))
                .addOnFailureListener(error -> callback.onComplete(false, error.getMessage()));
    }

    private void persistVehicle(VehicleEntry vehicleEntry,
                                VehicleEntry existingVehicle,
                                Garage targetGarage,
                                OperationCallback callback) {
        String vehicleId = ValidationUtils.isBlank(vehicleEntry.id)
                ? rootRef.child(NODE_VEHICLES).push().getKey()
                : vehicleEntry.id;
        if (vehicleId == null) {
            callback.onComplete(false, "Unable to create vehicle id.");
            return;
        }
        vehicleEntry.id = vehicleId;
        String previousGarageNumber = existingVehicle == null ? "" : safe(existingVehicle.garage_number);
        String targetGarageNumber = safe(vehicleEntry.garage_number);
        if (shouldLinkVehicle(vehicleEntry.status)
                && targetGarage != null
                && !ValidationUtils.isBlank(targetGarage.vehicle_owner_id)
                && !targetGarage.vehicle_owner_id.equals(vehicleId)) {
            callback.onComplete(false, "This garage already has an active vehicle assignment.");
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put(NODE_VEHICLES + "/" + vehicleId, vehicleEntry);
        if (!ValidationUtils.isBlank(previousGarageNumber)
                && !previousGarageNumber.equals(targetGarageNumber)) {
            updates.put(NODE_GARAGES + "/" + previousGarageNumber + "/vehicle_owner_id", "");
        }
        if (shouldLinkVehicle(vehicleEntry.status) && targetGarage != null) {
            updates.put(NODE_GARAGES + "/" + targetGarageNumber + "/vehicle_owner_id", vehicleId);
        } else if (!ValidationUtils.isBlank(previousGarageNumber)) {
            updates.put(NODE_GARAGES + "/" + previousGarageNumber + "/vehicle_owner_id", "");
        }
        rootRef.updateChildren(updates)
                .addOnSuccessListener(unused -> callback.onComplete(true, "Vehicle entry saved."))
                .addOnFailureListener(error -> callback.onComplete(false, error.getMessage()));
    }

    private RoomPayment buildRoomPayment(Room room, RentalPerson rental) {
        RoomPayment payment = new RoomPayment();
        payment.room_id = String.valueOf(room.room_number);
        payment.rental_id = rental.id;
        payment.room_label = "Room " + room.room_number;
        payment.rental_name = rental.name;
        payment.rental_phone_number = rental.phone_number;
        payment.rental_address = rental.address;
        payment.rental_status = rental.status;
        payment.rental_entry_date = rental.entry_date;
        payment.rental_start_r_date = rental.start_r_date;
        payment.room_rent = room.room_charge;
        payment.bijlibill = 0D;
        payment.bijli_charge = room.bijli_charge;
        payment.bill_start_unit = room.bijli_unit_start;
        payment.bill_current_unit = "";
        payment.status = BillingStatus.PENDING;
        payment.other_charge = room.extra_charge;
        payment.total_charge = BillingCalculator.calculateRoomTotal(room.room_charge, 0D, room.extra_charge);
        payment.rent_received = false;
        payment.bijlibill_received = false;
        payment.createdAt = TimeUtils.nowIsoTimestamp();
        BillingRules.syncRoomDerivedStatuses(payment);
        return payment;
    }

    private GaragePayment buildGaragePayment(Garage garage, VehicleEntry vehicle) {
        GaragePayment payment = new GaragePayment();
        payment.garage_id = garage.area_number;
        payment.vehicle_owner_id = vehicle.id;
        payment.garage_mark = garage.area_mark;
        payment.owner_name = vehicle.owner_name;
        payment.owner_number = vehicle.owner_number;
        payment.owner_address = vehicle.address;
        payment.vehicle_number = vehicle.vehicle_number;
        payment.vehicle_name = vehicle.vehicle_name;
        payment.other_vehicle_details = vehicle.other_vehicle_details;
        payment.vehicle_status = vehicle.status;
        payment.vehicle_entry_date = vehicle.entry_date;
        payment.vehicle_start_rent_date = vehicle.start_rent_date;
        payment.status = BillingStatus.PENDING;
        payment.charge = garage.charge;
        payment.charge_received = false;
        payment.createdAt = TimeUtils.nowIsoTimestamp();
        return payment;
    }

    private void mergeIfAbsent(String nodeName,
                               String monthId,
                               Map<String, Object> freshUpdates,
                               String successMessage,
                               OperationCallback callback) {
        rootRef.child(nodeName).child(monthId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> filtered = new HashMap<>();
                String keyPrefix = nodeName + "/" + monthId + "/";
                for (Map.Entry<String, Object> entry : freshUpdates.entrySet()) {
                    String shortKey = entry.getKey().substring(keyPrefix.length());
                    if (!snapshot.hasChild(shortKey)) {
                        filtered.put(entry.getKey(), entry.getValue());
                    }
                }
                if (filtered.isEmpty()) {
                    callback.onComplete(false, "Setup already exists for the selected month.");
                    return;
                }
                rootRef.updateChildren(filtered)
                        .addOnSuccessListener(unused -> callback.onComplete(true, successMessage))
                        .addOnFailureListener(error -> callback.onComplete(false, error.getMessage()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(false, error.getMessage());
            }
        });
    }

    private boolean hasConflictingVehicleNumber(String vehicleId, DataSnapshot snapshot) {
        for (DataSnapshot child : snapshot.getChildren()) {
            if (!safe(child.getKey()).equals(safe(vehicleId))) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldLinkRental(String status) {
        return "active".equalsIgnoreCase(status);
    }

    private boolean shouldLinkVehicle(String status) {
        return "active".equalsIgnoreCase(status);
    }

    private int parseRoomId(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(safe(value));
        } catch (NumberFormatException exception) {
            return 0D;
        }
    }

    private <T> ValueEventListener singleValue(Class<T> type, RepositoryCallback<T> callback) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                T data = snapshot.getValue(type);
                if (data == null) {
                    callback.onError("Record not found.");
                    return;
                }
                callback.onSuccess(data);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };
    }

    private String trimDouble(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeFallback(String value, String fallback) {
        return ValidationUtils.isBlank(value) ? fallback : value.trim();
    }
}
