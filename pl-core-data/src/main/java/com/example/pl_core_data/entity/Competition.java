package com.example.pl_core_data.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "competitions",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "country"})})
@Data
public class Competition {
    @Id
    private Integer id; // SofaScore ID (e.g., 17)

    @Column(nullable = false)
    private String name;

    private String country;
}