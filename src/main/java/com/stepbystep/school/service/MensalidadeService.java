package com.stepbystep.school.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import com.stepbystep.school.enums.StatusMensalidade;
import com.stepbystep.school.enums.StatusComprovantePagamento;
import com.stepbystep.school.model.Mensalidade;
import com.stepbystep.school.repository.MensalidadeRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class MensalidadeService {

    private static final String CAMPO_ID_ALUNO = "ID do aluno";
    private static final String CAMPO_ID_MENSALIDADE = "ID da mensalidade";
    private static final String MSG_MENSALIDADE_NAO_ENCONTRADA = "Mensalidade não encontrada com ID: ";

    private final AlunoService alunoService;
    private final MensalidadeRepository mensalidadeRepository;

    public MensalidadeService(
        AlunoService alunoService,
        MensalidadeRepository mensalidadeRepository
    ) {
        this.alunoService = alunoService;
        this.mensalidadeRepository = mensalidadeRepository;
    }

    public List<Mensalidade> listarMensalidadesPorAluno(UUID alunoId) {
        ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
        alunoService.obterAlunoPorId(alunoId);
        return mensalidadeRepository.findByAlunoIdOrderByDataVencimentoAsc(alunoId);
    }

    public Mensalidade criarMensalidade(UUID alunoId, BigDecimal valor, LocalDate dataVencimento) {
        ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
        ValidationUtils.validarCampoObrigatorio(valor, "Valor da mensalidade");
        ValidationUtils.validarCampoObrigatorio(dataVencimento, "Data de vencimento");

        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da mensalidade deve ser maior que zero.");
        }

        Mensalidade mensalidade = Mensalidade.builder()
            .aluno(alunoService.obterAlunoPorId(alunoId))
            .valor(valor.setScale(2, RoundingMode.HALF_UP))
            .dataVencimento(dataVencimento)
            .status(StatusMensalidade.PENDENTE)
            .build();

        return mensalidadeRepository.save(mensalidade);
    }

    public List<Mensalidade> listarMensalidadesFinanceiro() {
        return mensalidadeRepository.findAll().stream()
            .sorted((a, b) -> {
                LocalDate da = a.getDataVencimento();
                LocalDate db = b.getDataVencimento();
                if (da == null && db == null) {
                    return 0;
                }
                if (da == null) {
                    return 1;
                }
                if (db == null) {
                    return -1;
                }
                return db.compareTo(da);
            })
            .toList();
    }

    public List<Mensalidade> listarMensalidadesCobraveis(List<Mensalidade> mensalidades) {
        return mensalidades.stream()
            .filter(mensalidade -> mensalidade.getStatus() != StatusMensalidade.PAGO)
            .toList();
    }

    public List<Mensalidade> listarComprovantesPendentes() {
        return mensalidadeRepository.findByComprovanteStatusOrderByComprovanteDataEnvioAsc(StatusComprovantePagamento.PENDENTE);
    }

    public List<Mensalidade> listarMensalidadesEmAtraso(List<Mensalidade> mensalidades, LocalDate hoje) {
        ValidationUtils.validarCampoObrigatorio(hoje, "Data de referência");
        return mensalidades.stream()
            .filter(mensalidade -> mensalidade.getStatus() != StatusMensalidade.PAGO)
            .filter(mensalidade -> mensalidade.getDataVencimento() != null)
            .filter(mensalidade -> mensalidade.getDataVencimento().isBefore(hoje))
            .toList();
    }

    public BigDecimal calcularTotalRecebidoNoMes(List<Mensalidade> mensalidades, YearMonth referencia) {
        return mensalidades.stream()
            .filter(mensalidade -> mensalidade.getStatus() == StatusMensalidade.PAGO)
            .filter(mensalidade -> mensalidade.getDataPagamento() != null)
            .filter(mensalidade -> YearMonth.from(mensalidade.getDataPagamento().toLocalDate()).equals(referencia))
            .map(Mensalidade::getValor)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularTotalAReceberNoMes(List<Mensalidade> mensalidades, YearMonth referencia) {
        return mensalidades.stream()
            .filter(mensalidade -> mensalidade.getStatus() != StatusMensalidade.PAGO)
            .filter(mensalidade -> mensalidade.getDataVencimento() != null)
            .filter(mensalidade -> YearMonth.from(mensalidade.getDataVencimento()).equals(referencia))
            .map(Mensalidade::getValor)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long contarMensalidadesAtrasadas(List<Mensalidade> mensalidades, LocalDate hoje) {
        return mensalidades.stream()
            .filter(mensalidade -> mensalidade.getStatus() != StatusMensalidade.PAGO)
            .filter(mensalidade -> mensalidade.getDataVencimento() != null)
            .filter(mensalidade -> mensalidade.getDataVencimento().isBefore(hoje))
            .count();
    }

    public int calcularTaxaAdimplencia(List<Mensalidade> mensalidades) {
        if (mensalidades.isEmpty()) {
            return 0;
        }

        long pagas = mensalidades.stream()
            .filter(mensalidade -> mensalidade.getStatus() == StatusMensalidade.PAGO)
            .count();

        return (int) Math.round((pagas * 100.0) / mensalidades.size());
    }

    public Mensalidade confirmarPagamento(UUID alunoId, UUID mensalidadeId) {
        ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
        ValidationUtils.validarCampoObrigatorio(mensalidadeId, CAMPO_ID_MENSALIDADE);
        alunoService.obterAlunoPorId(alunoId);

        Mensalidade mensalidade = mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_MENSALIDADE_NAO_ENCONTRADA + mensalidadeId));

        mensalidade.setStatus(StatusMensalidade.PAGO);
        mensalidade.setDataPagamento(LocalDateTime.now());
        return mensalidadeRepository.save(mensalidade);
    }

    public Mensalidade registrarPagamentoManual(UUID alunoId, UUID mensalidadeId, LocalDate dataPagamento) {
        ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
        ValidationUtils.validarCampoObrigatorio(mensalidadeId, CAMPO_ID_MENSALIDADE);
        alunoService.obterAlunoPorId(alunoId);

        Mensalidade mensalidade = mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_MENSALIDADE_NAO_ENCONTRADA + mensalidadeId));

        mensalidade.setStatus(StatusMensalidade.PAGO);
        mensalidade.setComprovanteStatus(StatusComprovantePagamento.APROVADO);
        LocalDate dataBase = dataPagamento == null ? LocalDate.now() : dataPagamento;
        mensalidade.setDataPagamento(dataBase.atTime(LocalDateTime.now().toLocalTime()));

        return mensalidadeRepository.save(mensalidade);
    }

    public Mensalidade enviarComprovantePagamento(UUID alunoId, UUID mensalidadeId, String comprovanteArquivo, String observacaoAluno) {
        ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
        ValidationUtils.validarCampoObrigatorio(mensalidadeId, CAMPO_ID_MENSALIDADE);
        ValidationUtils.validarCampoStringObrigatorio(comprovanteArquivo, "Arquivo do comprovante");
        alunoService.obterAlunoPorId(alunoId);

        Mensalidade mensalidade = mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)
            .orElseThrow(() -> new IllegalArgumentException(MSG_MENSALIDADE_NAO_ENCONTRADA + mensalidadeId));

        if (mensalidade.getStatus() == StatusMensalidade.PAGO) {
            throw new IllegalStateException("Esta mensalidade já está paga.");
        }

        mensalidade.setComprovanteArquivo(comprovanteArquivo.trim());
        mensalidade.setComprovanteObservacaoAluno(observacaoAluno == null ? "" : observacaoAluno.trim());
        mensalidade.setComprovanteDataEnvio(LocalDateTime.now());
        mensalidade.setComprovanteStatus(StatusComprovantePagamento.PENDENTE);

        return mensalidadeRepository.save(mensalidade);
    }

    public Mensalidade aprovarComprovantePagamento(UUID mensalidadeId) {
        ValidationUtils.validarCampoObrigatorio(mensalidadeId, CAMPO_ID_MENSALIDADE);

        Mensalidade mensalidade = mensalidadeRepository.findById(mensalidadeId)
            .orElseThrow(() -> new IllegalArgumentException(MSG_MENSALIDADE_NAO_ENCONTRADA + mensalidadeId));

        mensalidade.setStatus(StatusMensalidade.PAGO);
        mensalidade.setDataPagamento(LocalDateTime.now());
        mensalidade.setComprovanteStatus(StatusComprovantePagamento.APROVADO);

        return mensalidadeRepository.save(mensalidade);
    }

    public Mensalidade rejeitarComprovantePagamento(UUID mensalidadeId) {
        ValidationUtils.validarCampoObrigatorio(mensalidadeId, CAMPO_ID_MENSALIDADE);

        Mensalidade mensalidade = mensalidadeRepository.findById(mensalidadeId)
            .orElseThrow(() -> new IllegalArgumentException(MSG_MENSALIDADE_NAO_ENCONTRADA + mensalidadeId));

        if (mensalidade.getStatus() == StatusMensalidade.PAGO) {
            throw new IllegalStateException("Não é possível rejeitar comprovante de mensalidade já paga.");
        }

        mensalidade.setComprovanteStatus(StatusComprovantePagamento.REJEITADO);
        return mensalidadeRepository.save(mensalidade);
    }

    @Transactional
    public int registrarAcordo(
        UUID alunoId,
        UUID mensalidadeId,
        BigDecimal valorNegociado,
        int parcelas,
        LocalDate primeiroVencimento
    ) {
        ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
        ValidationUtils.validarCampoObrigatorio(mensalidadeId, CAMPO_ID_MENSALIDADE);
        ValidationUtils.validarCampoObrigatorio(valorNegociado, "Valor negociado");
        ValidationUtils.validarCampoObrigatorio(primeiroVencimento, "Primeiro vencimento");

        if (parcelas <= 0) {
            throw new IllegalArgumentException("Quantidade de parcelas inválida.");
        }

        if (valorNegociado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor negociado deve ser maior que zero.");
        }

        if (parcelas > 24) {
            throw new IllegalArgumentException("Quantidade máxima de parcelas permitida é 24.");
        }

        alunoService.obterAlunoPorId(alunoId);

        Mensalidade mensalidadeOriginal = mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)
            .orElseThrow(() -> new IllegalArgumentException(MSG_MENSALIDADE_NAO_ENCONTRADA + mensalidadeId));

        if (mensalidadeOriginal.getStatus() == StatusMensalidade.PAGO) {
            throw new IllegalStateException("Não é possível registrar acordo para mensalidade já paga.");
        }

        BigDecimal valorParcelaBase = valorNegociado
            .divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP);
        BigDecimal totalParcelas = valorParcelaBase.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal diferenca = valorNegociado.subtract(totalParcelas);

        List<Mensalidade> novasParcelas = new ArrayList<>();
        for (int i = 0; i < parcelas; i++) {
            BigDecimal valorParcela = i == 0
                ? valorParcelaBase.add(diferenca)
                : valorParcelaBase;

            Mensalidade parcela = Mensalidade.builder()
                .aluno(mensalidadeOriginal.getAluno())
                .valor(valorParcela)
                .dataVencimento(primeiroVencimento.plusMonths(i))
                .status(StatusMensalidade.PENDENTE)
                .build();

            novasParcelas.add(parcela);
        }

        mensalidadeRepository.delete(mensalidadeOriginal);
        mensalidadeRepository.saveAll(novasParcelas);

        return parcelas;
    }

    public void excluirMensalidade(UUID alunoId, UUID mensalidadeId) {
        ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
        ValidationUtils.validarCampoObrigatorio(mensalidadeId, CAMPO_ID_MENSALIDADE);
        alunoService.obterAlunoPorId(alunoId);

        Mensalidade mensalidade = mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_MENSALIDADE_NAO_ENCONTRADA + mensalidadeId));

        if (mensalidade.getStatus() == StatusMensalidade.PAGO) {
            throw new IllegalStateException("Não é possível excluir cobrança de mensalidade já paga.");
        }

        mensalidadeRepository.delete(mensalidade);
    }

}
