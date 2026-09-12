package com.example.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Perazim Mission Church Campus location.
 * Compliant with Guidebook §18.
 */
public class Campus implements Serializable {

    private String id;
    private String name;
    private String address;
    private String pastorName;
    private String phoneNumber;
    private String email;
    private double latitude;
    private double longitude;
    private boolean isHeadquarters;

    public Campus() {
    }

    public Campus(String id, String name, String address, String pastorName,
                  String phoneNumber, String email, double latitude, double longitude,
                  boolean isHeadquarters) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.pastorName = pastorName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isHeadquarters = isHeadquarters;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPastorName() { return pastorName; }
    public void setPastorName(String pastorName) { this.pastorName = pastorName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isHeadquarters() { return isHeadquarters; }
    public void setHeadquarters(boolean headquarters) { isHeadquarters = headquarters; }
}
