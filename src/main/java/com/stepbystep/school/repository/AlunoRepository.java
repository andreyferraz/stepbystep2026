package com.stepbystep.school.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stepbystep.school.model.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, UUID> {

}
