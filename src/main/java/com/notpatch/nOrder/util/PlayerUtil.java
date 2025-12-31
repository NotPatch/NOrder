package com.notpatch.nOrder.util;

import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.Settings;
import com.notpatch.nOrder.hook.LuckPermsHook;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class PlayerUtil {

    public static Player getPlayer(OfflinePlayer offlinePlayer) {
        if (offlinePlayer instanceof Player player) {
            if (player.isOnline()) return player;
        }
        return null;
    }

    public static int getPlayerOrderLimit(Player player) {
        return getPermissionValue(player, Settings.ORDER_LIMIT_PERMISSION, 5, true);
    }

    public static int getPlayerOrderExpiration(Player player) {
        return getPermissionValue(player, Settings.ORDER_EXPIRATION_PERMISSION, 7, false);
    }

    public static boolean isPlayerAdmin(Player player) {
        if (player == null) return false;
        return player.hasPermission(Settings.ORDER_ADMIN_PERMISSION) || player.isOp();
    }

    /**
     * Gets the permission value for a player using LuckPerms if available, otherwise falls back to Bukkit permissions
     *
     * @param player           The player to check
     * @param permissionPrefix The permission prefix (e.g., "norder.limit")
     * @param defaultValue     The default value to return if no permission is found
     * @param isLimit          Whether this is for order limit (true) or expiration (false)
     * @return The highest permission value found
     */
    private static int getPermissionValue(Player player, String permissionPrefix, int defaultValue, boolean isLimit) {
        if (player == null) return defaultValue;

        // Try LuckPerms first
        LuckPermsHook luckPermsHook = NOrder.getInstance().getLuckPermsHook();
        if (luckPermsHook != null && luckPermsHook.isAvailable()) {
            return isLimit
                    ? luckPermsHook.getOrderLimit(player, defaultValue)
                    : luckPermsHook.getOrderExpiration(player, defaultValue);
        }

        // Fallback to Bukkit permissions
        return findHighestBukkitPermission(player, permissionPrefix, defaultValue);
    }

    /**
     * Finds the highest numeric permission value from Bukkit's permission system
     *
     * @param player           The player to check
     * @param permissionPrefix The permission prefix
     * @param defaultValue     The default value
     * @return The highest value found
     */
    private static int findHighestBukkitPermission(Player player, String permissionPrefix, int defaultValue) {
        final String prefix = (permissionPrefix + ".").toLowerCase();
        int highest = defaultValue;

        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;

            String permission = info.getPermission().toLowerCase();
            if (permission.startsWith(prefix)) {
                String numberPart = permission.substring(prefix.length());
                try {
                    int value = Integer.parseInt(numberPart);
                    if (value > highest) {
                        highest = value;
                    }
                } catch (NumberFormatException ignored) {
                    // Invalid number in permission node, skip it
                }
            }
        }

        return highest;
    }


}
