package com.gtechapps.ramjankimandir.model;

public class GaragePayment {
    public String garage_id = "";
    public String vehicle_owner_id = "";
    public String garage_mark = "";
    public String owner_name = "";
    public String owner_number = "";
    public String owner_address = "";
    public String vehicle_number = "";
    public String vehicle_name = "";
    public String other_vehicle_details = "";
    public String vehicle_status = "";
    public String vehicle_entry_date = "";
    public String vehicle_start_rent_date = "";
    public String status = BillingStatus.PENDING;
    public double charge;
    public boolean charge_received;
    public String createdAt = "";

    public GaragePayment() {
    }
}
