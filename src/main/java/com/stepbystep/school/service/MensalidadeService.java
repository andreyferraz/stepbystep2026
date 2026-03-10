package com.stepbystep.school.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

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
    private static final String MSG_MENSALIDADE_NAO_ENCONTRADA = "Mensalidade não encontrada com ID: ";

    private final AlunoService alunoService;
    private final MensalidadeRepository mensalidadeRepository;

    @Value("${pix.chave:00000000000}")
    private String pixChave;

    @Value("${pix.recebedor:STEP BY STEP SCHOOL}")
    private String pixRecebedor;

    @Value("${pix.cidade:SAO PAULO}")
    private String pixCidade;

    public MensalidadeService(AlunoService alunoService, MensalidadeRepository mensalidadeRepository) {
        this.alunoService = alunoService;
        this.mensalidadeRepository = mensalidadeRepository;
    }

    public List<Mensalidade> listarMensalidadesPorAluno(UUID alunoId) {
        ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
        alunoService.obterAlunoPorId(alunoId);
        return mensalidadeRepository.findByAlunoIdOrderByDataVencimentoAsc(alunoId);
    }

    public Mensalidade gerarPix(UUID alunoId, UUID mensalidadeId) {
        ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
        ValidationUtils.validarCampoObrigatorio(mensalidadeId, "ID da mensalidade");
        alunoService.obterAlunoPorId(alunoId);

        Mensalidade mensalidade = mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_MENSALIDADE_NAO_ENCONTRADA + mensalidadeId));

        if (mensalidade.getStatus() == StatusMensalidade.PAGO) {
            throw new IllegalStateException("Não é possível gerar PIX para mensalidade já paga");
        }

        if (mensalidade.getStatus() == null) {
            mensalidade.setStatus(StatusMensalidade.PENDENTE);
        }

        if (mensalidade.getPixCopiaECola() == null || mensalidade.getPixCopiaECola().isBlank()) {
            String txid = mensalidade.getId().toString().replace("-", "").substring(0, 25).toUpperCase(Locale.ROOT);
            mensalidade.setPixCopiaECola(gerarPixCopiaECola(mensalidade.getValor(), txid));
            mensalidade = mensalidadeRepository.save(mensalidade);
        }

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
        ValidationUtils.validarCampoObrigatorio(mensalidadeId, "ID da mensalidade");
        alunoService.obterAlunoPorId(alunoId);

        Mensalidade mensalidade = mensalidadeRepository.findByIdAndAlunoId(mensalidadeId, alunoId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_MENSALIDADE_NAO_ENCONTRADA + mensalidadeId));

        mensalidade.setStatus(StatusMensalidade.PAGO);
        mensalidade.setDataPagamento(LocalDateTime.now());
        return mensalidadeRepository.save(mensalidade);
    }

    private String gerarPixCopiaECola(BigDecimal valor, String txid) {
        ValidationUtils.validarCampoObrigatorio(valor, "Valor da mensalidade");
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da mensalidade deve ser maior que zero");
        }

        String merchantAccountInfo = campo("00", "br.gov.bcb.pix") + campo("01", pixChave);
        String payloadSemCrc = campo("00", "01")
                + campo("26", merchantAccountInfo)
                + campo("52", "0000")
                + campo("53", "986")
                + campo("54", valor.setScale(2, RoundingMode.HALF_UP).toPlainString())
                + campo("58", "BR")
                + campo("59", limitarTexto(pixRecebedor, 25))
                + campo("60", limitarTexto(pixCidade, 15))
                + campo("62", campo("05", limitarTexto(txid, 25)))
                + "6304";

        String crc = calcularCrc16(payloadSemCrc);
        return payloadSemCrc + crc;
    }

    private String campo(String id, String valor) {
        String tamanho = String.format("%02d", valor.length());
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
