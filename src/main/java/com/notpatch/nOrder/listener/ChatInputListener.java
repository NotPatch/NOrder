package com.notpatch.nOrder.listener;

import com.notpatch.nOrder.NOrder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatInputListener implements Listener {

    private final NOrder plugin;

    public ChatInputListener(NOrder plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (plugin.getChatInputManager().isAwaitingInput(event.getPlayer())) {
            event.setCancelled(true);
            plugin.getChatInputManager().handleInput(event.getPlayer(), event.getMessage());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getChatInputManager().cancelInput(event.getPlayer());
    }
}