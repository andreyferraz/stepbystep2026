package com.stepbystep.school.controller;

import java.net.MalformedURLException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Controller;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.stepbystep.school.dto.AdminAlunoCadastroRequest;
import com.stepbystep.school.enums.NivelAtual;
import com.stepbystep.school.enums.Role;
import com.stepbystep.school.enums.StatusMensalidade;
import com.stepbystep.school.enums.StatusPostagem;
import com.stepbystep.school.model.Aluno;
import com.stepbystep.school.model.Livro;
import com.stepbystep.school.model.MaterialEstudo;
import com.stepbystep.school.model.Mensalidade;
import com.stepbystep.school.model.Nota;
import com.stepbystep.school.model.Postagem;
import com.stepbystep.school.model.PreInscricao;
import com.stepbystep.school.model.Turma;
import com.stepbystep.school.model.Usuario;
import com.stepbystep.school.repository.AlunoRepository;
import com.stepbystep.school.repository.UsuarioRepository;
import com.stepbystep.school.service.FileUploadService;
import com.stepbystep.school.service.LivroService;
import com.stepbystep.school.service.MaterialEstudoService;
import com.stepbystep.school.service.MensalidadeService;
import com.stepbystep.school.service.NotaService;
import com.stepbystep.school.service.PostagemService;
import com.stepbystep.school.service.PreInscricaoService;
import com.stepbystep.school.service.TurmaService;
import com.stepbystep.school.service.UsuarioService;
import com.stepbystep.school.util.ValidationUtils;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.time.ZoneId;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private static final String REDIRECT_ALUNOS_PANEL = "redirect:/admin/dashboard?panel=alunos";
    private static final String REDIRECT_TURMAS_PANEL = "redirect:/admin/dashboard?panel=turmas";
    private static final String REDIRECT_MATERIAIS_PANEL = "redirect:/admin/dashboard?panel=materiais";
    private static final String REDIRECT_NOTAS_PANEL = "redirect:/admin/dashboard?panel=notas";
    private static final String REDIRECT_MENSALIDADES_PANEL = "redirect:/admin/dashboard?panel=mensalidades";
    private static final String REDIRECT_INADIMPLENCIA_PANEL = "redirect:/admin/dashboard?panel=inadimplencia";
    private static final String REDIRECT_PRE_INSCRICOES_PANEL = "redirect:/admin/dashboard?panel=pre-inscricoes";
    private static final String REDIRECT_LIVROS_PANEL = "redirect:/admin/dashboard?panel=livros";
    private static final String CAMPO_ID_ALUNO = "ID do Aluno";
    private static final String CAMPO_ID_TURMA = "ID da Turma";
    private static final String CAMPO_ID_MATERIAL = "ID do Material";
    private static final String CAMPO_ID_MENSALIDADE = "ID da Mensalidade";
    private static final String FEEDBACK_MATERIAL_FORM = "materialFormFeedback";
    private static final String FEEDBACK_NOTAS_FORM = "notasFormFeedback";
    private static final String FEEDBACK_MENSALIDADE_FORM = "mensalidadeFormFeedback";
    private static final String FEEDBACK_MENSALIDADE_CRIACAO = "mensalidadeCriacaoFeedback";
    private static final String FEEDBACK_MENSALIDADE_COBRANCA = "mensalidadeCobrancaFeedback";
    private static final String FEEDBACK_INADIMPLENCIA_LEMBRETE = "inadimplenciaLembreteFeedback";
    private static final String FEEDBACK_INADIMPLENCIA_ACORDO = "inadimplenciaAcordoFeedback";
    private static final String FEEDBACK_PRE_INSCRICAO_FORM = "preInscricaoFormFeedback";
    private static final String FEEDBACK_PRE_INSCRICAO_CONTATO = "preInscricaoContatoFeedback";
    private static final String FEEDBACK_PRE_INSCRICAO_DELETE = "preInscricaoDeleteFeedback";
    private static final String FEEDBACK_LIVRO_FORM = "livroFormFeedback";
    private static final String FEEDBACK_LIVRO_EDIT = "livroEditFeedback";
    private static final String FEEDBACK_LIVRO_DELETE = "livroDeleteFeedback";
    private static final String MSG_ALUNO_NAO_ENCONTRADO = "Aluno não encontrado com ID: ";
    private static final String MSG_USUARIO_ALUNO_NAO_ENCONTRADO = "Usuário do aluno não encontrado.";

    private final TurmaService turmaService;
    private final AlunoRepository alunoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final MaterialEstudoService materialEstudoService;
    private final NotaService notaService;
    private final MensalidadeService mensalidadeService;
    private final FileUploadService fileUploadService;
    private final PreInscricaoService preInscricaoService;
    private final PostagemService postagemService;
    private final LivroService livroService;

    @GetMapping("/admin/dashboard")
    public String adminDashboard(
        @RequestParam(name = "panel", required = false) String panel,
        @RequestParam(name = "alunoBusca", required = false) String alunoBusca,
        @RequestParam(name = "turmaBusca", required = false) String turmaBusca,
        @RequestParam(name = "materialBusca", required = false) String materialBusca,
        @RequestParam(name = "materialTurmaId", required = false) String materialTurmaId,
        @RequestParam(name = "notaBusca", required = false) String notaBusca,
        @RequestParam(name = "notaTurmaId", required = false) String notaTurmaId,
        @RequestParam(name = "inadBusca", required = false) String inadBusca,
        @RequestParam(name = "inadFaixa", required = false) String inadFaixa,
        @RequestParam(name = "preBusca", required = false) String preBusca,
        @RequestParam(name = "preStatus", required = false) String preStatus,
        @RequestParam(name = "blogBusca", required = false) String blogBusca,
        @RequestParam(name = "blogStatus", required = false) String blogStatus,
        @RequestParam(name = "blogCategoria", required = false) String blogCategoria,
        @RequestParam(name = "livroBusca", required = false) String livroBusca,
        @RequestParam(name = "notaBimestre", required = false) Integer notaBimestre,
        Model model
    ) {
        String alunoBuscaNormalizada = alunoBusca == null ? "" : alunoBusca.trim().toLowerCase(Locale.ROOT);
        String inadBuscaNormalizada = inadBusca == null ? "" : inadBusca.trim().toLowerCase(Locale.ROOT);
        String inadFaixaNormalizada = inadFaixa == null ? "" : inadFaixa.trim().toLowerCase(Locale.ROOT);
        String blogBuscaNormalizada = blogBusca == null ? "" : blogBusca.trim().toLowerCase(Locale.ROOT);
        String blogStatusNormalizado = blogStatus == null ? "" : blogStatus.trim().toLowerCase(Locale.ROOT);
        String blogCategoriaNormalizada = blogCategoria == null ? "" : blogCategoria.trim().toLowerCase(Locale.ROOT);
        UUID materialTurmaIdFiltrada = parseUuidOpcional(materialTurmaId);
        UUID notaTurmaIdFiltrada = parseUuidOpcional(notaTurmaId);

        List<Usuario> usuariosAlunos = listarUsuariosAlunosFiltrados(alunoBuscaNormalizada);

        List<MaterialEstudo> materiaisEstudo = materialEstudoService
            .listarMateriaisFiltrados(materialBusca, materialTurmaIdFiltrada);

        List<Nota> notasLancadas = notaService.listarNotasFiltradas(notaBusca, notaTurmaIdFiltrada, notaBimestre);
        List<Nota> notasHoje = notaService.listarLancamentosDoDia(notasLancadas, LocalDate.now());
        List<NotaService.ResumoTurmaNotas> notasTurmasAtencao = notaService
            .listarResumoTurmasEmAtencao(notasLancadas, 7.0, 75.0);
        List<Mensalidade> mensalidadesFinanceiro = mensalidadeService.listarMensalidadesFinanceiro();
        List<Mensalidade> mensalidadesCobraveis = mensalidadeService.listarMensalidadesCobraveis(mensalidadesFinanceiro);
        List<Mensalidade> mensalidadesProximosVencimentos = listarProximosVencimentos(mensalidadesCobraveis, LocalDate.now());
        long mensalidadesPixGeradas = mensalidadesFinanceiro.stream()
            .filter(mensalidade -> mensalidade.getPixCopiaECola() != null && !mensalidade.getPixCopiaECola().isBlank())
            .count();
        long mensalidadesPagasMesQtd = mensalidadesFinanceiro.stream()
            .filter(mensalidade -> mensalidade.getStatus() == StatusMensalidade.PAGO)
            .filter(mensalidade -> mensalidade.getDataPagamento() != null)
            .filter(mensalidade -> YearMonth.from(mensalidade.getDataPagamento().toLocalDate()).equals(YearMonth.now()))
            .count();
        long mensalidadesPendentesQtd = mensalidadesCobraveis.size();
        BigDecimal mensalidadesRecebidoMes = mensalidadeService.calcularTotalRecebidoNoMes(mensalidadesFinanceiro, YearMonth.now());
        BigDecimal mensalidadesAReceberMes = mensalidadeService.calcularTotalAReceberNoMes(mensalidadesFinanceiro, YearMonth.now());
        long mensalidadesAtrasadas = mensalidadeService.contarMensalidadesAtrasadas(mensalidadesFinanceiro, LocalDate.now());
        InadimplenciaPainelResumo inadimplenciaResumo = montarResumoInadimplencia(
            mensalidadesFinanceiro,
            inadBuscaNormalizada,
            inadFaixaNormalizada,
            LocalDate.now()
        );
        int mensalidadesTaxaAdimplencia = mensalidadeService.calcularTaxaAdimplencia(mensalidadesFinanceiro);
        long turmasComMateriais = materiaisEstudo.stream()
            .map(MaterialEstudo::getTurma)
            .filter(turma -> turma != null && turma.getId() != null)
            .map(Turma::getId)
            .distinct()
            .count();
        List<PreInscricao> preInscricoes = preInscricaoService.listarPreInscricoesFiltradas(preBusca, preStatus);
        List<PreInscricaoPainelItem> preInscricoesPainel = montarPreInscricoesPainel(preInscricoes);
        List<PreInscricaoPainelItem> preInscricoesRecentes = preInscricoesPainel.stream().limit(3).toList();
        long preInscricoesPendentes = preInscricaoService.contarPendentes(preInscricoes);
        long preInscricoesContatadas = preInscricaoService.contarContatadas(preInscricoes);
        long preInscricoesNovasSemana = preInscricaoService.contarNovasUltimosDias(preInscricoes, 7);
        List<Postagem> postagensAdminTodas = postagemService.listarPostagensAdmin();
        List<Postagem> postagensAdminFiltradas = filtrarPostagensBlogAdmin(
            postagensAdminTodas,
            blogBuscaNormalizada,
            blogStatusNormalizado,
            blogCategoriaNormalizada
        );
        long postagensPublicadasQtd = contarPostagensPorStatus(postagensAdminTodas, StatusPostagem.PUBLICADO);
        long postagensRascunhoQtd = contarPostagensPorStatus(postagensAdminTodas, StatusPostagem.RASCUNHO);
        long postagensAgendadasQtd = contarPostagensPorStatus(postagensAdminTodas, StatusPostagem.AGENDADO);
        List<Postagem> postagensTop = listarTopPostagensPublicadas(postagensAdminTodas, 3);
        List<Postagem> postagensAgendadas = listarPostagensAgendadas(postagensAdminTodas, 3);
        List<Livro> livros = listarLivrosFiltrados(livroBusca);
        long livrosComLinkCompra = livros.stream()
            .filter(livro -> !normalizarTextoOpcional(livro.getLinkCompra()).isBlank())
            .count();
        long livrosSemLinkCompra = livros.size() - livrosComLinkCompra;
        int livroAnoMaisRecente = livros.stream()
            .mapToInt(Livro::getAnoLancamento)
            .max()
            .orElse(0);

        model.addAttribute("isDashboard", true);
        model.addAttribute("turmas", turmaService.listarTurmasFiltradas(turmaBusca));
        model.addAttribute("usuariosAlunos", usuariosAlunos);
        model.addAttribute("materiaisEstudo", materiaisEstudo);
        model.addAttribute("alunoBusca", textoFiltro(alunoBusca));
        model.addAttribute("turmaBusca", textoFiltro(turmaBusca));
        model.addAttribute("materialBusca", textoFiltro(materialBusca));
        model.addAttribute("materialTurmaId", textoFiltro(materialTurmaId));
        model.addAttribute("materiaisTotal", materiaisEstudo.size());
        model.addAttribute("materiaisTurmasCount", turmasComMateriais);
        model.addAttribute("materiaisUploadsSemana", materialEstudoService.contarUploadsUltimosDias(materiaisEstudo, 7));
        model.addAttribute("notaBusca", textoFiltro(notaBusca));
        model.addAttribute("notaTurmaId", textoFiltro(notaTurmaId));
        model.addAttribute("notaBimestre", notaBimestre == null ? "" : notaBimestre);
        model.addAttribute("notasLancadas", notasLancadas);
        model.addAttribute("notasHoje", notasHoje);
        model.addAttribute("notasTurmasAtencao", notasTurmasAtencao);
        model.addAttribute("notasMediaGeral", String.format(Locale.forLanguageTag("pt-BR"), "%.1f", notaService.calcularMediaNotas(notasLancadas)));
        model.addAttribute("notasPresencaMedia", Math.round(notaService.calcularMediaPresenca(notasLancadas)));
        model.addAttribute("notasAbaixo75", notaService.contarAlunosAbaixoDePresenca(notasLancadas, 75.0));
        model.addAttribute("notasLancamentosTotal", notasLancadas.size());
        model.addAttribute("mensalidadesLista", mensalidadesFinanceiro);
        model.addAttribute("mensalidadesCobraveis", mensalidadesCobraveis);
        model.addAttribute("mensalidadesProximosVencimentos", mensalidadesProximosVencimentos);
        model.addAttribute("mensalidadesPixGeradasQtd", mensalidadesPixGeradas);
        model.addAttribute("mensalidadesPagasMesQtd", mensalidadesPagasMesQtd);
        model.addAttribute("mensalidadesPendentesQtd", mensalidadesPendentesQtd);
        model.addAttribute("mensalidadesRecebidoMesFmt", formatarMoeda(mensalidadesRecebidoMes));
        model.addAttribute("mensalidadesAReceberMesFmt", formatarMoeda(mensalidadesAReceberMes));
        model.addAttribute("mensalidadesAtrasadas", mensalidadesAtrasadas);
        model.addAttribute("mensalidadesTaxaAdimplencia", mensalidadesTaxaAdimplencia);
        model.addAttribute("preBusca", textoFiltro(preBusca));
        model.addAttribute("preStatus", textoFiltro(preStatus));
        model.addAttribute("preInscricoes", preInscricoesPainel);
        model.addAttribute("preInscricoesRecentes", preInscricoesRecentes);
        model.addAttribute("preInscricoesPendentesQtd", preInscricoesPendentes);
        model.addAttribute("preInscricoesContatadasQtd", preInscricoesContatadas);
        model.addAttribute("preInscricoesNovasSemanaQtd", preInscricoesNovasSemana);
        model.addAttribute("preInscricoesTotalQtd", preInscricoesPainel.size());
        model.addAttribute("blogBusca", textoFiltro(blogBusca));
        model.addAttribute("blogStatus", textoFiltro(blogStatus));
        model.addAttribute("blogCategoria", textoFiltro(blogCategoria));
        model.addAttribute("blogPostagens", postagensAdminFiltradas);
        model.addAttribute("blogPostagensTotalQtd", postagensAdminTodas.size());
        model.addAttribute("blogPostagensPublicadasQtd", postagensPublicadasQtd);
        model.addAttribute("blogPostagensRascunhoQtd", postagensRascunhoQtd);
        model.addAttribute("blogPostagensAgendadasQtd", postagensAgendadasQtd);
        model.addAttribute("blogTopPostagens", postagensTop);
        model.addAttribute("blogProximasPublicacoes", postagensAgendadas);
        model.addAttribute("livroBusca", textoFiltro(livroBusca));
        model.addAttribute("livros", livros);
        model.addAttribute("livrosTotal", livros.size());
        model.addAttribute("livrosComLinkCompra", livrosComLinkCompra);
        model.addAttribute("livrosSemLinkCompra", livrosSemLinkCompra);
        model.addAttribute("livroAnoMaisRecente", livroAnoMaisRecente > 0 ? livroAnoMaisRecente : "-");
        model.addAttribute("inadBusca", textoFiltro(inadBusca));
        model.addAttribute("inadFaixa", textoFiltro(inadFaixa));
        model.addAttribute("inadimplenciaCarteira", inadimplenciaResumo.carteira());
        model.addAttribute("inadimplenciaMensalidades", inadimplenciaResumo.mensalidadesEmAtraso());
        model.addAttribute("inadimplenciaAlunosQtd", inadimplenciaResumo.carteira().size());
        model.addAttribute("inadimplenciaTitulosAtrasoQtd", inadimplenciaResumo.titulosEmAtraso());
        model.addAttribute("inadimplenciaValorAbertoFmt", formatarMoeda(inadimplenciaResumo.valorEmAberto()));
        model.addAttribute("inadimplenciaCasosCriticosQtd", inadimplenciaResumo.casosCriticos());
        model.addAttribute("inadimplenciaFaixaAte15Qtd", inadimplenciaResumo.faixaAte15Qtd());
        model.addAttribute("inadimplenciaFaixa16a30Qtd", inadimplenciaResumo.faixa16a30Qtd());
        model.addAttribute("inadimplenciaFaixa31a60Qtd", inadimplenciaResumo.faixa31a60Qtd());
        model.addAttribute("inadimplenciaFaixa60MaisQtd", inadimplenciaResumo.faixa60MaisQtd());
        model.addAttribute("inadimplenciaFaixaAte15ValorFmt", formatarMoeda(inadimplenciaResumo.faixaAte15Valor()));
        model.addAttribute("inadimplenciaFaixa16a30ValorFmt", formatarMoeda(inadimplenciaResumo.faixa16a30Valor()));
        model.addAttribute("inadimplenciaFaixa31a60ValorFmt", formatarMoeda(inadimplenciaResumo.faixa31a60Valor()));
        model.addAttribute("inadimplenciaFaixa60MaisValorFmt", formatarMoeda(inadimplenciaResumo.faixa60MaisValor()));
        model.addAttribute("adminPanelInicial", panel == null || panel.isBlank() ? "overview" : panel);
        return "admin/dashboard";
    }

    @GetMapping("/admin/notificacoes/pre-inscricoes")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscarNotificacoesPreInscricoes(
        @RequestParam(name = "afterEpochMillis", required = false, defaultValue = "0") long afterEpochMillis,
        @RequestParam(name = "limit", required = false, defaultValue = "8") int limit
    ) {
        long referenciaSegura = Math.max(0L, afterEpochMillis);
        int limiteSeguro = Math.max(1, Math.min(limit, 20));

        List<PreInscricao> todas = preInscricaoService.listarPreInscricoes();
        List<NotificacaoPreInscricaoItem> recentes = todas.stream()
            .filter(item -> item.getDataLead() != null)
            .map(item -> new NotificacaoPreInscricaoItem(
                item.getId(),
                item.getNomeInteressado(),
                item.getWhatsapp(),
                montarCausaNotificacaoPreInscricao(item),
                "/admin/dashboard?panel=pre-inscricoes#lead-" + item.getId(),
                item.getDataLead().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            ))
            .sorted(Comparator.comparingLong(NotificacaoPreInscricaoItem::dataLeadEpochMillis).reversed())
            .limit(limiteSeguro)
            .toList();

        long unreadCount = todas.stream()
            .filter(item -> item.getDataLead() != null)
            .mapToLong(item -> item.getDataLead().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            .filter(epoch -> epoch > referenciaSegura)
            .count();

        long latestEpoch = todas.stream()
            .filter(item -> item.getDataLead() != null)
            .mapToLong(item -> item.getDataLead().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            .max()
            .orElse(referenciaSegura);

        return ResponseEntity.ok(Map.of(
            "unreadCount", unreadCount,
            "latestEpochMillis", latestEpoch,
            "items", recentes
        ));
    }

    @PostMapping("/admin/pre-inscricoes")
    public String cadastrarPreInscricaoAdmin(
        @RequestParam("nomeInteressado") String nomeInteressado,
        @RequestParam("whatsapp") String whatsapp,
        @RequestParam(name = "interesse", required = false) String interesse,
        @RequestParam(name = "origem", required = false) String origem,
        @RequestParam(name = "dataLead", required = false) LocalDate dataLead,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "mensagem", required = false) String mensagem,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoStringObrigatorio(nomeInteressado, "Nome do interessado");
            ValidationUtils.validarCampoStringObrigatorio(whatsapp, "WhatsApp");

            LocalDateTime dataReferencia = dataLead == null ? LocalDateTime.now() : dataLead.atStartOfDay();
            preInscricaoService.criarPreInscricaoAdmin(
                nomeInteressado,
                whatsapp,
                interesse,
                origem,
                dataReferencia,
                status,
                mensagem
            );

            redirectAttributes.addFlashAttribute(FEEDBACK_PRE_INSCRICAO_FORM, "Pré-inscrição cadastrada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_PRE_INSCRICAO_FORM, ex.getMessage());
        }

        return REDIRECT_PRE_INSCRICOES_PANEL;
    }

    @PostMapping("/admin/pre-inscricoes/contato")
    public String registrarContatoPreInscricao(
        @RequestParam("preInscricaoId") UUID preInscricaoId,
        @RequestParam(name = "canal", required = false) String canal,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "responsavel", required = false) String responsavel,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(preInscricaoId, "ID da pré-inscrição");
            preInscricaoService.registrarContatoLead(preInscricaoId, status);

            String canalNormalizado = normalizarTextoOpcional(canal);
            String responsavelNormalizado = normalizarTextoOpcional(responsavel);

            StringBuilder feedback = new StringBuilder("Contato da pré-inscrição registrado com sucesso.");
            if (!canalNormalizado.isBlank()) {
                feedback.append(" Canal: ").append(canalNormalizado).append('.');
            }
            if (!responsavelNormalizado.isBlank()) {
                feedback.append(" Responsável: ").append(responsavelNormalizado).append('.');
            }

            redirectAttributes.addFlashAttribute(FEEDBACK_PRE_INSCRICAO_CONTATO, feedback.toString());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_PRE_INSCRICAO_CONTATO, ex.getMessage());
        }

        return REDIRECT_PRE_INSCRICOES_PANEL;
    }

    @PostMapping("/admin/pre-inscricoes/excluir")
    public String excluirPreInscricao(
        @RequestParam("preInscricaoId") UUID preInscricaoId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(preInscricaoId, "ID da pré-inscrição");
            preInscricaoService.excluirPreInscricao(preInscricaoId);
            redirectAttributes.addFlashAttribute(FEEDBACK_PRE_INSCRICAO_DELETE, "Pré-inscrição excluída com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_PRE_INSCRICAO_DELETE, ex.getMessage());
        }

        return REDIRECT_PRE_INSCRICOES_PANEL;
    }

    @PostMapping("/admin/inadimplencia/enviar-lembrete")
    public String enviarLembreteInadimplencia(
        @RequestParam("alunoId") UUID alunoId,
        @RequestParam("mensalidadeId") UUID mensalidadeId,
        @RequestParam("canal") String canal,
        @RequestParam("template") String template,
        @RequestParam(name = "mensagem", required = false) String mensagem,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
            ValidationUtils.validarCampoObrigatorio(mensalidadeId, CAMPO_ID_MENSALIDADE);
            ValidationUtils.validarCampoStringObrigatorio(canal, "Canal");
            ValidationUtils.validarCampoStringObrigatorio(template, "Modelo de lembrete");

            List<Mensalidade> mensalidadesAluno = mensalidadeService.listarMensalidadesPorAluno(alunoId);
            Mensalidade mensalidade = mensalidadesAluno.stream()
                .filter(item -> item.getId() != null && item.getId().equals(mensalidadeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Mensalidade não encontrada para o aluno informado."));

            boolean emAtraso = mensalidade.getStatus() != StatusMensalidade.PAGO
                && mensalidade.getDataVencimento() != null
                && mensalidade.getDataVencimento().isBefore(LocalDate.now());

            if (!emAtraso) {
                throw new IllegalStateException("Lembretes só podem ser enviados para mensalidades em atraso.");
            }

            String observacaoMensagem = normalizarTextoOpcional(mensagem);
            String mensagemFinal = "Lembrete registrado para envio via " + canal.trim()
                + " (modelo " + template.trim() + ")."
                + (observacaoMensagem.isEmpty() ? "" : " Mensagem personalizada recebida.");
            redirectAttributes.addFlashAttribute(FEEDBACK_INADIMPLENCIA_LEMBRETE, mensagemFinal);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_INADIMPLENCIA_LEMBRETE, ex.getMessage());
        }

        return REDIRECT_INADIMPLENCIA_PANEL;
    }

    @PostMapping("/admin/inadimplencia/acordo")
    public String registrarAcordoInadimplencia(
        @RequestParam("alunoId") UUID alunoId,
        @RequestParam("mensalidadeId") UUID mensalidadeId,
        @RequestParam("valor") String valor,
        @RequestParam("parcelas") Integer parcelas,
        @RequestParam("primeiroVencimento") String primeiroVencimento,
        @RequestParam(name = "canal", required = false) String canal,
        @RequestParam(name = "responsavel", required = false) String responsavel,
        @RequestParam(name = "observacoes", required = false) String observacoes,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
            ValidationUtils.validarCampoObrigatorio(mensalidadeId, CAMPO_ID_MENSALIDADE);
            ValidationUtils.validarCampoStringObrigatorio(valor, "Valor negociado");
            ValidationUtils.validarCampoObrigatorio(parcelas, "Parcelas");
            ValidationUtils.validarCampoStringObrigatorio(primeiroVencimento, "Primeiro vencimento");

            BigDecimal valorNegociado = parseValorMonetario(valor);
            LocalDate dataPrimeiroVencimento = parseData(primeiroVencimento);
            int quantidadeParcelas = mensalidadeService.registrarAcordo(
                alunoId,
                mensalidadeId,
                valorNegociado,
                parcelas,
                dataPrimeiroVencimento
            );

            String canalNormalizado = normalizarTextoOpcional(canal);
            String responsavelNormalizado = normalizarTextoOpcional(responsavel);
            String observacoesNormalizadas = normalizarTextoOpcional(observacoes);

            StringBuilder mensagem = new StringBuilder();
            mensagem.append("Acordo registrado com sucesso em ")
                .append(quantidadeParcelas)
                .append(quantidadeParcelas == 1 ? " parcela." : " parcelas.");

            if (!canalNormalizado.isEmpty()) {
                mensagem.append(" Canal: ").append(canalNormalizado).append('.');
            }

            if (!responsavelNormalizado.isEmpty()) {
                mensagem.append(" Responsável: ").append(responsavelNormalizado).append('.');
            }

            if (!observacoesNormalizadas.isEmpty()) {
                mensagem.append(" Observações registradas.");
            }

            redirectAttributes.addFlashAttribute(FEEDBACK_INADIMPLENCIA_ACORDO, mensagem.toString());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_INADIMPLENCIA_ACORDO, ex.getMessage());
        }

        return REDIRECT_INADIMPLENCIA_PANEL;
    }

    @PostMapping("/admin/mensalidades/gerar-cobranca")
    public String gerarCobrancaMensalidade(
        @RequestParam("alunoId") UUID alunoId,
        @RequestParam("mensalidadeId") UUID mensalidadeId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
            ValidationUtils.validarCampoObrigatorio(mensalidadeId, CAMPO_ID_MENSALIDADE);

            Mensalidade mensalidade = mensalidadeService.gerarPix(alunoId, mensalidadeId);
            String qrCodeBase64 = mensalidadeService.gerarQrCodeBase64(mensalidade.getPixCopiaECola());

            redirectAttributes.addFlashAttribute(FEEDBACK_MENSALIDADE_COBRANCA, "Cobrança PIX gerada com sucesso.");
            redirectAttributes.addFlashAttribute("mensalidadePixCopiaCola", mensalidade.getPixCopiaECola());
            redirectAttributes.addFlashAttribute("mensalidadePixQrBase64", qrCodeBase64);
            redirectAttributes.addFlashAttribute("mensalidadePixId", mensalidade.getId());
            redirectAttributes.addFlashAttribute("mensalidadePixAlunoId", alunoId);
            redirectAttributes.addFlashAttribute("mensalidadePixStatus", mensalidade.getStatus() == null ? StatusMensalidade.PENDENTE.name() : mensalidade.getStatus().name());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_MENSALIDADE_COBRANCA, ex.getMessage());
        }

        return REDIRECT_MENSALIDADES_PANEL;
    }

    @PostMapping("/admin/mensalidades")
    public String criarMensalidade(
        @RequestParam("alunoId") UUID alunoId,
        @RequestParam("valor") String valor,
        @RequestParam("dataVencimento") String dataVencimento,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
            ValidationUtils.validarCampoStringObrigatorio(valor, "Valor da Mensalidade");
            ValidationUtils.validarCampoStringObrigatorio(dataVencimento, "Data de Vencimento");

            BigDecimal valorMonetario = parseValorMonetario(valor);
            LocalDate vencimento = parseData(dataVencimento);
            Mensalidade mensalidade = mensalidadeService.criarMensalidade(alunoId, valorMonetario, vencimento);

            redirectAttributes.addFlashAttribute(FEEDBACK_MENSALIDADE_CRIACAO, "Mensalidade criada com sucesso. Gere o QR Code para cobrança.");
            redirectAttributes.addFlashAttribute("mensalidadeCriadaAlunoId", alunoId);
            redirectAttributes.addFlashAttribute("mensalidadeCriadaId", mensalidade.getId());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_MENSALIDADE_FORM, ex.getMessage());
        }

        return REDIRECT_MENSALIDADES_PANEL;
    }

    @PostMapping("/admin/mensalidades/confirmar-pagamento")
    public String confirmarPagamentoMensalidade(
        @RequestParam("alunoId") UUID alunoId,
        @RequestParam("mensalidadeId") UUID mensalidadeId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
            ValidationUtils.validarCampoObrigatorio(mensalidadeId, CAMPO_ID_MENSALIDADE);

            mensalidadeService.confirmarPagamento(alunoId, mensalidadeId);
            redirectAttributes.addFlashAttribute(FEEDBACK_MENSALIDADE_FORM, "Pagamento confirmado e status atualizado para PAGO.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_MENSALIDADE_FORM, ex.getMessage());
        }

        return REDIRECT_MENSALIDADES_PANEL;
    }

    @PostMapping("/admin/mensalidades/registrar-pagamento")
    public String registrarPagamentoMensalidade(
        @RequestParam("alunoId") UUID alunoId,
        @RequestParam("mensalidadeId") UUID mensalidadeId,
        @RequestParam("metodo") String metodo,
        @RequestParam(name = "dataPagamento", required = false) LocalDate dataPagamento,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
            ValidationUtils.validarCampoObrigatorio(mensalidadeId, CAMPO_ID_MENSALIDADE);
            ValidationUtils.validarCampoStringObrigatorio(metodo, "Método de Pagamento");

            mensalidadeService.registrarPagamentoManual(alunoId, mensalidadeId, dataPagamento);
            redirectAttributes.addFlashAttribute(FEEDBACK_MENSALIDADE_FORM,
                "Pagamento registrado manualmente com sucesso (" + metodo.trim() + ").");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_MENSALIDADE_FORM, ex.getMessage());
        }

        return REDIRECT_MENSALIDADES_PANEL;
    }

    @PostMapping("/admin/mensalidades/excluir")
    public String excluirMensalidade(
        @RequestParam("alunoId") UUID alunoId,
        @RequestParam("mensalidadeId") UUID mensalidadeId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
            ValidationUtils.validarCampoObrigatorio(mensalidadeId, CAMPO_ID_MENSALIDADE);

            mensalidadeService.excluirMensalidade(alunoId, mensalidadeId);
            redirectAttributes.addFlashAttribute(FEEDBACK_MENSALIDADE_FORM, "Cobrança excluída com sucesso.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_MENSALIDADE_FORM, ex.getMessage());
        }

        return REDIRECT_MENSALIDADES_PANEL;
    }

    @GetMapping("/admin/notas/boletim/pdf")
    public ResponseEntity<byte[]> exportarBoletimPdf(
        @RequestParam(name = "notaBusca", required = false) String notaBusca,
        @RequestParam(name = "notaTurmaId", required = false) String notaTurmaId,
        @RequestParam(name = "notaBimestre", required = false) Integer notaBimestre
    ) {
        UUID notaTurmaIdFiltrada = parseUuidOpcional(notaTurmaId);
        List<Nota> notas = notaService.listarNotasFiltradas(notaBusca, notaTurmaIdFiltrada, notaBimestre);
        byte[] boletimPdf = notaService.gerarBoletimPdf(notas, notaBusca, notaTurmaIdFiltrada, notaBimestre);

        String nomeArquivo = "boletim-" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(nomeArquivo).build().toString())
            .body(boletimPdf);
    }

    @PostMapping("/admin/notas")
    public String lancarNota(
        @RequestParam("alunoId") UUID alunoId,
        @RequestParam(name = "turmaId", required = false) UUID turmaId,
        @RequestParam("atividade") String atividade,
        @RequestParam("valor") Double valor,
        @RequestParam(name = "presenca", required = false) String presenca,
        @RequestParam("bimestre") Integer bimestre,
        @RequestParam(name = "data", required = false) LocalDate data,
        @RequestParam(name = "observacoes", required = false) String observacoes,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
            ValidationUtils.validarCampoStringObrigatorio(atividade, "Atividade");
            ValidationUtils.validarCampoObrigatorio(valor, "Valor da nota");
            ValidationUtils.validarCampoObrigatorio(bimestre, "Bimestre");

            Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_ALUNO_NAO_ENCONTRADO + alunoId));

            if (aluno.getTurma() == null || aluno.getTurma().getId() == null) {
                throw new IllegalArgumentException("O aluno selecionado não possui turma associada.");
            }

            UUID turmaAssociadaAlunoId = aluno.getTurma().getId();
            if (turmaId != null && !turmaId.equals(turmaAssociadaAlunoId)) {
                throw new IllegalArgumentException("O aluno selecionado não pertence à turma informada.");
            }

            LocalDate dataReferencia = data == null ? LocalDate.now() : data;

            Nota nota = Nota.builder()
                .valor(valor)
                .bimestre(bimestre)
                .atividade(atividade)
                .presenca(presenca)
                .dataReferencia(dataReferencia)
                .descricao(normalizarTextoOpcional(observacoes))
                .build();

            notaService.cadastrarNota(alunoId, nota);
            redirectAttributes.addFlashAttribute(FEEDBACK_NOTAS_FORM, "Nota lançada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_NOTAS_FORM, ex.getMessage());
        }

        return REDIRECT_NOTAS_PANEL;
    }

    @PostMapping("/admin/presencas")
    public String lancarPresenca(
        @RequestParam("alunoId") UUID alunoId,
        @RequestParam(name = "turmaId", required = false) UUID turmaId,
        @RequestParam("presenca") String presenca,
        @RequestParam(name = "data", required = false) LocalDate data,
        @RequestParam(name = "observacoes", required = false) String observacoes,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
            ValidationUtils.validarCampoStringObrigatorio(presenca, "Presença");

            Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_ALUNO_NAO_ENCONTRADO + alunoId));

            if (aluno.getTurma() == null || aluno.getTurma().getId() == null) {
                throw new IllegalArgumentException("O aluno selecionado não possui turma associada.");
            }

            UUID turmaAssociadaAlunoId = aluno.getTurma().getId();
            if (turmaId != null && !turmaId.equals(turmaAssociadaAlunoId)) {
                throw new IllegalArgumentException("O aluno selecionado não pertence à turma informada.");
            }

            LocalDate dataReferencia = data == null ? LocalDate.now() : data;

            Nota presencaDiaria = Nota.builder()
                .valor(null)
                .bimestre(null)
                .atividade("Registro de presença")
                .presenca(presenca)
                .dataReferencia(dataReferencia)
                .descricao(normalizarTextoOpcional(observacoes))
                .build();

            notaService.cadastrarNota(alunoId, presencaDiaria);
            redirectAttributes.addFlashAttribute(FEEDBACK_NOTAS_FORM, "Presença lançada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_NOTAS_FORM, ex.getMessage());
        }

        return REDIRECT_NOTAS_PANEL;
    }

    @PostMapping("/admin/materiais")
    public String cadastrarMaterialEstudo(
        @RequestParam("titulo") String titulo,
        @RequestParam("descricao") String descricao,
        @RequestParam("turmaId") UUID turmaId,
        @RequestParam("arquivo") MultipartFile arquivo,
        @RequestParam(name = "dataUpload", required = false) LocalDate dataUpload,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoStringObrigatorio(titulo, "Título do Material");
            ValidationUtils.validarCampoStringObrigatorio(descricao, "Descrição do Material");
            ValidationUtils.validarCampoObrigatorio(turmaId, "Turma do Material");

            MaterialEstudo materialEstudo = MaterialEstudo.builder()
                .titulo(titulo.trim())
                .descricao(descricao.trim())
                .turma(turmaService.obterTurmaPorId(turmaId))
                .dataUpload(dataUpload == null ? LocalDateTime.now() : dataUpload.atStartOfDay())
                .build();

            materialEstudoService.salvarMaterialEstudo(materialEstudo, arquivo);
            redirectAttributes.addFlashAttribute(FEEDBACK_MATERIAL_FORM, "Material cadastrado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_MATERIAL_FORM, ex.getMessage());
        }

        return REDIRECT_MATERIAIS_PANEL;
    }

    @PostMapping("/admin/materiais/excluir")
    public String excluirMaterialEstudo(
        @RequestParam("materialId") UUID materialId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(materialId, CAMPO_ID_MATERIAL);
            materialEstudoService.excluirMaterialEstudo(materialId);
            redirectAttributes.addFlashAttribute(FEEDBACK_MATERIAL_FORM, "Material excluído com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_MATERIAL_FORM, ex.getMessage());
        }

        return REDIRECT_MATERIAIS_PANEL;
    }

    @GetMapping("/admin/materiais/arquivo/{materialId}")
    public ResponseEntity<Resource> baixarArquivoMaterial(@PathVariable("materialId") UUID materialId) {
        try {
            MaterialEstudo material = materialEstudoService.obterPorId(materialId);
            Path caminhoArquivo = fileUploadService.getCaminhoCompleto(material.getUrlArquivo());

            if (!Files.exists(caminhoArquivo)) {
                throw new IllegalArgumentException("Arquivo do material não encontrado.");
            }

            Resource recurso = new UrlResource(caminhoArquivo.toUri());
            String contentType = Files.probeContentType(caminhoArquivo);

            return ResponseEntity.ok()
                .contentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment().filename(material.getUrlArquivo()).build().toString())
                .body(recurso);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Arquivo do material inválido.");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Não foi possível baixar o arquivo do material.");
        }
    }

    @PostMapping("/admin/alunos")
    public String cadastrarAluno(
        @ModelAttribute AdminAlunoCadastroRequest request,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoStringObrigatorio(request.getNome(), "Nome do Aluno");
            ValidationUtils.validarCampoObrigatorio(request.getDataNascimento(), "Data de Nascimento do Aluno");
            ValidationUtils.validarCampoObrigatorio(request.getTurmaId(), "Turma do Aluno");
            ValidationUtils.validarCampoStringObrigatorio(request.getNivel(), "Nível Atual do Aluno");
            ValidationUtils.validarCampoStringObrigatorio(request.getTelefone(), "Telefone do Aluno");
            ValidationUtils.validarCampoStringObrigatorio(request.getEmail(), "E-mail do Aluno");
            ValidationUtils.validarCampoStringObrigatorio(request.getUsuarioSenha(), "Senha de acesso");

            String senha = request.getUsuarioSenha().trim();
            String senhaConfirmacao = request.getUsuarioSenhaConfirmacao() == null ? "" : request.getUsuarioSenhaConfirmacao().trim();

            if (!senha.equals(senhaConfirmacao)) {
                throw new IllegalArgumentException("As senhas informadas não coincidem.");
            }

            NivelAtual nivelAtual = parseNivel(request.getNivel());

            Aluno aluno = Aluno.builder()
                .nome(request.getNome().trim())
                .dataNascimento(request.getDataNascimento())
                .telefone(request.getTelefone().trim())
                .responsavel(normalizarTextoOpcional(request.getResponsavel()))
                .observacoes(normalizarTextoOpcional(request.getObservacoes()))
                .nivelAtual(nivelAtual)
                .turma(turmaService.obterTurmaPorId(request.getTurmaId()))
                .build();

            Aluno alunoSalvo = alunoRepository.save(aluno);

            Usuario usuario = Usuario.builder()
                .nome(alunoSalvo.getNome())
                .email(request.getEmail().trim().toLowerCase(Locale.ROOT))
                .senha(senha)
                .role(Role.ALUNO)
                .ativo(!"INATIVO".equalsIgnoreCase(request.getStatus()))
                .aluno(alunoSalvo)
                .build();

            usuarioService.criarUsuario(usuario);
            redirectAttributes.addFlashAttribute("alunoFormFeedback", "Aluno cadastrado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("alunoFormFeedback", ex.getMessage());
        }

        return REDIRECT_ALUNOS_PANEL;
    }

    @PostMapping("/admin/alunos/editar")
    public String editarAluno(
        @RequestParam("alunoId") java.util.UUID alunoId,
        @RequestParam("nome") String nome,
        @RequestParam("turmaId") java.util.UUID turmaId,
        @RequestParam("telefone") String telefone,
        @RequestParam(name = "responsavel", required = false) String responsavel,
        @RequestParam(name = "observacoes", required = false) String observacoes,
        @RequestParam("email") String email,
        @RequestParam("status") String status,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
            ValidationUtils.validarCampoStringObrigatorio(nome, "Nome do Aluno");
            ValidationUtils.validarCampoObrigatorio(turmaId, "Turma do Aluno");
            ValidationUtils.validarCampoStringObrigatorio(telefone, "Telefone do Aluno");
            ValidationUtils.validarCampoStringObrigatorio(email, "E-mail do Aluno");

            Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_ALUNO_NAO_ENCONTRADO + alunoId));

            Turma turma = turmaService.obterTurmaPorId(turmaId);

            Usuario usuario = usuarioRepository.findByAlunoId(alunoId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_USUARIO_ALUNO_NAO_ENCONTRADO));

            aluno.setNome(nome.trim());
            aluno.setTelefone(telefone.trim());
            aluno.setResponsavel(normalizarTextoOpcional(responsavel));
            aluno.setObservacoes(normalizarTextoOpcional(observacoes));
            aluno.setTurma(turma);
            alunoRepository.save(aluno);

            usuario.setNome(nome.trim());
            usuario.setEmail(email.trim().toLowerCase(Locale.ROOT));
            usuario.setAtivo(!"INATIVO".equalsIgnoreCase(status));
            usuarioRepository.save(usuario);

            redirectAttributes.addFlashAttribute("alunoEditFeedback", "Aluno atualizado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("alunoEditFeedback", ex.getMessage());
        }

        return REDIRECT_ALUNOS_PANEL;
    }

    @PostMapping("/admin/alunos/excluir")
    public String excluirAluno(
        @RequestParam("alunoId") java.util.UUID alunoId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);

            Usuario usuario = usuarioRepository.findByAlunoId(alunoId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_USUARIO_ALUNO_NAO_ENCONTRADO));

            Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_ALUNO_NAO_ENCONTRADO + alunoId));

            usuarioRepository.delete(usuario);
            alunoRepository.delete(aluno);

            redirectAttributes.addFlashAttribute("alunoDeleteFeedback", "Aluno excluído com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("alunoDeleteFeedback", ex.getMessage());
        }

        return REDIRECT_ALUNOS_PANEL;
    }

    @PostMapping("/admin/alunos/redefinir-senha")
    public String redefinirSenhaAluno(
        @RequestParam("alunoId") java.util.UUID alunoId,
        @RequestParam("usuarioLogin") String usuarioLogin,
        @RequestParam("novaSenha") String novaSenha,
        @RequestParam("novaSenhaConfirmacao") String novaSenhaConfirmacao,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(alunoId, CAMPO_ID_ALUNO);
            ValidationUtils.validarCampoStringObrigatorio(usuarioLogin, "Login do Usuário");
            ValidationUtils.validarCampoStringObrigatorio(novaSenha, "Nova senha");

            String senhaLimpa = novaSenha.trim();
            String senhaConfirmacaoLimpa = novaSenhaConfirmacao == null ? "" : novaSenhaConfirmacao.trim();

            if (!senhaLimpa.equals(senhaConfirmacaoLimpa)) {
                throw new IllegalArgumentException("As senhas informadas não coincidem.");
            }

            Usuario usuario = usuarioRepository.findByAlunoId(alunoId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_USUARIO_ALUNO_NAO_ENCONTRADO));

            String loginInformado = usuarioLogin.trim().toLowerCase(Locale.ROOT);
            if (!usuario.getEmail().equalsIgnoreCase(loginInformado)) {
                throw new IllegalArgumentException("Login informado não corresponde ao aluno selecionado.");
            }

            usuarioService.redefinirSenhaUsuario(usuario.getId(), senhaLimpa);
            redirectAttributes.addFlashAttribute("alunoSenhaFeedback", "Senha redefinida com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("alunoSenhaFeedback", ex.getMessage());
        }

        return REDIRECT_ALUNOS_PANEL;
    }

    @PostMapping("/admin/turmas")
    public String cadastrarTurma(
        @RequestParam("nome") String nome,
        @RequestParam("nivel") String nivel,
        @RequestParam("vagasTotal") int vagasTotal,
        @RequestParam("vagasOcupadas") int vagasOcupadas,
        @RequestParam("diasSemana") String diasSemana,
        @RequestParam("horario") String horario,
        @RequestParam(name = "observacoes", required = false) String observacoes,
        RedirectAttributes redirectAttributes
    ) {
        try {
            NivelAtual nivelAtualTurma = parseNivelTurma(nivel);

            Turma turma = Turma.builder()
                .nome(nome)
                .nivelAtual(nivelAtualTurma)
                .totalVagas(vagasTotal)
                .vagasOcupadas(vagasOcupadas)
                .diasSemana(diasSemana)
                .horario(horario)
                .observacoes(normalizarTextoOpcional(observacoes))
                .build();

            turmaService.criarTurma(turma);
            redirectAttributes.addFlashAttribute("turmaFormFeedback", "Turma cadastrada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("turmaFormFeedback", ex.getMessage());
        }

        return REDIRECT_TURMAS_PANEL;
    }

    @PostMapping("/admin/turmas/editar")
    public String editarTurma(
        @RequestParam("turmaId") java.util.UUID turmaId,
        @RequestParam("nome") String nome,
        @RequestParam("nivel") String nivel,
        @RequestParam("vagasTotal") int vagasTotal,
        @RequestParam("vagasOcupadas") int vagasOcupadas,
        @RequestParam("diasSemana") String diasSemana,
        @RequestParam("horario") String horario,
        @RequestParam(name = "observacoes", required = false) String observacoes,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(turmaId, CAMPO_ID_TURMA);
            NivelAtual nivelAtualTurma = parseNivelTurma(nivel);

            Turma turmaAtualizada = Turma.builder()
                .nome(nome)
                .nivelAtual(nivelAtualTurma)
                .totalVagas(vagasTotal)
                .vagasOcupadas(vagasOcupadas)
                .diasSemana(diasSemana)
                .horario(horario)
                .observacoes(normalizarTextoOpcional(observacoes))
                .build();

            turmaService.editarTurma(turmaId, turmaAtualizada);
            redirectAttributes.addFlashAttribute("turmaEditFeedback", "Turma atualizada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("turmaEditFeedback", ex.getMessage());
        }

        return REDIRECT_TURMAS_PANEL;
    }

    @PostMapping("/admin/turmas/excluir")
    public String excluirTurma(
        @RequestParam("turmaId") java.util.UUID turmaId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(turmaId, CAMPO_ID_TURMA);
            Turma turma = turmaService.obterTurmaPorId(turmaId);

            if (turma.getAlunos() != null && !turma.getAlunos().isEmpty()) {
                throw new IllegalArgumentException("Não é possível excluir a turma porque há alunos vinculados.");
            }

            turmaService.excluirTurma(turmaId);
            redirectAttributes.addFlashAttribute("turmaDeleteFeedback", "Turma excluída com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("turmaDeleteFeedback", ex.getMessage());
        }

        return REDIRECT_TURMAS_PANEL;
    }

    @PostMapping("/admin/livros")
    public String cadastrarLivro(
        @RequestParam("titulo") String titulo,
        @RequestParam("sinopse") String sinopse,
        @RequestParam("anoLancamento") Integer anoLancamento,
        @RequestParam(name = "linkCompra", required = false) String linkCompra,
        @RequestParam("imagemCapa") MultipartFile imagemCapa,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoStringObrigatorio(titulo, "Título da obra");
            ValidationUtils.validarCampoStringObrigatorio(sinopse, "Sinopse da obra");
            ValidationUtils.validarCampoObrigatorio(anoLancamento, "Ano de lançamento");

            Livro livro = Livro.builder()
                .titulo(titulo.trim())
                .sinopse(sinopse.trim())
                .anoLancamento(anoLancamento)
                .linkCompra(normalizarTextoOpcional(linkCompra))
                .build();

            livroService.salvarLivro(livro, imagemCapa);
            redirectAttributes.addFlashAttribute(FEEDBACK_LIVRO_FORM, "Obra literária cadastrada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_LIVRO_FORM, ex.getMessage());
        }

        return REDIRECT_LIVROS_PANEL;
    }

    @PostMapping("/admin/livros/editar")
    public String editarLivro(
        @RequestParam("livroId") UUID livroId,
        @RequestParam("titulo") String titulo,
        @RequestParam("sinopse") String sinopse,
        @RequestParam("anoLancamento") Integer anoLancamento,
        @RequestParam(name = "linkCompra", required = false) String linkCompra,
        @RequestParam(name = "imagemCapa", required = false) MultipartFile imagemCapa,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(livroId, "ID do livro");
            ValidationUtils.validarCampoStringObrigatorio(titulo, "Título da obra");
            ValidationUtils.validarCampoStringObrigatorio(sinopse, "Sinopse da obra");
            ValidationUtils.validarCampoObrigatorio(anoLancamento, "Ano de lançamento");

            Livro livro = Livro.builder()
                .id(livroId)
                .titulo(titulo.trim())
                .sinopse(sinopse.trim())
                .anoLancamento(anoLancamento)
                .linkCompra(normalizarTextoOpcional(linkCompra))
                .build();

            livroService.editarLivro(livro, imagemCapa);
            redirectAttributes.addFlashAttribute(FEEDBACK_LIVRO_EDIT, "Obra literária atualizada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_LIVRO_EDIT, ex.getMessage());
        }

        return REDIRECT_LIVROS_PANEL;
    }

    @PostMapping("/admin/livros/excluir")
    public String excluirLivro(
        @RequestParam("livroId") UUID livroId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(livroId, "ID do livro");
            livroService.excluirLivro(livroId);
            redirectAttributes.addFlashAttribute(FEEDBACK_LIVRO_DELETE, "Obra literária excluída com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(FEEDBACK_LIVRO_DELETE, ex.getMessage());
        }

        return REDIRECT_LIVROS_PANEL;
    }

    @GetMapping("/aluno/dashboard")
    public String alunoDashboard(Model model) {
        model.addAttribute("isDashboard", true);
        return "aluno/dashboard";
    }

    @GetMapping("/acesso-negado")
    public String acessoNegado() {
        return "errors/403";
    }

    private NivelAtual parseNivel(String nivel) {
        String nivelNormalizado = nivel == null ? "" : nivel.trim().toUpperCase(Locale.ROOT);

        if ("BASICO".equals(nivelNormalizado)) {
            return NivelAtual.INICIANTE;
        }

        try {
            return NivelAtual.valueOf(nivelNormalizado);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Nível do Aluno inválido.");
        }
    }

    private NivelAtual parseNivelTurma(String nivel) {
        return parseNivel(nivel);
    }

    private String normalizarTextoOpcional(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private List<Postagem> filtrarPostagensBlogAdmin(
        List<Postagem> origem,
        String buscaNormalizada,
        String statusNormalizado,
        String categoriaNormalizada
    ) {
        return origem.stream()
            .filter(postagem -> buscaNormalizada.isBlank() || correspondeBuscaPostagem(postagem, buscaNormalizada))
            .filter(postagem -> statusNormalizado.isBlank() || statusPostagemNormalizado(postagem).equals(statusNormalizado))
            .filter(postagem -> categoriaNormalizada.isBlank() || categoriaPostagemNormalizada(postagem).equals(categoriaNormalizada))
            .toList();
    }

    private boolean correspondeBuscaPostagem(Postagem postagem, String buscaNormalizada) {
        return contemTexto(postagem.getTitulo(), buscaNormalizada)
            || contemTexto(postagem.getAutor(), buscaNormalizada)
            || contemTexto(postagem.getResumo(), buscaNormalizada)
            || contemTexto(postagem.getConteudo(), buscaNormalizada);
    }

    private String statusPostagemNormalizado(Postagem postagem) {
        if (postagem.getStatus() == null) {
            return "rascunho";
        }
        return postagem.getStatus().name().toLowerCase(Locale.ROOT);
    }

    private String categoriaPostagemNormalizada(Postagem postagem) {
        return normalizarTextoOpcional(postagem.getCategoria()).toLowerCase(Locale.ROOT);
    }

    private long contarPostagensPorStatus(List<Postagem> postagens, StatusPostagem status) {
        return postagens.stream()
            .filter(postagem -> postagem.getStatus() == status)
            .count();
    }

    private List<Postagem> listarTopPostagensPublicadas(List<Postagem> postagens, int limite) {
        return postagens.stream()
            .filter(postagem -> postagem.getStatus() == StatusPostagem.PUBLICADO)
            .sorted(Comparator.comparing(Postagem::getDataPublicacao, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
            .limit(limite)
            .toList();
    }

    private List<Postagem> listarPostagensAgendadas(List<Postagem> postagens, int limite) {
        return postagens.stream()
            .filter(postagem -> postagem.getStatus() == StatusPostagem.AGENDADO)
            .sorted(Comparator.comparing(Postagem::getDataPublicacao, Comparator.nullsLast(LocalDateTime::compareTo)))
            .limit(limite)
            .toList();
    }

    private List<CarteiraInadimplenciaItem> montarCarteiraInadimplencia(
        List<Mensalidade> mensalidadesEmAtraso,
        String termoBusca,
        String faixaAtraso,
        LocalDate dataReferencia
    ) {
        Map<UUID, List<Mensalidade>> mensalidadesPorAluno = mensalidadesEmAtraso.stream()
            .filter(mensalidade -> mensalidade.getAluno() != null && mensalidade.getAluno().getId() != null)
            .collect(Collectors.groupingBy(mensalidade -> mensalidade.getAluno().getId()));

        return mensalidadesPorAluno.values().stream()
            .map(mensalidadesAluno -> montarCarteiraItem(mensalidadesAluno, dataReferencia))
            .filter(Objects::nonNull)
            .filter(item -> pertenceFaixaAtraso(item.diasEmAtraso(), faixaAtraso))
            .filter(item -> buscaInadimplenciaVazia(termoBusca)
                || contemTexto(item.alunoNome(), termoBusca)
                || contemTexto(item.turmaNome(), termoBusca)
                || contemTexto(item.responsavel(), termoBusca)
                || contemTexto(item.telefone(), termoBusca))
            .sorted(Comparator
                .comparingLong(CarteiraInadimplenciaItem::diasEmAtraso).reversed()
                .thenComparing(CarteiraInadimplenciaItem::alunoNome, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private CarteiraInadimplenciaItem montarCarteiraItem(List<Mensalidade> mensalidadesAluno, LocalDate dataReferencia) {
        if (mensalidadesAluno == null || mensalidadesAluno.isEmpty()) {
            return null;
        }

        Mensalidade mensalidadeMaisAntiga = mensalidadesAluno.stream()
            .filter(mensalidade -> mensalidade.getDataVencimento() != null)
            .min(Comparator.comparing(Mensalidade::getDataVencimento))
            .orElse(mensalidadesAluno.get(0));

        long diasEmAtraso = mensalidadesAluno.stream()
            .filter(mensalidade -> mensalidade.getDataVencimento() != null)
            .mapToLong(mensalidade -> java.time.temporal.ChronoUnit.DAYS.between(
                mensalidade.getDataVencimento(),
                dataReferencia
            ))
            .max()
            .orElse(0);

        BigDecimal valorEmAberto = mensalidadesAluno.stream()
            .map(Mensalidade::getValor)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Aluno aluno = mensalidadeMaisAntiga.getAluno();
        String turmaNome = aluno.getTurma() == null ? "-" : aluno.getTurma().getNome();
        String status = resolverStatusInadimplencia(diasEmAtraso);

        return new CarteiraInadimplenciaItem(
            aluno.getId(),
            mensalidadeMaisAntiga.getId(),
            aluno.getNome(),
            turmaNome == null || turmaNome.isBlank() ? "-" : turmaNome,
            normalizarTextoOpcional(aluno.getResponsavel()).isBlank() ? "-" : normalizarTextoOpcional(aluno.getResponsavel()),
            normalizarTextoOpcional(aluno.getTelefone()).isBlank() ? "-" : normalizarTextoOpcional(aluno.getTelefone()),
            valorEmAberto,
            diasEmAtraso,
            status,
            mensalidadesAluno.size()
        );
    }

    private boolean buscaInadimplenciaVazia(String termoBusca) {
        return termoBusca == null || termoBusca.isBlank();
    }

    private boolean pertenceFaixaAtraso(long dias, String faixaAtraso) {
        if (faixaAtraso == null || faixaAtraso.isBlank()) {
            return true;
        }

        return switch (faixaAtraso) {
            case "ate15" -> dias <= 15;
            case "16a30" -> dias >= 16 && dias <= 30;
            case "31a60" -> dias >= 31 && dias <= 60;
            case "60mais" -> dias > 60;
            default -> true;
        };
    }

    private BigDecimal somarValoresPorFaixa(List<CarteiraInadimplenciaItem> carteira, int minimo, int maximo) {
        return carteira.stream()
            .filter(item -> item.diasEmAtraso() >= minimo && item.diasEmAtraso() <= maximo)
            .map(CarteiraInadimplenciaItem::valorEmAberto)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private InadimplenciaPainelResumo montarResumoInadimplencia(
        List<Mensalidade> mensalidadesFinanceiro,
        String inadBuscaNormalizada,
        String inadFaixaNormalizada,
        LocalDate dataReferencia
    ) {
        List<Mensalidade> mensalidadesEmAtraso = mensalidadeService
            .listarMensalidadesEmAtraso(mensalidadesFinanceiro, dataReferencia);
        List<CarteiraInadimplenciaItem> carteira = montarCarteiraInadimplencia(
            mensalidadesEmAtraso,
            inadBuscaNormalizada,
            inadFaixaNormalizada,
            dataReferencia
        );

        BigDecimal valorEmAberto = carteira.stream()
            .map(CarteiraInadimplenciaItem::valorEmAberto)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long casosCriticos = carteira.stream()
            .filter(item -> item.diasEmAtraso() > 30)
            .count();

        long faixaAte15Qtd = carteira.stream().filter(item -> item.diasEmAtraso() <= 15).count();
        long faixa16a30Qtd = carteira.stream().filter(item -> item.diasEmAtraso() >= 16 && item.diasEmAtraso() <= 30).count();
        long faixa31a60Qtd = carteira.stream().filter(item -> item.diasEmAtraso() >= 31 && item.diasEmAtraso() <= 60).count();
        long faixa60MaisQtd = carteira.stream().filter(item -> item.diasEmAtraso() > 60).count();

        BigDecimal faixaAte15Valor = somarValoresPorFaixa(carteira, 0, 15);
        BigDecimal faixa16a30Valor = somarValoresPorFaixa(carteira, 16, 30);
        BigDecimal faixa31a60Valor = somarValoresPorFaixa(carteira, 31, 60);
        BigDecimal faixa60MaisValor = somarValoresPorFaixa(carteira, 61, Integer.MAX_VALUE);

        return new InadimplenciaPainelResumo(
            carteira,
            mensalidadesEmAtraso,
            mensalidadesEmAtraso.size(),
            valorEmAberto,
            casosCriticos,
            faixaAte15Qtd,
            faixa16a30Qtd,
            faixa31a60Qtd,
            faixa60MaisQtd,
            faixaAte15Valor,
            faixa16a30Valor,
            faixa31a60Valor,
            faixa60MaisValor
        );
    }

    private List<Usuario> listarUsuariosAlunosFiltrados(String alunoBuscaNormalizada) {
        return usuarioService.listarUsuarios().stream()
            .filter(usuario -> usuario.getRole() == Role.ALUNO)
            .filter(usuario -> usuario.getAluno() != null)
            .filter(usuario -> alunoBuscaNormalizada.isEmpty()
                || contemTexto(usuario.getNome(), alunoBuscaNormalizada)
                || contemTexto(usuario.getAluno().getTelefone(), alunoBuscaNormalizada)
                || (usuario.getAluno().getTurma() != null && contemTexto(usuario.getAluno().getTurma().getNome(), alunoBuscaNormalizada)))
            .sorted(Comparator.comparing(Usuario::getNome, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private List<Mensalidade> listarProximosVencimentos(List<Mensalidade> mensalidadesCobraveis, LocalDate referencia) {
        return mensalidadesCobraveis.stream()
            .filter(mensalidade -> mensalidade.getDataVencimento() != null)
            .filter(mensalidade -> !mensalidade.getDataVencimento().isBefore(referencia))
            .sorted(Comparator.comparing(Mensalidade::getDataVencimento))
            .limit(3)
            .toList();
    }

    private List<Livro> listarLivrosFiltrados(String livroBusca) {
        String buscaNormalizada = livroBusca == null ? "" : livroBusca.trim().toLowerCase(Locale.ROOT);

        return StreamSupport.stream(livroService.listarTodosLivros().spliterator(), false)
            .filter(livro -> buscaNormalizada.isBlank()
                || contemTexto(livro.getTitulo(), buscaNormalizada)
                || contemTexto(livro.getSinopse(), buscaNormalizada)
                || contemTexto(livro.getLinkCompra(), buscaNormalizada)
                || String.valueOf(livro.getAnoLancamento()).contains(buscaNormalizada))
            .sorted(Comparator.comparing(Livro::getTitulo, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
    }

    private List<PreInscricaoPainelItem> montarPreInscricoesPainel(List<PreInscricao> preInscricoes) {
        return preInscricoes.stream()
            .map(preInscricao -> {
                String mensagem = normalizarTextoOpcional(preInscricao.getMensagem());
                String interesse = extrairTrechoMensagem(mensagem, "Interesse:");
                String origem = extrairTrechoMensagem(mensagem, "Origem:");

                return new PreInscricaoPainelItem(
                    preInscricao.getId(),
                    preInscricao.getNomeInteressado(),
                    preInscricao.getWhatsapp(),
                    interesse,
                    origem,
                    mensagem,
                    preInscricao.getDataLead(),
                    preInscricao.isRespondido() ? "Contatado" : "Pendente",
                    preInscricao.isRespondido()
                );
            })
            .toList();
    }

    private String extrairTrechoMensagem(String mensagem, String marcador) {
        if (mensagem == null || mensagem.isBlank() || marcador == null || marcador.isBlank()) {
            return "-";
        }

        int inicioMarcador = mensagem.toLowerCase(Locale.ROOT).indexOf(marcador.toLowerCase(Locale.ROOT));
        if (inicioMarcador < 0) {
            return "-";
        }

        int inicioConteudo = inicioMarcador + marcador.length();
        int proximoSeparador = mensagem.indexOf("|", inicioConteudo);
        String trecho = proximoSeparador < 0
            ? mensagem.substring(inicioConteudo)
            : mensagem.substring(inicioConteudo, proximoSeparador);

        String normalizado = trecho.trim();
        return normalizado.isBlank() ? "-" : normalizado;
    }

    private String montarCausaNotificacaoPreInscricao(PreInscricao preInscricao) {
        String mensagem = normalizarTextoOpcional(preInscricao.getMensagem());
        String interesse = extrairTrechoMensagem(mensagem, "Interesse:");
        String origem = extrairTrechoMensagem(mensagem, "Origem:");

        boolean temInteresse = !"-".equals(interesse);
        boolean temOrigem = !"-".equals(origem);

        if (temInteresse && temOrigem) {
            return "Novo lead via " + origem + " com interesse em " + interesse + ".";
        }

        if (temOrigem) {
            return "Novo lead recebido via " + origem + ".";
        }

        if (temInteresse) {
            return "Novo lead com interesse em " + interesse + ".";
        }

        return "Nova pré-inscrição recebida pelo formulário.";
    }

    private String resolverStatusInadimplencia(long diasEmAtraso) {
        if (diasEmAtraso <= 15) {
            return "Lembrete";
        }
        if (diasEmAtraso <= 30) {
            return "Cobrança ativa";
        }
        return "Acordo sugerido";
    }

    private boolean contemTexto(String origem, String termoNormalizado) {
        return origem != null && origem.toLowerCase(Locale.ROOT).contains(termoNormalizado);
    }

    private String textoFiltro(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private UUID parseUuidOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(valor.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String formatarMoeda(BigDecimal valor) {
        BigDecimal seguro = valor == null ? BigDecimal.ZERO : valor;
        NumberFormat formatador = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        return formatador.format(seguro);
    }

    private BigDecimal parseValorMonetario(String valor) {
        String normalizado = valor == null ? "" : valor.trim();
        normalizado = normalizado.replace("R$", "").replace(" ", "").replace(".", "").replace(",", ".");

        try {
            return new BigDecimal(normalizado);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor da mensalidade inválido.");
        }
    }

    private LocalDate parseData(String data) {
        try {
            return LocalDate.parse(data.trim());
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new IllegalArgumentException("Data de vencimento inválida.");
        }
    }

    public record CarteiraInadimplenciaItem(
        UUID alunoId,
        UUID mensalidadeId,
        String alunoNome,
        String turmaNome,
        String responsavel,
        String telefone,
        BigDecimal valorEmAberto,
        long diasEmAtraso,
        String status,
        int titulosEmAtraso
    ) {}

    private record InadimplenciaPainelResumo(
        List<CarteiraInadimplenciaItem> carteira,
        List<Mensalidade> mensalidadesEmAtraso,
        long titulosEmAtraso,
        BigDecimal valorEmAberto,
        long casosCriticos,
        long faixaAte15Qtd,
        long faixa16a30Qtd,
        long faixa31a60Qtd,
        long faixa60MaisQtd,
        BigDecimal faixaAte15Valor,
        BigDecimal faixa16a30Valor,
        BigDecimal faixa31a60Valor,
        BigDecimal faixa60MaisValor
    ) {}

    public record PreInscricaoPainelItem(
        UUID id,
        String nomeInteressado,
        String whatsapp,
        String interesse,
        String origem,
        String mensagem,
        LocalDateTime dataLead,
        String status,
        boolean respondido
    ) {}

    public record NotificacaoPreInscricaoItem(
        UUID id,
        String nomeInteressado,
        String whatsapp,
        String causa,
        String destinoUrl,
        long dataLeadEpochMillis
    ) {}
}
