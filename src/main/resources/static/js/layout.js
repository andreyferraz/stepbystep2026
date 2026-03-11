(function () {
    var toggle = document.querySelector('[data-menu-toggle]');
    var menu = document.querySelector('[data-menu]');

    if (!toggle || !menu) {
        return;
    }

    toggle.addEventListener('click', function () {
        var opened = menu.classList.toggle('is-open');
        toggle.setAttribute('aria-expanded', String(opened));
    });

    menu.addEventListener('click', function (event) {
        if (event.target.tagName === 'A' && menu.classList.contains('is-open')) {
            menu.classList.remove('is-open');
            toggle.setAttribute('aria-expanded', 'false');
        }
    });

    window.addEventListener('resize', function () {
        if (window.innerWidth > 960 && menu.classList.contains('is-open')) {
            menu.classList.remove('is-open');
            toggle.setAttribute('aria-expanded', 'false');
        }
    });
})();
