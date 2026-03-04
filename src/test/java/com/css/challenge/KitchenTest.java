package com.css.challenge;

import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KitchenTest {

    private Kitchen kitchen;

    private static final int COOLER_CAP = 2;
    private static final int HEATER_CAP = 2;
    private static final int SHELF_CAP = 2;

    private static final String COLD = "cold";
    private static final String HOT = "hot";
    private static final String ROOM = "room";

    private static final String C1 = "c1";
    private static final String C2 = "c2";
    private static final String C3 = "c3";
    private static final String H1 = "h1";
    private static final String H2 = "h2";
    private static final String R1 = "r1";
    private static final String R2 = "r2";

    private static final int EXPIRED_FRESHNESS = 0;
    private static final int LESS_FRESH = 100;
    private static final int FRESH = 300;
    private static final int MORE_FRESH = 500;

    @BeforeEach
    void setUp() {
        kitchen = new Kitchen(COOLER_CAP, HEATER_CAP, SHELF_CAP);
    }

    private Order order(String id, String temp, int freshness) {
        return new Order(id, "Item", temp, 10, freshness);
    }

    @ParameterizedTest(name = "place {0} order → goes to {1}")
    @CsvSource({
            "cold, cooler",
            "hot,  heater",
            "room, shelf"
    })
    void placeOrder_idealStorageHasRoom_placedInIdealStorage(String temp, String expectedTarget) {
        // Arrange
        Order order = order("o1", temp, FRESH);

        // Act
        kitchen.placeOrder(order);

        // Assert
        Action sut = kitchen.get_actions().get(0);
        assertEquals(Action.PLACE, sut.getAction());
        assertEquals(expectedTarget, sut.getTarget());
    }

    @Test
    void placeOrder_idealFull_placedOnShelf() {
        // Arrange
        kitchen.placeOrder(order(C1, COLD, FRESH));
        kitchen.placeOrder(order(C2, COLD, FRESH));

        // Act
        kitchen.placeOrder(order(C3, COLD, FRESH));

        // Assert
        Action sut = kitchen.get_actions().get(kitchen.get_actions().size() - 1);
        assertEquals(Action.PLACE, sut.getAction());
        assertEquals("shelf", sut.getTarget());
        assertEquals(C3, sut.getId());
    }

    @Test
    void placeOrder_shelfFull_movesShelfOrderToIdealStorage() {
        // Arrange
        Kitchen smallKitchen = new Kitchen(1, 1, 2);
        // cooler
        smallKitchen.placeOrder(order(C1, COLD, FRESH));
        // heater
        smallKitchen.placeOrder(order(H1, HOT, FRESH));
        // shelf (heater full)
        smallKitchen.placeOrder(order(H2, HOT, FRESH)); 
        // shelf (cooler full, shelf full)
        smallKitchen.placeOrder(order(C2, COLD, FRESH));
        // free one heater
        smallKitchen.pickup(H1);

        // Act
        smallKitchen.placeOrder(order(C3, COLD, FRESH));

        // Assert
        Action sut = smallKitchen.get_actions().stream()
                .filter(a -> a.getAction().equals(Action.MOVE))
                .findFirst()
                .orElseThrow();
        assertEquals(H2, sut.getId());
        assertEquals("heater", sut.getTarget());
    }

    @Test
    void placeOrder_shelfFull_placesNewOrderOnShelfAfterMove() {
        // Arrange
        Kitchen smallKitchen = new Kitchen(1, 1, 2);
        smallKitchen.placeOrder(order(C1, COLD, FRESH));
        smallKitchen.placeOrder(order(H1, HOT, FRESH));
        smallKitchen.placeOrder(order(H2, HOT, FRESH));
        smallKitchen.placeOrder(order(C2, COLD, FRESH));
        smallKitchen.pickup(H1);

        // Act
        smallKitchen.placeOrder(order(C3, COLD, FRESH));

        // Assert
        List<Action> actions = smallKitchen.get_actions();
        Action sut = actions.get(actions.size() - 1);
        assertEquals(Action.PLACE, sut.getAction());
        assertEquals(C3, sut.getId());
        assertEquals("shelf", sut.getTarget());
    }

    @Test
    void placeOrder_noMovePossible_discardsLeastFreshOrder() {
        // Arrange
        kitchen.placeOrder(order(C1, COLD, FRESH));
        kitchen.placeOrder(order(C2, COLD, FRESH));
        kitchen.placeOrder(order(H1, HOT, FRESH));
        kitchen.placeOrder(order(H2, HOT, FRESH));
        kitchen.placeOrder(order(R1, ROOM, LESS_FRESH)); // least fresh
        kitchen.placeOrder(order(R2, ROOM, MORE_FRESH));

        // Act
        kitchen.placeOrder(order(C3, COLD, FRESH));

        // Assert
        Action sut = kitchen.get_actions().stream()
                .filter(a -> a.getAction().equals(Action.DISCARD))
                .findFirst()
                .orElseThrow();
        assertEquals(R1, sut.getId());
        assertEquals("shelf", sut.getTarget());
    }

    @Test
    void placeOrder_noMovePossible_placesNewOrderOnShelfAfterDiscard() {
        // Arrange
        kitchen.placeOrder(order(C1, COLD, FRESH));
        kitchen.placeOrder(order(C2, COLD, FRESH));
        kitchen.placeOrder(order(H1, HOT, FRESH));
        kitchen.placeOrder(order(H2, HOT, FRESH));
        kitchen.placeOrder(order(R1, ROOM, LESS_FRESH));
        kitchen.placeOrder(order(R2, ROOM, MORE_FRESH));

        // Act
        kitchen.placeOrder(order(C3, COLD, FRESH));

        // Assert
        List<Action> actions = kitchen.get_actions();
        Action sut = actions.get(actions.size() - 1);
        assertEquals(Action.PLACE, sut.getAction());
        assertEquals(C3, sut.getId());
        assertEquals("shelf", sut.getTarget());
    }

    @Test
    void pickup_validOrder_recordsPickupAction() {
        // Arrange
        kitchen.placeOrder(order(C1, COLD, FRESH));

        // Act
        kitchen.pickup(C1);

        // Assert
        Action sut = kitchen.get_actions().get(1);
        assertEquals(Action.PICKUP, sut.getAction());
        assertEquals(C1, sut.getId());
    }

    @Test
    void pickup_nonExistentOrder_noActionRecorded() {
        // Arrange & Act
        kitchen.pickup("missing");

        // Assert
        List<Action> sut = kitchen.get_actions();
        assertTrue(sut.isEmpty());
    }

    @Test
    void pickup_expiredOrder_recordsDiscardAction() {
        // Arrange
        kitchen.placeOrder(order(C1, COLD, EXPIRED_FRESHNESS));

        // Act
        kitchen.pickup(C1);

        // Assert
        Action sut = kitchen.get_actions().get(1);
        assertEquals(Action.DISCARD, sut.getAction());
        assertEquals(C1, sut.getId());
    }
}
