package com.css.challenge;

import com.css.challenge.client.Order;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StorageTest {

    private static final String O1 = "o1";
    private static final String O2 = "o2";

    private static final String STALE = "stale";
    private static final String MID = "mid";
    private static final String FRESH_ID = "fresh";

    private static final String COLD = "cold";

    private static final int LESS_FRESH = 100;
    private static final int FRESH = 300;
    private static final int MORE_FRESH = 500;

    private static final int DEFAULT_CAP = 5;

    private Order order(String id, int freshness) {
        return new Order(id, "Item", COLD, 10, freshness);
    }

    @Test
    void hasRoom_belowCapacity_returnsTrue() {
        // Arrange
        Storage storage = new Storage(StorageType.COOLER, 2);
        storage.store(order(O1, FRESH), StorageType.COOLER);

        // Act
        boolean sut = storage.hasRoom();

        // Assert
        assertTrue(sut);
    }

    @Test
    void hasRoom_atCapacity_returnsFalse() {
        // Arrange
        Storage storage = new Storage(StorageType.COOLER, 2);
        storage.store(order(O1, FRESH), StorageType.COOLER);
        storage.store(order(O2, FRESH), StorageType.COOLER);

        // Act
        boolean sut = storage.hasRoom();

        // Assert
        assertFalse(sut);
    }

    @Test
    void store_returnsStoredOrderWithCorrectId() {
        // Arrange
        Storage storage = new Storage(StorageType.COOLER, DEFAULT_CAP);

        // Act
        StoredOrder sut = storage.store(order(O1, FRESH), StorageType.COOLER);

        // Assert
        assertEquals(O1, sut.getOrder().getId());
    }

    @Test
    void store_returnsStoredOrderWithCorrectStorageType() {
        // Arrange
        Storage storage = new Storage(StorageType.COOLER, DEFAULT_CAP);

        // Act
        StoredOrder sut = storage.store(order(O1, FRESH), StorageType.COOLER);

        // Assert
        assertEquals(StorageType.COOLER, sut.getCurrentStorageType());
    }

    @Test
    void remove_removesCorrectOrder() {
        // Arrange
        Storage storage = new Storage(StorageType.COOLER, DEFAULT_CAP);
        StoredOrder s1 = storage.store(order(O1, FRESH), StorageType.COOLER);
        storage.store(order(O2, FRESH), StorageType.COOLER);

        // Act
        storage.remove(s1);

        // Assert
        List<StoredOrder> sut = storage.getOrdersByFreshness();
        assertEquals(1, sut.size());
        assertEquals(O2, sut.get(0).getOrder().getId());
    }

    @Test
    void move_addsExistingStoredOrder() {
        // Arrange
        Storage shelf = new Storage(StorageType.SHELF, DEFAULT_CAP);
        Storage cooler = new Storage(StorageType.COOLER, DEFAULT_CAP);
        StoredOrder stored = shelf.store(order(O1, FRESH), StorageType.SHELF);
        shelf.remove(stored);
        stored.moveTo(StorageType.COOLER);

        // Act
        cooler.move(stored);

        // Assert
        List<StoredOrder> sut = cooler.getOrdersByFreshness();
        assertEquals(1, sut.size());
        assertEquals(O1, sut.get(0).getOrder().getId());
    }

    @Test
    void getOrdersByFreshness_returnsSortedLeastFreshFirst() {
        // Arrange
        Storage storage = new Storage(StorageType.SHELF, DEFAULT_CAP);
        storage.store(order(FRESH_ID, MORE_FRESH), StorageType.SHELF);
        storage.store(order(STALE, LESS_FRESH), StorageType.SHELF);
        storage.store(order(MID, FRESH), StorageType.SHELF);

        // Act
        List<StoredOrder> sut = storage.getOrdersByFreshness();

        // Assert
        assertEquals(STALE, sut.get(0).getOrder().getId());
        assertEquals(MID, sut.get(1).getOrder().getId());
        assertEquals(FRESH_ID, sut.get(2).getOrder().getId());
    }

    @Test
    void removeLeastFresh_removesAndReturnsLeastFreshId() {
        // Arrange
        Storage storage = new Storage(StorageType.SHELF, DEFAULT_CAP);
        storage.store(order(FRESH_ID, MORE_FRESH), StorageType.SHELF);
        storage.store(order(STALE, LESS_FRESH), StorageType.SHELF);

        // Act
        String sut = storage.removeLeastFresh();

        // Assert
        assertEquals(STALE, sut);
    }

    @Test
    void removeLeastFresh_removesOrderFromStorage() {
        // Arrange
        Storage storage = new Storage(StorageType.SHELF, DEFAULT_CAP);
        storage.store(order(FRESH_ID, MORE_FRESH), StorageType.SHELF);
        storage.store(order(STALE, LESS_FRESH), StorageType.SHELF);

        // Act
        storage.removeLeastFresh();

        // Assert
        List<StoredOrder> sut = storage.getOrdersByFreshness();
        assertEquals(1, sut.size());
        assertEquals(FRESH_ID, sut.get(0).getOrder().getId());
    }

    @Test
    void removeLeastFresh_emptyStorage_returnsNull() {
        // Arrange
        Storage storage = new Storage(StorageType.SHELF, DEFAULT_CAP);

        // Act
        String sut = storage.removeLeastFresh();

        // Assert
        assertNull(sut);
    }
}
