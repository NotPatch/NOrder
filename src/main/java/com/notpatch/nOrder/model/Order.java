package com.notpatch.nOrder.model;

import lombok.Data;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Order {

    private final String id;
    private final UUID playerId;
    private final String playerName;
    private final ItemStack item;
    private final String customItemId;
    private final int amount;
    private final double price;
    private int delivered;
    private int collected;
    private final LocalDateTime createdAt;
    private final LocalDateTime expirationDate;
    private final boolean highlight;
    private OrderStatus status;

    public boolean isCustomItem() {
        return customItemId != null && !customItemId.isEmpty();
    }


    public int getRemaining() {
        return amount - delivered;
    }

    public void addDelivered(int quantity) {
        this.delivered += quantity;
        if (delivered > amount) {
            delivered = amount;
        }
    }

    public void addCollected(int quantity) {
        this.collected += quantity;
        if (collected > delivered) {
            collected = delivered;
        }
    }

    public Material getMaterial() {
        return item.getType();
    }

    public void removeDelivered(int quantity) {
        this.delivered -= quantity;
        if (this.delivered < 0) {
            this.delivered = 0;
        }
    }

    public boolean isOwner(Player player) {
        return this.playerId.equals(player.getUniqueId()) || this.playerName.equalsIgnoreCase(player.getName());
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationDate);
    }

    public long getRemainingHours() {
        return Duration.between(LocalDateTime.now(), expirationDate).toHours();
    }

}
