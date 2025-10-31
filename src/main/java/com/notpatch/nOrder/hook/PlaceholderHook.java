package com.notpatch.nOrder.hook;

import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.Settings;
import com.notpatch.nOrder.database.DatabaseManager;
import com.notpatch.nOrder.manager.OrderManager;
import com.notpatch.nOrder.manager.PlayerStatisticsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.format.DateTimeFormatter;

public class PlaceholderHook extends PlaceholderExpansion {

    private final NOrder main;
    private final OrderManager orderManager;
    private final PlayerStatisticsManager playerStatsManager;

    private final DateTimeFormatter dateTimeFormatter;

    public PlaceholderHook(NOrder main){
        this.main = main;
        this.orderManager = main.getOrderManager();
        this.playerStatsManager = main.getPlayerStatsManager();
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(Settings.DATE_FORMAT);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "norder";
    }

    @Override
    public @NotNull String getAuthor() {
        return "NotPatch";
    }

    @Override
    public @NotNull String getVersion() {
        return NOrder.getInstance().getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] parts = params.split("_");
        if (parts.length == 0) {
            return "";
        }

        // %norder_orders_total%
        if (parts.length >= 2 && parts[0].equals("orders") && parts[1].equals("total")) {
            return getOrdersTotal();
        }

        // %norder_order_<id>_*
        if (parts.length >= 3 && parts[0].equals("order")) {
            String orderId = parts[1];
            String field = parts[2];
            switch (field) {
                case "material" -> { return getOrderMaterial(orderId); }
                case "amount" -> { return getOrderAmount(orderId); }
                case "price" -> { return getOrderPrice(orderId); }
                case "buyer" -> { return getOrderBuyer(orderId); }
                case "status" -> { return getOrderStatus(orderId); }
                case "createDate" -> { return getOrderCreateDate(orderId); }
                case "expirationDate" -> { return getOrderExpirationDate(orderId); }
            }
        }

        // %norder_player_<player>_*
        if (parts.length >= 3 && parts[0].equals("player")) {
            String playerName = parts[1];
            String field = parts[2];
            switch (field) {
                case "totalOrders" -> { return getPlayerTotalOrders(playerName); }
                case "totalEarnings" -> { return getPlayerTotalEarnings(playerName); }
                case "totalDelivered" -> { return getPlayerTotalDeliveredItems(playerName); }
                case "totalCollected" -> { return getPlayerTotalCollectedItems(playerName); }
            }
        }

        return "";
    }

    private String getOrdersTotal() {
        return String.valueOf(orderManager.getAllOrders().size());
    }
    private String getOrderMaterial(String orderId) {
        if(orderManager.getOrderById(orderId) == null) return "";
        return orderManager.getOrderById(orderId).getMaterial().name();
    }

    private String getOrderAmount(String orderId) {
        if(orderManager.getOrderById(orderId) == null) return "";
        return String.valueOf(orderManager.getOrderById(orderId).getAmount());
    }
    private String getOrderPrice(String orderId) {
        if(orderManager.getOrderById(orderId) == null) return "";
        return String.valueOf(orderManager.getOrderById(orderId).getPrice());
    }
    private String getOrderBuyer(String orderId) {
        if(orderManager.getOrderById(orderId) == null) return "";
        return String.valueOf(orderManager.getOrderById(orderId).getPlayerName());
    }
    private String getOrderStatus(String orderId) {
        if(orderManager.getOrderById(orderId) == null) return "";
        return orderManager.getOrderById(orderId).getStatus().name();
    }
    private String getOrderCreateDate(String orderId) {
        if(orderManager.getOrderById(orderId) == null) return "";
        return orderManager.getOrderById(orderId).getCreatedAt().format(dateTimeFormatter);
    }

    private String getOrderExpirationDate(String orderId) {
        if(orderManager.getOrderById(orderId) == null) return "";
        return orderManager.getOrderById(orderId).getExpirationDate().format(dateTimeFormatter);
    }

    private String getPlayerTotalOrders(String playerName) {
        if(playerStatsManager.getStatisticsByName(playerName) == null) return "0";
        return playerStatsManager.getStatisticsByName(playerName).getTotalOrders() + "";
    }

    private String getPlayerTotalEarnings(String playerName) {
        if(playerStatsManager.getStatisticsByName(playerName) == null) return "0";
        return playerStatsManager.getStatisticsByName(playerName).getTotalEarnings() + "";
    }
    private String getPlayerTotalDeliveredItems(String playerName) {
        if(playerStatsManager.getStatisticsByName(playerName) == null) return "0";
        return playerStatsManager.getStatisticsByName(playerName).getDeliveredItems() + "";
    }
    private String getPlayerTotalCollectedItems(String playerName) {
        if(playerStatsManager.getStatisticsByName(playerName) == null) return "0";
        return playerStatsManager.getStatisticsByName(playerName).getCollectedItems() + "";
    }
}
