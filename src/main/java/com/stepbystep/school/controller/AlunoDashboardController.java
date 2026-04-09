package com.stepbystep.school.controller;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.security.Principal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.stepbystep.school.enums.StatusComprovantePagamento;
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
    private static final String ALUNO_DASHBOARD_BOLETIM_MEDIA_PADRAO = "--";
    private static final String ALUNO_DASHBOARD_BOLETIM_FALTAS_PADRAO = "Sem faltas registradas.";
    private static final String FEEDBACK_FINANCEIRO = "alunoFinanceiroFeedback";

    private static final Locale LOCALE_PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATO_DATA_PADRAO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UsuarioRepository usuarioRepository;
    private final FileUploadService fileUploadService;
    private final MaterialEstudoService materialEstudoService;
    private final NotaService notaService;
    private final MensalidadeService mensalidadeService;

    @GetMapping("/aluno/dashboard")
    public String alunoDashboard(
        Model model,
        Principal principal,
        @RequestParam(name = "boletimBimestre", required = false) Integer boletimBimestre,
        @RequestParam(name = "painel", required = false) String painel
    ) {
        Usuario usuarioLogado = buscarUsuarioLogado(principal);
        List<MaterialEstudo> materiaisTurma = listarMateriaisTurmaAluno(usuarioLogado);
        List<Nota> notasAluno = listarNotasAluno(usuarioLogado);
        Integer bimestreFiltro = normalizarBimestreFiltro(boletimBimestre);
        String nomeAlunoLogado = extrairPrimeiroNomeAluno(usuarioLogado);
        AlunoDashboardResumo resumoDashboard = montarResumoAlunoDashboard(usuarioLogado, materiaisTurma, notasAluno);
        AlunoBoletimResumo boletimResumo = montarResumoBoletim(notasAluno, bimestreFiltro);
        List<AlunoFinanceiroMensalidadeView> mensalidadesFinanceiro = listarMensalidadesFinanceiroAluno(usuarioLogado);

        model.addAttribute("isDashboard", true);
        model.addAttribute("alunoContaSemVinculo", usuarioLogado != null && usuarioLogado.getAluno() == null);
        model.addAttribute("alunoPainelInicial", normalizarPainelInicial(painel));
        model.addAttribute("alunoNome", nomeAlunoLogado);
        model.addAttribute("alunoProximaAulaResumo", resumoDashboard.proximaAulaResumo());
        model.addAttribute("alunoMaterialNovoResumo", resumoDashboard.materialNovoResumo());
        model.addAttribute("alunoFrequenciaResumo", resumoDashboard.frequenciaResumo());
        model.addAttribute("alunoMensalidadeResumo", resumoDashboard.mensalidadeResumo());
        model.addAttribute("alunoMateriaisTurma", materiaisTurma);
        model.addAttribute("alunoBoletimMediaB1", boletimResumo.mediaB1());
        model.addAttribute("alunoBoletimMediaB2", boletimResumo.mediaB2());
        model.addAttribute("alunoBoletimMediaB3", boletimResumo.mediaB3());
        model.addAttribute("alunoBoletimMediaB4", boletimResumo.mediaB4());
        model.addAttribute("alunoBoletimFaltasResumo", boletimResumo.faltasResumo());
        model.addAttribute("alunoBoletimLancamentos", boletimResumo.lancamentos());
        model.addAttribute("alunoBoletimBimestreSelecionado", bimestreFiltro);
        model.addAttribute("alunoMensalidadesFinanceiro", mensalidadesFinanceiro);
        return "aluno/dashboard";
    }

    @PostMapping("/aluno/mensalidades/enviar-comprovante")
    public String enviarComprovantePagamento(
        @RequestParam("mensalidadeId") UUID mensalidadeId,
        @RequestParam("comprovante") MultipartFile comprovante,
        @RequestParam(name = "observacao", required = false) String observacao,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {
        try {
            Usuario usuarioLogado = buscarUsuarioLogado(principal);
            if (usuarioLogado == null || usuarioLogado.getAluno() == null || usuarioLogado.getAluno().getId() == null) {
                throw new IllegalArgumentException("Conta de aluno sem vínculo. Procure a secretaria.");
            }

            if (comprovante == null || comprovante.isEmpty()) {
                throw new IllegalArgumentException("Selecione um comprovante para envio.");
            }

            String nomeArquivo = fileUploadService.salvarComprovante(comprovante);
            mensalidadeService.enviarComprovantePagamento(
                usuarioLogado.getAluno().getId(),
                mensalidadeId,
                nomeArquivo,
                observacao
            );

            redirectAttributes.addFlashAttribute(FEEDBACK_FINANCEIRO,
                "Comprovante enviado com sucesso. Aguarde a validação da secretaria.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_FINANCEIRO, ex.getMessage());
        }

        return "redirect:/aluno/dashboard?painel=financeiro";
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

    private AlunoDashboardResumo montarResumoAlunoDashboard(
        Usuario usuarioLogado,
        List<MaterialEstudo> materiaisTurma,
        List<Nota> notasAluno
    ) {
        if (usuarioLogado == null || usuarioLogado.getAluno() == null || usuarioLogado.getAluno().getId() == null) {
            return AlunoDashboardResumo.padrao();
        }

        Aluno alunoLogado = usuarioLogado.getAluno();
        Turma turmaAluno = alunoLogado.getTurma();

        return new AlunoDashboardResumo(
            montarResumoProximaAula(turmaAluno),
            montarResumoMaterialNovo(materiaisTurma),
            montarResumoFrequencia(notasAluno),
            montarResumoMensalidade(alunoLogado.getId())
        );
    }

    private List<Nota> listarNotasAluno(Usuario usuarioLogado) {
        if (usuarioLogado == null || usuarioLogado.getAluno() == null || usuarioLogado.getAluno().getId() == null) {
            return List.of();
        }

        return notaService.listarNotasPorAluno(usuarioLogado.getAluno().getId());
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

    private String montarResumoFrequencia(List<Nota> notasAluno) {
        long registrosPresenca = notasAluno.stream()
            .filter(nota -> !normalizarTextoOpcional(nota.getPresenca()).isBlank())
            .count();

        if (registrosPresenca == 0) {
            return ALUNO_DASHBOARD_FREQUENCIA_PADRAO;
        }

        long mediaPresenca = Math.round(notaService.calcularMediaPresenca(notasAluno));
        return mediaPresenca + "% de presença.";
    }

    private AlunoBoletimResumo montarResumoBoletim(List<Nota> notasAluno, Integer bimestreFiltro) {
        if (notasAluno == null || notasAluno.isEmpty()) {
            return AlunoBoletimResumo.padrao();
        }

        String mediaB1 = montarMediaPorBimestre(notasAluno, 1);
        String mediaB2 = montarMediaPorBimestre(notasAluno, 2);
        String mediaB3 = montarMediaPorBimestre(notasAluno, 3);
        String mediaB4 = montarMediaPorBimestre(notasAluno, 4);

        List<Nota> notasFiltradas = notasAluno.stream()
            .filter(nota -> bimestreFiltro == null || Objects.equals(nota.getBimestre(), bimestreFiltro))
            .toList();

        long faltas = notasFiltradas.stream()
            .filter(nota -> "FALTA".equalsIgnoreCase(normalizarTextoOpcional(nota.getPresenca())))
            .count();

        String faltasResumo = montarResumoFaltas(faltas, bimestreFiltro);

        List<AlunoBoletimLancamentoView> lancamentos = notasFiltradas.stream()
            .sorted(Comparator.comparing(Nota::getDataReferencia, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(8)
            .map(this::montarLancamentoBoletim)
            .toList();

        return new AlunoBoletimResumo(mediaB1, mediaB2, mediaB3, mediaB4, faltasResumo, lancamentos);
    }

    private String montarResumoFaltas(long faltas, Integer bimestreFiltro) {
        String sufixoBimestre = bimestreFiltro == null ? "" : " no B" + bimestreFiltro;

        if (faltas == 0) {
            return "Sem faltas registradas" + sufixoBimestre + ".";
        }

        String sufixoPlural = faltas == 1 ? "falta registrada" : "faltas registradas";
        return faltas + " " + sufixoPlural + sufixoBimestre + ".";
    }

    private String normalizarPainelInicial(String painel) {
        if ("materiais".equalsIgnoreCase(painel)) {
            return "materiais";
        }

        if ("boletim".equalsIgnoreCase(painel)) {
            return "boletim";
        }

        if ("financeiro".equalsIgnoreCase(painel)) {
            return "financeiro";
        }

        return "painel";
    }

    private Integer normalizarBimestreFiltro(Integer bimestre) {
        if (bimestre == null) {
            return null;
        }

        if (bimestre < 1 || bimestre > 4) {
            return null;
        }

        return bimestre;
    }

    private String montarMediaPorBimestre(List<Nota> notasAluno, int bimestre) {
        double media = notasAluno.stream()
            .filter(nota -> Objects.equals(nota.getBimestre(), bimestre))
            .map(Nota::getValor)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(Double.NaN);

        if (Double.isNaN(media)) {
            return ALUNO_DASHBOARD_BOLETIM_MEDIA_PADRAO;
        }

        return String.format(LOCALE_PT_BR, "%.1f", media);
    }

    private AlunoBoletimLancamentoView montarLancamentoBoletim(Nota nota) {
        String atividade = normalizarTextoOpcional(nota.getAtividade()).isBlank()
            ? "Registro de aula"
            : nota.getAtividade().trim();

        String notaResumo = nota.getValor() == null
            ? "Sem nota lançada."
            : "Nota: " + String.format(LOCALE_PT_BR, "%.1f", nota.getValor());

        String presencaNormalizada = normalizarTextoOpcional(nota.getPresenca());
        String presencaResumo = presencaNormalizada.isBlank()
            ? "Presença não informada."
            : "Presença: " + formatarPresenca(presencaNormalizada);

        String dataResumo = nota.getDataReferencia() == null
            ? "Data não informada"
            : nota.getDataReferencia().format(FORMATO_DATA_PADRAO);

        String descricao = normalizarTextoOpcional(nota.getDescricao());
        return new AlunoBoletimLancamentoView(atividade, notaResumo, presencaResumo, dataResumo, descricao);
    }

    private String formatarPresenca(String presenca) {
        String normalizada = presenca.trim().toUpperCase(Locale.ROOT);
        if ("PRESENTE".equals(normalizada)) {
            return "Presente";
        }

        if ("FALTA".equals(normalizada)) {
            return "Falta";
        }

        if ("JUSTIFICADA".equals(normalizada)) {
            return "Justificada";
        }

        return presenca;
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

    private List<AlunoFinanceiroMensalidadeView> listarMensalidadesFinanceiroAluno(Usuario usuarioLogado) {
        if (usuarioLogado == null || usuarioLogado.getAluno() == null || usuarioLogado.getAluno().getId() == null) {
            return List.of();
        }

        return mensalidadeService.listarMensalidadesPorAluno(usuarioLogado.getAluno().getId()).stream()
            .sorted(Comparator.comparing(Mensalidade::getDataVencimento, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(this::montarMensalidadeFinanceiroView)
            .toList();
    }

    private AlunoFinanceiroMensalidadeView montarMensalidadeFinanceiroView(Mensalidade mensalidade) {
        String competencia = mensalidade.getDataVencimento() == null
            ? "Competência não definida"
            : mensalidade.getDataVencimento().format(DateTimeFormatter.ofPattern("MM/yyyy"));

        String vencimento = mensalidade.getDataVencimento() == null
            ? "Vencimento não definido"
            : mensalidade.getDataVencimento().format(FORMATO_DATA_PADRAO);

        String valor = formatarMoeda(mensalidade.getValor());
        String statusPagamento = mensalidade.getStatus() == null ? "PENDENTE" : mensalidade.getStatus().name();

        String statusComprovante;
        if (mensalidade.getComprovanteStatus() == StatusComprovantePagamento.PENDENTE) {
            statusComprovante = "Comprovante em análise";
        } else if (mensalidade.getComprovanteStatus() == StatusComprovantePagamento.REJEITADO) {
            statusComprovante = "Comprovante rejeitado";
        } else if (mensalidade.getComprovanteStatus() == StatusComprovantePagamento.APROVADO) {
            statusComprovante = "Comprovante aprovado";
        } else {
            statusComprovante = "Sem comprovante enviado";
        }

        boolean permiteEnvioComprovante = mensalidade.getStatus() != StatusMensalidade.PAGO
            && mensalidade.getComprovanteStatus() != StatusComprovantePagamento.PENDENTE;

        return new AlunoFinanceiroMensalidadeView(
            mensalidade.getId(),
            competencia,
            valor,
            vencimento,
            statusPagamento,
            statusComprovante,
            permiteEnvioComprovante
        );
    }

    private String normalizarTextoOpcional(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String formatarMoeda(BigDecimal valor) {
        BigDecimal seguro = valor == null ? BigDecimal.ZERO : valor;
        NumberFormat formatador = NumberFormat.getCurrencyInstance(LOCALE_PT_BR);
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

    private record AlunoBoletimResumo(
        String mediaB1,
        String mediaB2,
        String mediaB3,
        String mediaB4,
        String faltasResumo,
        List<AlunoBoletimLancamentoView> lancamentos
    ) {
        private static AlunoBoletimResumo padrao() {
            return new AlunoBoletimResumo(
                ALUNO_DASHBOARD_BOLETIM_MEDIA_PADRAO,
                ALUNO_DASHBOARD_BOLETIM_MEDIA_PADRAO,
                ALUNO_DASHBOARD_BOLETIM_MEDIA_PADRAO,
                ALUNO_DASHBOARD_BOLETIM_MEDIA_PADRAO,
                ALUNO_DASHBOARD_BOLETIM_FALTAS_PADRAO,
                List.of()
            );
        }
    }

    private record AlunoBoletimLancamentoView(
        String atividade,
        String notaResumo,
        String presencaResumo,
        String dataResumo,
        String descricao
    ) {
    }

    private record AlunoFinanceiroMensalidadeView(
        UUID id,
        String competencia,
        String valor,
        String vencimento,
        String statusPagamento,
        String statusComprovante,
        boolean permiteEnvioComprovante
    ) {
    }
}
