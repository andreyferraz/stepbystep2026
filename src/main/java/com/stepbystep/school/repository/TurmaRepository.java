package com.stepbystep.school.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stepbystep.school.model.Turma;

public interface TurmaRepository extends JpaRepository<Turma, UUID> {

}
