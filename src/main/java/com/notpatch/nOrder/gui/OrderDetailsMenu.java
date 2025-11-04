package com.notpatch.nOrder.gui;

import com.notpatch.nOrder.LanguageLoader;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.model.Order;
import com.notpatch.nOrder.model.OrderStatus;
import com.notpatch.nlib.effect.NSound;
import com.notpatch.nlib.fastinv.FastInv;
import com.notpatch.nlib.util.ColorUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderDetailsMenu extends FastInv {

    private final NOrder main;

    private final Order order;

    @Getter
    private int itemsDelivered = 0;

    @Getter
    private double totalEarning = 0;

    public OrderDetailsMenu(Order order) {
        super(54, ColorUtil.hexColor(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration().getString("order-details-menu.title")));
        main = NOrder.getInstance();
        this.order = order;

        Bukkit.getScheduler().runTask(NOrder.getInstance(), () -> {
            for (int i = 0; i < getInventory().getSize(); i++) {
                getInventory().clear(i);
            }
        });
    }

    @Override
    protected void onClose(InventoryCloseEvent e) {
        super.onClose(e);
        Player player = (Player) e.getPlayer();

        processDelivery(player);
    }

    private void processDelivery(Player player) {
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

            if (totalAmount > order.getRemaining()) {
                int excess = totalAmount - order.getRemaining();
                totalAmount = order.getRemaining();

                if (excess > 0) {
                    ItemStack excessItem = new ItemStack(order.getMaterial(), excess);
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

                NSound.success(player);

                if (order.getRemaining() <= 0) {

                    Player orderOwner = Bukkit.getPlayer(order.getPlayerId());
                    if (orderOwner != null && orderOwner.isOnline()) {
                        orderOwner.sendMessage(LanguageLoader.getMessage("delivery-completed").replace("%material%", order.getMaterial().name()));
                        order.setStatus(OrderStatus.COMPLETED);
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
        NSound.click((Player) event.getWhoClicked());
    }

    @Override
    protected void onDrag(InventoryDragEvent event) {
        event.setCancelled(false);
    }

    private boolean isSameItem(ItemStack item1, ItemStack item2) {
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


}