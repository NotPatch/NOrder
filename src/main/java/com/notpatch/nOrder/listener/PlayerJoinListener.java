package com.notpatch.nOrder.listener;

import com.notpatch.nOrder.NOrder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        // Performans için sadece isim değiştiyse güncelle
        String currentName = player.getName();
        if (currentName != null && !currentName.isEmpty()) {
            NOrder.getInstance().getDatabaseManager().updatePlayerName(player.getUniqueId(), currentName);
        }

    }

}
