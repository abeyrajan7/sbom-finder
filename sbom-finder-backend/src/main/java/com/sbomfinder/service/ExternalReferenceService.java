package com.sbomfinder.service;

import com.sbomfinder.model.ExternalReference;
import com.sbomfinder.model.Sbom;
import com.sbomfinder.repository.ExternalReferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExternalReferenceService {

    @Autowired
    private ExternalReferenceRepository externalReferenceRepository;

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[\\w./%-]+", Pattern.CASE_INSENSITIVE);

    private static final List<String> EXTERNAL_REFERENCE_FILENAMES = List.of(
            "README.md", "readme.txt", "LICENSE", "NOTICE", "about.txt",
            "package.json", "package-lock.json", "requirements.txt", "Pipfile",
            "setup.py", "pom.xml", "go.mod", "Cargo.toml", "composer.json",
            "build.gradle", "build.gradle.kts", "environment.yml", "environment.yaml"
    );

    public List<String> extractExternalReferences(Path sourceDirectory) {
        List<String> externalLinks = new ArrayList<>();

        try {
            Files.walk(sourceDirectory)
                    .filter(path -> Files.isRegularFile(path) && EXTERNAL_REFERENCE_FILENAMES.contains(path.getFileName().toString().toLowerCase()))
                    .forEach(file -> {
                        try {
                            List<String> lines = Files.readAllLines(file);
                            for (String line : lines) {
                                Matcher matcher = URL_PATTERN.matcher(line);
                                while (matcher.find()) {
                                    externalLinks.add(matcher.group());
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("Failed to read file: " + file.getFileName());
                        }
                    });
        } catch (IOException e) {
            System.err.println("Failed to traverse source directory: " + e.getMessage());
        }

        return externalLinks;
    }

    // New method to save external references linked to an SBOM
    public void saveExternalReferences(Sbom sbom, List<String> externalLinks) {
        for (String link : externalLinks) {
            ExternalReference ref = new ExternalReference();
            ref.setSbom(sbom);
            ref.setReferenceCategory("EXTERNAL" );
            ref.setReferenceType("WEBSITE");
            ref.setReferenceLocator(link);
            externalReferenceRepository.save(ref);
        }
    }
}
