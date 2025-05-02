package com.sbomfinder.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String website; // optional
    private String contactInfo; // optional

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SoftwarePackage> packages = new ArrayList<>();

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // Getter for id
    public Long getId() {
        return id;
    }

    // Setter if needed
    public void setId(Long id) {
        this.id = id;
    }
}