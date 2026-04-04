package com.stepbystep.school.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stepbystep.school.model.GaleriaCategoria;

public interface GaleriaCategoriaRepository extends JpaRepository<GaleriaCategoria, UUID> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);

    Optional<GaleriaCategoria> findByNomeIgnoreCase(String nome);

    List<GaleriaCategoria> findAllByOrderByNomeAsc();
}
