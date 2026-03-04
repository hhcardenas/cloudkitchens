package com.css.challenge;

import com.css.challenge.client.Action;
import com.css.challenge.client.Order;

import static com.css.challenge.StorageType.SHELF;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe kitchen storage system that handles place and pickup actions
 * while respecting storage capacities and freshness. Uses a single lock since all
 * operations are heavily dependent on shelf storage.
 */
public class Kitchen {

    private static final Logger LOGGER = LoggerFactory.getLogger(Kitchen.class);

    private final Storage _cooler;
    private final Storage _heater;
    private final Storage _shelf;

    // Lookup of order ID to the storage it's currently in, for O(1) access during
    // pickup
    private final Map<String, StoredOrder> _storedOrderById = new HashMap<>();

    private final List<Action> _actions = new ArrayList<>();
    private final Lock _lock = new ReentrantLock();

    public Kitchen(int cooler_spaces, int heater_spaces, int shelf_spaces) {
        _cooler = new Storage(StorageType.COOLER, cooler_spaces);
        _heater = new Storage(StorageType.HEATER, heater_spaces);
        _shelf = new Storage(StorageType.SHELF, shelf_spaces);
    }

    /**
     * Places a cooked order into storage with this priority:
     * 1. Ideal temperature storage (cooler/heater/shelf) if there's room
     * 2. Shelf if ideal storage is full
     * 3. Move a shelf order to its ideal spot, then place new order on shelf
     * 4. Discard the shelf order with least remaining freshness, then place on
     * shelf
     */
    public void placeOrder(Order order) {
        StorageType idealType = Temperature.fromOrderTemp(order.getTemp()).getIdealStorageType();

        // Use a single lock for all place and pickup operations since they all interact
        // with the
        // shelf and there's low optimization opportunity from separate locks.
        _lock.lock();
        try {
            // 1. If ideal storage has room, place there directly
            if (storeOrderInStorage(order, idealType)) {
                LOGGER.info("Placed order {} in ideal storage {}", order.getId(), idealType);
                return;
            }

            // 2. Ideal storage is full. Store on shelf if there's room.
            if (storeOrderInStorage(order, SHELF)) {
                LOGGER.info("Placed order {} on shelf (ideal storage {} full)", order.getId(), idealType);
                return;
            }

            // 3. Shelf is also full. Try moving an existing shelf order to its ideal
            // storage to free up space.
            if (moveShelfOrderToIdeal()) {
                if (storeOrderInStorage(order, StorageType.SHELF)) {
                    LOGGER.info("Moved a shelf order to ideal storage to place order {} on shelf", order.getId());
                    return;
                } else {
                    LOGGER.error("Failed to store order {} on shelf after moving a shelf order to ideal storage",
                            order.getId());
                }
            }

            // 4. No moves possible — discard the order with least remaining freshness
            discardLeastFresh();
            if (storeOrderInStorage(order, StorageType.SHELF)) {
                LOGGER.info("Discarded least fresh shelf order to place order {} on shelf", order.getId());
            } else {
                LOGGER.error("Failed to store order {} on shelf after discarding least fresh shelf order",
                        order.getId());
            }
        } finally {
            _lock.unlock();
        }
    }

    /**
     * Picks up an order by ID. If expired, discards it instead.
     * Does nothing if the order is not present (already picked up or discarded).
     */
    public void pickup(String orderId) {
        _lock.lock();
        try {
            StoredOrder stored = _storedOrderById.getOrDefault(orderId, null);
            if (stored == null) {
                LOGGER.warn("Attempted pickup of non-existent order {}", orderId);
                return; // Order not present — no action taken
            }
            if (stored.isExpired()) {
                removeFromStorage(stored);
                recordAction(orderId, Action.DISCARD, stored.getCurrentStorageType());
            } else {
                removeFromStorage(stored);
                recordAction(orderId, Action.PICKUP, stored.getCurrentStorageType());
            }
        } finally {
            _lock.unlock();
        }
    }

    /** Returns a snapshot of all recorded actions. */
    public List<Action> get_actions() {
        _lock.lock();
        try {
            return new ArrayList<>(_actions);
        } finally {
            _lock.unlock();
        }
    }

    /**
     * Tries to store the order in the specified storage type. Returns true if
     * successful,
     * false if there's no room.
     */
    private boolean storeOrderInStorage(Order order, StorageType type) {
        if (getStorage(type).hasRoom()) {
            StoredOrder stored = getStorage(type).store(order, type);
            _storedOrderById.put(stored.getOrder().getId(), stored);
            recordAction(order.getId(), Action.PLACE, type);
            return true;
        }
        return false;
    }

    /**
     * Tries to move one shelf order back to its ideal storage (cooler or heater).
     * Returns true if a move was made.
     */
    private boolean moveShelfOrderToIdeal() {
        for (StoredOrder stored : _shelf.getOrdersByFreshness()) {
            StorageType idealType = stored.getIdealStorageType();
            if (!idealType.equals(StorageType.SHELF) && getStorage(idealType).hasRoom()) {

                // Remove from shelf
                _shelf.remove(stored);

                // Move to ideal storage and update
                stored.moveTo(idealType);
                getStorage(idealType).move(stored);

                LOGGER.info("Moved order {} from shelf to ideal storage {}", stored.getOrder().getId(), idealType);
                recordAction(stored.getOrder().getId(), Action.MOVE, idealType);
                return true;
            }
        }
        return false;
    }

    /**
     * Discards the shelf order with the least remaining freshness.
     */
    private void discardLeastFresh() {
        String discardedId = _shelf.removeLeastFresh();
        if (discardedId == null) {
            LOGGER.warn("Attempted to discard from shelf but it was empty");
            return;
        }
        _storedOrderById.remove(discardedId);
        recordAction(discardedId, Action.DISCARD, StorageType.SHELF);
    }

    /** Removes a stored order from whichever storage it's in. */
    private void removeFromStorage(StoredOrder stored) {
        StorageType type = stored.getCurrentStorageType();
        getStorage(type).remove(stored);
        _storedOrderById.remove(stored.getOrder().getId());
    }

    private void recordAction(String orderId, String action, StorageType target) {
        _actions.add(new Action(Instant.now(), orderId, action, target.toActionTarget()));
        LOGGER.info("{} {} on {}", action, orderId, target);
    }

    private Storage getStorage(StorageType type) {
        return switch (type) {
            case COOLER -> _cooler;
            case HEATER -> _heater;
            case SHELF -> _shelf;
        };
    }
}
