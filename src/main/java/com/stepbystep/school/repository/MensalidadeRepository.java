package com.stepbystep.school.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stepbystep.school.enums.StatusComprovantePagamento;
import com.stepbystep.school.model.Mensalidade;

public interface MensalidadeRepository extends JpaRepository<Mensalidade, UUID> {

	List<Mensalidade> findByAlunoIdOrderByDataVencimentoAsc(UUID alunoId);

	Optional<Mensalidade> findByIdAndAlunoId(UUID mensalidadeId, UUID alunoId);

	List<Mensalidade> findByComprovanteStatusOrderByComprovanteDataEnvioAsc(StatusComprovantePagamento comprovanteStatus);

}
