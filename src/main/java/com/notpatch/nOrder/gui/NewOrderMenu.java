package com.notpatch.nOrder.gui;

import com.notpatch.nOrder.LanguageLoader;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.Settings;
import com.notpatch.nOrder.model.Order;
import com.notpatch.nOrder.util.ItemStackHelper;
import com.notpatch.nOrder.util.PlayerUtil;
import com.notpatch.nlib.effect.NSound;
import com.notpatch.nlib.fastinv.FastInv;
import com.notpatch.nlib.util.ColorUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NewOrderMenu extends FastInv implements Listener {

    private final NOrder main;
    private final Configuration config;

    @Getter
    private ItemStack selectedItem;

    @Getter
    @Setter
    private int quantity = 1;

    @Getter
    @Setter
    private double pricePerItem = 1.0;

    @Getter
    @Setter
    private boolean isHighlighted = false;

    public NewOrderMenu() {
        super(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration().getInt("new-order-menu.size"),
                ColorUtil.hexColor(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration().getString("new-order-menu.title")));

        this.main = NOrder.getInstance();
        this.config = main.getConfigurationManager().getMenuConfiguration().getConfiguration();
        initializeMenu();
    }

    private void initializeMenu() {
        ConfigurationSection itemsSection = config.getConfigurationSection("new-order-menu.items");

        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);

                if (itemSection != null) {
                    ItemStack item = ItemStackHelper.fromSection(itemSection);
                    String action = itemSection.getString("action");

                    if (itemSection.contains("slot")) {
                        int slot = itemSection.getInt("slot");
                        setItem(slot, item, e -> {
                            if (action != null) {
                                handleMenuAction(action, e);
                            }
                        });
                    } else if (itemSection.contains("slots")) {
                        List<Integer> slots = parseSlots(itemSection.getString("slots"));
                        for (int slot : slots) {
                            setItem(slot, item, e -> {
                                if (action != null) {
                                    handleMenuAction(action, e);
                                }
                            });
                        }
                    }
                }
            }
        }

        updateMenuItems();
    }

    public void setSelectedItem(ItemStack item) {
        this.selectedItem = item;
        updateMenuItems();
    }

    public void setSelectedItem(Material material) {
        this.selectedItem = new ItemStack(material);
        updateMenuItems();
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

    public void updateMenuItems() {
        ConfigurationSection itemsSection = config.getConfigurationSection("new-order-menu.items");

        if (itemsSection != null) {
            ConfigurationSection selectItemSection = itemsSection.getConfigurationSection("select-item");
            if (selectItemSection != null) {
                int slot = selectItemSection.getInt("slot");
                if (selectedItem != null) {
                    ItemStack displayItem = selectedItem.clone();
                    ItemMeta meta = displayItem.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ColorUtil.hexColor(selectItemSection.getString("name").replace("%item%", selectedItem.getType().name())));
                        displayItem.setItemMeta(meta);
                    }
                    setItem(slot, displayItem, e -> handleMenuAction("select-item", e));
                } else {
                    setItem(slot,
                            ItemStackHelper.builder()
                                    .material(Material.valueOf(selectItemSection.getString("material")))
                                    .displayName(ColorUtil.hexColor(selectItemSection.getString("name")))
                                    .lore(ColorUtil.getColoredList(selectItemSection.getStringList("lore")))
                                    .build(),
                            e -> handleMenuAction("select-item", e));
                }
            }
        }

        ConfigurationSection quantitySection = itemsSection.getConfigurationSection("set-quantity");
        if (quantitySection != null) {
            Material material = Material.valueOf(quantitySection.getString("material"));
            String name = ColorUtil.hexColor(quantitySection.getString("name"));
            List<String> lore = quantitySection.getStringList("lore").stream()
                    .map(line -> line
                            .replace("%quantity%", String.valueOf(quantity))
                            .replace("%total_price%", String.format("%.2f", quantity * pricePerItem)))
                    .map(ColorUtil::hexColor)
                    .collect(Collectors.toList());

            ItemStack quantityItem = ItemStackHelper.builder().material(material)
                    .displayName(name)
                    .lore(lore)
                    .amount(Math.min(64, quantity))
                    .build();

            setItem(quantitySection.getInt("slot"), quantityItem,
                    e -> handleMenuAction("set-quantity", e));
        }

        ConfigurationSection priceSection = itemsSection.getConfigurationSection("set-price");
        if (priceSection != null) {
            Material material = Material.valueOf(priceSection.getString("material"));
            String name = ColorUtil.hexColor(priceSection.getString("name"));
            List<String> lore = priceSection.getStringList("lore").stream()
                    .map(line -> line
                            .replace("%price%", String.format("%.2f", pricePerItem))
                            .replace("%total_price%", String.format("%.2f", quantity * pricePerItem)))
                    .map(ColorUtil::hexColor)
                    .collect(Collectors.toList());

            setItem(priceSection.getInt("slot"),
                    ItemStackHelper.builder().material(material)
                            .displayName(name)
                            .lore(lore)
                            .build(),
                    e -> handleMenuAction("set-price", e));
        }

        ConfigurationSection confirmSection = itemsSection.getConfigurationSection("confirm");
        if (confirmSection != null) {
            Material material = Material.valueOf(confirmSection.getString("material"));
            String name = ColorUtil.hexColor(confirmSection.getString("name"));
            double totalPrice = quantity * pricePerItem;
            if (isHighlighted) {
                if (Settings.HIGHLIGHT_FEE < 0) {
                    return;
                }
                double feePercentage = Settings.HIGHLIGHT_FEE;
                totalPrice += totalPrice * feePercentage / 100;
            }
            double finalTotalPrice = totalPrice;
            List<String> lore = confirmSection.getStringList("lore").stream()
                    .map(line -> line
                            .replace("%item%", selectedItem != null ? selectedItem.getType().name() : "None")
                            .replace("%quantity%", String.valueOf(quantity))
                            .replace("%price%", String.format("%.2f", pricePerItem))
                            .replace("%total_price%", String.format("%.2f", finalTotalPrice)))
                    .map(ColorUtil::hexColor)
                    .collect(Collectors.toList());

            setItem(confirmSection.getInt("slot"),
                    ItemStackHelper.builder().material(material)
                            .displayName(name)
                            .lore(lore)
                            .build(),
                    e -> handleMenuAction("confirm-order", e));
        }

        ConfigurationSection highlightSection = itemsSection.getConfigurationSection("highlight");
        if (highlightSection != null) {
            Material material = Material.valueOf(highlightSection.getString("material"));
            String name = ColorUtil.hexColor(highlightSection.getString("name"));
            List<String> lore = highlightSection.getStringList("lore").stream()
                    .map(line -> line
                            .replace("%status%", isHighlighted ? LanguageLoader.getMessage("enabled") : LanguageLoader.getMessage("disabled"))
                            .replace("%fee%", String.format("%.2f", Settings.HIGHLIGHT_FEE)))
                    .map(ColorUtil::hexColor)
                    .collect(Collectors.toList());
            setItem(highlightSection.getInt("slot"),
                    ItemStackHelper.builder().material(material).glow(isHighlighted).lore(lore).displayName(name).build()
                    , e -> handleMenuAction("toggle-highlight", e));
        }
    }


    private void handleMenuAction(String action, InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player player)) return;

        switch (action) {
            case "select-item" -> {
                new ItemSelectMenu(this).open(player);
            }
            case "set-quantity" -> {
                player.closeInventory();
                player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("enter-quantity")));
                NOrder.getInstance().getChatInputManager().setAwaitingInput((Player) player, value -> {
                    processQuantityInput(player, value);
                });

            }
            case "set-price" -> {
                player.closeInventory();
                player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("enter-price")));
                NOrder.getInstance().getChatInputManager().setAwaitingInput((Player) player, value -> {
                    processPriceInput(player, value);
                });
            }

            case "toggle-highlight" -> {
                if (!humanEntity.hasPermission(Settings.HIGHLIGHT_PERMISSION)) {
                    player.sendMessage(LanguageLoader.getMessage("no-permission"));
                    NSound.error(player);
                    return;
                }
                setHighlighted(!isHighlighted());
                main.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                    updateMenuItems();
                    this.open(player);
                });

            }

            case "confirm-order" -> {
                if (selectedItem == null) {
                    player.sendMessage(LanguageLoader.getMessage("no-item-selected"));
                    NSound.error(player);
                    return;
                }

                if (quantity <= 0) {
                    player.sendMessage(LanguageLoader.getMessage("invalid-quantity"));
                    NSound.error(player);
                    return;
                }

                if (pricePerItem <= 0) {
                    player.sendMessage(LanguageLoader.getMessage("invalid-price"));
                    NSound.error(player);
                    return;
                }
                
                // Validate price per item against configured limits
                double minPrice = Settings.getMinPricePerItem(selectedItem.getType());
                double maxPrice = Settings.getMaxPricePerItem(selectedItem.getType());
                
                if (pricePerItem < minPrice) {
                    player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("price-too-low")
                            .replace("%min_price%", String.format("%.2f", minPrice))));
                    NSound.error(player);
                    return;
                }
                
                if (pricePerItem > maxPrice) {
                    player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("price-too-high")
                            .replace("%max_price%", String.format("%.2f", maxPrice))));
                    NSound.error(player);
                    return;
                }

                double totalPrice = quantity * pricePerItem;
                if (isHighlighted && Settings.HIGHLIGHT_FEE > 0) {
                    totalPrice += totalPrice * Settings.HIGHLIGHT_FEE / 100;
                }

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime expireAt = now.plusDays(PlayerUtil.getPlayerOrderExpiration(player));
                String id = main.getOrderManager().createRandomId();

                // Check if item is custom ItemsAdder item
                String customItemId = null;
                if (main.getItemsAdderHook() != null && main.getItemsAdderHook().isAvailable()) {
                    customItemId = main.getItemsAdderHook().getCustomItemId(selectedItem);
                }

                Order order = new Order(id, player.getUniqueId(), player.getName(), selectedItem, customItemId, quantity, pricePerItem, now, expireAt, isHighlighted);

                NOrder.getInstance().getOrderManager().addOrder(order);

                player.closeInventory();
            }
        }
    }

    private void processQuantityInput(Player player, String input) {
        try {
            int amount = Integer.parseInt(input);
            if (amount <= 0) {
                player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-quantity")));
                NOrder.getInstance().getChatInputManager().setAwaitingInput((Player) player, value -> {
                    processQuantityInput(player, value);
                });
                return;
            }

            setQuantity(amount);
            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("quantity-set")
                    .replace("%quantity%", String.valueOf(amount))));

            main.getMorePaperLib().scheduling().globalRegionalScheduler().runDelayed(() -> {
                updateMenuItems();
                this.open(player);
            }, 5L);

        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-quantity")));
        }
    }

    private void processPriceInput(Player player, String input) {
        try {
            double price = Double.parseDouble(input);
            
            // Get min and max price for the selected item
            double minPrice = selectedItem != null ? Settings.getMinPricePerItem(selectedItem.getType()) : Settings.MIN_PRICE_PER_ITEM;
            double maxPrice = selectedItem != null ? Settings.getMaxPricePerItem(selectedItem.getType()) : Settings.MAX_PRICE_PER_ITEM;
            
            if (price < minPrice) {
                player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("price-too-low")
                        .replace("%min_price%", String.format("%.2f", minPrice))));
                NOrder.getInstance().getChatInputManager().setAwaitingInput((Player) player, value -> {
                    processPriceInput(player, value);
                });
                return;
            }
            
            if (price > maxPrice) {
                player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("price-too-high")
                        .replace("%max_price%", String.format("%.2f", maxPrice))));
                NOrder.getInstance().getChatInputManager().setAwaitingInput((Player) player, value -> {
                    processPriceInput(player, value);
                });
                return;
            }

            setPricePerItem(price);
            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("price-set")
                    .replace("%price%", String.valueOf(price))));

            main.getMorePaperLib().scheduling().globalRegionalScheduler().runDelayed(() -> {
                updateMenuItems();
                this.open(player);
            }, 5L);

        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-price")));
        }
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        super.onClose(event);
        HumanEntity entity = event.getPlayer();
        Player player = (Player) entity;
        NOrder.getInstance().getChatInputManager().cancelInput(player);
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getCursor().getType() != Material.AIR) {
            NSound.click((Player) event.getWhoClicked());
        }
    }

}