# Rental Manager Pro

Android Studio app built with Java, XML, Firebase Authentication, and Firebase Realtime Database for managing:

- Room masters and rental people
- Garage masters and vehicle owners
- Billing months
- Monthly room payment setup and collection
- Monthly garage payment setup and collection

## Current scaffold

This repository now includes a PRD-aligned v1 foundation:

- Splash screen
- Email/password login
- Google login
- First-time Google profile auto-create in Realtime Database
- Bottom navigation with Home, Room, and Garage
- Settings workflow hub
- Generic create and edit forms for room, garage, rental, and vehicle records
- Tabbed record viewer for room, garage, rental, and vehicle data
- Month creation screen
- Payment setup screen for room and garage monthly nodes
- Room monthly billing screen with electricity calculation and final status lock
- Garage monthly billing screen with charge collection and final status lock
- Monthly detail screen for room and garage records

## Firebase setup

1. Create a Firebase project.
2. Register this Android app using the current `applicationId`.
3. Put `google-services.json` inside [`app`](D:/AndroidStudio/Ramjanki/app).
4. Enable `Email/Password` and `Google` sign-in in Firebase Authentication.
5. Create a Realtime Database for the project.

The Gradle setup keeps the project source buildable even before `google-services.json` is added, but live authentication and database flows require Firebase to be connected.

You can start from the sample Realtime Database rules file at [firebase-database-rules.json](D:/AndroidStudio/Ramjanki/firebase-database-rules.json).

## Realtime Database shape

The app expects these top-level nodes:

```text
/users/{uid}
/rooms/{roomNumber}
/garages/{areaNumber}
/rentals/{rentalId}
/vehicles/{vehicleId}
/months/{monthId}
/room_payments/{monthId}/{roomId}
/garage_payments/{monthId}/{garageId}
```

### User profile

Created automatically on first Google login:

```json
{
  "name": "Owner Name",
  "email_id": "owner@example.com",
  "role": "user",
  "room_p": false,
  "garage_p": false,
  "createAt": "2026-05-13T10:00:00",
  "session_id": "uuid-value"
}
```

## Main data models

### Room

```json
{
  "room_number": 1,
  "room_charge": 4000,
  "bijli_unit_start": 120,
  "bijli_charge": 8,
  "extra_charge": 0,
  "room_active": true,
  "rental_id": "",
  "createAt": "2026-05-13T10:00:00"
}
```

### Garage

```json
{
  "area_number": "G-1",
  "area_mark": "Front Bay",
  "vehicle_owner_id": "",
  "charge": 1200,
  "is_active": true,
  "create_at": "2026-05-13T10:00:00"
}
```

### Rental person

```json
{
  "id": "rental-id",
  "name": "Ramesh",
  "phone_number": "9876543210",
  "adhar_number": "",
  "address": "",
  "entry_date": "2026-05-01",
  "start_r_date": "2026-05-01",
  "status": "active",
  "room_number": "1",
  "createAt": "2026-05-13T10:00:00"
}
```

### Vehicle

```json
{
  "id": "vehicle-id",
  "owner_name": "Sohan",
  "owner_number": "9876501234",
  "owner_adhar_number": "",
  "address": "",
  "vehicle_number": "UP32AB1234",
  "vehicle_name": "Swift",
  "other_vehicle_details": "",
  "garage_number": "G-1",
  "entry_date": "2026-05-01",
  "start_rent_date": "2026-05-01",
  "status": "active",
  "createAt": "2026-05-13T10:00:00"
}
```

## Notes

- Room and garage setup currently skip unassigned masters when creating monthly payment nodes.
- Final monthly status uses `complete` and `transfer`.
- Room payment tracks `rent_received` and `bijlibill_received` separately from final settlement status.
- Garage payment tracks `charge_received` separately from final settlement status.
- Month payment nodes now keep room/rental and garage/vehicle snapshot fields so historical detail screens remain stable even if master data changes later.
- Final `complete` or `transfer` status is treated as locked in the repository, not only in UI.

## Next steps

- Add Firebase security rules aligned with role checks.
- Add stronger state-machine validation for billing transitions.
- Add instrumentation tests for the new form and billing flows.
