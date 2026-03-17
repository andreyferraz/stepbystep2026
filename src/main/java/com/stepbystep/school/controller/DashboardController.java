package com.stepbystep.school.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.stepbystep.school.dto.AdminAlunoCadastroRequest;
import com.stepbystep.school.enums.NivelAtual;
import com.stepbystep.school.enums.Role;
import com.stepbystep.school.model.Aluno;
import com.stepbystep.school.model.Turma;
import com.stepbystep.school.model.Usuario;
import com.stepbystep.school.repository.AlunoRepository;
import com.stepbystep.school.repository.UsuarioRepository;
import com.stepbystep.school.service.TurmaService;
import com.stepbystep.school.service.UsuarioService;
import com.stepbystep.school.util.ValidationUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Controller
public class DashboardController {

    private static final String REDIRECT_ALUNOS_PANEL = "redirect:/admin/dashboard?panel=alunos";
    private static final String REDIRECT_TURMAS_PANEL = "redirect:/admin/dashboard?panel=turmas";
    private static final String CAMPO_ID_ALUNO = "ID do Aluno";
    private static final String CAMPO_ID_TURMA = "ID da Turma";
    private static final String MSG_USUARIO_ALUNO_NAO_ENCONTRADO = "Usuário do aluno não encontrado.";

    private final TurmaService turmaService;
    private final AlunoRepository alunoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    public DashboardController(
        TurmaService turmaService,
        AlunoRepository alunoRepository,
        UsuarioRepository usuarioRepository,
        UsuarioService usuarioService
    ) {
        this.turmaService = turmaService;
        this.alunoRepository = alunoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(
        @RequestParam(name = "panel", required = false) String panel,
        Model model
    ) {
        List<Usuario> usuariosAlunos = usuarioService.listarUsuarios().stream()
            .filter(usuario -> usuario.getRole() == Role.ALUNO)
            .filter(usuario -> usuario.getAluno() != null)
            .sorted(Comparator.comparing(Usuario::getNome, String.CASE_INSENSITIVE_ORDER))
            .toList();

        model.addAttribute("isDashboard", true);
        model.addAttribute("turmas", turmaService.listarTurmas());
        model.addAttribute("usuariosAlunos", usuariosAlunos);
        model.addAttribute("adminPanelInicial", panel == null || panel.isBlank() ? "overview" : panel);
        return "admin/dashboard";
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
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado com ID: " + alunoId));

            Turma turma = turmaService.obterTurmaPorId(turmaId);

            Usuario usuario = usuarioRepository.findByAlunoId(alunoId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_USUARIO_ALUNO_NAO_ENCONTRADO));

            aluno.setNome(nome.trim());
            aluno.setTelefone(telefone.trim());
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
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado com ID: " + alunoId));

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
        @RequestParam("diasSemana") String diasSemana,
        @RequestParam("horario") String horario,
        RedirectAttributes redirectAttributes
    ) {
        try {
            Turma turma = Turma.builder()
                .nome(nome)
                .diasSemana(diasSemana)
                .horario(horario)
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
        @RequestParam("diasSemana") String diasSemana,
        @RequestParam("horario") String horario,
        RedirectAttributes redirectAttributes
    ) {
        try {
            ValidationUtils.validarCampoObrigatorio(turmaId, CAMPO_ID_TURMA);

            Turma turmaAtualizada = Turma.builder()
                .nome(nome)
                .diasSemana(diasSemana)
                .horario(horario)
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
}
