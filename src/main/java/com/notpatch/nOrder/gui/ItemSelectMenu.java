package com.notpatch.nOrder.gui;

import com.notpatch.nOrder.LanguageLoader;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.Settings;
import com.notpatch.nOrder.util.ItemStackHelper;
import com.notpatch.nlib.effect.NSound;
import com.notpatch.nlib.fastinv.FastInv;
import com.notpatch.nlib.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemSelectMenu extends FastInv {

    private final NOrder main;

    private final NewOrderMenu parentMenu;
    private int currentPage = 1;
    private final List<Material> availableItems;
    private final List<Integer> itemSlots;
    private final int itemsPerPage;
    private String searchQuery = "";

    public ItemSelectMenu(NewOrderMenu parentMenu) {
        super(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration().getInt("item-select-menu.size"),
                ColorUtil.hexColor(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration().getString("item-select-menu.title")));

        this.parentMenu = parentMenu;
        main = NOrder.getInstance();
        Configuration config = main.getConfigurationManager().getMenuConfiguration().getConfiguration();

        this.availableItems = Settings.availableItems;

        List<String> slotsStr = config.getStringList("item-select-menu.item-slots");
        this.itemSlots = slotsStr.stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        this.itemsPerPage = config.getInt("item-select-menu.pagination.items-per-page", 21);

        loadMenuItems();
        updateItems();
    }

    private void loadMenuItems() {
        Configuration config = main.getConfigurationManager().getMenuConfiguration().getConfiguration();
        ConfigurationSection itemsSection = config.getConfigurationSection("item-select-menu.items");

        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = ItemStackHelper.fromSection(itemSection);
                    String action = itemSection.getString("action", "");

                    if (itemSection.contains("slot")) {
                        int slot = itemSection.getInt("slot");
                        setItem(slot, item, e -> {
                            if (!action.isEmpty()) {
                                handleAction(action, (Player) e.getWhoClicked());
                            }
                        });
                    } else if (itemSection.contains("slots")) {
                        List<Integer> slots = parseSlots(itemSection.getString("slots"));
                        for (int slot : slots) {
                            setItem(slot, item);
                        }
                    }
                }
            }
        }
    }

    private void updateItems() {
        for (int slot : itemSlots) {
            setItem(slot, null);
        }

        List<Material> filteredItems = getFilteredItems();
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredItems.size());

        for (int i = 0; i < itemsPerPage && startIndex + i < endIndex; i++) {
            if (i >= itemSlots.size()) break;

            int slot = itemSlots.get(i);
            Material material = filteredItems.get(startIndex + i);

            setItem(slot, createItemButton(material), e -> {
                if (material.name().contains("ENCHANTED") || canBeEnchanted(material)) {
                    parentMenu.setSelectedItem(material);
                    new EnchantSelectMenu(parentMenu, material).open((Player) e.getWhoClicked());
                } else {
                    parentMenu.setSelectedItem(material);
                    parentMenu.updateMenuItems();
                    parentMenu.open((Player) e.getWhoClicked());
                }
            });
        }

        updateNavigationButtons(filteredItems.size());
    }

    private void updateNavigationButtons(int totalItems) {
        Configuration config = main.getConfigurationManager().getMenuConfiguration().getConfiguration();
        ConfigurationSection itemsSection = config.getConfigurationSection("item-select-menu.items");

        if (itemsSection != null) {
            ConfigurationSection previousSection = itemsSection.getConfigurationSection("previous-page");
            if (previousSection != null) {
                ItemStack previousButton = ItemStackHelper.fromSection(previousSection);
                if (currentPage > 1) {
                    setItem(previousSection.getInt("slot"), previousButton,
                            e -> handleAction("previous-page", (Player) e.getWhoClicked()));
                } else {
                    setItem(previousSection.getInt("slot"),
                            ItemStackHelper.builder()
                                    .material(previousButton.getType())
                                    .displayName(ColorUtil.hexColor("&8Previous Page"))
                                    .build());
                }
            }

            ConfigurationSection nextSection = itemsSection.getConfigurationSection("next-page");
            if (nextSection != null) {
                ItemStack nextButton = ItemStackHelper.fromSection(nextSection);
                if ((currentPage * itemsPerPage) < totalItems) {
                    setItem(nextSection.getInt("slot"), nextButton,
                            e -> handleAction("next-page", (Player) e.getWhoClicked()));
                } else {
                    setItem(nextSection.getInt("slot"),
                            ItemStackHelper.builder()
                                    .material(nextButton.getType())
                                    .displayName(ColorUtil.hexColor("&8Next Page"))
                                    .build());
                }
            }
        }
    }

    private ItemStack createItemButton(Material material) {
        Configuration config = main.getConfigurationManager().getMenuConfiguration().getConfiguration();
        ConfigurationSection template = config.getConfigurationSection("item-select-menu.items.select-item-template");

        if (template != null) {
            String name = ColorUtil.hexColor(template.getString("name", "&f%item_name%")
                    .replace("%item_name%", formatMaterialName(material)));

            List<String> lore = template.getStringList("lore");

            List<ItemFlag> itemFlags = new ArrayList<>();
            for (String flagStr : template.getStringList("item-flags")) {
                try {
                    itemFlags.add(ItemFlag.valueOf(flagStr));
                } catch (IllegalArgumentException e) {
                }
            }
            return ItemStackHelper.builder()
                    .material(material)
                    .displayName(name)
                    .lore(lore)
                    .flags(itemFlags)
                    .build();
        }

        return ItemStackHelper.builder()
                .material(material)
                .displayName(ColorUtil.hexColor("&f" + formatMaterialName(material)))
                .build();
    }


    private String formatMaterialName(Material material) {
        return Arrays.stream(material.name().split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private boolean canBeEnchanted(Material material) {
        ItemStack item = new ItemStack(material);
        return Arrays.stream(Enchantment.values())
                .anyMatch(enchantment -> enchantment.canEnchantItem(item));
    }

    private void handleAction(String action, Player player) {
        switch (action) {
            case "search-item" -> {
                player.closeInventory();
                player.sendMessage(LanguageLoader.getMessage("enter-item"));
                main.getChatInputManager().setAwaitingInput(player, input -> {
                    searchQuery = input;
                    main.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                        updateItems();
                        this.open(player);
                    });
                });
            }
            case "back" -> parentMenu.open(player);
            case "next-page" -> {
                if ((currentPage * itemsPerPage) < getFilteredItems().size()) {
                    currentPage++;
                    updateItems();
                }
            }
            case "previous-page" -> {
                if (currentPage > 1) {
                    currentPage--;
                    updateItems();
                }
            }
        }
    }

    private List<Material> getFilteredItems() {
        if (searchQuery.isEmpty()) {
            return availableItems;
        }
        return availableItems.stream()
                .filter(m -> formatMaterialName(m).toLowerCase()
                        .contains(searchQuery.toLowerCase()))
                .collect(Collectors.toList());
    }


    private List<Integer> parseSlots(String slotsString) {
        List<Integer> slots = new ArrayList<>();
        if (slotsString == null) return slots;

        slotsString = slotsString.replace("[", "").replace("]", "");
        String[] parts = slotsString.split(",");

        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());
                for (int i = start; i <= end; i++) {
                    slots.add(i);
                }
            } else {
                slots.add(Integer.parseInt(part));
            }
        }
        return slots;
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getCursor().getType() != Material.AIR) {
            NSound.click((Player) event.getWhoClicked());
        }
    }

}
