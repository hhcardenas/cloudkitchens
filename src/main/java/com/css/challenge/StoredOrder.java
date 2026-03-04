package com.css.challenge;

import com.css.challenge.client.Order;

import java.time.Duration;
import java.time.Instant;

/**
 * Wraps an Order with storage metadata needed for freshness tracking.
 * Freshness degrades at 1x when stored at ideal temperature, 2x otherwise (on shelf).
 */
public class StoredOrder implements Comparable<StoredOrder> {
    private final Order order;
    private StorageType currentStorageType;

    // Tracks cumulative freshness consumed from prior storage locations.
    // When an order moves from shelf to its ideal spot, we note the
    // freshness already used so getRemainingFreshness() stays accurate.
    private double freshnessConsumed;
    private Instant lastMoveTime;

    public StoredOrder(Order order, StorageType storageType) {
        this.order = order;
        this.currentStorageType = storageType;
        this.freshnessConsumed = 0;
        this.lastMoveTime = Instant.now();
    }

    public Order getOrder() {
        return order;
    }

    public StorageType getCurrentStorageType() {
        return currentStorageType;
    }

    /**
     * Updates the storage location. Banks the freshness consumed so far
     * at the previous location's degradation rate before switching.
     */
    public void moveTo(StorageType storageType) {
        bankFreshness();
        this.currentStorageType = storageType;
    }

    /**
     * Returns remaining freshness in seconds, accounting for degradation rate.
     * On ideal temp: degrades at 1 second per second.
     * On shelf (non-ideal): degrades at 2 seconds per second.
     */
    public double getRemainingFreshness() {
        double totalConsumed = freshnessConsumed + currentSegmentConsumed();
        return order.getFreshness() - totalConsumed;
    }

    public boolean isExpired() {
        return getRemainingFreshness() <= 0;
    }

    /**
     * Comparable for Sorting by remaining freshness (least fresh first). Freshness is computed at
     * call time so the ordering reflects current freshness.
     */
    @Override
    public int compareTo(StoredOrder other) {
        return Double.compare(this.getRemainingFreshness(), other.getRemainingFreshness());
    }

    public StorageType getIdealStorageType() {
        return Temperature.fromOrderTemp(order.getTemp()).getIdealStorageType();
    }

    private boolean isAtIdealTemp() {
        return getIdealStorageType().equals(currentStorageType);
    }

    private double currentSegmentConsumed() {
        double elapsed = Duration.between(lastMoveTime, Instant.now()).toMillis() / 1000.0;
        return isAtIdealTemp() ? elapsed : elapsed * 2;
    }

    private void bankFreshness() {
        freshnessConsumed += currentSegmentConsumed();
        lastMoveTime = Instant.now();
    }

    @Override
    public String toString() {
        return String.format("[%s (%s) at %s, freshness=%.1fs]",
            order.getName(), order.getId(), currentStorageType, getRemainingFreshness());
    }
}
