package com.assignment.secquraise;

import com.google.firebase.database.DataSnapshot;

public class Data {
    private int captureCount;
    private float frequency;
    private boolean isCharging;
    private int batteryPercentage;
    private boolean networkConnectivity;
    private double latitude;
    private String imageUrl;
    private String dateTimeUpdate;



    private String deviceID;

    public Data() {

    }

    public Data(DataSnapshot snapshot) {
        this.captureCount = snapshot.child("captureCount").getValue(Integer.class);
        this.frequency = snapshot.child("frequency").getValue(Float.class);
        this.isCharging = snapshot.child("isCharging").getValue(Boolean.class);
        this.batteryPercentage = snapshot.child("batteryPercentage").getValue(Integer.class);
        this.networkConnectivity = snapshot.child("networkConnectivity").getValue(Boolean.class);
        this.latitude = snapshot.child("latitude").getValue(Double.class);
        this.imageUrl = snapshot.child("imageUrl").getValue(String.class);
        this.dateTimeUpdate = snapshot.child("dateTimeUpdate").getValue(String.class);
    }

    public int getCaptureCount() {
        return captureCount;
    }

    public void setCaptureCount(int captureCount) {
        this.captureCount = captureCount;
    }

    public float getFrequency() {
        return frequency;
    }

    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    public boolean isCharging() {
        return isCharging;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }
    public void setCharging(boolean charging) {
        isCharging = charging;
    }

    public int getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(int batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }

    public boolean isNetworkConnectivity() {
        return networkConnectivity;
    }

    public void setNetworkConnectivity(boolean networkConnectivity) {
        this.networkConnectivity = networkConnectivity;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDateTimeUpdate() {
        return dateTimeUpdate;
    }

    public void setDateTimeUpdate(String dateTimeUpdate) {
        this.dateTimeUpdate = dateTimeUpdate;
    }
}
