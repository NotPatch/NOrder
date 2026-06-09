package com.notpatch.nOrder.gui;

import com.google.common.collect.ArrayListMultimap;
import com.notpatch.nOrder.LanguageLoader;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.Settings;
import com.notpatch.nOrder.model.Order;
import com.notpatch.nOrder.model.OrderSortType;
import com.notpatch.nOrder.util.ItemStackHelper;
import com.notpatch.nOrder.util.StringUtil;
import com.notpatch.nlib.effect.NSound;
import com.notpatch.nlib.fastinv.FastInv;
import com.notpatch.nlib.util.ColorUtil;
import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class MainOrderMenu extends FastInv {

    private final NOrder main;

    @Getter
    @Setter
    private int currentPage = 1;
    private final List<Integer> orderSlots;
    private final int itemsPerPage;
    private List<Order> filteredOrders;
    private Player player;
    private final OrderSortType sortType;
    private final boolean backOnClose;
    private final String parentMenuId;
    private boolean closedByAction = false;

    public MainOrderMenu() {
        this(1, NOrder.getInstance().getOrderManager().getAllOrders(), null, null, null, OrderSortType.HIGHLIGHTED);
    }

    public MainOrderMenu(Player player) {
        this(1, NOrder.getInstance().getOrderManager().getAllOrders(), null, null, player, OrderSortType.HIGHLIGHTED);
    }

    public MainOrderMenu(List<Order> orders) {
        this(1, orders, null, null, null, OrderSortType.HIGHLIGHTED);
    }

    public MainOrderMenu(int page, List<Order> orders) {
        this(page, orders, null, null, null, OrderSortType.HIGHLIGHTED);
    }

    public MainOrderMenu(int page, List<Order> orders, String filterType, String filterValue, Player player) {
        this(page, orders, filterType, filterValue, player, OrderSortType.HIGHLIGHTED);
    }

    public MainOrderMenu(int page, List<Order> orders, String filterType, String filterValue, Player player, OrderSortType sortType) {
        super(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration().getInt("main-order-menu.size"),
                ColorUtil.hexColor(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration().getString("main-order-menu.title")));

        main = NOrder.getInstance();
        Configuration configuration = main.getConfigurationManager().getMenuConfiguration().getConfiguration();

        this.currentPage = page;
        this.sortType = sortType;
        this.itemsPerPage = configuration.getInt("main-order-menu.pagination.items-per-page", 21);
        this.backOnClose = configuration.getBoolean("main-order-menu.back-on-close", false);
        this.parentMenuId = configuration.getString("main-order-menu.parent-menu-id", null);
        this.player = player;

        if (filterType != null && filterValue != null) {
            this.filteredOrders = filterOrders(orders, filterType, filterValue);
        } else {
            this.filteredOrders = new ArrayList<>(orders);
        }

        this.filteredOrders = sortOrders(this.filteredOrders);

        List<String> slotsStrList = configuration.getStringList("main-order-menu.order-slots");
        if (slotsStrList.isEmpty()) {
            String slotsStr = configuration.getString("main-order-menu.order-slots");
            this.orderSlots = parseSlots(slotsStr);
        } else {
            this.orderSlots = new ArrayList<>();
            for (String slotStr : slotsStrList) {
                try {
                    this.orderSlots.add(Integer.parseInt(slotStr));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        loadMenuItems(configuration);
        loadSortButton(configuration);
        loadOrderItems(this.filteredOrders);
    }

    private List<Order> filterOrders(List<Order> orders, String filterType, String filterValue) {
        return switch (filterType.toLowerCase()) {
            case "item" -> orders.stream()
                    .filter(order -> order.getMaterial().name().toLowerCase()
                            .contains(filterValue.toLowerCase()))
                    .toList();
            case "player" -> orders.stream()
                    .filter(order -> {
                        String playerName = order.getPlayerName();
                        return playerName != null && playerName.toLowerCase()
                                .contains(filterValue.toLowerCase());
                    })
                    .toList();
            default -> orders;
        };
    }

    private List<Order> sortOrders(List<Order> orders) {
        return switch (sortType) {
            case NAME -> orders.stream()
                    .sorted(Comparator.comparing(o -> o.getMaterial().name()))
                    .collect(Collectors.toList());
            case PRICE_ASC -> orders.stream()
                    .sorted(Comparator.comparingDouble(o -> o.getPrice() * o.getAmount()))
                    .collect(Collectors.toList());
            case PRICE_DESC -> orders.stream()
                    .sorted(Comparator.comparingDouble((Order o) -> o.getPrice() * o.getAmount()).reversed())
                    .collect(Collectors.toList());
            case DATE -> orders.stream()
                    .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                    .collect(Collectors.toList());
            case HIGHLIGHTED -> orders.stream()
                    .sorted(Comparator.comparing(Order::isHighlight).reversed())
                    .collect(Collectors.toList());
            case CUSTOM_ITEM -> orders.stream()
                    .filter(Order::isCustomItem)
                    .collect(Collectors.toList());
        };
    }

    private void loadSortButton(Configuration configuration) {
        ConfigurationSection sortSection = configuration.getConfigurationSection("main-order-menu.items.sort");
        if (sortSection == null) return;

        int slot = sortSection.getInt("slot");
        Material material;
        try {
            material = Material.valueOf(sortSection.getString("material", "HOPPER"));
        } catch (IllegalArgumentException e) {
            material = Material.HOPPER;
        }

        String sortName = LanguageLoader.getMessage("sort-type-" + sortType.name().toLowerCase().replace("_", "-"));
        String itemName = ColorUtil.hexColor(sortSection.getString("name", "&f&lSort").replace("%sort_type%", sortName));
        List<String> lore = sortSection.getStringList("lore").stream()
                .map(line -> ColorUtil.hexColor(line.replace("%sort_type%", sortName)))
                .collect(Collectors.toList());

        ItemStack sortItem = ItemStackHelper.builder()
                .material(material)
                .displayName(itemName)
                .lore(lore)
                .build();

        setItem(slot, sortItem, e -> handleMenuAction("sort-orders", e.getWhoClicked()));
    }

    private void loadMenuItems(Configuration configuration) {
        ConfigurationSection itemsSection = configuration.getConfigurationSection("main-order-menu.items");

        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);

                if (itemSection != null) {
                    ItemStack item = ItemStackHelper.fromSection(itemSection);
                    String action = itemSection.getString("action");

                    if (item.hasItemMeta()) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta == null) continue;

                        boolean hasPapi = player != null &&
                                main.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;

                        String name = meta.getDisplayName();
                        if (name != null && !name.isEmpty()) {
                            String processedName = hasPapi ? PlaceholderAPI.setPlaceholders(player, name) : name;
                            meta.setDisplayName(processedName);
                            item.setItemMeta(meta);
                        }

                        List<String> lore = meta.getLore();
                        if (lore != null) {
                            List<String> processedLore = lore.stream()
                                    .map(line -> hasPapi ? PlaceholderAPI.setPlaceholders(player, line) : line)
                                    .collect(Collectors.toList());

                            meta.setLore(processedLore);
                            item.setItemMeta(meta);
                        }
                    }


                    if (itemSection.contains("slot")) {
                        int slot = itemSection.getInt("slot");
                        setItem(slot, item, e -> {
                            if (action != null) {
                                handleMenuAction(action, e.getWhoClicked());
                            }
                        });
                    } else if (itemSection.contains("slots")) {
                        List<Integer> slots = parseSlots(itemSection.getString("slots"));
                        for (int slot : slots) {
                            setItem(slot, item, e -> {
                                if (action != null) {
                                    handleMenuAction(action, e.getWhoClicked());
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    private void loadOrderItems(List<Order> orders) {
        List<Order> allOrders = orders;

        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allOrders.size());

        if (startIndex >= allOrders.size() && !allOrders.isEmpty()) {
            currentPage = 1;
            startIndex = 0;
            endIndex = Math.min(itemsPerPage, allOrders.size());
        }

        List<Order> pageOrders = (startIndex < allOrders.size()) ?
                allOrders.subList(startIndex, endIndex) : new ArrayList<>();

        Configuration config = main.getConfigurationManager().getMenuConfiguration().getConfiguration();
        ConfigurationSection template = config.getConfigurationSection("main-order-menu.order-item-template");

        if (template != null) {
            for (int i = 0; i < pageOrders.size(); i++) {
                if (i >= orderSlots.size()) break;

                Order order = pageOrders.get(i);
                int slot = orderSlots.get(i);

                ItemStack orderItem = createOrderItem(order, template);

                setItem(slot, orderItem, e -> {
                    ;
                    handleOrderClick(order, e.getWhoClicked());
                });
            }
        }

    }

    private ItemStack createOrderItem(Order order, ConfigurationSection template) {
        String nameTemplate = template.getString("name", "&f&lSipariş");
        String name = StringUtil.replaceOrderPlaceholders(nameTemplate, order);

        List<String> loreTemplate = template.getStringList("lore");
        List<String> lore = new ArrayList<>();
        for (String line : loreTemplate) {
            lore.add(StringUtil.replaceOrderPlaceholders(line, order));
        }

        List<String> flags = template.getStringList("item-flags");
        List<ItemFlag> itemFlags = new ArrayList<>();
        for (String flag : flags) {
            try {
                String formattedFlag = flag.toUpperCase().replace("-", "_");
                ItemFlag itemFlag = ItemFlag.valueOf(formattedFlag);
                itemFlags.add(itemFlag);
            } catch (IllegalArgumentException e) {
            }
        }

        ItemStack item;
        Map<Enchantment, Integer> enchantments = new HashMap<>();

        if (order.isCustomItem()) {
            item = Settings.getCustomItemFromCache(order.getCustomItemId());

            if (item == null) {
                item = order.getItem().clone();
            }

            item.setAmount(1);

            ItemMeta originalMeta = item.getItemMeta();
            if (originalMeta != null) {
                enchantments = originalMeta.getEnchants();
            }
        } else {
            String materialStr = template.getString("material", "PAPER");
            if (materialStr.equals("%material%")) {
                materialStr = order.getMaterial().name();
            }

            Material material;
            try {
                material = Material.valueOf(materialStr);
            } catch (IllegalArgumentException e) {
                material = Material.PAPER;
            }

            item = new ItemStack(material, 1);

            ItemStack orderItem = order.getItem();
            if (orderItem != null) {
                ItemMeta meta = orderItem.getItemMeta();
                if (meta != null) {
                    if (orderItem.getType() == Material.ENCHANTED_BOOK && meta instanceof EnchantmentStorageMeta storageMeta) {
                        enchantments = storageMeta.getStoredEnchants();
                    } else {
                        enchantments = meta.getEnchants();
                    }
                }
            }
        }

        Map<Enchantment, Integer> finalEnchantments = enchantments;
        item.editMeta(meta -> {
            meta.setDisplayName(ColorUtil.hexColor(name));
            meta.setLore(lore.stream().map(ColorUtil::hexColor).toList());

            if (order.isHighlight()) {
                meta.addEnchant(Enchantment.FLAME, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            if (!finalEnchantments.isEmpty()) {
                for (Map.Entry<Enchantment, Integer> entry : finalEnchantments.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }

            for (ItemFlag flag : itemFlags) {
                meta.addItemFlags(flag);
            }

            if (itemFlags.contains(ItemFlag.HIDE_ATTRIBUTES)) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                meta.setAttributeModifiers(ArrayListMultimap.create());
            }

            try {
                meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
            } catch (IllegalArgumentException ignored) {
            }
        });

        return item;
    }


    private void handleOrderClick(Order order, HumanEntity hPlayer) {
        closedByAction = true;
        player.closeInventory();
        if (order.isOwner(player)) {
            main.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                new OrderTakeMenu(order).open(player);
            });

            return;
        }
        main.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
            new OrderDetailsMenu(order).open(player);
        });
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

    private void handleMenuAction(String action, HumanEntity player) {
        switch (action) {
            case "new-order" -> {
                closedByAction = true;
                player.closeInventory();
                main.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                    main.getNewOrderMenuManager().getOrCreateMenu((Player) player).open((Player) player);
                });
            }
            case "search-order" -> {
                closedByAction = true;
                player.closeInventory();
                player.sendMessage(LanguageLoader.getMessage("enter-item"));
                main.getChatInputManager().setAwaitingInput((Player) player, searchValue -> {
                    main.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                        new MainOrderMenu(1, main.getOrderManager().getAllOrders(),
                                "item", searchValue, (Player) player, sortType).open((Player) player);
                    });
                });
            }
            case "your-orders" -> {
                closedByAction = true;
                player.closeInventory();
                main.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                    new YourOrdersMenu((Player) player).open((Player) player);
                });
            }
            case "next-page" -> {
                if (currentPage < Math.ceil((double) filteredOrders.size() / itemsPerPage)) {
                    closedByAction = true;
                    player.closeInventory();
                    main.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                        new MainOrderMenu(currentPage + 1, filteredOrders, null, null, (Player) player, sortType).open((Player) player);
                    });
                }
            }
            case "previous-page" -> {
                if (currentPage > 1) {
                    closedByAction = true;
                    player.closeInventory();
                    main.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                        new MainOrderMenu(currentPage - 1, filteredOrders, null, null, (Player) player, sortType).open((Player) player);
                    });
                }
            }
            case "sort-orders" -> {
                closedByAction = true;
                player.closeInventory();
                OrderSortType nextSort = sortType.next();
                List<Order> baseOrders = main.getOrderManager().getAllOrders();
                main.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                    new MainOrderMenu(1, baseOrders, null, null, (Player) player, nextSort).open((Player) player);
                });
            }
            default -> {
                closedByAction = true;
                player.closeInventory();
                main.getDynamicMenuManager().openMenuById((Player) player, action);
            }
        }
    }

    @Override
    protected void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        super.onClose(event);
        if (closedByAction) return;
        if (!backOnClose) return;
        if (parentMenuId == null || parentMenuId.isEmpty()) return;

        main.getDynamicMenuManager().openMenuById(player, parentMenuId);
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getCursor().getType() != Material.AIR) {
            NSound.click((Player) event.getWhoClicked());
        }
    }
}