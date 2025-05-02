package com.sbomfinder.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackageParser {

    public static Pair<String, String> parseFlexiblePythonPackageLine(String line) {
        // Remove comments
        int commentIndex = line.indexOf('#');
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex).trim();
        }

        // Match common operators (==, >=, <=, ~=, >, <)
        Pattern pattern = Pattern.compile("^([a-zA-Z0-9_.\\-]+)([<>=!~]{1,2})([\\d\\.]+)");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String name = matcher.group(1).trim();
            String version = matcher.group(2).trim() + matcher.group(3).trim();
            return new Pair<>(name, version);
        } else if (line.contains("=>")) {
            String[] parts = line.split("=>");
            return new Pair<>(parts[0].trim(), parts.length > 1 ? parts[1].trim() : "Unknown");
        } else {
            return new Pair<>(line.trim(), "Unknown");
        }
    }
}
