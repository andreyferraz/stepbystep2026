package com.stepbystep.school.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stepbystep.school.model.Postagem;

public interface PostagemRepository extends JpaRepository<Postagem, UUID> {

}
