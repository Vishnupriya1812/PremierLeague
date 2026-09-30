package com.example.pl_core_data.repository;

import com.example.pl_core_data.entity.Player;
import jakarta.persistence.Id;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository

public interface PlayerRepository extends JpaRepository<Player, Integer> {

    /**
     * Find a player by their SofaScore ID.
     * Use this during ingestion to prevent duplicate player records.
     */
    @Id
    Optional<Player> findById(int id);

    /**
     * Find players by name.
     * Helpful for manual searches or debugging.
     */
    List<Player> findByNameContainingIgnoreCase(String name);

    /**
     * Find all players who play in a specific position (e.g., 'G', 'D', 'M', 'F').
     * Useful for building squad-list views.
     */
    List<Player> findByPosition(String position);


}