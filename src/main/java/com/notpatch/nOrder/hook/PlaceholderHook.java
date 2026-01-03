package com.notpatch.nOrder.hook;

import com.notpatch.nOrder.NOrder;
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

    private final String DEFAULT_DATE_PATTERN = "dd-MM HH:mm:ss";

    public PlaceholderHook(NOrder main) {
        this.main = main;
        this.orderManager = main.getOrderManager();
        this.playerStatsManager = main.getPlayerStatsManager();
        String datePattern = main.getConfig().getString("date-format");
        if (datePattern == null || datePattern.isEmpty()) {
            datePattern = DEFAULT_DATE_PATTERN;
        }
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(datePattern);
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
                case "material" -> {
                    return getOrderMaterial(orderId);
                }
                case "amount" -> {
                    return getOrderAmount(orderId);
                }
                case "price" -> {
                    return getOrderPrice(orderId);
                }
                case "buyer" -> {
                    return getOrderBuyer(orderId);
                }
                case "status" -> {
                    return getOrderStatus(orderId);
                }
                case "createDate" -> {
                    return getOrderCreateDate(orderId);
                }
                case "expirationDate" -> {
                    return getOrderExpirationDate(orderId);
                }
            }
        }

        // %norder_player_<type>% - uses the player from context
        // %norder_player_{player_name}_<type>% - looks up specific player
        if (parts.length >= 2 && parts[0].equals("player")) {
            // Check if this is a simple player stat request without a player name
            // e.g., %norder_player_totalOrders%
            if (parts.length == 2) {
                String field = parts[1];
                if (player == null) {
                    return "";
                }
                String playerName = player.getName();
                switch (field) {
                    case "totalOrders" -> {
                        return getPlayerTotalOrders(playerName);
                    }
                    case "totalEarnings" -> {
                        return getPlayerTotalEarnings(playerName);
                    }
                    case "totalDelivered" -> {
                        return getPlayerTotalDeliveredItems(playerName);
                    }
                    case "totalCollected" -> {
                        return getPlayerTotalCollectedItems(playerName);
                    }
                    case "currentOrder" -> {
                        return getPlayerCurrentOrders(player);
                    }
                    case "activeOrder" -> {
                        return getPlayerActiveOrders(player);
                    }
                    case "availableleftOrder" -> {
                        return getPlayerAvailableLeftOrders(player);
                    }
                    case "limitOrder" -> {
                        return getPlayerOrderLimit(player);
                    }
                }
            }
            
            // For player-specific stats: %norder_player_{player_name}_<type>%
            // We need to find the field name (last part) and extract player name from the middle
            if (parts.length >= 3) {
                String lastPart = parts[parts.length - 1];
                
                // Check if the last part is a valid field
                if (lastPart.equals("totalOrders") || lastPart.equals("totalEarnings") ||
                        lastPart.equals("totalDelivered") || lastPart.equals("totalCollected") ||
                        lastPart.equals("currentOrder") || lastPart.equals("activeOrder") ||
                        lastPart.equals("availableleftOrder") || lastPart.equals("limitOrder")) {

                    // Extract player name from parts[1] to parts[length-2]
                    // e.g., for "player_ItzFabbb____totalOrders", parts = ["player", "ItzFabbb", "", "", "", "totalOrders"]
                    // player name = "ItzFabbb___" (join parts[1] to parts[length-2] with underscores)
                    StringBuilder playerNameBuilder = new StringBuilder();
                    for (int i = 1; i < parts.length - 1; i++) {
                        if (i > 1) {
                            playerNameBuilder.append("_");
                        }
                        playerNameBuilder.append(parts[i]);
                    }
                    String playerName = playerNameBuilder.toString();

                    // Get OfflinePlayer for player-specific stats that need UUID
                    OfflinePlayer targetPlayer = org.bukkit.Bukkit.getOfflinePlayer(playerName);

                    switch (lastPart) {
                        case "totalOrders" -> {
                            return getPlayerTotalOrders(playerName);
                        }
                        case "totalEarnings" -> {
                            return getPlayerTotalEarnings(playerName);
                        }
                        case "totalDelivered" -> {
                            return getPlayerTotalDeliveredItems(playerName);
                        }
                        case "totalCollected" -> {
                            return getPlayerTotalCollectedItems(playerName);
                        }
                        case "currentOrder" -> {
                            return getPlayerCurrentOrders(targetPlayer);
                        }
                        case "activeOrder" -> {
                            return getPlayerActiveOrders(targetPlayer);
                        }
                        case "availableleftOrder" -> {
                            return getPlayerAvailableLeftOrders(targetPlayer);
                        }
                        case "limitOrder" -> {
                            return getPlayerOrderLimit(targetPlayer);
                        }
                    }
                }
            }
        }

        return "";
    }

    private String getOrdersTotal() {
        return String.valueOf(orderManager.getAllOrders().size());
    }

    private String getOrderMaterial(String orderId) {
        if (orderManager.getOrderById(orderId) == null) return "";
        return orderManager.getOrderById(orderId).getMaterial().name();
    }

    private String getOrderAmount(String orderId) {
        if (orderManager.getOrderById(orderId) == null) return "";
        return String.valueOf(orderManager.getOrderById(orderId).getAmount());
    }

    private String getOrderPrice(String orderId) {
        if (orderManager.getOrderById(orderId) == null) return "";
        return String.valueOf(orderManager.getOrderById(orderId).getPrice());
    }

    private String getOrderBuyer(String orderId) {
        if (orderManager.getOrderById(orderId) == null) return "";
        return String.valueOf(orderManager.getOrderById(orderId).getPlayerName());
    }

    private String getOrderStatus(String orderId) {
        if (orderManager.getOrderById(orderId) == null) return "";
        return orderManager.getOrderById(orderId).getStatus().name();
    }

    private String getOrderCreateDate(String orderId) {
        if (orderManager.getOrderById(orderId) == null) return "";
        return orderManager.getOrderById(orderId).getCreatedAt().format(dateTimeFormatter);
    }

    private String getOrderExpirationDate(String orderId) {
        if (orderManager.getOrderById(orderId) == null) return "";
        return orderManager.getOrderById(orderId).getExpirationDate().format(dateTimeFormatter);
    }

    private String getPlayerTotalOrders(String playerName) {
        if (playerStatsManager.getStatisticsByName(playerName) == null) return "0";
        return playerStatsManager.getStatisticsByName(playerName).getTotalOrders() + "";
    }

    private String getPlayerTotalEarnings(String playerName) {
        if (playerStatsManager.getStatisticsByName(playerName) == null) return "0";
        return playerStatsManager.getStatisticsByName(playerName).getTotalEarnings() + "";
    }

    private String getPlayerTotalDeliveredItems(String playerName) {
        if (playerStatsManager.getStatisticsByName(playerName) == null) return "0";
        return playerStatsManager.getStatisticsByName(playerName).getDeliveredItems() + "";
    }

    private String getPlayerTotalCollectedItems(String playerName) {
        if (playerStatsManager.getStatisticsByName(playerName) == null) return "0";
        return playerStatsManager.getStatisticsByName(playerName).getCollectedItems() + "";
    }

    private String getPlayerCurrentOrders(OfflinePlayer player) {
        if (player == null) return "0";
        return String.valueOf(orderManager.getPlayerOrderCount(player.getUniqueId()));
    }

    private String getPlayerActiveOrders(OfflinePlayer player) {
        if (player == null) return "0";
        return String.valueOf(orderManager.getPlayerOrders(player.getUniqueId()).stream()
                .filter(o -> o.getStatus() == com.notpatch.nOrder.model.OrderStatus.ACTIVE)
                .count());
    }

    private String getPlayerAvailableLeftOrders(OfflinePlayer player) {
        if (player == null || !player.isOnline()) return "0";
        int limit = com.notpatch.nOrder.util.PlayerUtil.getPlayerOrderLimit(player.getPlayer());
        int current = orderManager.getPlayerOrderCount(player.getUniqueId());
        return String.valueOf(Math.max(0, limit - current));
    }

    private String getPlayerOrderLimit(OfflinePlayer player) {
        if (player == null || !player.isOnline()) return "1";
        return String.valueOf(com.notpatch.nOrder.util.PlayerUtil.getPlayerOrderLimit(player.getPlayer()));
    }
}
