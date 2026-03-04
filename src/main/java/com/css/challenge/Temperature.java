package com.css.challenge;

public enum Temperature {
    COLD(StorageType.COOLER),
    HOT(StorageType.HEATER),
    ROOM(StorageType.SHELF);

    private StorageType _idealStorageType;
    private Temperature(StorageType idealStorageType) {
        _idealStorageType = idealStorageType;
    }

    public StorageType getIdealStorageType() {
        return _idealStorageType;
    }

    public static Temperature fromOrderTemp(String temp) {
        return valueOf(temp.toUpperCase());
    }
}
