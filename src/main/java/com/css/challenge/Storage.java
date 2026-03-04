package com.css.challenge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.css.challenge.client.Order;

public class Storage {

    private final StorageType _type;
    private final int _capacity;
    private final List<StoredOrder> _orders = new ArrayList<>();

    public Storage(StorageType type, int capacity) {
        _type = type;
        _capacity = capacity;
    }

    public StorageType getType() {
        return _type;
    }

    /**
     * Returns orders sorted by freshness (least fresh first), computed at call time
     * so the ordering reflects current remaining freshness.
     */
    public List<StoredOrder> getOrdersByFreshness() {
        List<StoredOrder> sorted = new ArrayList<>(_orders);
        Collections.sort(sorted);
        return sorted;
    }

    public String removeLeastFresh() {
        if (_orders.isEmpty()) {
            return null;
        }
        StoredOrder victim = Collections.min(_orders);
        _orders.remove(victim);
        return victim.getOrder().getId();
    }

    public boolean hasRoom() {
        return _orders.size() < _capacity;
    }

    StoredOrder store(Order order, StorageType type) {
        StoredOrder stored = new StoredOrder(order, type);
        _orders.add(stored);
        return stored;
    }

    void move(StoredOrder stored) {
        _orders.add(stored);
    }

    void remove(StoredOrder order) {
        _orders.remove(order);
    }

    @Override
    public String toString() {
        return _type.toString().toLowerCase();
    }
}