package com.gtechapps.ramjankimandir.model;

public class Room {
    public int room_number;
    public double room_charge;
    public double bijli_unit_start;
    public double bijli_charge;
    public double extra_charge;
    public boolean room_active;
    public String rental_id = "";
    public String createAt = "";
    public String status = "Available"; // Added to fix build error

    public Room() {
    }
}
