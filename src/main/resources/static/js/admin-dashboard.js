document.addEventListener("DOMContentLoaded", function () {
    var panelStorageKey = "adminDashboardActivePanel";
    var sidebar = document.getElementById("adminSidebar");
    var toggleButton = document.getElementById("adminMenuToggle");
    var panelHeading = document.getElementById("panelHeading");
    var navLinks = document.querySelectorAll(".admin-nav-link[data-panel-target]");
    var panels = document.querySelectorAll(".admin-panel[data-panel]");

    function findLinkByTarget(target) {
        return Array.from(navLinks).find(function (link) {
            return link.getAttribute("data-panel-target") === target;
        });
    }

    function activatePanel(target, title) {
        navLinks.forEach(function (link) {
            link.classList.toggle("is-active", link.getAttribute("data-panel-target") === target);
        });

        panels.forEach(function (panel) {
            panel.classList.toggle("is-active", panel.getAttribute("data-panel") === target);
        });

        if (panelHeading && title) {
            panelHeading.textContent = title;
        }
    }

    navLinks.forEach(function (link) {
        link.addEventListener("click", function () {
            var target = link.getAttribute("data-panel-target");
            var title = link.getAttribute("data-panel-title");
            activatePanel(target, title);
            localStorage.setItem(panelStorageKey, target);

            if (window.innerWidth <= 960 && sidebar) {
                sidebar.classList.remove("is-open");
            }
        });
    });

    var savedPanel = localStorage.getItem(panelStorageKey);
    if (savedPanel) {
        var savedLink = findLinkByTarget(savedPanel);
        if (savedLink) {
            activatePanel(savedPanel, savedLink.getAttribute("data-panel-title"));
        }
    }

    if (toggleButton && sidebar) {
        toggleButton.addEventListener("click", function () {
            sidebar.classList.toggle("is-open");
        });
    }

    var openModalButtons = document.querySelectorAll("[data-open-modal]");
    var closeModalButtons = document.querySelectorAll("[data-close-modal]");

    openModalButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            var target = button.getAttribute("data-open-modal");
            var modal = document.querySelector('[data-modal="' + target + '"]');
            if (modal) {
                if (target === "aluno-senha") {
                    var nomeAluno = button.getAttribute("data-aluno-nome") || "";
                    var alunoNomeInput = modal.querySelector("#alunoSenhaNome");
                    if (alunoNomeInput) {
                        alunoNomeInput.value = nomeAluno;
                    }
                }

                if (target === "aluno-editar") {
                    var map = {
                        "#editarAlunoId": "data-aluno-id",
                        "#editarAlunoNome": "data-aluno-nome",
                        "#editarAlunoTurma": "data-aluno-turma",
                        "#editarAlunoResponsavel": "data-aluno-responsavel",
                        "#editarAlunoTelefone": "data-aluno-telefone",
                        "#editarAlunoEmail": "data-aluno-email",
                        "#editarAlunoLogin": "data-aluno-login",
                        "#editarAlunoStatus": "data-aluno-status",
                        "#editarAlunoObservacoes": "data-aluno-observacoes"
                    };

                    Object.keys(map).forEach(function (selector) {
                        var input = modal.querySelector(selector);
                        if (input) {
                            input.value = button.getAttribute(map[selector]) || "";
                        }
                    });
                }

                if (target === "aluno-visualizar") {
                    var viewMap = {
                        "#visualizarAlunoId": "data-aluno-id",
                        "#visualizarAlunoNome": "data-aluno-nome",
                        "#visualizarAlunoDataNascimento": "data-aluno-data-nascimento",
                        "#visualizarAlunoTurma": "data-aluno-turma-label",
                        "#visualizarAlunoNivel": "data-aluno-nivel-label",
                        "#visualizarAlunoStatus": "data-aluno-status-label",
                        "#visualizarAlunoResponsavel": "data-aluno-responsavel",
                        "#visualizarAlunoTelefone": "data-aluno-telefone",
                        "#visualizarAlunoEmail": "data-aluno-email",
                        "#visualizarAlunoLogin": "data-aluno-login",
                        "#visualizarAlunoObservacoes": "data-aluno-observacoes"
                    };

                    Object.keys(viewMap).forEach(function (selector) {
                        var field = modal.querySelector(selector);
                        if (field) {
                            field.value = button.getAttribute(viewMap[selector]) || "";
                        }
                    });
                }

                if (target === "turma-editar") {
                    var turmaEditMap = {
                        "#editarTurmaId": "data-turma-id",
                        "#editarTurmaNome": "data-turma-nome",
                        "#editarTurmaNivel": "data-turma-nivel",
                        "#editarTurmaTeacher": "data-turma-teacher",
                        "#editarTurmaVagasTotal": "data-turma-vagas-total",
                        "#editarTurmaVagasOcupadas": "data-turma-vagas-ocupadas",
                        "#editarTurmaDias": "data-turma-dias",
                        "#editarTurmaHorario": "data-turma-horario",
                        "#editarTurmaStatus": "data-turma-status",
                        "#editarTurmaObservacoes": "data-turma-observacoes"
                    };

                    Object.keys(turmaEditMap).forEach(function (selector) {
                        var field = modal.querySelector(selector);
                        if (field) {
                            field.value = button.getAttribute(turmaEditMap[selector]) || "";
                        }
                    });
                }

                if (target === "turma-visualizar") {
                    var turmaViewMap = {
                        "#visualizarTurmaId": "data-turma-id",
                        "#visualizarTurmaNome": "data-turma-nome",
                        "#visualizarTurmaNivel": "data-turma-nivel-label",
                        "#visualizarTurmaTeacher": "data-turma-teacher",
                        "#visualizarTurmaVagas": "data-turma-vagas",
                        "#visualizarTurmaStatus": "data-turma-status-label",
                        "#visualizarTurmaDias": "data-turma-dias",
                        "#visualizarTurmaHorario": "data-turma-horario",
                        "#visualizarTurmaObservacoes": "data-turma-observacoes"
                    };

                    Object.keys(turmaViewMap).forEach(function (selector) {
                        var field = modal.querySelector(selector);
                        if (field) {
                            field.value = button.getAttribute(turmaViewMap[selector]) || "";
                        }
                    });
                }

                if (target === "turma-excluir") {
                    var turmaIdField = modal.querySelector("#excluirTurmaId");
                    var turmaNomeField = modal.querySelector("#excluirTurmaNome");
                    var avisoRegra = modal.querySelector("#excluirTurmaRegraAviso");
                    var confirmarExcluirBtn = modal.querySelector("#confirmarExcluirTurmaBtn");
                    var vagasOcupadas = Number(button.getAttribute("data-turma-vagas-ocupadas") || "0");

                    if (turmaIdField) {
                        turmaIdField.value = button.getAttribute("data-turma-id") || "";
                    }

                    if (turmaNomeField) {
                        turmaNomeField.value = button.getAttribute("data-turma-nome") || "";
                    }

                    if (confirmarExcluirBtn) {
                        confirmarExcluirBtn.disabled = vagasOcupadas > 0;
                    }

                    if (avisoRegra) {
                        if (vagasOcupadas > 0) {
                            avisoRegra.textContent = "Exclusão bloqueada: existem " + vagasOcupadas + " aluno(s) vinculado(s) a esta turma.";
                        } else {
                            avisoRegra.textContent = "Turma sem alunos vinculados. Exclusão liberada.";
                        }
                    }
                }

                modal.classList.add("is-open");
                if (typeof modal.showModal === "function" && !modal.open) {
                    modal.showModal();
                }
            }
        });
    });

    closeModalButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            var modal = button.closest(".modal-overlay");
            if (modal) {
                modal.classList.remove("is-open");
                if (typeof modal.close === "function" && modal.open) {
                    modal.close();
                }
            }
        });
    });

    document.querySelectorAll(".modal-overlay").forEach(function (modal) {
        modal.addEventListener("click", function (event) {
            if (event.target === modal) {
                modal.classList.remove("is-open");
                if (typeof modal.close === "function" && modal.open) {
                    modal.close();
                }
            }
        });
    });

    document.querySelectorAll("form[data-static-submit='true']").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            event.preventDefault();

            var senhaInput = form.querySelector("#alunoSenha");
            var confirmaSenhaInput = form.querySelector("#alunoSenhaConfirmacao");
            var novaSenhaInput = form.querySelector("#alunoNovaSenha");
            var novaSenhaConfirmacaoInput = form.querySelector("#alunoNovaSenhaConfirmacao");

            if (senhaInput && confirmaSenhaInput) {
                var senhasIguais = senhaInput.value === confirmaSenhaInput.value;
                confirmaSenhaInput.setCustomValidity(senhasIguais ? "" : "As senhas não coincidem.");
            }

            if (novaSenhaInput && novaSenhaConfirmacaoInput) {
                var novasSenhasIguais = novaSenhaInput.value === novaSenhaConfirmacaoInput.value;
                novaSenhaConfirmacaoInput.setCustomValidity(novasSenhasIguais ? "" : "As senhas não coincidem.");
            }

            if (!form.checkValidity()) {
                form.reportValidity();
                return;
            }

            var feedbackTarget = form.getAttribute("data-feedback-target");
            var feedback = feedbackTarget ? document.getElementById(feedbackTarget) : null;
            if (feedback) {
                if (feedbackTarget === "alunoSenhaFeedback") {
                    feedback.textContent = "Nova senha preparada para atualização. Integre este formulário ao backend para salvar a redefinição.";
                } else if (feedbackTarget === "alunoEditFeedback") {
                    feedback.textContent = "Dados do aluno preparados para atualização. Integre este formulário ao backend para persistir as alterações.";
                } else if (feedbackTarget === "turmaFormFeedback") {
                    feedback.textContent = "Turma preparada para cadastro. Integre este formulário ao backend para persistir no banco.";
                } else if (feedbackTarget === "turmaEditFeedback") {
                    feedback.textContent = "Dados da turma preparados para atualização. Integre este formulário ao backend para persistir as alterações.";
                } else if (feedbackTarget === "turmaDeleteFeedback") {
                    feedback.textContent = "Exclusão da turma preparada. Integre este formulário ao backend para efetivar a remoção.";
                } else {
                    feedback.textContent = "Aluno e credenciais de acesso preparados para cadastro. Integre este formulário ao backend para persistir no banco.";
                }
            }

            var modal = form.closest(".modal-overlay");
            if (modal) {
                modal.classList.remove("is-open");
                if (typeof modal.close === "function" && modal.open) {
                    modal.close();
                }
            }

            form.reset();

            if (confirmaSenhaInput) {
                confirmaSenhaInput.setCustomValidity("");
            }

            if (novaSenhaConfirmacaoInput) {
                novaSenhaConfirmacaoInput.setCustomValidity("");
            }
        });
    });
});
