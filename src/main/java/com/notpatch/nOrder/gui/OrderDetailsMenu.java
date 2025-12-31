package com.notpatch.nOrder.gui;

import com.notpatch.nOrder.LanguageLoader;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.model.Order;
import com.notpatch.nOrder.model.OrderStatus;
import com.notpatch.nOrder.util.ItemStackHelper;
import com.notpatch.nlib.effect.NSound;
import com.notpatch.nlib.fastinv.FastInv;
import com.notpatch.nlib.util.ColorUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.ArrayList;
import java.util.List;

public class OrderDetailsMenu extends FastInv {

    private final NOrder main;

    private final Order order;

    @Getter
    private int itemsDelivered = 0;

    @Getter
    private double totalEarning = 0;

    private boolean processed = false;

    public OrderDetailsMenu(Order order) {
        super(54, ColorUtil.hexColor(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration().getString("order-details-menu.title")));
        main = NOrder.getInstance();
        this.order = order;

        main.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
            for (int i = 0; i < getInventory().getSize(); i++) {
                getInventory().clear(i);
            }
        });
    }

    @Override
    protected void onClose(InventoryCloseEvent e) {
        super.onClose(e);

        if (processed) {
            return;
        }
        processed = true;

        Player player = (Player) e.getPlayer();

        processDelivery(player);
    }

    private void processDelivery(Player player) {
        if (order.getStatus() != OrderStatus.ACTIVE) {
            for (int i = 0; i < getInventory().getSize(); i++) {
                ItemStack item = getInventory().getItem(i);
                if (item != null && !item.getType().isAir()) {
                    player.getInventory().addItem(item).forEach((slot, leftover) ->
                            player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                    getInventory().clear(i);
                }
            }
            player.sendMessage(LanguageLoader.getMessage("order-not-active"));
            NSound.error(player);
            return;
        }

        List<ItemStack> validItems = new ArrayList<>();
        List<ItemStack> invalidItems = new ArrayList<>();

        for (int i = 0; i < getInventory().getSize(); i++) {
            ItemStack item = getInventory().getItem(i);
            if (item == null || item.getType().isAir()) continue;

            if (item.getType() == Material.SHULKER_BOX) {
                if (!(item.getItemMeta() instanceof BlockStateMeta)) {
                    invalidItems.add(item.clone());
                    getInventory().clear(i);
                    continue;
                }

                BlockStateMeta blockStateMeta = (BlockStateMeta) item.getItemMeta();
                if (!(blockStateMeta.getBlockState() instanceof ShulkerBox)) {
                    invalidItems.add(item.clone());
                    getInventory().clear(i);
                    continue;
                }

                ShulkerBox shulkerBox = (ShulkerBox) blockStateMeta.getBlockState();
                List<ItemStack> nonMatching = new ArrayList<>();

                for (ItemStack shulkerItem : shulkerBox.getInventory().getContents()) {
                    if (shulkerItem == null || shulkerItem.getType().isAir()) continue;
                    if (isSameItem(shulkerItem, order.getItem())) {
                        validItems.add(shulkerItem.clone());
                    } else {
                        nonMatching.add(shulkerItem.clone());
                    }
                }

                ItemStack returnedShulker = item.clone();
                BlockStateMeta retMeta = (BlockStateMeta) returnedShulker.getItemMeta();
                ShulkerBox returnedBox = (ShulkerBox) retMeta.getBlockState();
                returnedBox.getInventory().clear();
                for (int idx = 0; idx < nonMatching.size(); idx++) {
                    returnedBox.getInventory().setItem(idx, nonMatching.get(idx));
                }
                retMeta.setBlockState(returnedBox);
                returnedShulker.setItemMeta(retMeta);

                invalidItems.add(returnedShulker);

                getInventory().clear(i);
            } else {
                if (isSameItem(item, order.getItem())) {
                    validItems.add(item.clone());
                } else {
                    invalidItems.add(item.clone());
                }
                getInventory().clear(i);
            }
        }

        for (ItemStack item : invalidItems) {
            player.getInventory().addItem(item).forEach((slot, leftover) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }

        if (!invalidItems.isEmpty()) {
            player.sendMessage(LanguageLoader.getMessage("delivery-wrong-item").replace("%material%", order.getMaterial().name()));
        }

        if (!validItems.isEmpty()) {
            int totalAmount = 0;

            for (ItemStack item : validItems) {
                totalAmount += item.getAmount();
            }

            synchronized (order) {
                if (order.getStatus() != OrderStatus.ACTIVE) {
                    for (ItemStack item : validItems) {
                        player.getInventory().addItem(item).forEach((slot, leftover) ->
                                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                    }
                    player.sendMessage(LanguageLoader.getMessage("order-not-active"));
                    NSound.error(player);
                    return;
                }

                int currentRemaining = order.getRemaining();

                if (totalAmount > currentRemaining) {
                    int excess = totalAmount - currentRemaining;
                    totalAmount = currentRemaining;

                    if (excess > 0) {
                        ItemStack excessItem = order.getItem().clone();
                        excessItem.setAmount(excess);
                        player.getInventory().addItem(excessItem).forEach((slot, leftover) ->
                                player.getWorld().dropItemNaturally(player.getLocation(), leftover));

                        player.sendMessage(LanguageLoader.getMessage("delivery-excess-items").replace("%amount%", excess + ""));
                    }
                }

                if (totalAmount > 0) {
                    double earning = totalAmount * order.getPrice();

                    order.addDelivered(totalAmount);

                    player.sendMessage(LanguageLoader.getMessage("delivery-success").replace("%material%", order.getMaterial().name()).replace("%amount%", totalAmount + ""));
                    player.sendMessage(LanguageLoader.getMessage("delivery-earnings").replace("%amount%", String.format("%.2f", earning)));

                    main.getEconomy().depositPlayer(player, earning);
                    main.getPlayerStatsManager().getStatistics(player.getUniqueId()).addDeliveredItems(totalAmount);
                    main.getPlayerStatsManager().getStatistics(player.getUniqueId()).addTotalEarnings(earning);
                    main.getPlayerStatsManager().getStatistics(order.getPlayerId()).addCollectedItems(totalAmount);

                    main.getOrderLogger().logOrderDelivery(order, player.getName(), totalAmount, earning);

                    NSound.success(player);

                    if (order.getRemaining() <= 0) {
                        order.setStatus(OrderStatus.COMPLETED);
                        main.getOrderLogger().logOrderCompleted(order);

                        Player orderOwner = Bukkit.getPlayer(order.getPlayerId());
                        if (orderOwner != null && orderOwner.isOnline()) {
                            orderOwner.sendMessage(LanguageLoader.getMessage("delivery-completed").replace("%material%", order.getMaterial().name()));
                        }
                    }
                }
            }
        } else if (invalidItems.isEmpty()) {
            NSound.error(player);
        }
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        event.setCancelled(false);
        if (event.getCursor().getType() != Material.AIR) {
            NSound.click((Player) event.getWhoClicked());
        }

    }

    @Override
    protected void onDrag(InventoryDragEvent event) {
        event.setCancelled(false);
    }

    private boolean isSameItem(ItemStack item1, ItemStack item2) {
        return ItemStackHelper.isSameItem(item1, item2);
    }


}