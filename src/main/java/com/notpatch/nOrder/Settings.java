package com.notpatch.nOrder;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;

import java.util.*;
import java.util.stream.Collectors;

public class Settings {

    public Settings() {
    }

    public static String ORDER_MENU_PERMISSION;
    public static String ORDER_PERMISSION;

    public static String ORDER_ADMIN_PERMISSION;

    public static Collection<String> ORDER_ALIASES;
    public static Collection<String> ORDER_ADMIN_ALIASES;

    public static String ORDER_LIMIT_PERMISSION;
    public static String ORDER_EXPIRATION_PERMISSION;

    public static String DATE_FORMAT;

    public static double HIGHLIGHT_FEE;
    public static boolean SEND_WEBHOOKS;

    public static String HIGHLIGHT_PERMISSION;

    public static List<Material> availableItems = new ArrayList<>();

    public static int PROGRESS_BAR_LENGTH;
    public static char PROGRESS_BAR_COMPLETE_CHAR;
    public static char PROGRESS_BAR_INCOMPLETE_CHAR;
    public static String PROGRESS_BAR_COMPLETE_COLOR;
    public static String PROGRESS_BAR_INCOMPLETE_COLOR;


    public static void loadSettings() {
        Configuration config = NOrder.getInstance().getConfig();
        ORDER_PERMISSION = config.getString("permissions.order", "norder.use");
        ORDER_MENU_PERMISSION = config.getString("permissions.order-menu", "norder.menu");
        ORDER_ALIASES = config.getStringList("commands.order.aliases");
        ORDER_LIMIT_PERMISSION = config.getString("permissions.order-limit", "norder.limit");
        ORDER_ADMIN_ALIASES = config.getStringList("commands.order-admin.aliases");
        ORDER_ADMIN_PERMISSION = config.getString("permissions.order-admin", "norder.admin");
        HIGHLIGHT_FEE = config.getDouble("settings.highlight-fee", 2.5);
        SEND_WEBHOOKS = config.getBoolean("settings.send-webhooks", false);
        DATE_FORMAT = config.getString("date-format", "MM-dd HH:mm:ss");
        HIGHLIGHT_PERMISSION = config.getString("permissions.use-highlight", "norder.highlight");
        ORDER_EXPIRATION_PERMISSION = config.getString("permissions.order-expiration", "norder.expiration");
        List<Material> blacklist = config.getStringList("blacklist-items").stream()
                .map(Material::matchMaterial)
                .filter(Objects::nonNull)
                .toList();

        NOrder.getInstance().getMorePaperLib().scheduling().asyncScheduler().run(() -> {
            List<Material> items = Arrays.stream(Material.values())
                    .filter(m -> !m.isAir() && m.isItem() && !m.isLegacy())
                    .filter(m -> !blacklist.contains(m))
                    .collect(Collectors.toList());

            NOrder.getInstance().getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> availableItems = items);
        });

        PROGRESS_BAR_LENGTH = config.getInt("progress-bar.length", 20);
        PROGRESS_BAR_COMPLETE_CHAR = config.getString("progress-bar.complete-char", "█").charAt(0);
        PROGRESS_BAR_INCOMPLETE_CHAR = config.getString("progress-bar.incomplete-char", "░").charAt(0);
        PROGRESS_BAR_COMPLETE_COLOR = config.getString("progress-bar.complete-color", "&a");
        PROGRESS_BAR_INCOMPLETE_COLOR = config.getString("progress-bar.incomplete-color", "&7");
    }

}
