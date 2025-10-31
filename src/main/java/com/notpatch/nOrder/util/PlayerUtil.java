package com.notpatch.nOrder.util;

import com.notpatch.nOrder.Settings;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class PlayerUtil {

    public static Player getPlayer(OfflinePlayer offlinePlayer){
        if(offlinePlayer instanceof Player player){
            if(player.isOnline()) return player;
        }
        return null;
    }

    public static int getPlayerOrderLimit(Player player) {
        final String permissionPrefix = Settings.ORDER_LIMIT_PERMISSION + ".";
        int limit = 5;
        if (player == null) return limit;

        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String perm = info.getPermission();
            perm = perm.toLowerCase();
            if (perm.startsWith(permissionPrefix)) {
                String numPart = perm.substring(permissionPrefix.length());
                try {
                    int val = Integer.parseInt(numPart);
                    if (val > limit) limit = val;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return limit;
    }

    public static int getPlayerOrderExpiration(Player player) {
        final String permissionPrefix = Settings.ORDER_EXPIRATION_PERMISSION + ".";
        int limit = 7;
        if (player == null) return limit;

        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String perm = info.getPermission();
            perm = perm.toLowerCase();
            if (perm.startsWith(permissionPrefix)) {
                String numPart = perm.substring(permissionPrefix.length());
                try {
                    int val = Integer.parseInt(numPart);
                    if (val > limit) limit = val;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return limit;
    }

    public static boolean isPlayerAdmin(Player player) {
        final String adminPermission = Settings.ORDER_ADMIN_PERMISSION;
        if (player == null) return false;
        return player.hasPermission(adminPermission) || player.isOp();
    }



}
