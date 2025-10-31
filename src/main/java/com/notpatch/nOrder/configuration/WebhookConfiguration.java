package com.notpatch.nOrder.configuration;

import com.notpatch.nOrder.NOrder;
import com.notpatch.nlib.configuration.NConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class WebhookConfiguration extends NConfiguration {
    public WebhookConfiguration() {
        super(NOrder.getInstance(), "webhooks.yml");
    }
}
