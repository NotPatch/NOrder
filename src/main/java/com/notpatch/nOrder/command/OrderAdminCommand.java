package com.notpatch.nOrder.command;

import com.notpatch.nOrder.LanguageLoader;
import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.Settings;
import com.notpatch.nOrder.model.Order;
import com.notpatch.nOrder.model.OrderStatus;
import com.notpatch.nOrder.util.NumberFormatter;
import com.notpatch.nOrder.util.StringUtil;
import com.notpatch.nlib.effect.NSound;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OrderAdminCommand implements BasicCommand {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {

        Entity entity = commandSourceStack.getExecutor();
        CommandSender sender = commandSourceStack.getSender();

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> {
                if (args.length == 1) {
                    reloadConfigurations(sender);
                    if (entity instanceof Player player) {
                        NSound.success(player);
                    }
                }
            }
            case "info" -> {
                if (args.length == 2) {
                    handleInfoCommand(sender, entity, args[1]);
                } else {
                    sender.sendMessage(LanguageLoader.getMessage("admin-usage-info"));
                }
            }
            case "delete" -> {
                if (args.length == 2) {
                    handleDeleteCommand(sender, entity, args[1]);
                } else {
                    sender.sendMessage(LanguageLoader.getMessage("admin-usage-delete"));
                }
            }
            case "player" -> {
                if (args.length >= 2) {
                    int limit = args.length >= 3 ? parseIntOrDefault(args[2], 10) : 10;
                    handlePlayerCommand(sender, entity, args[1], limit);
                } else {
                    sender.sendMessage(LanguageLoader.getMessage("admin-usage-player"));
                }
            }
            default -> sendUsage(sender);
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(LanguageLoader.getMessage("admin-usage-reload"));
        sender.sendMessage(LanguageLoader.getMessage("admin-usage-info"));
        sender.sendMessage(LanguageLoader.getMessage("admin-usage-delete"));
        sender.sendMessage(LanguageLoader.getMessage("admin-usage-player"));
    }

    private void handleInfoCommand(CommandSender sender, Entity entity, String orderId) {
        Order order = NOrder.getInstance().getOrderManager().getOrderById(orderId);
        if (order != null) {
            if (entity instanceof Player player) {
                NSound.success(player);
            }

            sender.sendMessage(Component.text("═══════════ Order Info ═══════════", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("ID: ", NamedTextColor.GRAY).append(Component.text(order.getId(), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Owner: ", NamedTextColor.GRAY).append(Component.text(order.getPlayerName(), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Item: ", NamedTextColor.GRAY).append(Component.text(StringUtil.formatMaterialName(order.getMaterial()), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Amount: ", NamedTextColor.GRAY).append(Component.text(order.getAmount(), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Price per item: ", NamedTextColor.GRAY).append(Component.text(NumberFormatter.format(order.getPrice()), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Total Price: ", NamedTextColor.GRAY).append(Component.text(NumberFormatter.format(order.getPrice() * order.getAmount()), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Status: ", NamedTextColor.GRAY).append(Component.text(order.getStatus().name(), getStatusColor(order.getStatus()))));
            sender.sendMessage(Component.text("Delivered: ", NamedTextColor.GRAY).append(Component.text(order.getDelivered() + "/" + order.getAmount(), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Collected: ", NamedTextColor.GRAY).append(Component.text(order.getCollected() + "/" + order.getDelivered(), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Highlight: ", NamedTextColor.GRAY).append(Component.text(order.isHighlight() ? "Yes" : "No", order.isHighlight() ? NamedTextColor.GREEN : NamedTextColor.RED)));
            sender.sendMessage(Component.text("Created: ", NamedTextColor.GRAY).append(Component.text(order.getCreatedAt().format(formatter), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Expires: ", NamedTextColor.GRAY).append(Component.text(order.getExpirationDate().format(formatter), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD));

            Component actions = Component.text("[Delete]", NamedTextColor.RED)
                    .hoverEvent(HoverEvent.showText(Component.text("Click to delete order")))
                    .clickEvent(ClickEvent.suggestCommand("/orderadmin delete " + orderId));
            sender.sendMessage(actions);
        } else {
            sender.sendMessage(LanguageLoader.getMessage("order-not-found").replace("%id%", orderId));
            if (entity instanceof Player player) {
                NSound.error(player);
            }
        }
    }

    private void handleDeleteCommand(CommandSender sender, Entity entity, String orderId) {
        Order order = NOrder.getInstance().getOrderManager().getOrderById(orderId);
        if (order != null) {
            String adminName = entity instanceof Player player ? player.getName() : "Console";
            NOrder.getInstance().getOrderLogger().logAdminAction(adminName, "DELETE", order);
            NOrder.getInstance().getOrderManager().removeOrder(order);
            sender.sendMessage(LanguageLoader.getMessage("order-deleted").replace("%id%", orderId));
            if (entity instanceof Player player) {
                NSound.success(player);
            }
        } else {
            sender.sendMessage(LanguageLoader.getMessage("order-not-found").replace("%id%", orderId));
            if (entity instanceof Player player) {
                NSound.error(player);
            }
        }
    }


    private void handlePlayerCommand(CommandSender sender, Entity entity, String playerName, int limit) {
        List<Order> playerOrders = NOrder.getInstance().getOrderManager().getPlayerOrdersIncludingCompleted(playerName);

        if (playerOrders.isEmpty()) {
            sender.sendMessage(LanguageLoader.getMessage("player-no-orders").replace("%player%", playerName));
            if (entity instanceof Player player) {
                NSound.error(player);
            }
            return;
        }

        sender.sendMessage(Component.text("═══════ Orders by " + playerName + " ═══════", NamedTextColor.GOLD));

        int count = 0;
        for (Order order : playerOrders) {
            if (count >= limit) break;

            Component orderEntry = Component.text("• ", NamedTextColor.GRAY)
                    .append(Component.text(order.getId(), NamedTextColor.AQUA)
                            .hoverEvent(HoverEvent.showText(Component.text("Click to view details")))
                            .clickEvent(ClickEvent.runCommand("/orderadmin info " + order.getId())))
                    .append(Component.text(" | ", NamedTextColor.GRAY))
                    .append(Component.text(StringUtil.formatMaterialName(order.getMaterial()), NamedTextColor.WHITE))
                    .append(Component.text(" | ", NamedTextColor.GRAY))
                    .append(Component.text(order.getDelivered() + "/" + order.getAmount(), NamedTextColor.YELLOW))
                    .append(Component.text(" | ", NamedTextColor.GRAY))
                    .append(Component.text(order.getStatus().name(), getStatusColor(order.getStatus())));

            sender.sendMessage(orderEntry);
            count++;
        }

        sender.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.GOLD));


        if (entity instanceof Player player) {
            NSound.success(player);
        }
    }

    private NamedTextColor getStatusColor(OrderStatus status) {
        return switch (status) {
            case ACTIVE -> NamedTextColor.GREEN;
            case COMPLETED -> NamedTextColor.BLUE;
            case CANCELLED -> NamedTextColor.RED;
            case ARCHIVED -> NamedTextColor.GRAY;
        };
    }


    private int parseIntOrDefault(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {

        List<String> suggestions = List.of("reload", "info", "delete", "player");

        if (args.length == 0) {
            return suggestions;
        } else if (args.length == 1) {
            String input = args[0].toLowerCase();
            return suggestions.stream()
                    .filter(suggestion -> suggestion.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();

            return switch (subCommand) {
                case "info", "delete" -> NOrder.getInstance().getOrderManager().getAllOrders().stream()
                        .map(Order::getId)
                        .filter(id -> id.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
                case "player" -> Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
                default -> Collections.emptyList();
            };
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("player")) {
                return List.of("10", "20", "50", "100");
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

    private void reloadConfigurations(CommandSender sender) {
        NOrder main = NOrder.getInstance();
        main.reloadConfig();
        main.saveDefaultConfig();
        main.saveConfig();

        main.getLanguageLoader().loadLangs();
        Settings.loadSettings();
        main.getConfigurationManager().reloadConfigurations();
        main.getWebhookManager().loadWebhooks();
        sender.sendMessage("§aConfigurations reloaded.");
    }

}
