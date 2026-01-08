package com.notpatch.nOrder.util;

import com.google.common.collect.ArrayListMultimap;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nlib.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemStackHelper {

    public static boolean isSameItem(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) return false;

        if (NOrder.getInstance().getCustomItemManager() != null &&
                NOrder.getInstance().getCustomItemManager().hasAnyProvider()) {

            boolean isCustom1 = NOrder.getInstance().getCustomItemManager().isCustomItem(item1);
            boolean isCustom2 = NOrder.getInstance().getCustomItemManager().isCustomItem(item2);

            if (isCustom1 && isCustom2) {
                return NOrder.getInstance().getCustomItemManager().isSameCustomItem(item1, item2);
            }

            if (isCustom1 != isCustom2) return false;
        }

        return bukkitSameItem(item1, item2);
    }

    private static boolean bukkitSameItem(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return false;
        }

        if (item1.getType() != item2.getType()) {
            return false;
        }

        if (item1.hasItemMeta() != item2.hasItemMeta()) {
            return false;
        }

        if (item1 instanceof Damageable && item2 instanceof Damageable) {
            Damageable dmg1 = (Damageable) item1.getItemMeta();
            if (dmg1.getDamage() < item1.getType().getMaxDurability()) {
                return false;
            }
            Damageable dmg2 = (Damageable) item2.getItemMeta();
            if (dmg2.getDamage() < item2.getType().getMaxDurability()) {
                return false;
            }
        }

        if (item1.getItemMeta() == null || item2.getItemMeta() == null) {
            return true;
        }

        if (item1.getItemMeta().hasEnchants() != item2.getItemMeta().hasEnchants()) {
            return false;
        }

        if (item1.getItemMeta().hasEnchants()) {
            Map<Enchantment, Integer> enchants1 = item1.getItemMeta().getEnchants();
            Map<Enchantment, Integer> enchants2 = item2.getItemMeta().getEnchants();

            if (enchants1.size() != enchants2.size()) {
                return false;
            }

            for (Map.Entry<Enchantment, Integer> entry : enchants1.entrySet()) {
                Enchantment enchant = entry.getKey();
                Integer level1 = entry.getValue();
                Integer level2 = enchants2.get(enchant);

                if (level2 == null || !level1.equals(level2)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Gets the display name for an item (custom or vanilla)
     */
    public static String getItemDisplayName(ItemStack item) {
        if (item == null) return "Unknown";

        if (NOrder.getInstance().getCustomItemManager() != null &&
                NOrder.getInstance().getCustomItemManager().hasAnyProvider()) {

            String customName = NOrder.getInstance().getCustomItemManager().getCustomItemDisplayName(item);
            if (customName != null) return customName;
        }

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }

        return formatMaterialName(item.getType());
    }

    public static String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * Creates an ItemStack from a configuration section.
     * Supports: material, name, lore, amount, glow, item-flags, custom-model-data
     */
    public static ItemStack fromSection(ConfigurationSection section) {
        if (section == null) return new ItemStack(Material.STONE);

        String materialStr = section.getString("material", "STONE");
        Material material;
        try {
            material = Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }

        int amount = section.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);

        // Parse item flags first to use in editMeta
        List<String> flags = section.getStringList("item-flags");
        List<ItemFlag> itemFlags = new ArrayList<>();
        for (String flag : flags) {
            try {
                String formattedFlag = flag.toUpperCase().replace("-", "_");
                ItemFlag itemFlag = ItemFlag.valueOf(formattedFlag);
                itemFlags.add(itemFlag);
            } catch (IllegalArgumentException ignored) {
            }
        }

        item.editMeta(meta -> {
            // Display name
            String name = section.getString("name");
            if (name != null && !name.isEmpty()) {
                meta.setDisplayName(ColorUtil.hexColor(name));
            }

            // Lore
            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ColorUtil.hexColor(line));
                }
                meta.setLore(coloredLore);
            }

            // Custom model data
            if (section.contains("custom-model-data") && section.isInt("custom-model-data")) {
                int customModelData = section.getInt("custom-model-data");
                meta.setCustomModelData(customModelData);
            }

            // Glow effect
            if (section.getBoolean("glow", false)) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            // Item flags
            for (ItemFlag flag : itemFlags) {
                meta.addItemFlags(flag);
            }

            // Hide attributes if flag is present
            if (itemFlags.contains(ItemFlag.HIDE_ATTRIBUTES)) {
                meta.setAttributeModifiers(ArrayListMultimap.create());
            }

            // Try to add HIDE_ADDITIONAL_TOOLTIP (1.20.5+)
            try {
                meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
            } catch (IllegalArgumentException ignored) {
            }
        });

        return item;
    }

    /**
     * Creates an ItemStack builder for fluent API style.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an ItemStack builder with specified material.
     */
    public static Builder builder(Material material) {
        return new Builder().material(material);
    }

    public static class Builder {
        private Material material = Material.STONE;
        private int amount = 1;
        private String displayName;
        private List<String> lore = new ArrayList<>();
        private boolean glow = false;
        private List<ItemFlag> itemFlags = new ArrayList<>();

        public Builder material(Material material) {
            this.material = material;
            return this;
        }

        public Builder amount(int amount) {
            this.amount = Math.max(1, Math.min(64, amount));
            return this;
        }

        public Builder displayName(String name) {
            this.displayName = name;
            return this;
        }

        public Builder lore(List<String> lore) {
            this.lore = lore != null ? lore : new ArrayList<>();
            return this;
        }

        public Builder lore(String... lines) {
            this.lore = new ArrayList<>(List.of(lines));
            return this;
        }

        public Builder glow(boolean glow) {
            this.glow = glow;
            return this;
        }

        public Builder addFlag(ItemFlag flag) {
            this.itemFlags.add(flag);
            return this;
        }

        public Builder flags(List<ItemFlag> flags) {
            this.itemFlags = flags != null ? flags : new ArrayList<>();
            return this;
        }

        public Builder flagsFromStrings(List<String> flags) {
            if (flags == null) return this;
            for (String flag : flags) {
                try {
                    String formattedFlag = flag.toUpperCase().replace("-", "_");
                    ItemFlag itemFlag = ItemFlag.valueOf(formattedFlag);
                    this.itemFlags.add(itemFlag);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return this;
        }

        public ItemStack build() {
            ItemStack item = new ItemStack(material, amount);

            item.editMeta(meta -> {
                if (displayName != null && !displayName.isEmpty()) {
                    meta.setDisplayName(ColorUtil.hexColor(displayName));
                }

                if (!lore.isEmpty()) {
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : lore) {
                        coloredLore.add(ColorUtil.hexColor(line));
                    }
                    meta.setLore(coloredLore);
                }

                if (glow) {
                    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                for (ItemFlag flag : itemFlags) {
                    meta.addItemFlags(flag);
                }

                // Hide attributes if flag is present
                if (itemFlags.contains(ItemFlag.HIDE_ATTRIBUTES)) {
                    meta.setAttributeModifiers(ArrayListMultimap.create());
                }

                // Try to add HIDE_ADDITIONAL_TOOLTIP (1.20.5+)
                try {
                    meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
                } catch (IllegalArgumentException ignored) {
                }
            });

            return item;
        }
    }
}

