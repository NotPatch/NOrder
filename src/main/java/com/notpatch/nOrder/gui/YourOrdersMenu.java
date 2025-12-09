package com.notpatch.nOrder.gui;

import com.notpatch.nOrder.LanguageLoader;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.model.Order;
import com.notpatch.nOrder.model.OrderStatus;
import com.notpatch.nOrder.util.StringUtil;
import com.notpatch.nlib.builder.ItemBuilder;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class YourOrdersMenu extends FastInv {

    private final NOrder main;

    @Getter
    @Setter
    private int currentPage = 1;
    private final List<Integer> orderSlots;
    private final int itemsPerPage;
    private final Player player;

    public YourOrdersMenu(Player player) {
        this(player, 1);
    }

    public YourOrdersMenu(Player player, int page) {
        super(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration().getInt("your-orders-menu.size"),
                ColorUtil.hexColor(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration().getString("your-orders-menu.title")));

        this.player = player;
        main = NOrder.getInstance();
        Configuration configuration = main.getConfigurationManager().getMenuConfiguration().getConfiguration();

        this.currentPage = page;
        this.itemsPerPage = configuration.getInt("your-orders-menu.pagination.items-per-page", 21);

        List<String> slotsStrList = configuration.getStringList("your-orders-menu.order-slots");
        this.orderSlots = new ArrayList<>();
        for (String slotStr : slotsStrList) {
            try {
                this.orderSlots.add(Integer.parseInt(slotStr));
            } catch (NumberFormatException ignored) {
            }
        }

        loadMenuItems(configuration);
        loadPlayerOrders();
    }

    private void loadMenuItems(Configuration configuration) {
        ConfigurationSection itemsSection = configuration.getConfigurationSection("your-orders-menu.items");

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

    private void loadPlayerOrders() {
        List<Order> playerOrders = main.getOrderManager().getPlayerOrdersIncludingCompleted(player.getName());

        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, playerOrders.size());

        if (startIndex >= playerOrders.size() && !playerOrders.isEmpty()) {
            currentPage = 1;
            startIndex = 0;
            endIndex = Math.min(itemsPerPage, playerOrders.size());
        }

        List<Order> pageOrders = (startIndex < playerOrders.size()) ?
                playerOrders.subList(startIndex, endIndex) : new ArrayList<>();

        Configuration config = main.getConfigurationManager().getMenuConfiguration().getConfiguration();
        ConfigurationSection template = config.getConfigurationSection("your-orders-menu.order-item-template");

        if (template != null) {
            for (int i = 0; i < pageOrders.size(); i++) {
                if (i >= orderSlots.size()) break;

                Order order = pageOrders.get(i);
                int slot = orderSlots.get(i);

                ItemStack orderItem = createOrderItem(order, template);

                setItem(slot, orderItem, e -> {
                    handleOrderClick(order, e.getWhoClicked(), e);
                });
            }
        }

        updatePaginationButtons(playerOrders.size());
    }

    private ItemStack createOrderItem(Order order, ConfigurationSection template) {
        String materialStr = template.getString("material", "PAPER");
        Material material = Material.valueOf(materialStr.replace("%material%", order.getMaterial().name()));

        String nameTemplate = template.getString("name", "&f&lSipariş");
        String name = StringUtil.replaceOrderPlaceholders(nameTemplate, order);

        List<String> loreTemplate = template.getStringList("lore");
        List<String> lore = new ArrayList<>();

        for (String line : loreTemplate) {
            lore.add(StringUtil.replaceOrderPlaceholders(line, order));
        }

        return ItemBuilder.builder()
                .material(material)
                .displayName(ColorUtil.hexColor(name))
                .lore(lore.stream().map(ColorUtil::hexColor).toList())
                .build().build();
    }


    private void handleOrderClick(Order order, HumanEntity player, InventoryClickEvent event) {
        if (order.getStatus() == OrderStatus.ARCHIVED)
            return;
        player.closeInventory();
        if (event.getClick() == ClickType.SHIFT_RIGHT) {
            player.sendMessage(LanguageLoader.getMessage("enter-confirm"));
            main.getChatInputManager().setAwaitingInput((Player) player, value -> {
                processRemoveOrder(((Player) player).getPlayer(), value, order);
            });
            return;
        }
        main.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
            new OrderTakeMenu(order).open((Player) player);
        });
    }

    private void processRemoveOrder(Player player, String input, Order order) {
        if (input.equalsIgnoreCase("confirm")) {
            main.getOrderManager().cancelOrder(order);
        } else {
            main.getChatInputManager().cancelInput(player);
        }
    }

    private void updatePaginationButtons(int totalOrders) {
        int totalPages = (int) Math.ceil((double) totalOrders / itemsPerPage);

        ItemStack nextButton = getInventory().getItem(53);
        ItemStack prevButton = getInventory().getItem(45);

        if (nextButton != null) {
            if (currentPage < totalPages) {
                setItem(53, nextButton, e -> {
                    e.getWhoClicked().closeInventory();
                    new YourOrdersMenu((Player) e.getWhoClicked(), currentPage + 1).open((Player) e.getWhoClicked());
                });
            } else {
                setItem(53, nextButton);
            }
        }

        if (prevButton != null) {
            if (currentPage > 1) {
                setItem(45, prevButton, e -> {
                    e.getWhoClicked().closeInventory();
                    new YourOrdersMenu((Player) e.getWhoClicked(), currentPage - 1).open((Player) e.getWhoClicked());
                });
            } else {
                setItem(45, prevButton);
            }
        }
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
            case "back":
                player.closeInventory();
                main.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                    new MainOrderMenu().open((Player) player);
                });
                break;
            case "next-page":
                if (currentPage < Math.ceil((double) main.getOrderManager().getPlayerOrders(player.getName()).size() / itemsPerPage)) {
                    player.closeInventory();
                    new YourOrdersMenu((Player) player, currentPage + 1).open((Player) player);
                }
                break;
            case "previous-page":
                if (currentPage > 1) {
                    player.closeInventory();
                    new YourOrdersMenu((Player) player, currentPage - 1).open((Player) player);
                }
                break;
        }
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getCursor().getType() != Material.AIR) {
            NSound.click((Player) event.getWhoClicked());
        }
    }

}
