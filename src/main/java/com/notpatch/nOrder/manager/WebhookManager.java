package com.notpatch.nOrder.manager;

import com.notpatch.nOrder.NOrder;
import com.notpatch.nOrder.model.DiscordWebhook;
import lombok.Getter;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class WebhookManager {

    private NOrder main;

    public WebhookManager(NOrder main) {
        this.main = main;
    }

    @Getter
    private HashMap<String, DiscordWebhook> webhooks = new HashMap<>();

    public void loadWebhooks() {
        webhooks.clear();
        Configuration config = main.getConfigurationManager().getWebhookConfiguration().getConfiguration();
        ConfigurationSection section = config.getConfigurationSection("webhooks");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                String path = "webhooks." + id;

                DiscordWebhook webhook = new DiscordWebhook(config.getString(path + ".url"));
                webhook.setContent(config.getString(path + ".content"));
                webhook.setUsername(config.getString(path + ".username", "nOrder"));
                webhook.setAvatarUrl(config.getString(path + ".avatarURL"));


                ConfigurationSection embeds = config.getConfigurationSection(path + ".embeds");
                if (embeds != null) {
                    for (String embedId : embeds.getKeys(false)) {
                        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();
                        String base = path + ".embeds." + embedId;
                        embed.setColor(Color.decode(config.getString(base + ".color", "#000000")));
                        embed.setTitle(config.getString(base + ".title", ""));
                        embed.setDescription(config.getString(base + ".description", ""));
                        embed.setUrl(config.getString(base + ".URL", ""));
                        embed.setImage(config.getString(base + ".imageURL", ""));
                        embed.setThumbnail(config.getString(base + ".thumbnailURL", ""));

                        if (config.contains(base + ".footer")) {
                            embed.setFooter(
                                    config.getString(base + ".footer.text", ""),
                                    config.getString(base + ".footer.iconURL", "")
                            );
                        }

                        if (config.contains(base + ".fields")) {
                            for (Map<?, ?> fieldMap : config.getMapList(base + ".fields")) {
                                String fieldName = (String) fieldMap.get("name");
                                String fieldValue = (String) fieldMap.get("value");
                                Boolean fieldInline = fieldMap.containsKey("inline") ? (Boolean) fieldMap.get("inline") : false;

                                if (fieldName != null && fieldValue != null) {
                                    embed.addField(fieldName, fieldValue, fieldInline);
                                }
                            }
                        }

                        webhook.addEmbed(embed);
                    }
                }
                webhooks.put(id, webhook);
            }
        }
    }



}