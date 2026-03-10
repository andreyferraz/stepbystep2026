package com.stepbystep.school.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stepbystep.school.model.MaterialEstudo;

public interface MaterialEstudoRepository extends JpaRepository<MaterialEstudo, UUID> {

    List<MaterialEstudo> findByTurmaId(UUID turmaId);

}
