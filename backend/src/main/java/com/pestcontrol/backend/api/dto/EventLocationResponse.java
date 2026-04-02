package com.pestcontrol.backend.api.dto;

import com.pestcontrol.backend.domain.Location;

public class EventLocationResponse {
    private Long locationId;
    private String name;
    private String addressLine;
    private String city;
    private String province;
    private String postalCode;

    public EventLocationResponse(Location location) {
        this.locationId = location.getLocationId();
        this.name = location.getName();
        this.addressLine = location.getAddressLine();
        this.city = location.getCity();
        this.province = location.getProvince();
        this.postalCode = location.getPostalCode();
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
}