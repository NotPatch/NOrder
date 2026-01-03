package com.notpatch.nOrder.manager;

import com.notpatch.nOrder.Settings;
import com.notpatch.nOrder.hook.customitem.CustomItemProvider;
import com.notpatch.nOrder.hook.customitem.ItemsAdderProvider;
import com.notpatch.nOrder.hook.customitem.NexoProvider;
import com.notpatch.nlib.util.NLogger;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CustomItemManager {

    private final List<CustomItemProvider> providers = new ArrayList<>();

    public CustomItemManager() {
        registerProviderSafely("ItemsAdder", ItemsAdderProvider::new);
        //registerProviderSafely("MMOItems", MMOItemsProvider::new);
        registerProviderSafely("Nexo", NexoProvider::new);
    }

    private void registerProviderSafely(String name, ProviderSupplier supplier) {
        try {
            if (Settings.DEBUG) {
                NLogger.info("Attempting to load " + name + " provider...");
            }
            CustomItemProvider provider = supplier.get();
            registerProvider(provider);
        } catch (NoClassDefFoundError e) {
            if (Settings.DEBUG) {
                NLogger.info("Skipping " + name + " provider (missing dependency: " + e.getMessage() + ")");
            }
        } catch (ClassNotFoundException e) {
            if (Settings.DEBUG) {
                NLogger.info("Skipping " + name + " provider (class not found: " + e.getMessage() + ")");
            }
        } catch (Exception e) {
            NLogger.error("Failed to load " + name + " provider: " + e.getClass().getName() + " - " + e.getMessage());
            if (Settings.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    private void registerProvider(CustomItemProvider provider) {
        if (provider.isAvailable()) {
            providers.add(provider);
            NLogger.info(provider.getProviderName() + " custom item provider registered.");
        } else {
            if (Settings.DEBUG) {
                NLogger.info(provider.getProviderName() + " provider loaded but not available (plugin not enabled)");
            }
        }
    }

    @FunctionalInterface
    private interface ProviderSupplier {
        CustomItemProvider get() throws Exception;
    }

    public boolean hasAnyProvider() {
        return !providers.isEmpty();
    }

    public List<CustomItemProvider> getProviders() {
        return new ArrayList<>(providers);
    }

    public boolean isCustomItem(ItemStack item) {
        if (item == null) return false;
        for (CustomItemProvider provider : providers) {
            try {
                if (provider.isCustomItem(item)) {
                    return true;
                }
            } catch (NoClassDefFoundError | Exception e) {
            }
        }
        return false;
    }

    public String getCustomItemId(ItemStack item) {
        if (item == null) return null;
        for (CustomItemProvider provider : providers) {
            try {
                if (provider.isCustomItem(item)) {
                    String itemId = provider.getCustomItemId(item);
                    if (itemId != null) {
                        if (!itemId.contains(":")) {
                            return provider.getProviderName().toLowerCase() + ":" + itemId;
                        }
                        return provider.getProviderName().toLowerCase() + ":" + itemId;
                    }
                }
            } catch (NoClassDefFoundError | Exception e) {
            }
        }
        return null;
    }

    public ItemStack getCustomItem(String fullItemId) {
        if (fullItemId == null) return null;

        String[] parts = fullItemId.split(":", 2);

        if (parts.length >= 2) {
            String firstPart = parts[0].toLowerCase();

            for (CustomItemProvider provider : providers) {
                if (provider.getProviderName().toLowerCase().equals(firstPart)) {
                    String itemId = fullItemId.substring(firstPart.length() + 1);
                    if (Settings.DEBUG) {
                        NLogger.info("Loading " + provider.getProviderName() + " item: " + itemId);
                    }
                    try {
                        return provider.getCustomItem(itemId);
                    } catch (NoClassDefFoundError | Exception e) {
                        NLogger.error("Failed to load from " + provider.getProviderName() + ": " + e.getMessage());
                        return null;
                    }
                }
            }

            for (CustomItemProvider provider : providers) {
                try {
                    ItemStack item = provider.getCustomItem(fullItemId);
                    if (item != null) {
                        if (Settings.DEBUG) {
                            NLogger.info("Loaded item from " + provider.getProviderName() + ": " + fullItemId);
                        }
                        return item;
                    }
                } catch (NoClassDefFoundError | Exception e) {
                }
            }
        } else {
            for (CustomItemProvider provider : providers) {
                try {
                    ItemStack item = provider.getCustomItem(fullItemId);
                    if (item != null) {
                        if (Settings.DEBUG) {
                            NLogger.info("Loaded item from " + provider.getProviderName() + ": " + fullItemId);
                        }
                        return item;
                    }
                } catch (NoClassDefFoundError | Exception e) {
                }
            }
        }

        if (Settings.DEBUG) {
            NLogger.error("Failed to load custom item: " + fullItemId);
        }
        return null;
    }

    public String getCustomItemDisplayName(ItemStack item) {
        if (item == null) return null;
        for (CustomItemProvider provider : providers) {
            try {
                if (provider.isCustomItem(item)) {
                    return provider.getCustomItemDisplayName(item);
                }
            } catch (NoClassDefFoundError | Exception e) {
            }
        }
        return null;
    }

    public boolean isSameCustomItem(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) return false;

        for (CustomItemProvider provider : providers) {
            try {
                if (provider.isCustomItem(item1) && provider.isCustomItem(item2)) {
                    return provider.isSameCustomItem(item1, item2);
                }
            } catch (NoClassDefFoundError | Exception e) {
            }
        }
        return false;
    }

    public List<ItemStack> getCustomItemsFromIds(List<String> itemIds) {
        List<ItemStack> allItems = new ArrayList<>();
        if (itemIds == null) return allItems;

        for (String itemId : itemIds) {
            ItemStack item = getCustomItem(itemId);
            if (item != null) {
                allItems.add(item);
            }
        }

        return allItems;
    }

    public CustomItemProvider getProviderForItem(ItemStack item) {
        if (item == null) return null;
        for (CustomItemProvider provider : providers) {
            try {
                if (provider.isCustomItem(item)) {
                    return provider;
                }
            } catch (NoClassDefFoundError | Exception e) {
            }
        }
        return null;
    }
}

