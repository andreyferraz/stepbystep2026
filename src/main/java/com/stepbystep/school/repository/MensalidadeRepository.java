package com.stepbystep.school.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stepbystep.school.model.Mensalidade;

public interface MensalidadeRepository extends JpaRepository<Mensalidade, UUID> {

}
