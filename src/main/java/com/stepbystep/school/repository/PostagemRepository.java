package com.stepbystep.school.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stepbystep.school.enums.StatusPostagem;
import com.stepbystep.school.model.Postagem;

public interface PostagemRepository extends JpaRepository<Postagem, UUID> {

	List<Postagem> findAllByOrderByDataPublicacaoDesc();

	List<Postagem> findByStatusOrderByDataPublicacaoDesc(StatusPostagem status);

	Optional<Postagem> findBySlugAndStatus(String slug, StatusPostagem status);

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, UUID id);

}
