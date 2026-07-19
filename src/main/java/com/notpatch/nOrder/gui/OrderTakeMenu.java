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
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OrderTakeMenu extends FastInv {

    private final NOrder main;
    private final Order order;
    private final boolean backOnClose;
    private final String parentMenuId;
    private boolean closedByAction = false;

    public OrderTakeMenu(Order order) {
        super(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration()
                        .getInt("order-take-menu.size"),
                ColorUtil.hexColor(NOrder.getInstance().getConfigurationManager().getMenuConfiguration()
                        .getConfiguration().getString("order-take-menu.title")));
        this.main = NOrder.getInstance();
        this.order = order;
        Configuration config = main.getConfigurationManager().getMenuConfiguration().getConfiguration();
        this.backOnClose = config.getBoolean("order-take-menu.back-on-close", false);
        this.parentMenuId = config.getString("order-take-menu.parent-menu-id", null);

        setupMenu();
    }

    private void setupMenu() {
        Configuration config = main.getConfigurationManager().getMenuConfiguration().getConfiguration();

        ConfigurationSection fillerSection = config.getConfigurationSection("order-take-menu.items.filler");
        if (fillerSection != null) {
            ItemStack fillerItem = ItemStackHelper.fromSection(fillerSection);
            setItems(config.getIntegerList("order-take-menu.items.filler.slots"), fillerItem);
        }

        ConfigurationSection backSection = config.getConfigurationSection("order-take-menu.items.back");
        if (backSection != null) {
            ItemStack backItem = ItemStackHelper.fromSection(backSection);
            setItem(backSection.getInt("slot"), backItem, e -> {
                closedByAction = true;
                e.getWhoClicked().closeInventory();
                new YourOrdersMenu((Player) e.getWhoClicked()).open((Player) e.getWhoClicked());
            });
        }

        ConfigurationSection takeAllSection = config.getConfigurationSection("order-take-menu.items.take-all");
        if (takeAllSection != null) {
            ItemStack takeAllItem = ItemStackHelper.fromSection(takeAllSection);
            setItem(takeAllSection.getInt("slot"), takeAllItem, e -> handleTakeAll((Player) e.getWhoClicked()));
        }

        updateInfoItem();
        loadDeliveredItems();
    }

    private void updateInfoItem() {
        Configuration config = main.getConfigurationManager().getMenuConfiguration().getConfiguration();
        ConfigurationSection infoSection = config.getConfigurationSection("order-take-menu.items.info");

        if (infoSection != null) {
            ItemStack infoItem = ItemStackHelper.fromSection(infoSection);
            ItemMeta meta = infoItem.getItemMeta();

            if (meta != null && meta.hasLore()) {
                List<String> lore = new ArrayList<>();
                int collected = order.getCollected();
                int delivered = order.getDelivered();
                int total = order.getAmount();

                for (String line : meta.getLore()) {
                    String processedLine = line
                            .replace("%collected%", String.valueOf(collected))
                            .replace("%delivered%", String.valueOf(delivered))
                            .replace("%total%", String.valueOf(total));
                    lore.add(ColorUtil.hexColor(processedLine));
                }

                meta.setLore(lore);
                infoItem.setItemMeta(meta);
            }

            setItem(infoSection.getInt("slot"), infoItem);
        }
    }

    private void loadDeliveredItems() {
        List<Integer> slots = main.getConfigurationManager()
                .getMenuConfiguration().getConfiguration()
                .getIntegerList("order-take-menu.delivery-slots");

        for (int slot : slots) {
            getInventory().setItem(slot, null);
        }

        int remainingAmount = order.getDelivered() - order.getCollected();
        int slotIndex = 0;
        int maxStackSize = order.getItem().getMaxStackSize();

        while (remainingAmount > 0 && slotIndex < slots.size()) {
            ItemStack item = order.getItem().clone();
            int stackSize = Math.min(maxStackSize, remainingAmount);
            item.setAmount(stackSize);

            int finalSlot = slots.get(slotIndex);
            setItem(finalSlot, item, e -> handleItemClick(e, finalSlot));

            remainingAmount -= stackSize;
            slotIndex++;
        }
    }

    private void handleItemClick(InventoryClickEvent e, int clickedSlot) {
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        ItemStack clickedItem = getInventory().getItem(clickedSlot);

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        int itemAmount = clickedItem.getAmount();

        if (e.getClick() == ClickType.DROP) {
            getInventory().setItem(clickedSlot, null);

            order.addCollected(itemAmount);
            main.getOrderLogger().logItemCollection(order, itemAmount);

            player.getWorld().dropItemNaturally(player.getLocation(), clickedItem.clone());

            NSound.success(player);
            checkAndArchiveOrder(player);

            updateInfoItem();
            loadDeliveredItems();
            return;
        }

        if (e.isLeftClick()) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(clickedItem.clone());

            int leftoverAmount = 0;
            for (ItemStack remaining : leftover.values()) {
                leftoverAmount += remaining.getAmount();
            }
            int addedAmount = itemAmount - leftoverAmount;

            if (addedAmount > 0) {
                order.addCollected(addedAmount);
                main.getOrderLogger().logItemCollection(order, addedAmount);

                NSound.success(player);
                checkAndArchiveOrder(player);

                updateInfoItem();
                loadDeliveredItems();
            }

            if (leftoverAmount > 0) {
                player.sendMessage(LanguageLoader.getMessage("inventory-full"));
                NSound.error(player);
            }
        }
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

            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            hasItems = true;

            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());

            if (leftover.isEmpty()) {
                totalCollected += item.getAmount();
                getInventory().setItem(slot, null);
            } else {
                ItemStack remaining = leftover.get(0);
                int addedAmount = item.getAmount() - remaining.getAmount();

                if (addedAmount > 0) {
                    totalCollected += addedAmount;
                }

                inventoryFull = true;
                break;
            }
        }

        if (totalCollected > 0) {
            order.addCollected(totalCollected);
            main.getOrderLogger().logItemCollection(order, totalCollected);
            main.getLogger().info("Collected (Take All): " + totalCollected);

            if (inventoryFull) {
                player.sendMessage(LanguageLoader.getMessage("inventory-full"));
            }

            NSound.success(player);
            checkAndArchiveOrder(player);

            updateInfoItem();
            loadDeliveredItems();
        } else if (inventoryFull) {
            player.sendMessage(LanguageLoader.getMessage("inventory-full"));
            NSound.error(player);
        } else if (!hasItems) {
            player.sendMessage(LanguageLoader.getMessage("no-items-to-collect"));
            NSound.error(player);
        }
    }

    private void checkAndArchiveOrder(Player player) {
        if (order.getStatus() == OrderStatus.COMPLETED && order.getCollected() >= order.getDelivered()) {
            order.setStatus(OrderStatus.ARCHIVED);
            main.getOrderLogger().logOrderArchived(order);
            closedByAction = true;
            player.closeInventory();
            main.getOrderManager().removeOrder(order);
        }
    }


    @Override
    protected void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        super.onClose(event);
        if (closedByAction) return;
        if (!backOnClose) return;
        if (parentMenuId == null || parentMenuId.isEmpty()) return;

        main.getDynamicMenuManager().openMenuById((Player) event.getPlayer(), parentMenuId);
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);
        if (event.getCursor().getType() != Material.AIR) {
            NSound.click(player);
        }
    }
}