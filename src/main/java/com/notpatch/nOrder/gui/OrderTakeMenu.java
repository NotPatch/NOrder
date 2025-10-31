package com.notpatch.nOrder.gui;

import com.notpatch.nOrder.LanguageLoader;
import com.notpatch.nOrder.model.OrderStatus;
import com.notpatch.nlib.effect.NSound;
import com.notpatch.nlib.fastinv.FastInv;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.model.Order;
import com.notpatch.nlib.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderTakeMenu extends FastInv {

    private final Order order;
    private final Map<Integer, ItemStack> deliveredItems;

    public OrderTakeMenu(Order order) {
        super(NOrder.getInstance().getConfigurationManager().getMenuConfiguration().getConfiguration()
                        .getInt("order-take-menu.size"),
                ColorUtil.hexColor(NOrder.getInstance().getConfigurationManager().getMenuConfiguration()
                        .getConfiguration().getString("order-take-menu.title")));

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
        Configuration config = NOrder.getInstance().getConfigurationManager()
                .getMenuConfiguration().getConfiguration();

        setItems(config.getIntegerList("order-take-menu.items.filler.slots"),
                createItem(Material.valueOf(config.getString("order-take-menu.items.filler.material")),
                        config.getString("order-take-menu.items.filler.name"),
                        config.getStringList("order-take-menu.items.filler.lore")));

        setItem(config.getInt("order-take-menu.items.back.slot"),
                createItem(Material.valueOf(config.getString("order-take-menu.items.back.material")),
                        config.getString("order-take-menu.items.back.name"),
                        config.getStringList("order-take-menu.items.back.lore")),
                e -> {
                    e.getWhoClicked().closeInventory();
                    new YourOrdersMenu((Player) e.getWhoClicked()).open((Player) e.getWhoClicked());
                });

        setItem(config.getInt("order-take-menu.items.info.slot"),
                createItem(Material.valueOf(config.getString("order-take-menu.items.info.material")),
                        config.getString("order-take-menu.items.info.name"),
                        config.getStringList("order-take-menu.items.info.lore")));
    }

    private void loadDeliveredItems() {
        List<Integer> slots = NOrder.getInstance().getConfigurationManager()
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

        if (clickedItem == null || clickedItem.getType() == Material.AIR && clickedItem.getType() == order.getMaterial()) return;

        e.setCurrentItem(null);


        if (e.getClick() == ClickType.DROP) {
            player.getWorld().dropItemNaturally(player.getLocation(), clickedItem);
            e.setCurrentItem(null);
        }
        else if (e.isLeftClick()) {
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(clickedItem);

                e.setCurrentItem(null);
            } else {
                player.sendMessage(LanguageLoader.getMessage("inventory-full"));
            }
        }

        order.addCollected(clickedItem.getAmount());

        if(order.getCollected() >= order.getDelivered()) {
            order.setStatus(OrderStatus.ARCHIVED);
            player.closeInventory();
            NOrder.getInstance().getOrderManager().removeOrder(order);
        }

        ((Player) player).updateInventory();
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.hexColor(name));
            meta.setLore(lore.stream().map(ColorUtil::hexColor).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        NSound.click(player);
    }
}
