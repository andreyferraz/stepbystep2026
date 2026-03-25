package com.stepbystep.school.controller;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.stepbystep.school.dto.AdminAlunoCadastroRequest;
import com.stepbystep.school.enums.NivelAtual;
import com.stepbystep.school.enums.Role;
import com.stepbystep.school.model.Aluno;
import com.stepbystep.school.model.MaterialEstudo;
import com.stepbystep.school.model.Nota;
import com.stepbystep.school.model.Turma;
import com.stepbystep.school.model.Usuario;
import com.stepbystep.school.repository.AlunoRepository;
import com.stepbystep.school.repository.UsuarioRepository;
import com.stepbystep.school.service.FileUploadService;
import com.stepbystep.school.service.MaterialEstudoService;
import com.stepbystep.school.service.NotaService;
import com.stepbystep.school.service.TurmaService;
import com.stepbystep.school.service.UsuarioService;
import com.stepbystep.school.util.ValidationUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
public class DashboardController {

    private static final String REDIRECT_ALUNOS_PANEL = "redirect:/admin/dashboard?panel=alunos";
    private static final String REDIRECT_TURMAS_PANEL = "redirect:/admin/dashboard?panel=turmas";
    private static final String REDIRECT_MATERIAIS_PANEL = "redirect:/admin/dashboard?panel=materiais";
    private static final String REDIRECT_NOTAS_PANEL = "redirect:/admin/dashboard?panel=notas";
    private static final String CAMPO_ID_ALUNO = "ID do Aluno";
    private static final String CAMPO_ID_TURMA = "ID da Turma";
    private static final String CAMPO_ID_MATERIAL = "ID do Material";
    private static final String FEEDBACK_MATERIAL_FORM = "materialFormFeedback";
    private static final String FEEDBACK_NOTAS_FORM = "notasFormFeedback";
    private static final String MSG_ALUNO_NAO_ENCONTRADO = "Aluno não encontrado com ID: ";
    private static final String MSG_USUARIO_ALUNO_NAO_ENCONTRADO = "Usuário do aluno não encontrado.";

    private final TurmaService turmaService;
    private final AlunoRepository alunoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final MaterialEstudoService materialEstudoService;
    private final NotaService notaService;
    private final FileUploadService fileUploadService;

    public DashboardController(
        TurmaService turmaService,
        AlunoRepository alunoRepository,
        UsuarioRepository usuarioRepository,
        UsuarioService usuarioService,
        MaterialEstudoService materialEstudoService,
        NotaService notaService,
        FileUploadService fileUploadService
    ) {
        this.turmaService = turmaService;
        this.alunoRepository = alunoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.materialEstudoService = materialEstudoService;
        this.notaService = notaService;
        this.fileUploadService = fileUploadService;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(
        @RequestParam(name = "panel", required = false) String panel,
        @RequestParam(name = "alunoBusca", required = false) String alunoBusca,
        @RequestParam(name = "turmaBusca", required = false) String turmaBusca,
        @RequestParam(name = "materialBusca", required = false) String materialBusca,
        @RequestParam(name = "materialTurmaId", required = false) String materialTurmaId,
        @RequestParam(name = "notaBusca", required = false) String notaBusca,
        @RequestParam(name = "notaTurmaId", required = false) String notaTurmaId,
        @RequestParam(name = "notaBimestre", required = false) Integer notaBimestre,
        Model model
    ) {
        String alunoBuscaNormalizada = alunoBusca == null ? "" : alunoBusca.trim().toLowerCase(Locale.ROOT);
        UUID materialTurmaIdFiltrada = parseUuidOpcional(materialTurmaId);
        UUID notaTurmaIdFiltrada = parseUuidOpcional(notaTurmaId);

        List<Usuario> usuariosAlunos = usuarioService.listarUsuarios().stream()
            .filter(usuario -> usuario.getRole() == Role.ALUNO)
            .filter(usuario -> usuario.getAluno() != null)
            .filter(usuario -> alunoBuscaNormalizada.isEmpty()
                || contemTexto(usuario.getNome(), alunoBuscaNormalizada)
                || contemTexto(usuario.getAluno().getTelefone(), alunoBuscaNormalizada)
                || (usuario.getAluno().getTurma() != null && contemTexto(usuario.getAluno().getTurma().getNome(), alunoBuscaNormalizada)))
            .sorted(Comparator.comparing(Usuario::getNome, String.CASE_INSENSITIVE_ORDER))
            .toList();

        List<MaterialEstudo> materiaisEstudo = materialEstudoService
            .listarMateriaisFiltrados(materialBusca, materialTurmaIdFiltrada);

        List<Nota> notasLancadas = notaService.listarNotasFiltradas(notaBusca, notaTurmaIdFiltrada, notaBimestre);
        List<Nota> notasHoje = notaService.listarLancamentosDoDia(notasLancadas, LocalDate.now());
        List<NotaService.ResumoTurmaNotas> notasTurmasAtencao = notaService
            .listarResumoTurmasEmAtencao(notasLancadas, 7.0, 75.0);
        long turmasComMateriais = materiaisEstudo.stream()
            .map(MaterialEstudo::getTurma)
            .filter(turma -> turma != null && turma.getId() != null)
            .map(Turma::getId)
            .distinct()
            .count();

        model.addAttribute("isDashboard", true);
        model.addAttribute("turmas", turmaService.listarTurmasFiltradas(turmaBusca));
        model.addAttribute("usuariosAlunos", usuariosAlunos);
        model.addAttribute("materiaisEstudo", materiaisEstudo);
        model.addAttribute("alunoBusca", alunoBusca == null ? "" : alunoBusca.trim());
        model.addAttribute("turmaBusca", turmaBusca == null ? "" : turmaBusca.trim());
        model.addAttribute("materialBusca", materialBusca == null ? "" : materialBusca.trim());
        model.addAttribute("materialTurmaId", materialTurmaId == null ? "" : materialTurmaId.trim());
        model.addAttribute("materiaisTotal", materiaisEstudo.size());
        model.addAttribute("materiaisTurmasCount", turmasComMateriais);
        model.addAttribute("materiaisUploadsSemana", materialEstudoService.contarUploadsUltimosDias(materiaisEstudo, 7));
        model.addAttribute("notaBusca", notaBusca == null ? "" : notaBusca.trim());
        model.addAttribute("notaTurmaId", notaTurmaId == null ? "" : notaTurmaId.trim());
        model.addAttribute("notaBimestre", notaBimestre == null ? "" : notaBimestre);
        model.addAttribute("notasLancadas", notasLancadas);
        model.addAttribute("notasHoje", notasHoje);
        model.addAttribute("notasTurmasAtencao", notasTurmasAtencao);
        model.addAttribute("notasMediaGeral", String.format(Locale.forLanguageTag("pt-BR"), "%.1f", notaService.calcularMediaNotas(notasLancadas)));
        model.addAttribute("notasPresencaMedia", Math.round(notaService.calcularMediaPresenca(notasLancadas)));
        model.addAttribute("notasAbaixo75", notaService.contarAlunosAbaixoDePresenca(notasLancadas, 75.0));
        model.addAttribute("notasLancamentosTotal", notasLancadas.size());
        model.addAttribute("adminPanelInicial", panel == null || panel.isBlank() ? "overview" : panel);
        return "admin/dashboard";
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

    private boolean contemTexto(String origem, String termoNormalizado) {
        return origem != null && origem.toLowerCase(Locale.ROOT).contains(termoNormalizado);
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
}
