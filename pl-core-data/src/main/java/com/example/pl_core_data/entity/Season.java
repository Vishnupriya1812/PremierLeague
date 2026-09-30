package com.example.pl_core_data.entity;

import lombok.Data;
import jakarta.persistence.*;

@Entity
@Table(name = "seasons")
@Data
public class Season {
    @Id
    private Integer id; // SofaScore Season ID
    private String year; // e.g., "2024/2025"

    @ManyToOne
    @JoinColumn(name = "competition_id")
    private Competition competition;
}