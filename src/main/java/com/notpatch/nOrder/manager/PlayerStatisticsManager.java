package com.notpatch.nOrder.manager;

import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.database.DatabaseManager;
import com.notpatch.nOrder.model.PlayerStatistics;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.UUID;

public class PlayerStatisticsManager {

    private final NOrder main;
    private final DatabaseManager databaseManager;

    @Getter
    private HashMap<UUID, PlayerStatistics> statisticsMap = new HashMap<>();

    public PlayerStatisticsManager(NOrder main){
        this.main = main;
        this.databaseManager = main.getDatabaseManager();
    }

    public void loadStatistics() {
        String query = "SELECT * FROM player_stats";

        try (Connection conn = databaseManager.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            statisticsMap.clear();

            while (rs.next()) {
                UUID playerId = UUID.fromString(rs.getString("player_id"));
                PlayerStatistics stats = new PlayerStatistics(playerId, rs.getString("player_name"));
                stats.setDeliveredItems(rs.getInt("delivered_items"));
                stats.setCollectedItems(rs.getInt("collected_items"));
                stats.setTotalOrders(rs.getInt("total_orders"));
                stats.setTotalEarnings(rs.getDouble("total_earnings"));
                statisticsMap.put(playerId, stats);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveStatistics() {
        String query = """
            INSERT INTO player_stats (player_id, player_name, delivered_items, collected_items, total_orders, total_earnings)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_id) DO UPDATE SET
            player_name = ?,
            delivered_items = ?,
            collected_items = ?,
            total_orders = ?,
            total_earnings = ?
            """;

        try (Connection conn = databaseManager.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            for (PlayerStatistics stats : statisticsMap.values()) {
                stmt.setString(1, stats.getPlayerId().toString());
                stmt.setString(2, stats.getPlayerName());
                stmt.setInt(3, stats.getDeliveredItems());
                stmt.setInt(4, stats.getCollectedItems());
                stmt.setInt(5, stats.getTotalOrders());
                stmt.setDouble(6, stats.getTotalEarnings());

                stmt.setString(7, stats.getPlayerName());
                stmt.setInt(8, stats.getDeliveredItems());
                stmt.setInt(9, stats.getCollectedItems());
                stmt.setInt(10, stats.getTotalOrders());
                stmt.setDouble(11, stats.getTotalEarnings());

                stmt.addBatch();
            }
            stmt.executeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PlayerStatistics getStatistics(UUID playerId) {
        return statisticsMap.computeIfAbsent(playerId, id -> new PlayerStatistics(id, Bukkit.getOfflinePlayer(id).getName()));
    }

    public void addStatistics(PlayerStatistics stats) {
        statisticsMap.put(stats.getPlayerId(), stats);
    }

    public PlayerStatistics getStatisticsByName(String playerName) {
        for (PlayerStatistics stats : statisticsMap.values()) {
            if (stats.getPlayerName().equalsIgnoreCase(playerName)) {
                return stats;
            }
        }
        return null;
    }

}
