package com.example.pl_core_data.repository;
import com.example.pl_core_data.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Integer> {
    boolean existsByYear(String year);
    List<Season> findByYearIn(List<String> years);
    Optional<Season> findByYear(String year);
}
