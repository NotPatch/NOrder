package com.notpatch.nOrder;

import com.notpatch.nOrder.command.OrderAdminCommand;
import com.notpatch.nOrder.command.OrderCommand;
import com.notpatch.nOrder.database.DatabaseManager;
import com.notpatch.nOrder.gui.NewOrderMenu;
import com.notpatch.nOrder.hook.Metrics;
import com.notpatch.nOrder.hook.PlaceholderHook;
import com.notpatch.nOrder.listener.ChatInputListener;
import com.notpatch.nOrder.manager.*;
import com.notpatch.nlib.NLib;
import com.notpatch.nlib.compatibility.NCompatibility;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.morepaperlib.MorePaperLib;

public final class NOrder extends JavaPlugin {

    @Getter
    private static NOrder instance;

    @Getter
    private DatabaseManager databaseManager;

    @Getter
    private OrderManager orderManager;

    @Getter
    private PlayerStatisticsManager playerStatsManager;

    @Getter
    private ConfigurationManager configurationManager;

    @Getter
    private Economy economy;

    @Getter
    private LanguageLoader languageLoader;

    @Getter
    private ChatInputManager chatInputManager;

    @Getter
    private WebhookManager webhookManager;

    @Getter
    private Metrics metrics;

    @Getter
    private MorePaperLib morePaperLib;

    @Getter
    private OrderLogger orderLogger;

    @Override
    public void onEnable() {
        instance = this;

        NLib.initialize(this);

        morePaperLib = new MorePaperLib(this);

        saveDefaultConfig();
        saveConfig();

        Settings.loadSettings();

        NCompatibility compatibility = new NCompatibility();
        compatibility.
                checkBukkit("Paper", "Purpur", "Leaf", "Folia")
                .checkVersion("1.21.4", "1.21.10")
                .checkPlugin("PlaceholderAPI", false)
                .onSuccess(() -> {
                    new PlaceholderHook(this).register();

                })
                .checkPlugin("Vault", true)
                .onSuccess(() -> {
                    if (NLib.getInstance().getPlugin().getServer().getPluginManager().getPlugin("Vault") == null) {
                        return;
                    }
                    RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = NLib.getInstance().getPlugin().getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                    if (rsp == null) {
                        return;
                    }
                    economy = rsp.getProvider();
                });


        databaseManager = new DatabaseManager(this);
        databaseManager.connect();
        databaseManager.createTables();

        orderLogger = new OrderLogger(this);


        orderManager = new OrderManager(this);
        orderManager.loadOrders();

        playerStatsManager = new PlayerStatisticsManager(this);
        playerStatsManager.loadStatistics();

        orderManager.startCleanupTask();

        configurationManager = new ConfigurationManager();
        configurationManager.loadConfigurations();

        languageLoader = new LanguageLoader();
        languageLoader.loadLangs();

        webhookManager = new WebhookManager(this);
        webhookManager.loadWebhooks();

        compatibility.validate();

        chatInputManager = new ChatInputManager();

        getServer().getPluginManager().registerEvents(new NewOrderMenu(), this);

        registerCommand("order", Settings.ORDER_ALIASES, new OrderCommand());
        registerCommand("orderadmin", Settings.ORDER_ADMIN_ALIASES, new OrderAdminCommand());

        getServer().getPluginManager().registerEvents(new ChatInputListener(this), this);

        metrics = new Metrics(this, 27885);

        UpdateChecker updateChecker = new UpdateChecker(this);
        updateChecker.checkUpdates();


    }

    @Override
    public void onDisable() {
        if (orderManager != null) orderManager.saveOrders();
        if (playerStatsManager != null) playerStatsManager.saveStatistics();
        if (databaseManager != null) databaseManager.disconnect();
        if (configurationManager != null) configurationManager.saveConfigurations();
        if (morePaperLib != null) morePaperLib.scheduling().cancelGlobalTasks();
        if (metrics != null) metrics.shutdown();
    }

}
