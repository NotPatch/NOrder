package com.notpatch.nOrder.command;

import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.Settings;
import com.notpatch.nOrder.gui.MainOrderMenu;
import com.notpatch.nOrder.gui.OrderDetailsMenu;
import com.notpatch.nOrder.model.Order;
import com.notpatch.nlib.effect.NSound;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class OrderCommand implements BasicCommand {

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {
        Entity executor = commandSourceStack.getExecutor();
        if(!(executor instanceof Player player)) return;

        if(args.length > 0){
            String[] split = args[0].split(":");
            if(split.length == 2){
                if(split[0].equalsIgnoreCase("id")){
                    Order order = NOrder.getInstance().getOrderManager().getOrderById(split[1]);
                    if(order != null){
                        new OrderDetailsMenu(order).open(player);
                        NSound.click(player);
                        return;
                    }
                }
                if(split[0].equalsIgnoreCase("player")){
                    List<Order> orders = NOrder.getInstance().getOrderManager().getPlayerOrders(split[1]);
                    if(orders != null){
                        new MainOrderMenu(orders).open(player);
                        NSound.click(player);
                        return;
                    }
                }
                if(split[0].equalsIgnoreCase("material")){
                    List<Order> orders = NOrder.getInstance().getOrderManager().getOrdersByMaterial(split[1]);
                    if(orders != null){
                        new MainOrderMenu(orders).open(player);
                        NSound.click(player);
                        return;
                    }
                }
            }
        }

        new MainOrderMenu().open(player);
        NSound.click(player);

    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        List<String> suggestions = List.of("id:", "player:", "item:");
        if (args.length == 0 || (args.length == 1 && args[0].isEmpty())) {
            return suggestions;
        }
        else if (args.length == 1) {
            String input = args[0].toLowerCase();
            return suggestions.stream()
                    .filter(suggestion -> suggestion.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if(sender instanceof Player){
            return sender.hasPermission(Settings.ORDER_MENU_PERMISSION);
        }
        return false;
    }

    @Override
    public @Nullable String permission() {
        return Settings.ORDER_MENU_PERMISSION;
    }
}
