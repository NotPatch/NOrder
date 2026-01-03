package com.notpatch.nOrder.hook.customitem;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.Settings;
import com.notpatch.nlib.util.NLogger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class NexoProvider implements CustomItemProvider, Listener {

    private final boolean available;

    @Getter
    private boolean isInitialized = false;

    public NexoProvider() {
        boolean nexoFound = false;
        try {
            if (Bukkit.getPluginManager().getPlugin("Nexo") != null) {
                if (Settings.DEBUG) {
                    NLogger.info("Nexo plugin detected, checking API availability...");
                }
                Class.forName("com.nexomc.nexo.api.NexoItems");
                nexoFound = true;
                if (Settings.DEBUG) {
                    NLogger.info("Nexo API is available!");
                }

                Bukkit.getPluginManager().registerEvents(this, NOrder.getInstance());
            } else {
                if (Settings.DEBUG) {
                    NLogger.info("Nexo plugin not found in PluginManager");
                }
            }
        } catch (ClassNotFoundException e) {
            NLogger.error("Nexo plugin found but API class not available: " + e.getMessage());
        } catch (Exception e) {
            NLogger.error("Error checking Nexo availability: " + e.getMessage());
        }
        this.available = nexoFound;
    }

    @EventHandler
    public void onNexoItemsLoaded(NexoItemsLoadedEvent event) {
        isInitialized = true;
        Settings.loadCustomItems();
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String getProviderName() {
        return "Nexo";
    }

    @Override
    public boolean isCustomItem(ItemStack item) {
        if (!available || item == null) return false;
        try {
            return NexoItems.exists(item);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getCustomItemId(ItemStack item) {
        if (!available || item == null) return null;
        try {
            return NexoItems.idFromItem(item);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ItemStack getCustomItem(String itemId) {
        if (!available || itemId == null) return null;
        try {
            if (Settings.DEBUG) {
                NLogger.info("Nexo: Attempting to load item with ID: " + itemId);
            }
            var itemBuilder = NexoItems.itemFromId(itemId);
            if (itemBuilder == null) {
                if (Settings.DEBUG) {
                    NLogger.error("Nexo: itemFromId returned null for: " + itemId);
                }
                return null;
            }
            ItemStack result = itemBuilder.build();
            if (Settings.DEBUG) {
                NLogger.info("Nexo: Successfully loaded item: " + itemId);
            }
            return result;
        } catch (Exception e) {
            NLogger.error("Nexo: Failed to load item '" + itemId + "': " + e.getClass().getName() + " - " + e.getMessage());
            if (Settings.DEBUG) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    public String getCustomItemDisplayName(ItemStack item) {
        if (!available || item == null) return null;
        try {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                return item.getItemMeta().getDisplayName();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isSameCustomItem(ItemStack item1, ItemStack item2) {
        if (!available || item1 == null || item2 == null) return false;
        try {
            String id1 = getCustomItemId(item1);
            String id2 = getCustomItemId(item2);
            return id1 != null && id1.equals(id2);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<ItemStack> getCustomItemsFromIds(List<String> itemIds) {
        List<ItemStack> items = new ArrayList<>();
        if (!available || itemIds == null) return items;

        for (String itemId : itemIds) {
            if (itemId == null || itemId.trim().isEmpty()) continue;
            try {
                ItemStack item = getCustomItem(itemId.trim());
                if (item != null) {
                    items.add(item);
                }
            } catch (Exception e) {
                NLogger.warn("Failed to load Nexo item: " + itemId + " - " + e.getMessage());
            }
        }

        NLogger.info("Loaded " + items.size() + " Nexo items from config.");
        return items;
    }
}

