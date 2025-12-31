package com.notpatch.nOrder.command;

import com.notpatch.nOrder.LanguageLoader;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.Settings;
import com.notpatch.nOrder.gui.MainOrderMenu;
import com.notpatch.nOrder.gui.OrderDetailsMenu;
import com.notpatch.nOrder.gui.OrderTakeMenu;
import com.notpatch.nOrder.model.Order;
import com.notpatch.nOrder.util.PlayerUtil;
import com.notpatch.nlib.effect.NSound;
import com.notpatch.nlib.util.ColorUtil;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OrderCommand implements BasicCommand {

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {
        Entity executor = commandSourceStack.getExecutor();
        if (!(executor instanceof Player player)) return;

        if (args.length > 0) {
            // Handle "create" subcommand: /order create <item> <quantity> <price>
            if (args[0].equalsIgnoreCase("create")) {
                handleCreateCommand(player, args);
                return;
            }
            
            String[] split = args[0].split(":");
            if (split.length == 2) {
                if (split[0].equalsIgnoreCase("id")) {
                    Order order = NOrder.getInstance().getOrderManager().getOrderById(split[1]);
                    if (order != null) {
                        if (order.getPlayerId() == player.getUniqueId()) {
                            new OrderTakeMenu(order).open(player);
                            NSound.click(player);
                            return;
                        }
                        new OrderDetailsMenu(order).open(player);
                        NSound.click(player);
                        return;
                    }
                }
                if (split[0].equalsIgnoreCase("player")) {
                    List<Order> orders = NOrder.getInstance().getOrderManager().getPlayerOrders(split[1]);
                    if (orders != null) {
                        new MainOrderMenu(orders).open(player);
                        NSound.click(player);
                        return;
                    }
                }
                if (split[0].equalsIgnoreCase("material")) {
                    List<Order> orders = NOrder.getInstance().getOrderManager().getOrdersByMaterial(split[1]);
                    if (orders != null) {
                        new MainOrderMenu(orders).open(player);
                        NSound.click(player);
                        return;
                    }
                }
            }
        }

        new MainOrderMenu(player).open(player);
        NSound.click(player);

    }

    /**
     * Handles the /order create <item> <quantity> <price> command
     * All validations are performed before any economy transaction
     */
    private void handleCreateCommand(Player player, String[] args) {
        if (args.length != 4) {
            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("create-command-usage")));
            NSound.error(player);
            return;
        }

        String materialName = args[1].toUpperCase();
        String quantityStr = args[2];
        String priceStr = args[3];

        Material material = Material.matchMaterial(materialName);
        if (material == null || !material.isItem() || material.isAir()) {
            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-item")
                    .replace("%item%", materialName)));
            NSound.error(player);
            return;
        }

        if (!Settings.availableItems.contains(material)) {
            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-item")
                    .replace("%item%", materialName)));
            NSound.error(player);
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-quantity")));
                NSound.error(player);
                return;
            }
            if (quantity > 1000000) {
                player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-quantity")));
                NSound.error(player);
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-quantity")));
            NSound.error(player);
            return;
        }

        double pricePerItem;
        try {
            pricePerItem = Double.parseDouble(priceStr);
            if (pricePerItem <= 0) {
                player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-price")));
                NSound.error(player);
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("invalid-price")));
            NSound.error(player);
            return;
        }

        double minPrice = Settings.getMinPricePerItem(material);
        double maxPrice = Settings.getMaxPricePerItem(material);
        
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
        

        if (!PlayerUtil.isPlayerAdmin(player)) {
            int playerOrderLimit = PlayerUtil.getPlayerOrderLimit(player);
            if (NOrder.getInstance().getOrderManager().getPlayerOrderCount(player.getUniqueId()) >= playerOrderLimit) {
                player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("order-limit-reached")
                        .replace("%limit%", String.valueOf(playerOrderLimit))));
                NSound.error(player);
                return;
            }
            
            if (NOrder.getInstance().getEconomy().getBalance(player) < totalPrice) {
                player.sendMessage(ColorUtil.hexColor(LanguageLoader.getMessage("not-enough-money")));
                NSound.error(player);
                return;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusDays(PlayerUtil.getPlayerOrderExpiration(player));
        String id = NOrder.getInstance().getOrderManager().createRandomId();
        
        ItemStack item = new ItemStack(material);

        String customItemId = null;
        if (NOrder.getInstance().getItemsAdderHook() != null &&
                NOrder.getInstance().getItemsAdderHook().isAvailable()) {
            customItemId = NOrder.getInstance().getItemsAdderHook().getCustomItemId(item);
        }

        Order order = new Order(id, player.getUniqueId(), player.getName(), item, customItemId, quantity, pricePerItem, now, expireAt, false);

        NOrder.getInstance().getOrderManager().addOrder(order);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].isEmpty())) {
            List<String> suggestions = List.of("create", "id:", "player:", "item:");
            return suggestions;
        } else if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> suggestions = List.of("create", "id:", "player:", "item:");
            return suggestions.stream()
                    .filter(suggestion -> suggestion.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            String input = args[1].toUpperCase();
            return Settings.availableItems.stream()
                    .map(Material::name)
                    .filter(name -> name.startsWith(input))
                    .limit(50)
                    .collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return List.of("1", "64", "128");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            return List.of("1.0", "10.0", "100.0");
        }

        return Collections.emptyList();
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if (sender instanceof Player) {
            return sender.hasPermission(Settings.ORDER_MENU_PERMISSION);
        }
        return false;
    }

    @Override
    public @Nullable String permission() {
        return Settings.ORDER_MENU_PERMISSION;
    }
}
