package com.notpatch.nOrder.configuration;

import com.notpatch.nOrder.NOrder;
import com.notpatch.nlib.configuration.NConfiguration;

public class MenuConfiguration extends NConfiguration {

    public MenuConfiguration() {
        super(NOrder.getInstance(), "menu.yml");
    }
}
