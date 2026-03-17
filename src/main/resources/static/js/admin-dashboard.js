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

    function activatePanelFromQuery() {
        var params = new URLSearchParams(window.location.search);
        var panelFromQuery = params.get("panel");

        if (!panelFromQuery) {
            return false;
        }

        var queryLink = findLinkByTarget(panelFromQuery);
        if (!queryLink) {
            return false;
        }

        activatePanel(panelFromQuery, queryLink.getAttribute("data-panel-title"));
        localStorage.setItem(panelStorageKey, panelFromQuery);
        return true;
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

    var panelDefinidoPorQuery = activatePanelFromQuery();

    if (!panelDefinidoPorQuery) {
        var savedPanel = localStorage.getItem(panelStorageKey);
        if (savedPanel) {
            var savedLink = findLinkByTarget(savedPanel);
            if (savedLink) {
                activatePanel(savedPanel, savedLink.getAttribute("data-panel-title"));
            }
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
                    var alunoId = button.getAttribute("data-aluno-id") || "";
                    var nomeAluno = button.getAttribute("data-aluno-nome") || "";
                    var loginAluno = button.getAttribute("data-aluno-login") || "";
                    var alunoIdInput = modal.querySelector("#alunoSenhaId");
                    var alunoNomeInput = modal.querySelector("#alunoSenhaNome");
                    var alunoLoginInput = modal.querySelector("#alunoSenhaLogin");

                    if (alunoIdInput) {
                        alunoIdInput.value = alunoId;
                    }

                    if (alunoNomeInput) {
                        alunoNomeInput.value = nomeAluno;
                    }

                    if (alunoLoginInput) {
                        alunoLoginInput.value = loginAluno;
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
                } else if (feedbackTarget === "materialFormFeedback") {
                    feedback.textContent = "Material preparado para cadastro. Integre este formulário ao backend para salvar e publicar no portal do aluno.";
                } else if (feedbackTarget === "materialPastaFeedback") {
                    feedback.textContent = "Pasta preparada para criacao. Integre este formulário ao backend para persistir a estrutura de materiais.";
                } else if (feedbackTarget === "notasFormFeedback") {
                    feedback.textContent = "Lancamento de nota e presenca preparado. Integre este formulario ao backend para atualizar diario e boletim.";
                } else if (feedbackTarget === "mensalidadeFormFeedback") {
                    feedback.textContent = "Pagamento registrado em modo estatico. Integre este formulario ao backend para atualizar status da mensalidade e salvar a baixa.";
                } else if (feedbackTarget === "mensalidadeCobrancaFeedback") {
                    feedback.textContent = "Cobranca gerada em modo estatico. Integre este formulario ao backend para emitir PIX/boleto e notificar os responsaveis.";
                } else if (feedbackTarget === "inadimplenciaLembreteFeedback") {
                    feedback.textContent = "Lembrete preparado em modo estatico. Integre ao backend para enviar a mensagem e registrar o historico de contato.";
                } else if (feedbackTarget === "inadimplenciaAcordoFeedback") {
                    feedback.textContent = "Acordo preparado em modo estatico. Integre ao backend para salvar parcelas, vencimentos e saldo devedor atualizado.";
                } else if (feedbackTarget === "preInscricaoFormFeedback") {
                    feedback.textContent = "Lead preparado para cadastro em modo estatico. Integre ao backend para persistir a pre-inscricao e iniciar o fluxo comercial.";
                } else if (feedbackTarget === "preInscricaoContatoFeedback") {
                    feedback.textContent = "Contato do lead preparado em modo estatico. Integre ao backend para salvar historico e atualizar status automaticamente.";
                } else if (feedbackTarget === "blogFormFeedback") {
                    feedback.textContent = "Postagem preparada em modo estatico. Integre ao backend para salvar e publicar no blog da escola.";
                } else if (feedbackTarget === "blogEditFeedback") {
                    feedback.textContent = "Edicao de postagem preparada em modo estatico. Integre ao backend para versionar e atualizar o conteudo publicado.";
                } else if (feedbackTarget === "livroFormFeedback") {
                    feedback.textContent = "Obra literaria preparada em modo estatico. Integre ao backend para salvar dados e upload da imagem de capa.";
                } else if (feedbackTarget === "livroEditFeedback") {
                    feedback.textContent = "Edicao da obra preparada em modo estatico. Integre ao backend para atualizar catalogo, capa e link de compra.";
                } else if (feedbackTarget === "galeriaFormFeedback") {
                    feedback.textContent = "Foto preparada para upload em modo estatico. Integre ao backend para armazenar o arquivo e atualizar a galeria.";
                } else if (feedbackTarget === "galeriaEditFeedback") {
                    feedback.textContent = "Edicao de foto preparada em modo estatico. Integre ao backend para salvar metadados e substituir a imagem quando necessario.";
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

    var formNovoAluno = document.getElementById("formNovoAluno");
    if (formNovoAluno) {
        formNovoAluno.addEventListener("submit", function (event) {
            var senhaInput = formNovoAluno.querySelector("#alunoSenha");
            var confirmaSenhaInput = formNovoAluno.querySelector("#alunoSenhaConfirmacao");

            if (!senhaInput || !confirmaSenhaInput) {
                return;
            }

            var senhasIguais = senhaInput.value === confirmaSenhaInput.value;
            confirmaSenhaInput.setCustomValidity(senhasIguais ? "" : "As senhas não coincidem.");

            if (!formNovoAluno.checkValidity()) {
                event.preventDefault();
                formNovoAluno.reportValidity();
            }
        });
    }

    var formEditarAluno = document.getElementById("formEditarAluno");
    if (formEditarAluno) {
        formEditarAluno.addEventListener("submit", function (event) {
            if (!formEditarAluno.checkValidity()) {
                event.preventDefault();
                formEditarAluno.reportValidity();
            }
        });
    }

    var formRedefinirSenhaAluno = document.getElementById("formRedefinirSenhaAluno");
    if (formRedefinirSenhaAluno) {
        formRedefinirSenhaAluno.addEventListener("submit", function (event) {
            var novaSenhaInput = formRedefinirSenhaAluno.querySelector("#alunoNovaSenha");
            var novaSenhaConfirmacaoInput = formRedefinirSenhaAluno.querySelector("#alunoNovaSenhaConfirmacao");

            if (!novaSenhaInput || !novaSenhaConfirmacaoInput) {
                return;
            }

            var novasSenhasIguais = novaSenhaInput.value === novaSenhaConfirmacaoInput.value;
            novaSenhaConfirmacaoInput.setCustomValidity(novasSenhasIguais ? "" : "As senhas não coincidem.");

            if (!formRedefinirSenhaAluno.checkValidity()) {
                event.preventDefault();
                formRedefinirSenhaAluno.reportValidity();
            }
        });
    }

    var formNovaTurma = document.getElementById("formNovaTurma");
    if (formNovaTurma) {
        formNovaTurma.addEventListener("submit", function (event) {
            if (!formNovaTurma.checkValidity()) {
                event.preventDefault();
                formNovaTurma.reportValidity();
            }
        });
    }

    var formEditarTurma = document.getElementById("formEditarTurma");
    if (formEditarTurma) {
        formEditarTurma.addEventListener("submit", function (event) {
            if (!formEditarTurma.checkValidity()) {
                event.preventDefault();
                formEditarTurma.reportValidity();
            }
        });
    }
});
