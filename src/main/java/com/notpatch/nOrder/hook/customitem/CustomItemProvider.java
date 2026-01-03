package com.notpatch.nOrder.hook.customitem;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface CustomItemProvider {

    boolean isAvailable();

    String getProviderName();

    boolean isCustomItem(ItemStack item);

    String getCustomItemId(ItemStack item);

    ItemStack getCustomItem(String itemId);

    String getCustomItemDisplayName(ItemStack item);

    boolean isSameCustomItem(ItemStack item1, ItemStack item2);

    List<ItemStack> getCustomItemsFromIds(List<String> itemIds);
}

