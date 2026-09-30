package com.example.pl_core_data.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "teams")
@Data
public class Team {
    @Id
    private Integer id;
    private String name;
    private String shortName;
    private String stadiumName;
}