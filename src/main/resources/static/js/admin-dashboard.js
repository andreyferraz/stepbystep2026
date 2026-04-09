document.addEventListener("DOMContentLoaded", function () {
    var panelStorageKey = "adminDashboardActivePanel";
    var preInscricaoNotificationStorageKey = "adminPreInscricaoLastSeenEpoch";
    var sidebar = document.getElementById("adminSidebar");
    var toggleButton = document.getElementById("adminMenuToggle");
    var panelHeading = document.getElementById("panelHeading");
    var navLinks = document.querySelectorAll(".admin-nav-link[data-panel-target]");
    var panels = document.querySelectorAll(".admin-panel[data-panel]");
    var bellButton = document.getElementById("adminBellBtn");
    var bellBadge = document.getElementById("adminBellBadge");
    var notificationsPanel = document.getElementById("adminNotificationsPanel");
    var notificationsList = document.getElementById("adminNotificationsList");
    var notificationsMarkReadButton = document.getElementById("adminNotificationsMarkRead");
    var latestNotificationEpochMillis = Number(localStorage.getItem(preInscricaoNotificationStorageKey) || "0");

    function formatarDataHora(epochMillis) {
        if (!epochMillis || Number.isNaN(epochMillis)) {
            return "agora";
        }

        return new Date(epochMillis).toLocaleString("pt-BR", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        });
    }

    function atualizarBadgeNotificacoes(unreadCount) {
        if (!bellBadge) {
            return;
        }

        var quantidade = Number(unreadCount || 0);
        if (quantidade <= 0) {
            bellBadge.hidden = true;
            bellBadge.textContent = "0";
            return;
        }

        bellBadge.hidden = false;
        bellBadge.textContent = quantidade > 99 ? "99+" : String(quantidade);
    }

    function renderizarNotificacoes(items) {
        if (!notificationsList) {
            return;
        }

        notificationsList.innerHTML = "";
        if (!Array.isArray(items) || items.length === 0) {
            var empty = document.createElement("li");
            empty.className = "admin-notification-empty";
            empty.textContent = "Sem notificações no momento.";
            notificationsList.appendChild(empty);
            return;
        }

        items.forEach(function (item) {
            var li = document.createElement("li");
            li.className = "admin-notification-item";

            var link = document.createElement("a");
            link.className = "admin-notification-link";
            link.href = item.destinoUrl || "/admin/dashboard?panel=pre-inscricoes";

            var nome = document.createElement("strong");
            nome.textContent = item.nomeInteressado || "Nova pré-inscrição";

            var causa = document.createElement("p");
            causa.className = "admin-notification-cause";
            causa.textContent = item.causa || "Nova pré-inscrição recebida.";

            var detalhes = document.createElement("p");
            var telefone = item.whatsapp ? " • " + item.whatsapp : "";
            detalhes.textContent = formatarDataHora(item.dataLeadEpochMillis) + telefone;

            link.appendChild(nome);
            link.appendChild(causa);
            link.appendChild(detalhes);
            li.appendChild(link);
            notificationsList.appendChild(li);
        });
    }

    function destacarLeadPorHash() {
        var hash = window.location.hash || "";
        if (hash.indexOf("#lead-") !== 0) {
            return;
        }

        var linha = document.querySelector(hash);
        if (!linha) {
            return;
        }

        linha.classList.remove("admin-highlight-row");
        window.setTimeout(function () {
            linha.classList.add("admin-highlight-row");
            linha.scrollIntoView({ behavior: "smooth", block: "center" });
        }, 60);
    }

    function marcarNotificacoesComoLidas() {
        if (latestNotificationEpochMillis > 0) {
            localStorage.setItem(preInscricaoNotificationStorageKey, String(latestNotificationEpochMillis));
        }
        atualizarBadgeNotificacoes(0);
    }

    function painelNotificacoesEstaAberto() {
        if (!notificationsPanel) {
            return false;
        }

        if (typeof notificationsPanel.open === "boolean") {
            return notificationsPanel.open;
        }

        return !notificationsPanel.hidden;
    }

    function abrirPainelNotificacoes() {
        if (!notificationsPanel) {
            return;
        }

        notificationsPanel.hidden = false;
        if (typeof notificationsPanel.show === "function" && !notificationsPanel.open) {
            notificationsPanel.show();
        }

        if (bellButton) {
            bellButton.setAttribute("aria-expanded", "true");
        }
    }

    function fecharPainelNotificacoes() {
        if (!notificationsPanel) {
            return;
        }

        if (typeof notificationsPanel.close === "function" && notificationsPanel.open) {
            notificationsPanel.close();
        }

        notificationsPanel.hidden = true;
        if (bellButton) {
            bellButton.setAttribute("aria-expanded", "false");
        }
    }

    function consultarNotificacoesPreInscricoes() {
        var afterEpoch = Number(localStorage.getItem(preInscricaoNotificationStorageKey) || "0");
        var url = "/admin/notificacoes/pre-inscricoes?afterEpochMillis=" + encodeURIComponent(afterEpoch) + "&limit=8";

        fetch(url, { headers: { "Accept": "application/json" } })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("Falha ao carregar notificações");
                }
                return response.json();
            })
            .then(function (payload) {
                latestNotificationEpochMillis = Number(payload.latestEpochMillis || 0);
                atualizarBadgeNotificacoes(payload.unreadCount || 0);
                renderizarNotificacoes(payload.items || []);
            })
            .catch(function () {
                renderizarNotificacoes([]);
            });
    }

    if (bellButton && notificationsPanel) {
        bellButton.addEventListener("click", function () {
            if (painelNotificacoesEstaAberto()) {
                fecharPainelNotificacoes();
                return;
            }

            abrirPainelNotificacoes();
            marcarNotificacoesComoLidas();
        });

        document.addEventListener("click", function (event) {
            if (!painelNotificacoesEstaAberto()) {
                return;
            }

            if (notificationsPanel.contains(event.target) || bellButton.contains(event.target)) {
                return;
            }

            fecharPainelNotificacoes();
        });

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape" && painelNotificacoesEstaAberto()) {
                fecharPainelNotificacoes();
            }
        });
    }

    if (notificationsMarkReadButton) {
        notificationsMarkReadButton.addEventListener("click", function () {
            marcarNotificacoesComoLidas();
        });
    }

    window.addEventListener("hashchange", destacarLeadPorHash);

    consultarNotificacoesPreInscricoes();
    window.setInterval(consultarNotificacoesPreInscricoes, 30000);

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

    function syncPanelInUrl(target) {
        if (!window.history || typeof window.history.replaceState !== "function") {
            return;
        }

        var url = new URL(window.location.href);
        url.searchParams.set("panel", target);

        // Keep only the filter that belongs to the active panel.
        if (target !== "alunos") {
            url.searchParams.delete("alunoBusca");
        }

        if (target !== "turmas") {
            url.searchParams.delete("turmaBusca");
        }

        if (target !== "materiais") {
            url.searchParams.delete("materialBusca");
            url.searchParams.delete("materialTurmaId");
        }

        if (target !== "notas") {
            url.searchParams.delete("notaBusca");
            url.searchParams.delete("notaTurmaId");
            url.searchParams.delete("notaBimestre");
        }

        if (target !== "galeria") {
            url.searchParams.delete("galeriaBusca");
            url.searchParams.delete("galeriaCategoriaId");
        }

        window.history.replaceState({}, "", url.toString());
    }

    navLinks.forEach(function (link) {
        link.addEventListener("click", function () {
            var target = link.getAttribute("data-panel-target");
            var title = link.getAttribute("data-panel-title");
            activatePanel(target, title);
            syncPanelInUrl(target);
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

    destacarLeadPorHash();

    if (toggleButton && sidebar) {
        toggleButton.addEventListener("click", function () {
            sidebar.classList.toggle("is-open");
        });
    }

    var openModalButtons = document.querySelectorAll("[data-open-modal]");
    var closeModalButtons = document.querySelectorAll("[data-close-modal]");
    var editorSelectionRange = null;
    var notaAlunoSelect = document.getElementById("notaAluno");
    var notaTurmaSelect = document.getElementById("notaTurma");
    var notaDataInput = document.getElementById("notaData");
    var presencaAlunoSelect = document.getElementById("presencaAluno");
    var presencaTurmaSelect = document.getElementById("presencaTurma");
    var presencaDataInput = document.getElementById("presencaData");
    var mensalidadeAlunoSelect = document.getElementById("mensalidadeAluno");
    var mensalidadeFinanceiraSelect = document.getElementById("mensalidadeFinanceira");
    var inadimplenciaAlunoLembreteSelect = document.getElementById("inadimplenciaAlunoLembrete");
    var inadimplenciaMensalidadeLembreteSelect = document.getElementById("inadimplenciaMensalidadeLembrete");
    var inadimplenciaAlunoAcordoSelect = document.getElementById("inadimplenciaAlunoAcordo");
    var inadimplenciaMensalidadeAcordoSelect = document.getElementById("inadimplenciaMensalidadeAcordo");

    // Defensive reset: prevents stale dialog/backdrop state after form submit + navigation.
    document.querySelectorAll(".modal-overlay").forEach(function (modal) {
        modal.classList.remove("is-open");
        if (typeof modal.close === "function" && modal.open) {
            modal.close();
        }
    });

    function sincronizarTurmaPorAlunoSelecionado(alunoSelect, turmaSelect) {
        if (!alunoSelect || !turmaSelect) {
            return;
        }

        var alunoSelecionado = alunoSelect.options[alunoSelect.selectedIndex];
        if (!alunoSelecionado) {
            return;
        }

        var turmaId = alunoSelecionado.getAttribute("data-turma-id") || "";
        if (!turmaId) {
            return;
        }

        turmaSelect.value = turmaId;
    }

    function preencherDataAtualNoLancamento(dataInput) {
        if (!dataInput) {
            return;
        }

        var hoje = new Date();
        var ano = String(hoje.getFullYear());
        var mes = String(hoje.getMonth() + 1).padStart(2, "0");
        var dia = String(hoje.getDate()).padStart(2, "0");
        dataInput.value = ano + "-" + mes + "-" + dia;
    }

    function salvarSelecaoEditor(editor) {
        var selection = window.getSelection();
        if (!selection || selection.rangeCount === 0) {
            return;
        }

        var range = selection.getRangeAt(0);
        if (editor.contains(range.commonAncestorContainer)) {
            editorSelectionRange = range;
        }
    }

    function restaurarSelecaoEditor(editor) {
        if (!editorSelectionRange) {
            editor.focus();
            return;
        }

        var selection = window.getSelection();
        if (!selection) {
            return;
        }

        selection.removeAllRanges();
        selection.addRange(editorSelectionRange);
    }

    function sincronizarEditorComTextarea(editor) {
        if (!editor) {
            return;
        }

        var textareaId = editor.getAttribute("data-editor-textarea");
        if (!textareaId) {
            return;
        }

        var textarea = document.getElementById(textareaId);
        if (!textarea) {
            return;
        }

        textarea.value = editor.innerHTML.trim();
    }

    function inicializarEditorRico(editorId) {
        var editor = document.getElementById(editorId);
        if (!editor) {
            return;
        }

        editor.addEventListener("input", function () {
            sincronizarEditorComTextarea(editor);
        });

        editor.addEventListener("keyup", function () {
            salvarSelecaoEditor(editor);
        });

        editor.addEventListener("mouseup", function () {
            salvarSelecaoEditor(editor);
        });

        sincronizarEditorComTextarea(editor);
    }

    function obterTokenCsrf(form) {
        if (!form) {
            return null;
        }

        var tokenInput = form.querySelector("input[type='hidden']");
        if (!tokenInput || !tokenInput.name || !tokenInput.value) {
            return null;
        }

        return {
            name: tokenInput.name,
            value: tokenInput.value
        };
    }

    function inserirImagemNoEditor(editor, urlImagem) {
        restaurarSelecaoEditor(editor);
        var inseriu = document.execCommand("insertImage", false, urlImagem);
        if (!inseriu) {
            editor.innerHTML += "<p><img src=\"" + urlImagem + "\" alt=\"Imagem do conteúdo\"></p>";
        }
        sincronizarEditorComTextarea(editor);
    }

    function uploadImagemEditor(form, file, callbackSucesso) {
        var formData = new FormData();
        formData.append("imagem", file);

        var tokenCsrf = obterTokenCsrf(form);
        if (tokenCsrf) {
            formData.append(tokenCsrf.name, tokenCsrf.value);
        }

        fetch("/admin/blog/postagens/imagens", {
            method: "POST",
            body: formData
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("Falha ao enviar imagem.");
                }
                return response.json();
            })
            .then(function (payload) {
                if (!payload || !payload.url) {
                    throw new Error("Resposta de upload inválida.");
                }
                callbackSucesso(payload.url);
            })
            .catch(function () {
                window.alert("Nao foi possivel enviar a imagem para o conteúdo.");
            });
    }

    document.querySelectorAll("[data-editor-action]").forEach(function (button) {
        button.addEventListener("click", function () {
            var editorId = button.getAttribute("data-editor-target");
            var action = button.getAttribute("data-editor-action");
            var value = button.getAttribute("data-editor-value") || "";
            var editor = document.getElementById(editorId);
            if (!editor || !action) {
                return;
            }

            var form = button.closest("form");
            editor.focus();
            restaurarSelecaoEditor(editor);

            if (action === "createLink") {
                var url = window.prompt("Informe a URL do link:", "https://");
                if (!url) {
                    return;
                }
                document.execCommand("createLink", false, url);
                sincronizarEditorComTextarea(editor);
                return;
            }

            if (action === "insertImage") {
                var uploadInputId = button.getAttribute("data-editor-upload-input");
                var uploadInput = uploadInputId ? document.getElementById(uploadInputId) : null;
                if (!uploadInput) {
                    return;
                }

                uploadInput.value = "";
                uploadInput.onchange = function () {
                    if (!uploadInput.files || uploadInput.files.length === 0) {
                        return;
                    }
                    uploadImagemEditor(form, uploadInput.files[0], function (urlImagem) {
                        inserirImagemNoEditor(editor, urlImagem);
                    });
                };
                uploadInput.click();
                return;
            }

            if (action === "formatBlock") {
                document.execCommand(action, false, "<" + value + ">");
            } else {
                document.execCommand(action, false, value);
            }

            sincronizarEditorComTextarea(editor);
        });
    });

    inicializarEditorRico("blogConteudoEditor");

    var formNovaPostagem = document.getElementById("formNovaPostagem");
    if (formNovaPostagem) {
        formNovaPostagem.addEventListener("submit", function () {
            var editor = document.getElementById("blogConteudoEditor");
            sincronizarEditorComTextarea(editor);
        });
    }

    function filtrarMensalidadesPagamentoManual() {
        if (!mensalidadeAlunoSelect || !mensalidadeFinanceiraSelect) {
            return;
        }

        var alunoIdSelecionado = mensalidadeAlunoSelect.value || "";

        Array.from(mensalidadeFinanceiraSelect.options).forEach(function (option, index) {
            if (index === 0) {
                option.hidden = false;
                return;
            }

            var alunoIdOption = option.getAttribute("data-aluno-id") || "";
            var deveExibir = !alunoIdSelecionado || alunoIdOption === alunoIdSelecionado;
            option.hidden = !deveExibir;

            if (!deveExibir && option.selected) {
                mensalidadeFinanceiraSelect.value = "";
            }
        });
    }

    function filtrarMensalidadesInadimplencia(alunoSelect, mensalidadeSelect) {
        if (!alunoSelect || !mensalidadeSelect) {
            return;
        }

        var alunoIdSelecionado = alunoSelect.value || "";

        Array.from(mensalidadeSelect.options).forEach(function (option, index) {
            if (index === 0) {
                option.hidden = false;
                return;
            }

            var alunoIdOption = option.getAttribute("data-aluno-id") || "";
            var deveExibir = !alunoIdSelecionado || alunoIdOption === alunoIdSelecionado;
            option.hidden = !deveExibir;

            if (!deveExibir && option.selected) {
                mensalidadeSelect.value = "";
            }
        });
    }

    if (notaAlunoSelect) {
        notaAlunoSelect.addEventListener("change", function () {
            sincronizarTurmaPorAlunoSelecionado(notaAlunoSelect, notaTurmaSelect);
        });
    }

    if (presencaAlunoSelect) {
        presencaAlunoSelect.addEventListener("change", function () {
            sincronizarTurmaPorAlunoSelecionado(presencaAlunoSelect, presencaTurmaSelect);
        });
    }

    if (mensalidadeAlunoSelect) {
        mensalidadeAlunoSelect.addEventListener("change", filtrarMensalidadesPagamentoManual);
        filtrarMensalidadesPagamentoManual();
    }

    if (inadimplenciaAlunoLembreteSelect && inadimplenciaMensalidadeLembreteSelect) {
        inadimplenciaAlunoLembreteSelect.addEventListener("change", function () {
            filtrarMensalidadesInadimplencia(inadimplenciaAlunoLembreteSelect, inadimplenciaMensalidadeLembreteSelect);
        });
        filtrarMensalidadesInadimplencia(inadimplenciaAlunoLembreteSelect, inadimplenciaMensalidadeLembreteSelect);
    }

    if (inadimplenciaAlunoAcordoSelect && inadimplenciaMensalidadeAcordoSelect) {
        inadimplenciaAlunoAcordoSelect.addEventListener("change", function () {
            filtrarMensalidadesInadimplencia(inadimplenciaAlunoAcordoSelect, inadimplenciaMensalidadeAcordoSelect);
        });
        filtrarMensalidadesInadimplencia(inadimplenciaAlunoAcordoSelect, inadimplenciaMensalidadeAcordoSelect);
    }

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
                        "#editarTurmaVagasTotal": "data-turma-vagas-total",
                        "#editarTurmaVagasOcupadas": "data-turma-vagas-ocupadas",
                        "#editarTurmaDias": "data-turma-dias",
                        "#editarTurmaHorario": "data-turma-horario",
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
                    var alunosVinculados = Number(button.getAttribute("data-turma-alunos-vinculados") || button.getAttribute("data-turma-vagas-ocupadas") || "0");

                    if (turmaIdField) {
                        turmaIdField.value = button.getAttribute("data-turma-id") || "";
                    }

                    if (turmaNomeField) {
                        turmaNomeField.value = button.getAttribute("data-turma-nome") || "";
                    }

                    if (confirmarExcluirBtn) {
                        confirmarExcluirBtn.disabled = alunosVinculados > 0;
                    }

                    if (avisoRegra) {
                        if (alunosVinculados > 0) {
                            avisoRegra.textContent = "Exclusão bloqueada: existem " + alunosVinculados + " aluno(s) vinculado(s) a esta turma.";
                        } else {
                            avisoRegra.textContent = "Turma sem alunos vinculados. Exclusão liberada.";
                        }
                    }
                }

                if (target === "nota-lancar") {
                    sincronizarTurmaPorAlunoSelecionado(notaAlunoSelect, notaTurmaSelect);
                    preencherDataAtualNoLancamento(notaDataInput);
                }

                if (target === "presenca-lancar") {
                    sincronizarTurmaPorAlunoSelecionado(presencaAlunoSelect, presencaTurmaSelect);
                    preencherDataAtualNoLancamento(presencaDataInput);
                }

                if (target === "mensalidade-registrar") {
                    if (mensalidadeAlunoSelect) {
                        mensalidadeAlunoSelect.value = button.getAttribute("data-aluno-id") || mensalidadeAlunoSelect.value;
                    }
                    filtrarMensalidadesPagamentoManual();
                    if (mensalidadeFinanceiraSelect) {
                        mensalidadeFinanceiraSelect.value = button.getAttribute("data-mensalidade-id") || mensalidadeFinanceiraSelect.value;
                    }
                }

                if (target === "inadimplencia-lembrete") {
                    if (inadimplenciaAlunoLembreteSelect) {
                        inadimplenciaAlunoLembreteSelect.value = button.getAttribute("data-aluno-id") || inadimplenciaAlunoLembreteSelect.value;
                    }
                    filtrarMensalidadesInadimplencia(inadimplenciaAlunoLembreteSelect, inadimplenciaMensalidadeLembreteSelect);
                    if (inadimplenciaMensalidadeLembreteSelect) {
                        inadimplenciaMensalidadeLembreteSelect.value = button.getAttribute("data-mensalidade-id") || inadimplenciaMensalidadeLembreteSelect.value;
                    }
                }

                if (target === "inadimplencia-acordo") {
                    if (inadimplenciaAlunoAcordoSelect) {
                        inadimplenciaAlunoAcordoSelect.value = button.getAttribute("data-aluno-id") || inadimplenciaAlunoAcordoSelect.value;
                    }
                    filtrarMensalidadesInadimplencia(inadimplenciaAlunoAcordoSelect, inadimplenciaMensalidadeAcordoSelect);
                    if (inadimplenciaMensalidadeAcordoSelect) {
                        inadimplenciaMensalidadeAcordoSelect.value = button.getAttribute("data-mensalidade-id") || inadimplenciaMensalidadeAcordoSelect.value;
                    }
                }

                if (target === "preinscricao-contato") {
                    var preInscricaoLeadSelect = modal.querySelector("#preInscricaoLeadContato");
                    var preInscricaoId = button.getAttribute("data-preinscricao-id") || "";

                    if (preInscricaoLeadSelect && preInscricaoId) {
                        preInscricaoLeadSelect.value = preInscricaoId;
                    }
                }

                if (target === "blog-editar") {
                    var blogEditMap = {
                        "#blogEditarPostagemId": "data-blog-id",
                        "#blogEditarTitulo": "data-blog-titulo",
                        "#blogEditarCategoria": "data-blog-categoria",
                        "#blogEditarStatus": "data-blog-status",
                        "#blogEditarConteudo": "data-blog-conteudo",
                        "#blogEditarAutor": "data-blog-autor",
                        "#blogEditarDataPublicacao": "data-blog-data",
                        "#blogEditarResumo": "data-blog-resumo"
                    };

                    Object.keys(blogEditMap).forEach(function (selector) {
                        var field = modal.querySelector(selector);
                        if (field) {
                            field.value = button.getAttribute(blogEditMap[selector]) || "";
                        }
                    });
                }

                if (target === "livro-editar") {
                    var livroEditMap = {
                        "#livroEditarId": "data-livro-id",
                        "#livroEditarTitulo": "data-livro-titulo",
                        "#livroEditarAno": "data-livro-ano",
                        "#livroEditarSinopse": "data-livro-sinopse",
                        "#livroEditarLinkCompra": "data-livro-link"
                    };

                    Object.keys(livroEditMap).forEach(function (selector) {
                        var field = modal.querySelector(selector);
                        if (field) {
                            field.value = button.getAttribute(livroEditMap[selector]) || "";
                        }
                    });
                }

                if (target === "galeria-editar") {
                    var galeriaEditMap = {
                        "#galeriaEditarId": "data-galeria-id",
                        "#galeriaEditarLegenda": "data-galeria-legenda",
                        "#galeriaEditarDataUpload": "data-galeria-data",
                        "#galeriaEditarCategoria": "data-galeria-categoria-id"
                    };

                    Object.keys(galeriaEditMap).forEach(function (selector) {
                        var field = modal.querySelector(selector);
                        if (field) {
                            field.value = button.getAttribute(galeriaEditMap[selector]) || "";
                        }
                    });
                }

                if (target === "galeria-categoria-editar") {
                    var categoriaEditMap = {
                        "#galeriaCategoriaEditarId": "data-galeria-categoria-id",
                        "#galeriaCategoriaEditarNome": "data-galeria-categoria-nome"
                    };

                    Object.keys(categoriaEditMap).forEach(function (selector) {
                        var field = modal.querySelector(selector);
                        if (field) {
                            field.value = button.getAttribute(categoriaEditMap[selector]) || "";
                        }
                    });
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
                } else if (feedbackTarget === "notasFormFeedback") {
                    feedback.textContent = "Lancamento de nota e presenca preparado. Integre este formulario ao backend para atualizar diario e boletim.";
                } else if (feedbackTarget === "mensalidadeFormFeedback") {
                    feedback.textContent = "Pagamento registrado em modo estatico. Integre este formulario ao backend para atualizar status da mensalidade e salvar a baixa.";
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
