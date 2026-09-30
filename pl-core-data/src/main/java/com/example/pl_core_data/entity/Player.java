package com.example.pl_core_data.entity;

import com.example.pl_core_data.DTO.TopPerformersDTO;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "players")
@Data
@SqlResultSetMapping(
        name = "TopScorerMapping",
        classes = @ConstructorResult(
                targetClass = TopPerformersDTO.class,
                columns = {
                        @ColumnResult(name = "id",type = Integer.class),
                        @ColumnResult(name = "playerName"),
                        @ColumnResult(name = "teamName"),
                        @ColumnResult(name = "totalValue", type = Integer.class)
                }
        )
)
public class Player {
    @Id
    private Integer id;
    private String name;
    private String slug;
    private String position; // G, D, M, F
    private Integer height;
    private String country;
    private String marketValue;


}
