package com.css.challenge;

public enum StorageType {
    COOLER,
    HEATER,
    SHELF;

    public String toActionTarget() {
        return this.toString().toLowerCase();
    }
}
