package com.example.pl_core_data.repository;

import com.example.pl_core_data.entity.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Integer> {
    boolean existsById(Integer id);
}
