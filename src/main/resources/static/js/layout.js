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

    function initGallery() {
        var filterButtons = Array.prototype.slice.call(document.querySelectorAll('[data-gallery-filter]'));
        var items = Array.prototype.slice.call(document.querySelectorAll('[data-gallery-item]'));
        var lightbox = document.querySelector('[data-gallery-lightbox]');
        var lightboxImage = document.querySelector('[data-gallery-lightbox-image]');
        var lightboxCaption = document.querySelector('[data-gallery-lightbox-caption]');
        var openButtons = Array.prototype.slice.call(document.querySelectorAll('[data-gallery-open]'));
        var closeButtons = Array.prototype.slice.call(document.querySelectorAll('[data-gallery-close]'));
        var prevButton = document.querySelector('[data-gallery-prev]');
        var nextButton = document.querySelector('[data-gallery-next]');

        if (!items.length) {
            return;
        }

        var activeFilter = 'all';
        var visibleItems = items.slice();
        var currentIndex = 0;

        function applyFilter(filter) {
            activeFilter = filter;

            filterButtons.forEach(function (button) {
                button.classList.toggle('is-active', button.getAttribute('data-gallery-filter') === filter);
            });

            items.forEach(function (item) {
                var category = item.getAttribute('data-category');
                var shouldShow = filter === 'all' || category === filter;
                item.classList.toggle('is-hidden', !shouldShow);
            });

            visibleItems = items.filter(function (item) {
                return !item.classList.contains('is-hidden');
            });
        }

        function openLightbox(index) {
            if (!lightbox || !lightboxImage || !lightboxCaption || !visibleItems.length) {
                return;
            }

            currentIndex = index;
            var currentItem = visibleItems[currentIndex];
            var imageSrc = currentItem.getAttribute('data-image');
            var title = currentItem.getAttribute('data-title');
            var image = currentItem.querySelector('img');

            lightboxImage.src = imageSrc;
            lightboxImage.alt = image ? image.alt : title;
            lightboxCaption.textContent = title;

            lightbox.hidden = false;
            lightbox.setAttribute('aria-hidden', 'false');
            document.body.style.overflow = 'hidden';
        }

        function closeLightbox() {
            if (!lightbox) {
                return;
            }

            lightbox.hidden = true;
            lightbox.setAttribute('aria-hidden', 'true');
            document.body.style.overflow = '';
        }

        function showPrevious() {
            if (!visibleItems.length) {
                return;
            }
            currentIndex = (currentIndex - 1 + visibleItems.length) % visibleItems.length;
            openLightbox(currentIndex);
        }

        function showNext() {
            if (!visibleItems.length) {
                return;
            }
            currentIndex = (currentIndex + 1) % visibleItems.length;
            openLightbox(currentIndex);
        }

        filterButtons.forEach(function (button) {
            button.addEventListener('click', function () {
                applyFilter(button.getAttribute('data-gallery-filter'));
            });
        });

        openButtons.forEach(function (button) {
            button.addEventListener('click', function () {
                var item = button.closest('[data-gallery-item]');
                visibleItems = items.filter(function (galleryItem) {
                    return !galleryItem.classList.contains('is-hidden');
                });

                var index = visibleItems.indexOf(item);
                if (index >= 0) {
                    openLightbox(index);
                }
            });
        });

        closeButtons.forEach(function (button) {
            button.addEventListener('click', closeLightbox);
        });

        if (prevButton) {
            prevButton.addEventListener('click', showPrevious);
        }

        if (nextButton) {
            nextButton.addEventListener('click', showNext);
        }

        document.addEventListener('keydown', function (event) {
            if (!lightbox || lightbox.hidden) {
                return;
            }

            if (event.key === 'Escape') {
                closeLightbox();
            }

            if (event.key === 'ArrowLeft') {
                showPrevious();
            }

            if (event.key === 'ArrowRight') {
                showNext();
            }
        });

        applyFilter(activeFilter);
    }

    function initPasswordToggle() {
    var toggle = document.querySelector('[data-password-toggle]');
    var passwordInput = document.querySelector('#password');

    if (!toggle || !passwordInput) {
        return;
    }

    toggle.addEventListener('click', function () {
        var isPassword = passwordInput.getAttribute('type') === 'password';
        passwordInput.setAttribute('type', isPassword ? 'text' : 'password');
        toggle.setAttribute('aria-label', isPassword ? 'Ocultar senha' : 'Mostrar ou ocultar senha');
    });
}

    function initPreInscricaoForm() {
        var form = document.querySelector('[data-pre-inscricao-form]');
        if (!form) {
            return;
        }

        var feedback = form.querySelector('[data-pre-inscricao-feedback]');
        var submitButton = form.querySelector('.lead-submit');
        if (!feedback || !submitButton) {
            return;
        }

        function mostrarFeedback(mensagem, sucesso) {
            feedback.hidden = false;
            feedback.textContent = mensagem;
            feedback.classList.toggle('lead-submit-feedback-error', !sucesso);
            feedback.setAttribute('aria-live', sucesso ? 'polite' : 'assertive');
        }

        form.addEventListener('submit', async function (event) {
            event.preventDefault();

            submitButton.disabled = true;
            submitButton.textContent = 'Enviando...';
            feedback.hidden = true;
            feedback.classList.remove('lead-submit-feedback-error');

            try {
                var resposta = await fetch(form.action, {
                    method: form.method || 'POST',
                    body: new FormData(form),
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest',
                        'Accept': 'application/json'
                    }
                });

                var dados = null;
                try {
                    dados = await resposta.json();
                } catch (_) {
                    dados = null;
                }

                if (!resposta.ok) {
                    var mensagemErro = dados && dados.mensagem
                        ? dados.mensagem
                        : 'Nao foi possivel processar sua pre-inscricao. Tente novamente.';
                    mostrarFeedback(mensagemErro, false);
                    return;
                }

                var mensagemSucesso = dados && dados.mensagem
                    ? dados.mensagem
                    : 'Pre-inscricao enviada com sucesso. Em breve entraremos em contato.';
                mostrarFeedback(mensagemSucesso, true);
                form.reset();
            } catch (_) {
                mostrarFeedback('Falha de conexao. Verifique sua internet e tente novamente.', false);
            } finally {
                submitButton.disabled = false;
                submitButton.textContent = 'Enviar Interesse';
            }
        });
    }

    initSpaceCarousel();

    initGallery();

    initPasswordToggle();

    initPreInscricaoForm();

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
