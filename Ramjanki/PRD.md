# PRD: Rental Management App (Room + Garage)

## 1) Product Summary
यह app एक rental management system होगा, जिसमें केवल दो operational modules होंगे: Room Management और Garage Management. App flow में पहले splash screen, फिर login screen आएगी. Login में email/password और Google login दोनों होंगे. First-time Google login पर user profile Firebase Realtime Database में auto-create होगा.

App का navigation structure bottom tab आधारित होगा: Home, Room, Garage, और top bar में Settings icon होगा. Settings से create/update और setup workflows access होंगे. Technology stack Android Studio + Java + XML होगी, backend/storage के लिए केवल Firebase Realtime Database.

## 2) Product Objective
इस app का objective है:
* rooms और garage spaces को create, assign, track, edit, and bill करना
* monthly payment setup और monthly collection tracking maintain करना
* tenant/vehicle owner records को structured रूप में रखना
* owner को एक ही app में operational visibility देना

यह objective आपके दिए गए flow से directly निकलता है: create entities, view list, edit records, month setup, payment setup, monthly room/garage billing, और detail screens.

## 3) Scope

### In scope
* Splash screen
* Login with email/password
* Login with Google
* First-time user profile creation in Firebase
* Main activity with bottom navigation
* Settings screen
* Create room
* Create garage
* Rental person entry
* Vehicle entry
* View created data lists
* Edit existing records
* Month creation and month list
* Setup payment for room and garage
* Monthly room billing screen
* Monthly garage billing screen
* Detail screens for room and garage monthly records

### Out of scope
* UPI/payment gateway integration
* SMS/WhatsApp reminders
* Multi-tenant organization support
* Cloud Firestore migration
* Web dashboard
* PDF invoice generation
* Offline-first sync engine

## 4) Assumptions and Product Decisions
* I am treating this app as an owner-operated internal system, not a consumer-facing marketplace app.
* I am treating “Garadge” as Garage in the PRD.

## 5) User Roles and Permissions

### Roles
* **Owner/Admin:** full access to create, edit, setup payments, update status, and view all records
* **User:** default role for new Google sign-ins. can be upgraded later if needed.

### Permission flags
* `room_p = false` by default
* `garage_p = false` by default

## 6) Core User Journeys

### 6.1 First launch and authentication
1. App opens splash screen.
2. User lands on login screen.
3. User signs in with email + password, or Google login.
4. On first successful Google login, system checks whether user exists.
5. If user does not exist, create profile in Realtime Database with: `name`, `email_id`, `role = user`, `room_p = false`, `garage_p = false`, `createAt`, `session_id`.

### 6.2 Main navigation
After login:
* Bottom navigation with Home / Room / Garage
* Top settings icon

### 6.3 Settings workflows
Settings screen will contain:
* Create Room
* Create Garage
* Rental Entry
* Vehicle Entry
* View created entries details
* Month list
* Create month
* Setup payment

## 7) Functional Requirements

### 7.1 Authentication module
* **FR-1: Email/password login**
* **FR-2: Google login** (Profile creation on first login)

### 7.2 Room creation
* **Fields:** room number, room charge, bijli unit start, bijli charge, extra charge, room_active, rental_id, createAt.
* **Validation:** Unique room number, numeric validation.

### 7.3 Garage creation
* **Fields:** area number, area mark, vehicle owner id, charge, is_active, create_at.
* **Validation:** Unique area number.

### 7.4 Rental person entry
* **Fields:** id, name, phone_number, adhar_number, address, entry date, start_r_date, status, room_number, createAt.

### 7.5 Vehicle entry
* **Fields:** id, owner_name, owner_number, owner_adhar number, address, vehicle number, vehicle name, other details, garage_number, entry date, start rent date, status, createAt.

### 7.6 View created data screen
* ViewPager with 4 tabs: Room list, Garage list, Rental list, Vehicle list.
* Edit button for each item to update records.

### 7.7 Month management
* **Fields:** month_id, month name, createAt.
* **Logic:** Prevent duplicate month creation. Latest month selected by default.

### 7.8 Setup payment
* Writes monthly defaults into `room_payment` and `garage_payment` nodes for active assignments.

### 7.9 Monthly room billing screen
* Display room payment items for selected month.
* Input for current unit -> Calculate bijlibill.
* Buttons: Bijli bill received, Room rent received, Status update (Finalize).

### 7.10 Monthly garage billing screen
* Mirror room billing flow for garages.

### 7.11 Monthly detail activity
* Detailed screen showing full information for a selected month record.

## 8) Data Model (Firebase Realtime Database)
* `users/{uid}`
* `rooms/{roomNumber}`
* `garages/{areaNumber}`
* `rentals/{id}`
* `vehicles/{id}`
* `months/{monthId}`
* `room_payments/{monthId}/{roomId}`
* `garage_payments/{monthId}/{garageId}`

## 9) Validation and Business Rules
* IDs must be unique.
* Numeric fields must reject text.
* Finalized status is irreversible.
* Active room/garage can have only one assigned rental/owner at a time.

## 10) Non-functional Requirements
* Performance: Lazy loading for lists.
* Security: Firebase Auth + Database Rules.
* UX: Clear empty states, consistent labeling.
