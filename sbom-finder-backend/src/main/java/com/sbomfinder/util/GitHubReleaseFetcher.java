package com.sbomfinder.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class GitHubReleaseFetcher {

    public static String fetchLatestReleaseName(String repoUrl) {
        try {
            // Extract owner and repo name from repoUrl
            String[] parts = repoUrl.replace("https://", "").split("/");
            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid GitHub URL");
            }
            String owner = parts[1];
            String repo = parts[2].replaceAll(".git$", "");

            // GitHub API URL
            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest";

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            conn.disconnect();

            // Parse JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.toString());

            // Return release name if exists, else fallback to tag_name
            if (root.has("name") && !root.get("name").asText().isBlank()) {
                return root.get("name").asText();
            } else if (root.has("tag_name")) {
                return root.get("tag_name").asText();
            } else {
                return "Unknown Release";
            }

        } catch (Exception e) {
            System.err.println("Error fetching GitHub release: " + e.getMessage());
            return "Unknown Release";
        }
    }


    public static String extractVersionFromDependencyFile(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();

        try {
            String content = Files.readString(filePath);

            if (fileName.equals("package.json")) {
                // Node.js projects
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(content);
                if (root.has("version")) {
                    return root.get("version").asText();
                }
            } else if (fileName.equals("pom.xml")) {
                // Maven Java projects
                Pattern pattern = Pattern.compile("<version>(.*?)</version>");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } else if (fileName.equals("build.gradle")) {
                // Gradle projects
                Pattern pattern = Pattern.compile("versionName\\s+\"(.*?)\"");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } else if (fileName.equals("setup.py")) {
                // Python setup.py
                Pattern pattern = Pattern.compile("version\\s*=\\s*\"(.*?)\"");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } else if (fileName.equals("cargo.toml")) {
                // Rust Cargo
                Pattern pattern = Pattern.compile("version\\s*=\\s*\"(.*?)\"");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } else if (fileName.equals("composer.json")) {
                // PHP Composer
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(content);
                if (root.has("version")) {
                    return root.get("version").asText();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "unknown release";
    }

}
