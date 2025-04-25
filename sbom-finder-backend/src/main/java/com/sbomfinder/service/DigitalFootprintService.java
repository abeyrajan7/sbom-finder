package com.sbomfinder.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;

@Service
public class DigitalFootprintService {

    // List of common dependency files with their extensions or names
    private static final Set<String> DEPENDENCY_FILE_NAMES = Set.of(
            "package.json", "package-lock.json", "pom.xml", "build.gradle",
            "requirements.txt", "requirement.txt", "pipfile", "setup.py",
            "go.mod", "composer.json", "cargo.toml"
    );

    // List of supported file extensions
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "json", "xml", "txt", "py", "go", "toml", "gradle"
    );

    public List<String> generateDigitalFootprints(List<Path> files) throws IOException, NoSuchAlgorithmException {
        List<String> footprints = new ArrayList<>();

        for (Path filePath : files) {
            String fileName = filePath.getFileName().toString().toLowerCase();
            String fileExtension = getFileExtension(fileName);

            // Check if the file name or extension matches any dependency file criteria
            if (isDependencyFile(fileName, fileExtension)) {
                byte[] fileContent = Files.readAllBytes(filePath); // Read content from Path
                String hash = sha256(fileContent);

                StringBuilder footprint = new StringBuilder();
                footprint.append("File: ").append(fileName).append("\n")
                        .append("SHA-256: ").append(hash).append("\n\n");
                footprint.append("Generated At: ").append(LocalDateTime.now());

                footprints.add(footprint.toString());
            } else {
                throw new IllegalArgumentException("The provided file " + fileName + " is not a recognized dependency file.");
            }
        }

        return footprints;
    }

    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf(".");
        if (lastIndex != -1 && lastIndex < fileName.length() - 1) {
            return fileName.substring(lastIndex + 1);
        }
        return "";
    }

    private boolean isDependencyFile(String fileName, String fileExtension) {
        return DEPENDENCY_FILE_NAMES.contains(fileName) || SUPPORTED_EXTENSIONS.contains(fileExtension);
    }

    private String sha256(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

}
