package com.css.challenge;

import com.css.challenge.client.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class StoredOrderTest {

    private static final String COLD = "cold";
    private static final String ROOM = "room";

    private static final String O1 = "o1";
    private static final String O2 = "o2";

    private static final int EXPIRED_FRESHNESS = 0;
    private static final int LESS_FRESH = 100;
    private static final int FRESH = 300;
    private static final int MORE_FRESH = 500;

    private Order order(String id, String temp, int freshness) {
        return new Order(id, "Item", temp, 10, freshness);
    }

    @ParameterizedTest(name = "{0} order → ideal storage is {1}")
    @CsvSource({
            "cold, COOLER",
            "hot,  HEATER",
            "room, SHELF"
    })
    void getIdealStorageType_returnsCorrectStorage(String temp, StorageType expectedType) {
        // Arrange
        StoredOrder stored = new StoredOrder(order(O1, temp, FRESH), StorageType.SHELF);

        // Act
        StorageType sut = stored.getIdealStorageType();

        // Assert
        assertEquals(expectedType, sut);
    }

    @Test
    void getRemainingFreshness_startsNearOrderFreshness() {
        // Arrange
        StoredOrder stored = new StoredOrder(order(O1, COLD, FRESH), StorageType.COOLER);

        // Act
        double sut = stored.getRemainingFreshness();

        // Assert
        assertEquals(FRESH, sut, 1.0);
    }

    @Test
    void getRemainingFreshness_degradesFasterOnShelfForNonIdeal() throws InterruptedException {
        // Arrange
        StoredOrder atIdeal = new StoredOrder(order(O1, COLD, FRESH), StorageType.COOLER);
        StoredOrder onShelf = new StoredOrder(order(O2, COLD, FRESH), StorageType.SHELF);
        Thread.sleep(100);

        // Act
        double sut = onShelf.getRemainingFreshness();

        // Assert
        assertTrue(sut < atIdeal.getRemainingFreshness(),
                "Non-ideal storage should degrade freshness faster");
    }

    @Test
    void getRemainingFreshness_roomOnShelf_degradesSlowerThanColdOnShelf() throws InterruptedException {
        // Arrange
        StoredOrder roomOnShelf = new StoredOrder(order(O1, ROOM, FRESH), StorageType.SHELF);
        StoredOrder coldOnShelf = new StoredOrder(order(O2, COLD, FRESH), StorageType.SHELF);
        Thread.sleep(100);

        // Act
        double sut = roomOnShelf.getRemainingFreshness();

        // Assert
        assertTrue(sut > coldOnShelf.getRemainingFreshness(),
                "Room-temp order on shelf should degrade slower than cold order on shelf");
    }

    @Test
    void isExpired_highFreshness_returnsFalse() {
        // Arrange
        StoredOrder stored = new StoredOrder(order(O1, COLD, FRESH), StorageType.COOLER);

        // Act
        boolean sut = stored.isExpired();

        // Assert
        assertFalse(sut);
    }

    @Test
    void isExpired_zeroFreshness_returnsTrue() {
        // Arrange
        StoredOrder stored = new StoredOrder(order(O1, COLD, EXPIRED_FRESHNESS), StorageType.COOLER);

        // Act
        boolean sut = stored.isExpired();

        // Assert
        assertTrue(sut);
    }

    @Test
    void moveTo_updatesStorageType() {
        // Arrange
        StoredOrder stored = new StoredOrder(order(O1, COLD, FRESH), StorageType.SHELF);

        // Act
        stored.moveTo(StorageType.COOLER);

        // Assert
        StorageType sut = stored.getCurrentStorageType();
        assertEquals(StorageType.COOLER, sut);
    }

    @Test
    void moveTo_banksFreshness_degradationRateSlowsDown() throws InterruptedException {
        // Arrange
        StoredOrder stored = new StoredOrder(order(O1, COLD, FRESH), StorageType.SHELF);
        Thread.sleep(100);
        double dropOnShelf = FRESH - stored.getRemainingFreshness();

        // Act
        stored.moveTo(StorageType.COOLER);
        double freshnessAfterMove = stored.getRemainingFreshness();
        Thread.sleep(100);

        // Assert
        double sut = freshnessAfterMove - stored.getRemainingFreshness();
        assertTrue(sut < dropOnShelf,
                "Degradation at ideal temp should be slower than on shelf");
    }

    @Test
    void compareTo_lessFreshComesFirst() {
        // Arrange
        StoredOrder stale = new StoredOrder(order(O1, COLD, LESS_FRESH), StorageType.COOLER);
        StoredOrder fresh = new StoredOrder(order(O2, COLD, MORE_FRESH), StorageType.COOLER);

        // Act
        int sut = stale.compareTo(fresh);

        // Assert
        assertTrue(sut < 0, "Stale order should come before fresh order");
    }

    @Test
    void compareTo_moreFreshComesLast() {
        // Arrange
        StoredOrder stale = new StoredOrder(order(O1, COLD, LESS_FRESH), StorageType.COOLER);
        StoredOrder fresh = new StoredOrder(order(O2, COLD, MORE_FRESH), StorageType.COOLER);

        // Act
        int sut = fresh.compareTo(stale);

        // Assert
        assertTrue(sut > 0, "Fresh order should come after stale order");
    }

    @Test
    void compareTo_equalFreshness_returnsZero() {
        // Arrange
        StoredOrder a = new StoredOrder(order(O1, COLD, FRESH), StorageType.COOLER);
        StoredOrder b = new StoredOrder(order(O2, COLD, FRESH), StorageType.COOLER);

        // Act
        int sut = a.compareTo(b);

        // Assert
        assertEquals(0, sut);
    }
}
