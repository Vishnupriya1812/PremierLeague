package com.example.pl_core_data.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // Crucial for Hibernate introspection
@AllArgsConstructor
public class StandingsDTO {
    private Integer id;
    private String name;
    private Integer played;
    private Integer won;
    private Integer drew;
    private Integer lost;
    private Integer gf;
    private Integer ga;
    private Integer gd;
    private Integer points;
}
