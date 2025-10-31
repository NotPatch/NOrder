package com.notpatch.nOrder.model;

import lombok.Data;

import java.util.UUID;

@Data
public class PlayerStatistics {

    private final UUID playerId;
    private final String playerName;
    private int deliveredItems;
    private int collectedItems;

    private int totalOrders;

    private double totalEarnings;

    public void addDeliveredItems(int amount) {
        this.deliveredItems += amount;
    }

    public void addCollectedItems(int amount) {
        this.collectedItems += amount;
    }

    public void addTotalOrders(int amount) {
        this.totalOrders += amount;
    }

    public void addTotalEarnings(double amount) {
        this.totalEarnings += amount;
    }

}
