package com.notpatch.nOrder;


import com.notpatch.nlib.util.ColorUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class LanguageLoader {

    private final NOrder main;

    private final HashMap<String, String> translationMap = new HashMap<>();
    private final HashMap<String, List<String>> translationListMap = new HashMap<>();

    public LanguageLoader() {
        this.main = NOrder.getInstance();

        String[] supportedLanguages = {"en_US.yml", "tr_TR.yml"};

        for (String langFile : supportedLanguages) {
            File userLangFile = new File(main.getDataFolder(), "languages/" + langFile);
            if (!userLangFile.exists()) {
                main.saveResource("languages/" + langFile, false);
            }
            updateLanguageFile(langFile);
        }

        loadLangs();
    }

    private void updateLanguageFile(String langFileName) {
        File userLangFile = new File(main.getDataFolder(), "languages/" + langFileName);
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userLangFile);

        InputStream defaultStream = main.getResource("languages/" + langFileName);
        if (defaultStream == null) {
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);

            boolean updated = false;
            for (String key : defaultConfig.getKeys(true)) {
                if (!userConfig.isSet(key)) {
                    userConfig.set(key, defaultConfig.get(key));
                    updated = true;
                }
            }

            if (updated) {
                try {
                    userConfig.save(userLangFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String get(String path) {
        String value = translationMap.getOrDefault(path, "Translation not found for path: " + path);
        if (main.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null)
            value = PlaceholderAPI.setPlaceholders(null, value);
        return ColorUtil.hexColor(value);
    }

    public List<String> getList(String path) {
        return ColorUtil.getColoredList(translationListMap.getOrDefault(path, Collections.singletonList("Translation list not found for path: " + path)));
    }

    public void loadLangs() {
        File languageFile = new File(main.getDataFolder(), "languages/" + main.getConfig().getString("lang", "en_US") + ".yml");

        FileConfiguration translations = YamlConfiguration.loadConfiguration(languageFile);
        translationMap.clear();
        translationListMap.clear();

        for (String key : translations.getKeys(true)) {
            if (translations.isList(key)) {
                translationListMap.put(key, translations.getStringList(key));
            } else if (translations.isString(key)) {
                translationMap.put(key, translations.getString(key));
            }
        }
    }

    public String getLanguage() {
        return main.getConfig().getString("lang", "en_US");
    }

    public static String getMessage(String path) {
        return NOrder.getInstance().getLanguageLoader().get(path);
    }

    public static List<String> getMessageList(String path) {
        return NOrder.getInstance().getLanguageLoader().getList(path);
    }
}