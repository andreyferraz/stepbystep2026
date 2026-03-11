(function () {
    var toggle = document.querySelector('[data-menu-toggle]');
    var menu = document.querySelector('[data-menu]');
    var navLinks = menu ? Array.prototype.slice.call(menu.querySelectorAll('a')) : [];

    function initSpaceCarousel() {
        var carousel = document.querySelector('[data-space-carousel]');

        if (!carousel) {
            return;
        }

        var slides = Array.prototype.slice.call(carousel.querySelectorAll('.space-slide'));
        var prevBtn = carousel.querySelector('[data-space-prev]');
        var nextBtn = carousel.querySelector('[data-space-next]');
        var dotsHost = carousel.querySelector('[data-space-dots]');
        var currentIndex = 0;

        if (!slides.length || !prevBtn || !nextBtn || !dotsHost) {
            return;
        }

        var dots = slides.map(function (_, index) {
            var dot = document.createElement('button');
            dot.type = 'button';
            dot.className = 'space-dot';
            dot.setAttribute('aria-label', 'Ir para foto ' + (index + 1));
            dotsHost.appendChild(dot);
            return dot;
        });

        function render() {
            slides.forEach(function (slide, index) {
                slide.classList.toggle('is-active', index === currentIndex);
            });

            dots.forEach(function (dot, index) {
                dot.classList.toggle('is-active', index === currentIndex);
            });
        }

        prevBtn.addEventListener('click', function () {
            currentIndex = (currentIndex - 1 + slides.length) % slides.length;
            render();
        });

        nextBtn.addEventListener('click', function () {
            currentIndex = (currentIndex + 1) % slides.length;
            render();
        });

        dots.forEach(function (dot, index) {
            dot.addEventListener('click', function () {
                currentIndex = index;
                render();
            });
        });

        render();
    }

    initSpaceCarousel();

    if (!toggle || !menu) {
        return;
    }

    function normalizePath(path) {
        if (!path || path === '/') {
            return '/';
        }
        return path.endsWith('/') ? path.slice(0, -1) : path;
    }

    function parseLinkUrl(link) {
        return new URL(link.getAttribute('href'), window.location.origin);
    }

    function setActiveNavLink() {
        var currentPath = normalizePath(window.location.pathname);
        var currentHash = window.location.hash;
        var activeLink = null;

        navLinks.forEach(function (link) {
            link.classList.remove('is-active');
        });

        if (currentPath === '/' && currentHash) {
            activeLink = navLinks.find(function (link) {
                var url = parseLinkUrl(link);
                return normalizePath(url.pathname) === '/' && url.hash === currentHash;
            }) || null;
        }

        if (!activeLink) {
            activeLink = navLinks.find(function (link) {
                var url = parseLinkUrl(link);
                return normalizePath(url.pathname) === currentPath && !url.hash;
            }) || null;
        }

        if (!activeLink && currentPath === '/') {
            activeLink = navLinks.find(function (link) {
                return normalizePath(parseLinkUrl(link).pathname) === '/' && !parseLinkUrl(link).hash;
            }) || null;
        }

        if (activeLink) {
            activeLink.classList.add('is-active');
        }
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

        if (event.target.tagName === 'A') {
            setActiveNavLink();
        }
    });

    window.addEventListener('resize', function () {
        if (window.innerWidth > 960 && menu.classList.contains('is-open')) {
            menu.classList.remove('is-open');
            toggle.setAttribute('aria-expanded', 'false');
        }
    });

    window.addEventListener('hashchange', setActiveNavLink);

    setActiveNavLink();
})();
