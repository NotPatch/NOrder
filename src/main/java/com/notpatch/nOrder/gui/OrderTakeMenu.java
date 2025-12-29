package com.notpatch.nOrder.gui;

import com.notpatch.nOrder.LanguageLoader;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.model.Order;
import com.notpatch.nOrder.model.OrderStatus;
import com.notpatch.nOrder.util.ItemStackHelper;
import com.notpatch.nlib.effect.NSound;
import com.notpatch.nlib.fastinv.FastInv;
import com.notpatch.nlib.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderTakeMenu extends FastInv {

    private final NOrder main;

    private final Order order;
    private final Map<Integer, ItemStack> deliveredItems;

    public OrderTakeMenu(Order order) {
        super(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration()
                        .getInt("order-take-menu.size"),
                ColorUtil.hexColor(NOrder.getInstance().getConfigurationManager().getMenuConfiguration()
                        .getConfiguration().getString("order-take-menu.title")));
        main = NOrder.getInstance();
        this.order = order;
        this.deliveredItems = new HashMap<>();

        int remainingItems = order.getDelivered() - order.getCollected();
        int stackIndex = 0;

        while (remainingItems > 0) {
            ItemStack item = order.getItem();
            int stackSize = Math.min(64, remainingItems);
            item.setAmount(stackSize);
            this.deliveredItems.put(stackIndex++, item);
            remainingItems -= stackSize;
        }

        loadMenuItems();
        loadDeliveredItems();
    }

    private void loadMenuItems() {
        Configuration config = main.getConfigurationManager()
                .getMenuConfiguration().getConfiguration();

        ConfigurationSection fillerSection = config.getConfigurationSection("order-take-menu.items.filler");
        if (fillerSection != null) {
            ItemStack fillerItem = ItemStackHelper.fromSection(fillerSection);
            setItems(config.getIntegerList("order-take-menu.items.filler.slots"), fillerItem);
        }

        ConfigurationSection backSection = config.getConfigurationSection("order-take-menu.items.back");
        if (backSection != null) {
            ItemStack backItem = ItemStackHelper.fromSection(backSection);
            setItem(backSection.getInt("slot"), backItem, e -> {
                e.getWhoClicked().closeInventory();
                new YourOrdersMenu((Player) e.getWhoClicked()).open((Player) e.getWhoClicked());
            });
        }

        ConfigurationSection takeAllSection = config.getConfigurationSection("order-take-menu.items.take-all");
        if (takeAllSection != null) {
            ItemStack takeAllItem = ItemStackHelper.fromSection(takeAllSection);
            setItem(takeAllSection.getInt("slot"), takeAllItem, e -> handleTakeAll((Player) e.getWhoClicked()));
        }

        ConfigurationSection infoSection = config.getConfigurationSection("order-take-menu.items.info");
        if (infoSection != null) {
            ItemStack infoItem = ItemStackHelper.fromSection(infoSection);
            setItem(infoSection.getInt("slot"), infoItem);
        }
    }

    private void loadDeliveredItems() {
        List<Integer> slots = main.getConfigurationManager()
                .getMenuConfiguration().getConfiguration()
                .getIntegerList("order-take-menu.delivery-slots");

        int slotIndex = 0;
        for (ItemStack item : deliveredItems.values()) {
            if (slotIndex >= slots.size()) break;
            if (item.getAmount() > 0) {
                setItem(slots.get(slotIndex), item, this::handleItemClick);
                slotIndex++;
            }
        }
    }

    private void handleItemClick(InventoryClickEvent e) {
        e.setCancelled(true);
        HumanEntity player = e.getWhoClicked();
        ItemStack clickedItem = e.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR || clickedItem.getType() != order.getMaterial())
            return;

        if (e.getClick() == ClickType.DROP) {
            player.getWorld().dropItemNaturally(player.getLocation(), clickedItem);
            e.setCurrentItem(null);
        } else if (e.isLeftClick()) {
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(clickedItem);
                e.setCurrentItem(null);
            } else {
                player.sendMessage(LanguageLoader.getMessage("inventory-full"));
                return;
            }
        } else {
            return;
        }

        order.addCollected(clickedItem.getAmount());
        main.getOrderLogger().logItemCollection(order, clickedItem.getAmount());

        checkAndArchiveOrder(player);

        ((Player) player).updateInventory();
    }

    private void handleTakeAll(Player player) {
        List<Integer> slots = main.getConfigurationManager()
                .getMenuConfiguration().getConfiguration()
                .getIntegerList("order-take-menu.delivery-slots");

        int totalCollected = 0;
        boolean hasItems = false;
        boolean inventoryFull = false;

        for (int slot : slots) {
            ItemStack item = getInventory().getItem(slot);
            if (item == null || item.getType() == Material.AIR || item.getType() != order.getMaterial()) {
                continue;
            }

            hasItems = true;

            if (player.getInventory().firstEmpty() == -1) {
                inventoryFull = true;
                break;
            }

            player.getInventory().addItem(item);
            totalCollected += item.getAmount();
            getInventory().setItem(slot, null);
        }

        if (totalCollected > 0) {
            order.addCollected(totalCollected);
            main.getOrderLogger().logItemCollection(order, totalCollected);
            NSound.success(player);
            checkAndArchiveOrder(player);
        } else if (inventoryFull) {
            player.sendMessage(LanguageLoader.getMessage("inventory-full"));
            NSound.error(player);
        } else if (!hasItems) {
            player.sendMessage(LanguageLoader.getMessage("no-items-to-collect"));
            NSound.error(player);
        }

        player.updateInventory();
    }

    private void checkAndArchiveOrder(HumanEntity player) {
        if (order.getStatus() == OrderStatus.COMPLETED) {
            if (order.getCollected() >= order.getDelivered()) {
                order.setStatus(OrderStatus.ARCHIVED);
                main.getOrderLogger().logOrderArchived(order);
                player.closeInventory();
                main.getOrderManager().removeOrder(order);
            }
        }
    }


    @Override
    protected void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getCursor().getType() != Material.AIR) {
            NSound.click(player);
        }
    }
}
