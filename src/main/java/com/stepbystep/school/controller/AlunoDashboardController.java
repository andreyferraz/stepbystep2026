package com.stepbystep.school.controller;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.security.Principal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.stepbystep.school.enums.StatusMensalidade;
import com.stepbystep.school.model.Aluno;
import com.stepbystep.school.model.MaterialEstudo;
import com.stepbystep.school.model.Mensalidade;
import com.stepbystep.school.model.Nota;
import com.stepbystep.school.model.Turma;
import com.stepbystep.school.model.Usuario;
import com.stepbystep.school.repository.UsuarioRepository;
import com.stepbystep.school.service.FileUploadService;
import com.stepbystep.school.service.MaterialEstudoService;
import com.stepbystep.school.service.MensalidadeService;
import com.stepbystep.school.service.NotaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AlunoDashboardController {

    private static final String ALUNO_DASHBOARD_NOME_PADRAO = "Aluno";
    private static final String ALUNO_DASHBOARD_PROXIMA_AULA_PADRAO = "Turma ainda não definida.";
    private static final String ALUNO_DASHBOARD_MATERIAL_PADRAO = "Nenhum material disponível no momento.";
    private static final String ALUNO_DASHBOARD_FREQUENCIA_PADRAO = "Sem registros de presença.";
    private static final String ALUNO_DASHBOARD_MENSALIDADE_PADRAO = "Nenhuma cobrança pendente.";

    private final UsuarioRepository usuarioRepository;
    private final FileUploadService fileUploadService;
    private final MaterialEstudoService materialEstudoService;
    private final NotaService notaService;
    private final MensalidadeService mensalidadeService;

    @GetMapping("/aluno/dashboard")
    public String alunoDashboard(Model model, Principal principal) {
        Usuario usuarioLogado = buscarUsuarioLogado(principal);
        List<MaterialEstudo> materiaisTurma = listarMateriaisTurmaAluno(usuarioLogado);
        String nomeAlunoLogado = extrairPrimeiroNomeAluno(usuarioLogado);
        AlunoDashboardResumo resumoDashboard = montarResumoAlunoDashboard(usuarioLogado, materiaisTurma);

        model.addAttribute("isDashboard", true);
        model.addAttribute("alunoNome", nomeAlunoLogado);
        model.addAttribute("alunoProximaAulaResumo", resumoDashboard.proximaAulaResumo());
        model.addAttribute("alunoMaterialNovoResumo", resumoDashboard.materialNovoResumo());
        model.addAttribute("alunoFrequenciaResumo", resumoDashboard.frequenciaResumo());
        model.addAttribute("alunoMensalidadeResumo", resumoDashboard.mensalidadeResumo());
        model.addAttribute("alunoMateriaisTurma", materiaisTurma);
        return "aluno/dashboard";
    }

    @GetMapping("/aluno/materiais/arquivo/{materialId}")
    public ResponseEntity<Resource> baixarArquivoMaterialAluno(
        @PathVariable("materialId") UUID materialId,
        Principal principal
    ) {
        Usuario usuarioLogado = buscarUsuarioLogado(principal);
        if (!materialPertenceATurmaDoAluno(materialId, usuarioLogado)) {
            return ResponseEntity.status(403).build();
        }

        try {
            MaterialEstudo material = materialEstudoService.obterPorId(materialId);
            Path caminhoArquivo = fileUploadService.getCaminhoCompleto(material.getUrlArquivo());

            if (!Files.exists(caminhoArquivo)) {
                return ResponseEntity.notFound().build();
            }

            Resource recurso = new UrlResource(caminhoArquivo.toUri());
            String contentType = Files.probeContentType(caminhoArquivo);

            return ResponseEntity.ok()
                .contentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment().filename(material.getUrlArquivo()).build().toString())
                .body(recurso);
        } catch (MalformedURLException ex) {
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private Usuario buscarUsuarioLogado(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return null;
        }

        String email = principal.getName().trim().toLowerCase(Locale.ROOT);
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    private String extrairPrimeiroNomeAluno(Usuario usuarioLogado) {
        if (usuarioLogado == null) {
            return ALUNO_DASHBOARD_NOME_PADRAO;
        }

        String nomeCompleto = usuarioLogado.getAluno() != null && usuarioLogado.getAluno().getNome() != null
            ? usuarioLogado.getAluno().getNome()
            : usuarioLogado.getNome();

        String nomeNormalizado = normalizarTextoOpcional(nomeCompleto);
        if (nomeNormalizado.isBlank()) {
            return ALUNO_DASHBOARD_NOME_PADRAO;
        }

        return nomeNormalizado.split("\\s+")[0];
    }

    private AlunoDashboardResumo montarResumoAlunoDashboard(Usuario usuarioLogado, List<MaterialEstudo> materiaisTurma) {
        if (usuarioLogado == null || usuarioLogado.getAluno() == null || usuarioLogado.getAluno().getId() == null) {
            return AlunoDashboardResumo.padrao();
        }

        Aluno alunoLogado = usuarioLogado.getAluno();
        Turma turmaAluno = alunoLogado.getTurma();

        return new AlunoDashboardResumo(
            montarResumoProximaAula(turmaAluno),
            montarResumoMaterialNovo(materiaisTurma),
            montarResumoFrequencia(alunoLogado.getId()),
            montarResumoMensalidade(alunoLogado.getId())
        );
    }

    private List<MaterialEstudo> listarMateriaisTurmaAluno(Usuario usuarioLogado) {
        if (usuarioLogado == null
            || usuarioLogado.getAluno() == null
            || usuarioLogado.getAluno().getTurma() == null
            || usuarioLogado.getAluno().getTurma().getId() == null) {
            return List.of();
        }

        return materialEstudoService.listarPorTurma(usuarioLogado.getAluno().getTurma().getId()).stream()
            .sorted(Comparator.comparing(MaterialEstudo::getDataUpload,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    private String montarResumoProximaAula(Turma turmaAluno) {
        if (turmaAluno == null) {
            return ALUNO_DASHBOARD_PROXIMA_AULA_PADRAO;
        }

        String diasSemana = normalizarTextoOpcional(turmaAluno.getDiasSemana());
        String horario = normalizarTextoOpcional(turmaAluno.getHorario());

        if (!diasSemana.isBlank() && !horario.isBlank()) {
            return diasSemana + ", " + horario;
        }

        if (!horario.isBlank()) {
            return horario;
        }

        if (!diasSemana.isBlank()) {
            return diasSemana;
        }

        return ALUNO_DASHBOARD_PROXIMA_AULA_PADRAO;
    }

    private String montarResumoMaterialNovo(List<MaterialEstudo> materiaisTurma) {
        if (materiaisTurma == null || materiaisTurma.isEmpty()) {
            return ALUNO_DASHBOARD_MATERIAL_PADRAO;
        }

        long materiaisNovos = materialEstudoService.contarUploadsUltimosDias(materiaisTurma, 7);
        return materiaisNovos > 0
            ? materiaisNovos + " novo(s) nos últimos 7 dias."
            : materiaisTurma.size() + " material(is) disponível(is).";
    }

    private boolean materialPertenceATurmaDoAluno(UUID materialId, Usuario usuarioLogado) {
        if (materialId == null
            || usuarioLogado == null
            || usuarioLogado.getAluno() == null
            || usuarioLogado.getAluno().getTurma() == null
            || usuarioLogado.getAluno().getTurma().getId() == null) {
            return false;
        }

        MaterialEstudo material = materialEstudoService.obterPorId(materialId);
        return material.getTurma() != null
            && material.getTurma().getId() != null
            && usuarioLogado.getAluno().getTurma().getId().equals(material.getTurma().getId());
    }

    private String montarResumoFrequencia(UUID alunoId) {
        List<Nota> notasAluno = notaService.listarNotasFiltradas(null, null, null).stream()
            .filter(nota -> nota.getAluno() != null && alunoId.equals(nota.getAluno().getId()))
            .toList();

        long registrosPresenca = notasAluno.stream()
            .filter(nota -> !normalizarTextoOpcional(nota.getPresenca()).isBlank())
            .count();

        if (registrosPresenca == 0) {
            return ALUNO_DASHBOARD_FREQUENCIA_PADRAO;
        }

        long mediaPresenca = Math.round(notaService.calcularMediaPresenca(notasAluno));
        return mediaPresenca + "% de presença.";
    }

    private String montarResumoMensalidade(UUID alunoId) {
        Mensalidade proximaMensalidade = mensalidadeService.listarMensalidadesPorAluno(alunoId).stream()
            .filter(mensalidade -> mensalidade.getStatus() != StatusMensalidade.PAGO)
            .filter(mensalidade -> mensalidade.getDataVencimento() != null)
            .min(Comparator.comparing(Mensalidade::getDataVencimento))
            .orElse(null);

        if (proximaMensalidade == null) {
            return ALUNO_DASHBOARD_MENSALIDADE_PADRAO;
        }

        long diasParaVencer = ChronoUnit.DAYS.between(LocalDate.now(), proximaMensalidade.getDataVencimento());
        String valorFormatado = formatarMoeda(proximaMensalidade.getValor());

        if (diasParaVencer < 0) {
            return valorFormatado + " em atraso há " + Math.abs(diasParaVencer) + " dia(s).";
        }

        if (diasParaVencer == 0) {
            return valorFormatado + " vence hoje.";
        }

        return valorFormatado + " vence em " + diasParaVencer + " dia(s).";
    }

    private String normalizarTextoOpcional(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String formatarMoeda(BigDecimal valor) {
        BigDecimal seguro = valor == null ? BigDecimal.ZERO : valor;
        NumberFormat formatador = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        return formatador.format(seguro);
    }

    private record AlunoDashboardResumo(
        String proximaAulaResumo,
        String materialNovoResumo,
        String frequenciaResumo,
        String mensalidadeResumo
    ) {
        private static AlunoDashboardResumo padrao() {
            return new AlunoDashboardResumo(
                ALUNO_DASHBOARD_PROXIMA_AULA_PADRAO,
                ALUNO_DASHBOARD_MATERIAL_PADRAO,
                ALUNO_DASHBOARD_FREQUENCIA_PADRAO,
                ALUNO_DASHBOARD_MENSALIDADE_PADRAO
            );
        }
    }
}
