package com.notpatch.nOrder.manager;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInputManager {

    private final Map<UUID, Consumer<String>> awaitingInput = new HashMap<>();

    public void setAwaitingInput(Player player, Consumer<String> callback) {
        awaitingInput.put(player.getUniqueId(), callback);
    }

    public boolean isAwaitingInput(Player player) {
        return awaitingInput.containsKey(player.getUniqueId());
    }

    public void handleInput(Player player, String input) {
        Consumer<String> callback = awaitingInput.remove(player.getUniqueId());
        if (callback != null) {
            callback.accept(input);
        }
    }

    public void cancelInput(Player player) {
        awaitingInput.remove(player.getUniqueId());
    }
}