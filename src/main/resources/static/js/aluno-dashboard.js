(function () {
    var navItems = Array.prototype.slice.call(document.querySelectorAll('[data-student-nav]'));
    var panels = Array.prototype.slice.call(document.querySelectorAll('[data-student-panel]'));

    if (!navItems.length || !panels.length) {
        return;
    }

    function activateSection(sectionKey) {
        panels.forEach(function (panel) {
            panel.classList.toggle('is-active', panel.getAttribute('data-student-panel') === sectionKey);
        });

        navItems.forEach(function (item) {
            item.classList.toggle('is-active', item.getAttribute('data-student-nav') === sectionKey);
        });
    }

    navItems.forEach(function (item) {
        item.addEventListener('click', function () {
            activateSection(item.getAttribute('data-student-nav'));
        });
    });

    activateSection('painel');
})();
