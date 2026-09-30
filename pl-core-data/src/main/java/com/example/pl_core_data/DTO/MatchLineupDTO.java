package com.example.pl_core_data.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchLineupDTO {
    private Integer id;
    private String playerName;  // From Join
    private String teamName;    // From Join
    private String position;
    private Double rating;
}
