package com.notpatch.nOrder.manager;

import com.notpatch.nOrder.LanguageLoader;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.Settings;
import com.notpatch.nOrder.model.DiscordWebhook;
import com.notpatch.nOrder.model.Order;
import com.notpatch.nOrder.model.OrderStatus;
import com.notpatch.nOrder.util.PlayerUtil;
import com.notpatch.nOrder.util.StringUtil;
import com.notpatch.nlib.effect.NSound;
import com.notpatch.nlib.util.NLogger;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.io.IOException;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class OrderManager {

    private final NOrder main;

    private final Map<UUID, List<Order>> ordersByPlayer = new ConcurrentHashMap<>();
    
    private Object cleanupTask;
    private Object autoSaveTask;

    public OrderManager(NOrder main) {
        this.main = main;
    }

    public void loadOrders() {
        if (!main.getDatabaseManager().isConnectionValid()) {
            NLogger.error("Database connection is null. Cannot load orders.");
            return;
        }

        try (Connection conn = main.getDatabaseManager().getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM orders")) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String orderId = rs.getString("order_id");
                UUID playerId = UUID.fromString(rs.getString("player_id"));
                String playerName = rs.getString("player_name");

                String materialName = rs.getString("material");
                Material material = Material.valueOf(materialName);
                ItemStack item = new ItemStack(material);

                String customItemId = rs.getString("custom_item_id");

                String enchantmentsStr = rs.getString("enchantments");
                if (enchantmentsStr != null && !enchantmentsStr.isEmpty()) {
                    try {
                        String[] enchantPairs = enchantmentsStr.split(",");
                        for (String pair : enchantPairs) {
                            String[] parts = pair.split(":");
                            if (parts.length == 2) {
                                Enchantment enchant = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(parts[0]));
                                int level = Integer.parseInt(parts[1]);
                                if (enchant != null) {
                                    if (material == Material.ENCHANTED_BOOK) {
                                        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                                        if (meta != null) {
                                            meta.addStoredEnchant(enchant, level, true);
                                            item.setItemMeta(meta);
                                        }
                                    } else {
                                        item.addEnchantment(enchant, level);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        NLogger.warn("Error parsing enchantments for order " + orderId + ": " + e.getMessage());
                    }
                }

                int amount = rs.getInt("amount");
                double price = rs.getDouble("price");
                int delivered = rs.getInt("delivered");
                int collected = rs.getInt("collected");
                LocalDateTime expiresAt = rs.getTimestamp("expires_at").toLocalDateTime();
                LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                boolean highlight = rs.getBoolean("highlight");
                String status = rs.getString("status");

                if (status.equalsIgnoreCase("ARCHIVED") || status.equalsIgnoreCase("CANCELLED")) continue;

                Order order = new Order(orderId, playerId, playerName, item, customItemId, amount, price, createdAt, expiresAt, highlight);
                order.setStatus(OrderStatus.valueOf(status));
                order.setDelivered(delivered);
                order.setCollected(collected);

                addOrderAdmin(order);
            }

            NLogger.info("Total " + getAllOrders().size() + " orders loaded successfully.");

        } catch (SQLException e) {
            NLogger.error("An error occurred while loading orders: " + e.getMessage());
        }
    }


    public void saveOrders() {
        if (!main.getDatabaseManager().isConnectionValid()) {
            NLogger.error("Database connection is null. Cannot save orders.");
            return;
        }
        String sql = """
                INSERT INTO orders (order_id, player_id, player_name, material, custom_item_id, enchantments, amount, price, delivered, collected, created_at, expires_at, highlight, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                delivered = VALUES(delivered),
                collected = VALUES(collected),
                status = VALUES(status)
                """;

        if (main.getDatabaseManager().isUsingSQLite()) {
            sql = """
                    INSERT OR REPLACE INTO orders (order_id, player_id, player_name, material, custom_item_id, enchantments, amount, price, delivered, collected, created_at, expires_at, highlight, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
        }

        try (Connection conn = main.getDatabaseManager().getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (List<Order> orders : ordersByPlayer.values()) {
                for (Order order : orders) {

                    stmt.setString(1, order.getId());
                    stmt.setString(2, order.getPlayerId().toString());
                    stmt.setString(3, order.getPlayerName());
                    stmt.setString(4, order.getMaterial().name());
                    stmt.setString(5, order.getCustomItemId()); // custom_item_id

                    String enchantments = formatEnchantments(order.getItem());
                    stmt.setString(6, enchantments);

                    stmt.setInt(7, order.getAmount());
                    stmt.setDouble(8, order.getPrice());
                    stmt.setInt(9, order.getDelivered());
                    stmt.setInt(10, order.getCollected());
                    stmt.setTimestamp(11, Timestamp.valueOf(order.getCreatedAt()));
                    stmt.setTimestamp(12, Timestamp.valueOf(order.getExpirationDate()));
                    stmt.setBoolean(13, order.isHighlight());
                    stmt.setString(14, order.getStatus().name());

                    stmt.addBatch();
                }
            }

            stmt.executeBatch();
            NLogger.info("Orders saved successfully.");

        } catch (SQLException e) {
            NLogger.error("Failed to save orders: " + e.getMessage());
        }
    }

    private String formatEnchantments(ItemStack item) {
        if (item == null) return "";

        Map<Enchantment, Integer> enchants;
        if (item.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
            if (meta != null) {
                enchants = meta.getStoredEnchants();
            } else {
                return "";
            }
        } else {
            enchants = item.getEnchantments();
        }

        if (enchants.isEmpty()) return "";

        return enchants.entrySet().stream()
                .map(entry -> entry.getKey().getKey().getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
    }


    public List<Order> getPlayerOrders(UUID playerId) {
        return ordersByPlayer.computeIfAbsent(playerId, k -> new ArrayList<>());
    }

    public List<Order> getPlayerOrders(String playerName) {
        return ordersByPlayer.values().stream()
                .filter(orders -> !orders.isEmpty() &&
                        orders.getFirst().getPlayerName().equalsIgnoreCase(playerName) && orders.getFirst().getStatus() == OrderStatus.ACTIVE)
                .findFirst()
                .orElse(new ArrayList<>());
    }

    public List<Order> getPlayerOrdersIncludingCompleted(UUID playerId) {
        return ordersByPlayer.computeIfAbsent(playerId, k -> new ArrayList<>());
    }

    public List<Order> getPlayerOrdersIncludingCompleted(String playerName) {
        return ordersByPlayer.values().stream()
                .filter(orders -> !orders.isEmpty() &&
                        orders.getFirst().getPlayerName().equalsIgnoreCase(playerName))
                .findFirst()
                .map(orders -> orders.stream()
                        .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
    }

    public List<Order> getOrdersByMaterial(String material) {
        return getAllOrders().stream()
                .filter(o -> o.getMaterial().name().toUpperCase().contains(material.toUpperCase()))
                .collect(Collectors.toList());
    }

    public void addOrderAdmin(Order order) {
        getPlayerOrders(order.getPlayerId()).add(order);
    }

    public int getPlayerOrderCount(UUID playerId) {
        return getPlayerOrders(playerId).size();
    }

    public List<Order> getHighlightedOrdersFirst() {
        return getAllOrders().stream()
                .sorted(Comparator.comparing(Order::isHighlight).reversed())
                .collect(Collectors.toList());
    }

    public void addOrder(Order order) {
        double totalPrice = order.getPrice() * order.getAmount();

        if (order.isHighlight() && Settings.HIGHLIGHT_FEE > 0) {
            totalPrice += totalPrice * Settings.HIGHLIGHT_FEE / 100;
        }

        OfflinePlayer offlinePlayer = main.getServer().getOfflinePlayer(order.getPlayerId());
        if (PlayerUtil.getPlayer(offlinePlayer) == null) {
            return;
        }

        Player player = PlayerUtil.getPlayer(offlinePlayer);

        if (PlayerUtil.isPlayerAdmin(player)) {
            getPlayerOrders(order.getPlayerId()).add(order);
            player.sendMessage(LanguageLoader.getMessage("order-created")
                    .replace("%material%", order.getMaterial().name())
                    .replace("%amount%", String.valueOf(order.getAmount()))
                    .replace("%total_price%", String.format("%.2f", totalPrice))
                    .replace("%price%", String.valueOf(order.getPrice())));
            order.setStatus(OrderStatus.ACTIVE);
            main.getPlayerStatsManager().getStatistics(order.getPlayerId()).addTotalOrders(1);
            main.getOrderLogger().logOrderCreated(order, totalPrice);
            NSound.success(player);
            broadcastOrder(order, totalPrice);
            return;
        }

        int playerOrderLimit = PlayerUtil.getPlayerOrderLimit(player);

        if (getPlayerOrderCount(order.getPlayerId()) >= playerOrderLimit) {
            player.sendMessage(LanguageLoader.getMessage("order-limit-reached")
                    .replace("%limit%", String.valueOf(playerOrderLimit)));
            NSound.error(player);
            return;
        }

        if (main.getEconomy().getBalance(offlinePlayer) < totalPrice) {
            player.sendMessage(LanguageLoader.getMessage("not-enough-money"));
            NSound.error(player);
            return;
        }

        main.getEconomy().withdrawPlayer(offlinePlayer, totalPrice);
        getPlayerOrders(order.getPlayerId()).add(order);
        player.sendMessage(LanguageLoader.getMessage("order-created")
                .replace("%material%", order.getMaterial().name())
                .replace("%amount%", String.valueOf(order.getAmount()))
                .replace("%total_price%", String.format("%.2f", totalPrice))
                .replace("%price%", String.valueOf(order.getPrice())));
        order.setStatus(OrderStatus.ACTIVE);
        main.getPlayerStatsManager().getStatistics(order.getPlayerId()).addTotalOrders(1);
        main.getOrderLogger().logOrderCreated(order, totalPrice);
        NSound.success(player);
        broadcastOrder(order, totalPrice);
        DiscordWebhook webhook = main.getWebhookManager().getWebhooks().get("order-create");
        if (webhook != null) {
            DiscordWebhook clonedWebhook = webhook.clone();

            String content = clonedWebhook.getContent()
                    .replace("%player%", order.getPlayerName());

            clonedWebhook.setContent(content);

            for (DiscordWebhook.EmbedObject embed : clonedWebhook.getEmbeds()) {
                embed.setDescription(embed.getDescription()
                        .replace("%player%", order.getPlayerName()));
                embed.setTitle(embed.getTitle().replace("%material%", order.getMaterial().name()));
                embed.setThumbnail("https://mc.nerothe.com/img/1.21.8/minecraft_" + order.getMaterial().name().toLowerCase() + ".png");
                embed.setAuthor(player.getName(), null, "https://api.mcheads.org/head/" + player.getName());
                for (int i = 0; i < embed.getFields().size(); i++) {
                    DiscordWebhook.EmbedObject.Field field = embed.getFields().get(i);
                    String value = field.getValue()
                            .replace("%material%", order.getMaterial().name())
                            .replace("%amount%", String.valueOf(order.getAmount()))
                            .replace("%price%", String.format("%.2f", order.getPrice()))
                            .replace("%total_price%", String.format("%.2f", order.getPrice() * order.getAmount()))
                            .replace("%enchantments%", formatEnchantmentsForWebhook(order.getItem()))
                            .replace("%duration%", order.getRemainingHours() + " saat");

                    embed.getFields().set(i, new DiscordWebhook.EmbedObject.Field(
                            field.getName(),
                            value,
                            field.isInline()
                    ));
                }
            }

            try {
                clonedWebhook.execute();
            } catch (IOException e) {
                NLogger.error("An error occurred while sending order creation webhook: " + e.getMessage());
            }
        }
    }

    private String formatEnchantmentsForWebhook(ItemStack item) {
        if (item == null) return "-";

        Map<Enchantment, Integer> enchants;
        if (item.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
            if (meta != null) {
                enchants = meta.getStoredEnchants();
            } else {
                return "-";
            }
        } else {
            enchants = item.getEnchantments();
        }

        if (enchants.isEmpty()) return "-";

        return enchants.entrySet().stream()
                .map(entry -> entry.getKey().getKey().getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    public void cancelOrder(Order order) {
        OfflinePlayer offlinePlayer = main.getServer().getOfflinePlayer(order.getPlayerId());
        if (PlayerUtil.getPlayer(offlinePlayer) == null) {
            return;
        }
        Player player = PlayerUtil.getPlayer(offlinePlayer);

        double refundAmount = (order.getAmount() - order.getDelivered()) * order.getPrice();
        main.getEconomy().depositPlayer(offlinePlayer, refundAmount);
        main.getOrderLogger().logOrderCancelled(order, refundAmount);
        removeOrder(order);
        if (player.isOnline()) {
            player.sendMessage(LanguageLoader.getMessage("order-cancelled")
                    .replace("%id%", order.getId())
                    .replace("%material%", StringUtil.formatMaterialName(order.getMaterial()))
                    .replace("%amount%", String.valueOf(order.getAmount() - order.getDelivered()))
                    .replace("%refund_amount%", String.format("%.2f", refundAmount)));
            NSound.success(player);
        }

    }

    public boolean removeOrder(Order order) {
        List<Order> playerOrders = ordersByPlayer.get(order.getPlayerId());
        if (playerOrders == null) return false;

        boolean removed = playerOrders.removeIf(o -> o.getId().equals(order.getId()));

        if (playerOrders.isEmpty()) {
            ordersByPlayer.remove(order.getPlayerId());
        }

        try (Connection conn = main.getDatabaseManager().getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM orders WHERE order_id = ?")) {

            stmt.setString(1, order.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            NLogger.error("Failed to remove order from database: " + e.getMessage());
            return false;
        }

        return removed;
    }

    public void updateOrderStatusInDatabase(Order order) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";

        try (Connection conn = main.getDatabaseManager().getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, order.getStatus().name());
            stmt.setString(2, order.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            NLogger.error("Failed to update order status in database: " + e.getMessage());
        }
    }

    public String createRandomId() {
        int length = 6;
        final String chars = "0123456789";
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void broadcastOrder(Order order, double totalPrice) {
        if (!Settings.BROADCAST_ENABLED) return;
        if (totalPrice < Settings.BROADCAST_MIN_TOTAL_PRICE) return;

        String playerName = order.getPlayerName();

        if (playerName == null || playerName.isEmpty()) {
            OfflinePlayer offlinePlayer = main.getServer().getOfflinePlayer(order.getPlayerId());
            playerName = offlinePlayer.getName();
            if (playerName == null) {
                playerName = "";
            }
        }

        String message = LanguageLoader.getMessage("order-broadcast")
                .replace("%player%", playerName)
                .replace("%material%", StringUtil.formatMaterialName(order.getMaterial()))
                .replace("%amount%", String.valueOf(order.getAmount()))
                .replace("%price%", String.format("%.2f", order.getPrice()))
                .replace("%total_price%", String.format("%.2f", totalPrice));

        main.getServer().broadcast(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }

    public void cleanExpiredOrders() {
        List<Order> expiredOrders = new ArrayList<>();

        for (List<Order> orders : new ArrayList<>(ordersByPlayer.values())) {
            for (Order order : new ArrayList<>(orders)) {
                if (order.getStatus() != OrderStatus.ACTIVE) continue;

                if (order.isExpired()) {
                    expiredOrders.add(order);
                }
            }
        }

        for (Order order : expiredOrders) {
            order.setStatus(OrderStatus.COMPLETED);

            double refundAmount = Math.max(0, order.getAmount() - order.getDelivered()) * order.getPrice();
            if (refundAmount > 0) {
                OfflinePlayer player = main.getServer().getOfflinePlayer(order.getPlayerId());
                main.getEconomy().depositPlayer(player, refundAmount);
            }
            main.getOrderLogger().logOrderExpired(order, refundAmount);

            removeOrder(order);

            order.setStatus(OrderStatus.ARCHIVED);
            main.getOrderLogger().logOrderArchived(order);

            updateOrderStatusInDatabase(order);
        }

        ordersByPlayer.values().removeIf(List::isEmpty);

        /*try (Connection conn = main.getDatabaseManager().getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM orders WHERE expires_at < ?")) {
            stmt.setTimestamp(1, Timestamp.valueOf(now));
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                NLogger.info("Cleaned " + deleted + " expired orders");
            }
        } catch (SQLException e) {
            NLogger.error("Failed to clean expired orders: " + e.getMessage());
        }*/
    }

    public List<Order> getAllOrders() {
        return ordersByPlayer.values().stream()
                .flatMap(List::stream)
                .filter(o -> o.getStatus() == OrderStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    public List<Order> getOrdersByMaterial(UUID playerId, Material material) {
        return getPlayerOrders(playerId).stream()
                .filter(o -> o.getMaterial() == material)
                .filter(o -> o.getStatus() == OrderStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    public List<Order> getCompletedOrders() {
        return getAllOrders().stream()
                .filter(o -> o.getDelivered() >= o.getAmount())
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .collect(Collectors.toList());
    }

    public List<Order> getPendingOrders() {
        return getAllOrders().stream()
                .filter(o -> o.getDelivered() < o.getAmount())
                .collect(Collectors.toList());
    }

    public Order getOrderById(String orderId) {
        for (Order order : getAllOrders()) {
            if (order.getId().equals(orderId)) {
                return order;
            }
        }
        return null;
    }

    public void startCleanupTask() {
        cleanupTask = main.getMorePaperLib().scheduling().asyncScheduler().runAtFixedRate(
                this::cleanExpiredOrders,
                Duration.ofMinutes(10),

                Duration.ofMinutes(10)
        );
    }

    public void startAutoSaveTask() {
        int intervalMinutes = Settings.AUTO_SAVE_INTERVAL_MINUTES;
        if (intervalMinutes <= 0) {
            NLogger.warn("Auto-save interval is disabled or invalid. Orders will only be saved on server shutdown.");
            return;
        }
        
        autoSaveTask = main.getMorePaperLib().scheduling().asyncScheduler().runAtFixedRate(
                this::saveOrders,
                Duration.ofMinutes(intervalMinutes),
                Duration.ofMinutes(intervalMinutes)
        );
        NLogger.info("Auto-save task started with " + intervalMinutes + " minute interval.");
    }
    
    public void stopTasks() {
        if (cleanupTask != null) {
            main.getMorePaperLib().scheduling().cancelGlobalTasks();
        }
        if (autoSaveTask != null) {
            main.getMorePaperLib().scheduling().cancelGlobalTasks();
        }
    }

}
