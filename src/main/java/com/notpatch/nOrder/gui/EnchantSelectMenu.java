package com.notpatch.nOrder.gui;

import com.notpatch.nOrder.NOrder;
import com.notpatch.nlib.builder.ItemBuilder;
import com.notpatch.nlib.effect.NSound;
import com.notpatch.nlib.fastinv.FastInv;
import com.notpatch.nlib.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.*;
import java.util.stream.Collectors;

public class EnchantSelectMenu extends FastInv {

    private final NewOrderMenu parentMenu;
    private final Material selectedMaterial;
    private final Map<Enchantment, Integer> selectedEnchants = new HashMap<>();
    private final List<Enchantment> availableEnchants;
    private final Configuration config;
    private List<Integer> fillerSlots = new ArrayList<>();

    public EnchantSelectMenu(NewOrderMenu parentMenu, Material material) {
        super(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration().getInt("enchant-select-menu.size"),
                ColorUtil.hexColor(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration().getString("enchant-select-menu.title")));

        this.parentMenu = parentMenu;
        this.selectedMaterial = material;
        this.config = NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration();

        ItemStack item = new ItemStack(material);
        this.availableEnchants = Arrays.stream(Enchantment.values())
                .filter(enchantment -> enchantment.canEnchantItem(item))
                .collect(Collectors.toList());

        updateEnchantmentItems();
        loadMenuItems();

    }

    private void loadMenuItems() {
        Configuration config = NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration();
        ConfigurationSection itemsSection = config.getConfigurationSection("enchant-select-menu.items");

        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = ItemBuilder.getItemFromSection(itemSection);
                    String action = itemSection.getString("action", "");

                    if (itemSection.contains("slot")) {
                        setItem(itemSection.getInt("slot"), item, e -> {
                            if (!action.isEmpty()) {
                                fillerSlots.add(itemSection.getInt("slot"));
                                handleAction(action, (Player) e.getWhoClicked());
                            }
                        });
                    } else if (itemSection.contains("slots")) {
                        List<Integer> slots = parseSlots(itemSection.getString("slots"));
                        for (int slot : slots) {
                            fillerSlots.add(slot);
                            setItem(slot, item);
                        }
                    }
                }
            }
        }
    }

    private void updateEnchantmentItems() {
        for (int i = 10; i < 45; i++) {
            if(fillerSlots.contains(i)) continue;
            setItem(i, null);
        }

        for (int i = 0; i < availableEnchants.size(); i++) {
            if (i >= 28) break;

            Enchantment enchant = availableEnchants.get(i);
            int slot = 10 + (i % 7) + ((i / 7) * 9);

            setItem(slot, createEnchantButton(enchant), e -> {
                if (e.isLeftClick()) {
                    increaseEnchantLevel(enchant);
                } else if (e.isRightClick()) {
                    decreaseEnchantLevel(enchant);
                }
                updateEnchantmentItems();
            });
        }
    }

    private ItemStack createEnchantButton(Enchantment enchant) {
        int level = selectedEnchants.getOrDefault(enchant, 0);
        String enchantName = formatEnchantmentName(enchant.getKey().getKey());

        List<String> lore = new ArrayList<>();
        for (String line : config.getStringList("enchant-select-menu.items.enchant-item-template.lore")) {
            line = ColorUtil.hexColor(line
                    .replace("%level%", String.valueOf(level > 0 ? level : "None"))
                    .replace("%max_level%", String.valueOf(enchant.getMaxLevel())));
            lore.add(line);
        }

        String displayName = ColorUtil.hexColor(config.getString("enchant-select-menu.items.enchant-item-template.name", "&f" + enchantName)
                .replace("%enchant_name%", enchantName));

        return ItemBuilder.builder()
                .material(Material.ENCHANTED_BOOK)
                .displayName(displayName)
                .lore(lore)
                .glow(level > 0)
                .build()
                .build();
    }

    private void increaseEnchantLevel(Enchantment enchant) {
        int currentLevel = selectedEnchants.getOrDefault(enchant, 0);
        if (currentLevel < enchant.getMaxLevel()) {
            selectedEnchants.put(enchant, currentLevel + 1);
        }
    }

    private void decreaseEnchantLevel(Enchantment enchant) {
        int currentLevel = selectedEnchants.getOrDefault(enchant, 0);
        if (currentLevel > 0) {
            if (currentLevel == 1) {
                selectedEnchants.remove(enchant);
            } else {
                selectedEnchants.put(enchant, currentLevel - 1);
            }
        }
    }

    private void handleAction(String action, Player player) {
        switch (action) {
            case "confirm-enchants" -> {
                ItemStack item = new ItemStack(selectedMaterial);
                if (selectedMaterial == Material.ENCHANTED_BOOK) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                    if (meta != null) {
                        selectedEnchants.forEach((enchant, level) ->
                                meta.addStoredEnchant(enchant, level, true));
                        item.setItemMeta(meta);
                    }
                } else {
                    selectedEnchants.forEach((enchant, level) ->
                            item.addEnchantment(enchant, level));
                }
                parentMenu.setSelectedItem(item);
                parentMenu.updateMenuItems();
                parentMenu.open(player);
            }
            case "back" -> parentMenu.open(player);
        }
    }

    private String formatEnchantmentName(String name) {
        return Arrays.stream(name.split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private List<Integer> parseSlots(String slotsString) {
        List<Integer> slots = new ArrayList<>();
        if (slotsString == null || slotsString.isEmpty()) return slots;

        slotsString = slotsString.replace("[", "").replace("]", "").replace(" ", "");

        for (String part : slotsString.split(",")) {
            if (part.contains("-")) {
                String[] range = part.split("-");
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                for (int i = start; i <= end; i++) {
                    slots.add(i);
                }
            } else if (!part.isEmpty()) {
                slots.add(Integer.parseInt(part));
            }
        }

        return slots;
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        NSound.click(player);
    }

}
