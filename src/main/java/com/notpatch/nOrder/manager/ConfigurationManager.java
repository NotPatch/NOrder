package com.notpatch.nOrder.manager;

import com.notpatch.nOrder.configuration.CustomItemConfiguration;
import com.notpatch.nOrder.configuration.MenuConfiguration;
import com.notpatch.nOrder.configuration.WebhookConfiguration;
import com.notpatch.nlib.configuration.NConfiguration;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationManager {

    @Getter
    private final MenuConfiguration menuConfiguration;
    @Getter
    private final WebhookConfiguration webhookConfiguration;
    @Getter
    private final CustomItemConfiguration customItemConfiguration;

    @Getter
    public List<NConfiguration> configurations = new ArrayList<>();

    public ConfigurationManager() {
        configurations.add(menuConfiguration = new MenuConfiguration());
        configurations.add(webhookConfiguration = new WebhookConfiguration());
        configurations.add(customItemConfiguration = new CustomItemConfiguration());
    }

    public void loadConfigurations() {
        configurations.forEach(NConfiguration::loadConfiguration);
    }

    public void saveConfigurations() {
        configurations.forEach(NConfiguration::saveConfiguration);
    }

    public void reloadConfigurations() {
        configurations.forEach(NConfiguration::reloadConfiguration);
    }

}
