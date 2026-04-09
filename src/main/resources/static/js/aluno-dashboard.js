(function () {
    var navItems = Array.prototype.slice.call(document.querySelectorAll('[data-student-nav]'));
    var panels = Array.prototype.slice.call(document.querySelectorAll('[data-student-panel]'));
    var content = document.querySelector('[data-student-content]');
    var allowedSections = ['painel', 'materiais', 'boletim', 'financeiro'];

    if (!navItems.length || !panels.length) {
        return;
    }

    function activateSection(sectionKey) {
        if (allowedSections.indexOf(sectionKey) === -1) {
            sectionKey = 'painel';
        }

        panels.forEach(function (panel) {
            panel.classList.toggle('is-active', panel.getAttribute('data-student-panel') === sectionKey);
        });

        navItems.forEach(function (item) {
            item.classList.toggle('is-active', item.getAttribute('data-student-nav') === sectionKey);
        });

        var params = new URLSearchParams(window.location.search);
        if (sectionKey === 'painel') {
            params.delete('painel');
        } else {
            params.set('painel', sectionKey);
        }

        var query = params.toString();
        var nextUrl = query ? (window.location.pathname + '?' + query) : window.location.pathname;
        window.history.replaceState({}, '', nextUrl);
    }

    navItems.forEach(function (item) {
        item.addEventListener('click', function () {
            activateSection(item.getAttribute('data-student-nav'));
        });
    });

    var panelFromQuery = new URLSearchParams(window.location.search).get('painel');
    var panelFromServer = content ? content.getAttribute('data-student-initial-panel') : null;
    activateSection(panelFromQuery || panelFromServer || 'painel');
})();
