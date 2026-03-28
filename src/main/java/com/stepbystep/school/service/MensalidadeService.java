package com.stepbystep.school.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.stepbystep.school.enums.StatusMensalidade;
import com.stepbystep.school.model.Mensalidade;
import com.stepbystep.school.repository.MensalidadeRepository;
import com.stepbystep.school.util.ValidationUtils;

@Service
public class MensalidadeService {

    private static final String CAMPO_ID_ALUNO = "ID do aluno";
    private static final String CAMPO_ID_MENSALIDADE = "ID da mensalidade";
    private static final String CAMPO_TXID_PIX = "TXID PIX";
    private static final String MSG_MENSALIDADE_NAO_ENCONTRADA = "Mensalidade não encontrada com ID: ";
    private static final String PIX_REFERENCE_LABEL_ESTATICO = "***";

    private final AlunoService alunoService;
    private final MensalidadeRepository mensalidadeRepository;
    private final MercadoPagoPixApiService mercadoPagoPixApiService;

    @Value("${pix.chave:00000000000}")
    private String pixChave;

    @Value("${pix.recebedor:STEP BY STEP SCHOOL}")
    private String pixRecebedor;

    @Value("${pix.cidade:SAO PAULO}")
    private String pixCidade;

    @Value("${mercadopago.api.enabled:true}")
    private boolean mercadoPagoApiEnabled;

    public MensalidadeService(
        AlunoService alunoService,
        MensalidadeRepository mensalidadeRepository,
        MercadoPagoPixApiService mercadoPagoPixApiService
    ) {
        this.alunoService = alunoService;
        this.mensalidadeRepository = mensalidadeRepository;
        this.mercadoPagoPixApiService = mercadoPagoPixApiService;
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

    public Mensalidade gerarPix(UUID alunoId, UUID mensalidadeId) {
        ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
        ValidationUtils.validarCampoObrigatorio(mensalidadeId, CAMPO_ID_MENSALIDADE);
        alunoService.obterAlunoPorId(alunoId);

        Mensalidade mensalidade = mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_MENSALIDADE_NAO_ENCONTRADA + mensalidadeId));

        if (mensalidade.getStatus() == StatusMensalidade.PAGO) {
            throw new IllegalStateException("Não é possível gerar PIX para mensalidade já paga");
        }

        if (mensalidade.getStatus() == null) {
            mensalidade.setStatus(StatusMensalidade.PENDENTE);
        }

        String txid = gerarTxidMensalidade(mensalidade.getId());
        if (mercadoPagoApiEnabled) {
            MercadoPagoPixApiService.DynamicPixPayload payload = mercadoPagoPixApiService
                .criarCobrancaPix(mensalidade, txid);
            mensalidade.setPixCopiaECola(payload.pixCopiaECola());
            mensalidade.setPixTxid(payload.txid());
        } else {
            mensalidade.setPixCopiaECola(gerarPixCopiaECola(mensalidade.getValor()));
            mensalidade.setPixTxid(txid);
        }
        mensalidade = mensalidadeRepository.save(mensalidade);

        return mensalidade;
    }

    public String gerarQrCodeBase64(String pixCopiaECola) {
        ValidationUtils.validarCampoStringObrigatorio(pixCopiaECola, "Payload PIX");
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(pixCopiaECola, BarcodeFormat.QR_CODE, 300, 300);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Não foi possível gerar o QR Code do PIX", e);
        }
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
        LocalDate dataBase = dataPagamento == null ? LocalDate.now() : dataPagamento;
        mensalidade.setDataPagamento(dataBase.atTime(LocalDateTime.now().toLocalTime()));

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

    public Mensalidade confirmarPagamentoPorTxid(String txid, LocalDateTime dataPagamento) {
        ValidationUtils.validarCampoStringObrigatorio(txid, CAMPO_TXID_PIX);

        Mensalidade mensalidade = mensalidadeRepository.findByPixTxid(txid.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Mensalidade não encontrada para o TXID informado."));

        if (mensalidade.getStatus() == StatusMensalidade.PAGO) {
            return mensalidade;
        }

        mensalidade.setStatus(StatusMensalidade.PAGO);
        mensalidade.setDataPagamento(dataPagamento == null ? LocalDateTime.now() : dataPagamento);
        return mensalidadeRepository.save(mensalidade);
    }

    private String gerarPixCopiaECola(BigDecimal valor) {
        ValidationUtils.validarCampoObrigatorio(valor, "Valor da mensalidade");
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da mensalidade deve ser maior que zero");
        }

        // Para QR estático por chave PIX, usar "***" evita rejeição por PSP tentando validar cobrança dinâmica inexistente.
        String referenceLabel = PIX_REFERENCE_LABEL_ESTATICO;

        String merchantAccountInfo = campo("00", "br.gov.bcb.pix") + campo("01", pixChave);
        String payloadSemCrc = campo("00", "01")
                + campo("26", merchantAccountInfo)
                + campo("52", "0000")
                + campo("53", "986")
                + campo("54", valor.setScale(2, RoundingMode.HALF_UP).toPlainString())
                + campo("58", "BR")
                + campo("59", limitarTexto(pixRecebedor, 25))
                + campo("60", limitarTexto(pixCidade, 15))
                + campo("62", campo("05", referenceLabel))
                + "6304";

        String crc = calcularCrc16(payloadSemCrc);
        return payloadSemCrc + crc;
    }

    private String gerarTxidMensalidade(UUID mensalidadeId) {
        String txidBase = mensalidadeId.toString().replace("-", "").toUpperCase(Locale.ROOT);
        return limitarTexto(txidBase, 25);
    }

    private String campo(String id, String valor) {
        int tamanhoUtf8 = valor.getBytes(StandardCharsets.UTF_8).length;
        String tamanho = String.format("%02d", tamanhoUtf8);
        return id + tamanho + valor;
    }

    private String limitarTexto(String valor, int tamanhoMaximo) {
        String normalizado = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (normalizado.length() > tamanhoMaximo) {
            return normalizado.substring(0, tamanhoMaximo);
        }
        return normalizado;
    }

    private String calcularCrc16(String payload) {
        int polinomio = 0x1021;
        int resultado = 0xFFFF;
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            resultado ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((resultado & 0x8000) != 0) {
                    resultado = (resultado << 1) ^ polinomio;
                } else {
                    resultado <<= 1;
                }
                resultado &= 0xFFFF;
            }
        }
        return String.format("%04X", resultado);
    }

}
