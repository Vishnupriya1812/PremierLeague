package com.example.pl_core_data.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopPerformersDTO {
    private Integer id;
    private String playerName;
    private String teamName;
    private Integer totalValue; // Generic name so we can reuse for assists/goals
}
