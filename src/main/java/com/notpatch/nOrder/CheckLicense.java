package com.notpatch.nOrder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CheckLicense {

    private static String licenseKey;
    public static String URL = "https://ls.minebazaar.net/api/checklicense?licenseKey=";
    public static String PRODUCT = "NOrder";

    public CheckLicense(String licenseKey) {

        String serverIp = Bukkit.getServer().getIp();
        String JSON_URL = URL + licenseKey + "&product=" + PRODUCT + "&serverIp=" + serverIp;

        try {
            String jsonString = readUrl(JSON_URL);

            JSONObject json = new JSONObject(jsonString);
            String status = json.getString("status");
            String buyer = json.getString("buyer");
            String message = json.optString("message", "");

            if ("VALID".equals(status)) {
                if (buyer.isEmpty()) {
                    Bukkit.getConsoleSender().sendMessage("Status: " + status);
                } else {
                    Bukkit.getConsoleSender().sendMessage("Status: " + status + ", Buyer: " + buyer);
                }
            } else if ("INVALID".equals(status)) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&fLicense &c " + status + " &7&o(" + licenseKey + ")"));
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&fInfo: &7" + message));
                Bukkit.getPluginManager().disablePlugin(NOrder.getInstance());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(NOrder.getInstance());
        }
    }

    public static String readUrl(String urlString) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

}