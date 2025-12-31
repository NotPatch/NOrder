package com.notpatch.nOrder.util;

import com.notpatch.nOrder.NOrder;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class NumberFormatter {

    private static final List<String> suffixes;
    private static final int decimalPlaces;

    static {
        FileConfiguration config = NOrder.getInstance().getConfig();
        suffixes = config.getStringList("number-format.suffixes");
        decimalPlaces = config.getInt("number-format.decimal-places", 1);
    }

    public static String format(double value) {

        if (value < 1000) {
            if (decimalPlaces <= 0) {
                return String.valueOf((long) value);
            }
            return String.format("%." + decimalPlaces + "f", value);
        }

        int exponent = (int) (Math.log(value) / Math.log(1000));
        if (exponent >= suffixes.size()) {
            exponent = suffixes.size() - 1;
        }

        double formattedValue = value / Math.pow(1000, exponent);
        String suffix = suffixes.get(exponent);

        return String.format("%." + decimalPlaces + "f%s", formattedValue, suffix);
    }
}
