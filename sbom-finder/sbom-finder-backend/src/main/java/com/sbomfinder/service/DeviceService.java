package com.sbomfinder.service;

import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.JsonNode;
import com.sbomfinder.model.Sbom;
import com.sbomfinder.model.Device;
import com.sbomfinder.repository.SoftwarePackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sbomfinder.model.SoftwarePackage;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.sbomfinder.model.Vulnerability;
import com.sbomfinder.repository.VulnerabilityRepository;
import java.net.URI;
import java.net.URISyntaxException;
import com.sbomfinder.dto.VulnerabilityDTO;
import java.util.stream.Collectors;


@Service
public class DeviceService {
    @Autowired
    private SoftwarePackageRepository softwarePackageRepository;
    public List<VulnerabilityDTO> getVulnerabilitiesForDevice(Device device) {
        Set<Vulnerability> allVulns = new HashSet<>();


        List<SoftwarePackage> packages = softwarePackageRepository.findByDeviceId(device.getId());

        for (SoftwarePackage pkg : packages) {
            allVulns.addAll(pkg.getVulnerabilities());
        }

        return allVulns.stream().map(v -> {
            VulnerabilityDTO dto = new VulnerabilityDTO();
            dto.setCveId(v.getCveId());
            dto.setDescription(v.getDescription());
            dto.setSeverity(v.getSeverity());
            dto.setSourceUrl(v.getSourceUrl());
            dto.setSeverityLevel(dto.getSeverityLevel());
            return dto;
        }).collect(Collectors.toList());
    }
}