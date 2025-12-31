package com.notpatch.nOrder;

import com.notpatch.nOrder.hook.ItemsAdderHook;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class Settings {

    public Settings() {
    }

    public static String ORDER_MENU_PERMISSION;
    public static String ORDER_PERMISSION;

    public static String ORDER_ADMIN_PERMISSION;

    public static Collection<String> ORDER_ALIASES;
    public static Collection<String> ORDER_ADMIN_ALIASES;

    public static String ORDER_LIMIT_PERMISSION;
    public static String ORDER_EXPIRATION_PERMISSION;

    public static String DATE_FORMAT;

    public static double HIGHLIGHT_FEE;
    public static boolean SEND_WEBHOOKS;

    public static double MIN_PRICE_PER_ITEM;
    public static double MAX_PRICE_PER_ITEM;
    public static Map<Material, Double> MIN_PRICE_PER_ITEM_OVERRIDES = new HashMap<>();
    public static Map<Material, Double> MAX_PRICE_PER_ITEM_OVERRIDES = new HashMap<>();

    public static String HIGHLIGHT_PERMISSION;

    public static List<Material> availableItems = new ArrayList<>();

    public static int PROGRESS_BAR_LENGTH;
    public static char PROGRESS_BAR_COMPLETE_CHAR;
    public static char PROGRESS_BAR_INCOMPLETE_CHAR;
    public static String PROGRESS_BAR_COMPLETE_COLOR;
    public static String PROGRESS_BAR_INCOMPLETE_COLOR;

    public static boolean BROADCAST_ENABLED;
    public static double BROADCAST_MIN_TOTAL_PRICE;

    public static boolean ITEMSADDER_ENABLED;
    public static List<ItemStack> customItems = new ArrayList<>();
    public static Map<String, ItemStack> customItemsCache = new HashMap<>();


    public static void loadSettings() {
        Configuration config = NOrder.getInstance().getConfig();
        ORDER_PERMISSION = config.getString("permissions.order", "norder.use");
        ORDER_MENU_PERMISSION = config.getString("permissions.order-menu", "norder.menu");
        ORDER_ALIASES = config.getStringList("commands.order.aliases");
        ORDER_LIMIT_PERMISSION = config.getString("permissions.order-limit", "norder.limit");
        ORDER_ADMIN_ALIASES = config.getStringList("commands.order-admin.aliases");
        ORDER_ADMIN_PERMISSION = config.getString("permissions.order-admin", "norder.admin");
        HIGHLIGHT_FEE = config.getDouble("settings.highlight-fee", 2.5);
        SEND_WEBHOOKS = config.getBoolean("settings.send-webhooks", false);
        MIN_PRICE_PER_ITEM = config.getDouble("settings.min-price-per-item", 0.01);
        MAX_PRICE_PER_ITEM = config.getDouble("settings.max-price-per-item", 1000);
        
        // Load per-item price overrides
        MIN_PRICE_PER_ITEM_OVERRIDES.clear();
        MAX_PRICE_PER_ITEM_OVERRIDES.clear();
        ConfigurationSection perItemPriceSection = config.getConfigurationSection("per-item-price");
        if (perItemPriceSection != null) {
            for (String materialName : perItemPriceSection.getKeys(false)) {
                Material material = Material.matchMaterial(materialName);
                if (material != null) {
                    ConfigurationSection itemSection = perItemPriceSection.getConfigurationSection(materialName);
                    if (itemSection != null) {
                        if (itemSection.contains("min-price-per-item")) {
                            MIN_PRICE_PER_ITEM_OVERRIDES.put(material, itemSection.getDouble("min-price-per-item"));
                        }
                        if (itemSection.contains("max-price-per-item")) {
                            MAX_PRICE_PER_ITEM_OVERRIDES.put(material, itemSection.getDouble("max-price-per-item"));
                        }
                    }
                }
            }
        }
        
        DATE_FORMAT = config.getString("date-format", "MM-dd HH:mm:ss");
        HIGHLIGHT_PERMISSION = config.getString("permissions.use-highlight", "norder.highlight");
        ORDER_EXPIRATION_PERMISSION = config.getString("permissions.order-expiration", "norder.expiration");
        List<Material> blacklist = config.getStringList("blacklist-items").stream()
                .map(Material::matchMaterial)
                .filter(Objects::nonNull)
                .toList();

        PROGRESS_BAR_LENGTH = config.getInt("progress-bar.length", 20);
        PROGRESS_BAR_COMPLETE_CHAR = config.getString("progress-bar.complete-symbol", "█").charAt(0);
        PROGRESS_BAR_INCOMPLETE_CHAR = config.getString("progress-bar.incomplete-symbol", "░").charAt(0);
        PROGRESS_BAR_COMPLETE_COLOR = config.getString("progress-bar.complete-color", "&a");
        PROGRESS_BAR_INCOMPLETE_COLOR = config.getString("progress-bar.incomplete-color", "&7");

        BROADCAST_ENABLED = config.getBoolean("settings.broadcast.enabled", true);
        BROADCAST_MIN_TOTAL_PRICE = config.getDouble("settings.broadcast.min-total-price", 1000);

        ITEMSADDER_ENABLED = config.getBoolean("settings.itemsadder-support", true);

        loadCustomItems();

        NOrder.getInstance().getMorePaperLib().scheduling().asyncScheduler().run(() -> {
            List<Material> items = Arrays.stream(Material.values())
                    .filter(m -> !m.isAir() && m.isItem() && !m.isLegacy())
                    .filter(m -> !blacklist.contains(m))
                    .collect(Collectors.toList());

            NOrder.getInstance().getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> availableItems = items);
        });
    }


    private static void loadCustomItems() {
        customItems.clear();
        customItemsCache.clear();

        if (!ITEMSADDER_ENABLED) {
            return;
        }

        ItemsAdderHook itemsAdderHook = NOrder.getInstance().getItemsAdderHook();
        if (itemsAdderHook == null || !itemsAdderHook.isAvailable()) {
            return;
        }

        Configuration customItemsConfig = NOrder.getInstance()
                .getConfigurationManager()
                .getCustomItemConfiguration()
                .getConfiguration();

        List<String> itemIds = customItemsConfig.getStringList("items");

        if (itemIds.isEmpty()) {
            return;
        }

        customItems = NOrder.getInstance().getItemsAdderHook().getCustomItemsFromIds(itemIds);

        if (!customItems.isEmpty()) {
            for (ItemStack item : customItems) {
                String customId = NOrder.getInstance().getItemsAdderHook().getCustomItemId(item);
                if (customId != null) {
                    customItemsCache.put(customId, item.clone());
                }
            }
            NOrder.getInstance().getLogger().info("✓ Cached " + customItemsCache.size() + " custom items");
        }
    }

    public static ItemStack getCustomItemFromCache(String customItemId) {
        if (customItemId == null || customItemsCache.isEmpty()) {
            return null;
        }
        ItemStack cached = customItemsCache.get(customItemId);
        return cached != null ? cached.clone() : null;
    }

    /**
     * Get the minimum price per item for a specific material
     * @param material The material to get the minimum price for
     * @return The minimum price per item
     */
    public static double getMinPricePerItem(Material material) {
        return MIN_PRICE_PER_ITEM_OVERRIDES.getOrDefault(material, MIN_PRICE_PER_ITEM);
    }

    /**
     * Get the maximum price per item for a specific material
     * @param material The material to get the maximum price for
     * @return The maximum price per item
     */
    public static double getMaxPricePerItem(Material material) {
        return MAX_PRICE_PER_ITEM_OVERRIDES.getOrDefault(material, MAX_PRICE_PER_ITEM);
    }

}
