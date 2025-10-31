package com.notpatch.nOrder.gui;

import com.notpatch.nOrder.LanguageLoader;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.Settings;
import com.notpatch.nOrder.model.Order;
import com.notpatch.nOrder.model.OrderStatus;
import com.notpatch.nOrder.util.PlayerUtil;
import com.notpatch.nlib.builder.ItemBuilder;
import com.notpatch.nlib.effect.NSound;
import com.notpatch.nlib.fastinv.FastInv;
import com.notpatch.nlib.util.ColorUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class NewOrderMenu extends FastInv implements Listener {
    
    private final NOrder plugin;
    private final Configuration config;
    
    @Getter @Setter
    private ItemStack selectedItem;
    
    @Getter @Setter
    private int quantity = 1;
    
    @Getter @Setter
    private double pricePerItem = 1.0;

    @Getter @Setter
    private boolean isHighlighted = false;
    
    private final Map<UUID, ChatInputState> playerInputStates = new HashMap<>();
    private BukkitTask inputTimeoutTask;
    
    private enum ChatInputState {
        NONE, WAITING_FOR_ITEM, WAITING_FOR_QUANTITY, WAITING_FOR_PRICE
    }
    
    public NewOrderMenu() {
        super(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration().getInt("new-order-menu.size"), 
              ColorUtil.hexColor(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration().getString("new-order-menu.title")));
        
        this.plugin = NOrder.getInstance();
        this.config = plugin.getConfigurationManager().getMenuConfiguration().getConfiguration();
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
        initializeMenu();
    }
    
    private void initializeMenu() {
        ConfigurationSection itemsSection = config.getConfigurationSection("new-order-menu.items");
        
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                
                if (itemSection != null) {
                    ItemStack item = ItemBuilder.getItemFromSection(itemSection);
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

    private String formatItemName(ItemStack item) {
        String name = item.getType().name();
        if (item.getEnchantments().isEmpty()) {
            return formatMaterialName(name);
        }
        return formatMaterialName(name) + " (Enchanted)";
    }

    private String formatMaterialName(String name) {
        return Arrays.stream(name.split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
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
                            ItemBuilder.builder()
                                    .material(Material.valueOf(selectItemSection.getString("material")))
                                    .displayName(ColorUtil.hexColor(selectItemSection.getString("name")))
                                    .lore(ColorUtil.getColoredList(selectItemSection.getStringList("lore")))
                                    .build()
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

            ItemStack quantityItem = ItemBuilder.builder().material(material)
                    .displayName(name)
                    .lore(lore)
                    .amount(Math.min(64, quantity))
                    .build().build();

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
                    ItemBuilder.builder().material(material)
                            .displayName(name)
                            .lore(lore)
                            .build().build(),
                    e -> handleMenuAction("set-price", e));
        }

        ConfigurationSection confirmSection = itemsSection.getConfigurationSection("confirm");
        if (confirmSection != null) {
            Material material = Material.valueOf(confirmSection.getString("material"));
            String name = ColorUtil.hexColor(confirmSection.getString("name"));
            double totalPrice = quantity * pricePerItem;
            if (isHighlighted) {
                if(Settings.HIGHLIGHT_FEE < 0){
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
                    ItemBuilder.builder().material(material)
                            .displayName(name)
                            .lore(lore)
                            .build().build(),
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
                        ItemBuilder.builder().material(material).glow(isHighlighted).lore(lore).displayName(name).build().build()
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
                requestChatInput(player, ChatInputState.WAITING_FOR_QUANTITY);
            }
            case "set-price" -> {
                player.closeInventory();
                requestChatInput(player, ChatInputState.WAITING_FOR_PRICE);
            }

            case "toggle-highlight" -> {
                if(!humanEntity.hasPermission(Settings.HIGHLIGHT_PERMISSION)){
                    player.sendMessage(LanguageLoader.getMessage("no-permission"));
                    NSound.error(player);
                    return;
                }
                setHighlighted(!isHighlighted());
                Bukkit.getScheduler().runTaskLater(plugin , () -> {
                    updateMenuItems();
                    this.open(player);
                }, 5L);
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

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime expireAt = now.plusDays(PlayerUtil.getPlayerOrderExpiration(player));
                String id = plugin.getOrderManager().createRandomId();
                Order order = new Order(id, player.getUniqueId(), player.getName(), selectedItem, quantity, pricePerItem, now, expireAt, isHighlighted);

                NOrder.getInstance().getOrderManager().addOrder(order);

                player.closeInventory();
            }
        }
    }


    private void requestChatInput(Player player, ChatInputState state) {
        playerInputStates.put(player.getUniqueId(), state);

        if (inputTimeoutTask != null) {
            inputTimeoutTask.cancel();
        }

        switch (state) {
            case WAITING_FOR_ITEM -> player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("enter-item")));
            case WAITING_FOR_QUANTITY -> player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("enter-quantity")));
            case WAITING_FOR_PRICE -> player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("enter-price")));
        }

        inputTimeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (playerInputStates.containsKey(player.getUniqueId())) {
                playerInputStates.remove(player.getUniqueId());
                player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("input-timeout")));
            }
        }, 30 * 20L);
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (!playerInputStates.containsKey(playerId)) {
            return;
        }
        
        event.setCancelled(true);
        String message = event.getMessage();
        ChatInputState state = playerInputStates.get(playerId);

        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (state) {
                case WAITING_FOR_ITEM -> processItemInput(player, message);
                case WAITING_FOR_QUANTITY -> processQuantityInput(player, message);
                case WAITING_FOR_PRICE -> processPriceInput(player, message);
            }
        });
    }

    private void processItemInput(Player player, String input) {
        try {
            Material material = Material.valueOf(input.toUpperCase());
            setSelectedItem(material);

            if (config.getConfigurationSection("new-order-menu.item-prices") != null &&
                    config.getConfigurationSection("new-order-menu.item-prices").contains(material.name())) {
                setPricePerItem(config.getDouble("new-order-menu.item-prices." + material.name()));
            }

            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("item-selected").replace("%item%", material.name())));
            playerInputStates.remove(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                updateMenuItems();
                this.open(player);
            }, 5L);

        } catch (IllegalArgumentException e) {
            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-item")));
            requestChatInput(player, ChatInputState.WAITING_FOR_ITEM);
        }
    }

    private void processQuantityInput(Player player, String input) {
        try {
            int amount = Integer.parseInt(input);
            if (amount <= 0) {
                player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-quantity")));
                requestChatInput(player, ChatInputState.WAITING_FOR_QUANTITY);
                return;
            }

            setQuantity(amount);
            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("quantity-set")
                    .replace("%quantity%", String.valueOf(amount))));
            playerInputStates.remove(player.getUniqueId());

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                updateMenuItems();
                this.open(player);
            }, 5L);

        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-quantity")));
            requestChatInput(player, ChatInputState.WAITING_FOR_QUANTITY);
        }
    }

    private void processPriceInput(Player player, String input) {
        try {
            double price = Double.parseDouble(input);
            if (price <= 0) {
                player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-price")));
                requestChatInput(player, ChatInputState.WAITING_FOR_PRICE);
                return;
            }

            setPricePerItem(price);
            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("price-set")
                    .replace("%price%", String.valueOf(price))));
            playerInputStates.remove(player.getUniqueId());

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                updateMenuItems();
                this.open(player);
            }, 5L);

        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-price")));
            requestChatInput(player, ChatInputState.WAITING_FOR_PRICE);
        }
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        super.onClose(event);
        HumanEntity entity = event.getPlayer();
        Player player = (Player) entity;
        playerInputStates.remove(player.getUniqueId());
        if(inputTimeoutTask != null) {
            inputTimeoutTask.cancel();
            inputTimeoutTask = null;
        }
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        NSound.click(player);
    }

}