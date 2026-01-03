package com.notpatch.nOrder.hook.customitem;

import com.notpatch.nOrder.Settings;
import com.notpatch.nlib.util.NLogger;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemsAdderProvider implements CustomItemProvider {

    private final boolean available;

    public ItemsAdderProvider() {
        boolean iaFound = false;
        try {
            if (Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
                if (Settings.DEBUG) {
                    NLogger.info("ItemsAdder plugin detected, checking API availability...");
                }
                Class.forName("dev.lone.itemsadder.api.CustomStack");
                iaFound = true;
                if (Settings.DEBUG) {
                    NLogger.info("ItemsAdder API is available!");
                }
            } else {
                if (Settings.DEBUG) {
                    NLogger.info("ItemsAdder plugin not found in PluginManager");
                }
            }
        } catch (ClassNotFoundException e) {
            NLogger.error("ItemsAdder plugin found but API class not available: " + e.getMessage());
        } catch (Exception e) {
            NLogger.error("Error checking ItemsAdder availability: " + e.getMessage());
        }
        this.available = iaFound;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String getProviderName() {
        return "ItemsAdder";
    }

    @Override
    public boolean isCustomItem(ItemStack item) {
        if (!available || item == null) return false;
        try {
            return CustomStack.byItemStack(item) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getCustomItemId(ItemStack item) {
        if (!available || item == null) return null;
        try {
            CustomStack customStack = CustomStack.byItemStack(item);
            return customStack != null ? customStack.getNamespacedID() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ItemStack getCustomItem(String itemId) {
        if (!available || itemId == null) return null;
        try {
            if (Settings.DEBUG) {
                NLogger.info("ItemsAdder: Attempting to load item with ID: " + itemId);
            }
            CustomStack customStack = CustomStack.getInstance(itemId);
            if (customStack == null) {
                if (Settings.DEBUG) {
                    NLogger.error("ItemsAdder: CustomStack.getInstance returned null for: " + itemId);
                }
                return null;
            }
            ItemStack result = customStack.getItemStack();
            if (Settings.DEBUG) {
                NLogger.info("ItemsAdder: Successfully loaded item: " + itemId);
            }
            return result;
        } catch (Exception e) {
            NLogger.error("ItemsAdder: Failed to load item '" + itemId + "': " + e.getClass().getName() + " - " + e.getMessage());
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
            CustomStack customStack = CustomStack.byItemStack(item);
            return customStack != null ? customStack.getDisplayName() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isSameCustomItem(ItemStack item1, ItemStack item2) {
        if (!available || item1 == null || item2 == null) return false;
        try {
            CustomStack custom1 = CustomStack.byItemStack(item1);
            CustomStack custom2 = CustomStack.byItemStack(item2);
            if (custom1 == null || custom2 == null) return false;
            return custom1.getNamespacedID().equals(custom2.getNamespacedID());
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
                NLogger.error("Failed to load ItemsAdder item: " + itemId + " - " + e.getMessage());
            }
        }

        NLogger.info("Loaded " + items.size() + " ItemsAdder items from config.");
        return items;
    }
}

