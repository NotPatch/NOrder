package com.notpatch.nOrder.command;

import com.notpatch.nOrder.LanguageLoader;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.Settings;
import com.notpatch.nOrder.model.Order;
import com.notpatch.nlib.effect.NSound;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OrderAdminCommand implements BasicCommand {

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {

        if (args.length == 0) {
            commandSourceStack.getExecutor().sendMessage(LanguageLoader.getMessage("admin-usage-reload"));
            commandSourceStack.getExecutor().sendMessage(LanguageLoader.getMessage("admin-usage-info"));
            commandSourceStack.getExecutor().sendMessage(LanguageLoader.getMessage("admin-usage-delete"));
            return;
        }

        if(args.length == 1){
            if(args[0].equalsIgnoreCase("reload")){
                reloadConfigurations(commandSourceStack.getExecutor());
                NSound.success((Player) commandSourceStack.getExecutor());
            }
        }


        if(args.length == 2){
            if(args[0].equalsIgnoreCase("info")){
                String orderId = args[1];
                Order order = NOrder.getInstance().getOrderManager().getOrderById(orderId);
                if(order != null){
                    int amount = order.getAmount();
                    double price = order.getPrice();
                    String status = order.getStatus().name();
                    Material itemMaterial = order.getItem().getType();
                    String buyerName = order.getPlayerName();

                    int delivered = order.getDelivered();
                    NSound.success((Player) commandSourceStack.getExecutor());
                    commandSourceStack.getExecutor().sendMessage(LanguageLoader.getMessage("order-info")
                            .replace("%id%", orderId)
                            .replace("%buyer%", buyerName)
                            .replace("%item%", itemMaterial.name())
                            .replace("%amount%", String.valueOf(amount))
                            .replace("%price%", String.valueOf(price))
                            .replace("%status%", status)
                            .replace("%delivered%", String.valueOf(delivered))

                    );
                    return;
                }else{
                    commandSourceStack.getExecutor().sendMessage(LanguageLoader.getMessage("order-not-found").replace("%id%", orderId));
                    NSound.error((Player) commandSourceStack.getExecutor());
                    return;
                }
            }
            if(args[0].equalsIgnoreCase("delete")){
                String orderId = args[1];
                Order order = NOrder.getInstance().getOrderManager().getOrderById(orderId);
                if(order != null){
                    NOrder.getInstance().getOrderManager().removeOrder(order);
                    commandSourceStack.getExecutor().sendMessage(LanguageLoader.getMessage("order-deleted").replace("%id%", orderId));
                    NSound.success((Player) commandSourceStack.getExecutor());
                    return;
                }else{
                    commandSourceStack.getExecutor().sendMessage(LanguageLoader.getMessage("order-not-found").replace("%id%", orderId));
                    NSound.error((Player) commandSourceStack.getExecutor());
                    return;
                }
            }
        }

    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {

        List<String> suggestions = List.of("reload", "info", "delete");

        if (args.length == 0) {
            return suggestions;
        }
        else if (args.length == 1) {
            String input = args[0].toLowerCase();
            return suggestions.stream()
                    .filter(suggestion -> suggestion.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        else if (args.length == 2) {
            if(args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("delete")){
                String input = args[1].toLowerCase();
                return NOrder.getInstance().getOrderManager().getAllOrders().stream()
                        .map(Order::getId)
                        .filter(id -> id.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission(Settings.ORDER_ADMIN_PERMISSION);
    }

    @Override
    public @Nullable String permission() {
        return Settings.ORDER_ADMIN_PERMISSION;
    }

    private void reloadConfigurations(Entity executor){
        NOrder main = NOrder.getInstance();
        main.reloadConfig();
        main.saveDefaultConfig();
        main.saveConfig();

        main.getLanguageLoader().loadLangs();
        Settings.loadSettings();
        main.getConfigurationManager().reloadConfigurations();
        main.getWebhookManager().loadWebhooks();
        executor.sendMessage("§aConfigurations reloaded.");
    }

}
