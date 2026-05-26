package com.gtechapps.ramjankimandir.model;

public class RoomPayment {
    public String room_id = "";
    public String rental_id = "";
    public String room_label = "";
    public String rental_name = "";
    public String rental_phone_number = "";
    public String rental_address = "";
    public String rental_status = "";
    public String rental_entry_date = "";
    public String rental_start_r_date = "";
    public String bijlibill_status = BillingStatus.NOT_CALCULATED;
    public String rent_status = BillingStatus.PENDING;
    public double room_rent;
    public double bijlibill;
    public double bijli_charge;
    public double bill_start_unit;
    public String bill_current_unit = "";
    public String status = BillingStatus.PENDING;
    public double other_charge;
    public double total_charge;
    public boolean rent_received;
    public boolean bijlibill_received;
    public String createdAt = "";

    public RoomPayment() {
    }
}
