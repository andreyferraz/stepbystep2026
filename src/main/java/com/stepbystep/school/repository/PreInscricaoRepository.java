package com.stepbystep.school.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stepbystep.school.model.PreInscricao;

public interface PreInscricaoRepository extends JpaRepository<PreInscricao, UUID> {

	java.util.List<PreInscricao> findAllByOrderByDataLeadDesc();

}
