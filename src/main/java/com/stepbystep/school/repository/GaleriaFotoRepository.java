package com.stepbystep.school.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stepbystep.school.model.GaleriaFoto;

public interface GaleriaFotoRepository extends JpaRepository<GaleriaFoto, UUID> {

	long countByCategoriaId(UUID categoriaId);

}
