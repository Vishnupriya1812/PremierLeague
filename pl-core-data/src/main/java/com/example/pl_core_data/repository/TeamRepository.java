package com.example.pl_core_data.repository;
import com.example.pl_core_data.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {
    // Helpful for checking if we already know this team
    boolean existsById(Integer id);
    Optional<Team> findByNameIgnoreCase(String name);;
}
