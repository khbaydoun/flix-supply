package com.khalil.supply.line;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LineRepository extends JpaRepository<Line, Long> {

    @EntityGraph(attributePaths = "brand")
    Optional<Line> findByCode(String code);

    @EntityGraph(attributePaths = "brand")
    List<Line> findAllByOrderByCodeAsc();
}
