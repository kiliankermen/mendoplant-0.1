/* Plant Finder - interacciones de la portada */
(function () {
    'use strict';

    function iniciar() {
        // Toggle del panel de filtros (desplegable).
        // Puede haber dos botones: el del formulario (desktop) y una copia
        // móvil dentro de las categorías rápidas. Ambos alternan el panel.
        var botonesFiltro = document.querySelectorAll('.mp-filter-btn');
        var panel = document.getElementById('filtroPanel');
        var filtroScroll = panel ? panel.querySelector('.mp-filtro-scroll') : null;
        function actualizarDegradeFiltro() {
            if (!filtroScroll) return;
            var hayScroll = filtroScroll.scrollHeight > filtroScroll.clientHeight + 4;
            var alFondo = filtroScroll.scrollTop + filtroScroll.clientHeight >= filtroScroll.scrollHeight - 4;
            panel.classList.toggle('al-fondo', !hayScroll || alFondo);
        }
        // Valores con los que se cargó el panel; sirven para detectar si el
        // usuario toca "Aplicar filtros" sin haber modificado ningún valor.
        var valoresIniciales = null;
        function serializarFiltro() {
            var datos = [];
            for (var par of new FormData(panel).entries()) {
                datos.push(par[0] + '=' + par[1]);
            }
            return datos.sort().join('&');
        }
        function todosLosBotones(fn, abierto) {
            botonesFiltro.forEach(function (b) {
                fn(b, abierto);
            });
        }
        function etiquetaDe(b) {
            return b.querySelector('span[data-cerrar]') || b.querySelector('.mp-filter-btn span');
        }
        function pintarBotones(abierto) {
            todosLosBotones(function (b, abierto) {
                b.setAttribute('aria-expanded', abierto ? 'true' : 'false');
                b.classList.toggle('on', abierto);
                var etiqueta = etiquetaDe(b);
                if (etiqueta) {
                    etiqueta.textContent = abierto ? 'Aplicar filtros'
                        : (etiqueta.getAttribute('data-cerrar') || 'Buscar por precio');
                }
            }, abierto);
        }
        function cerrarPanel() {
            abierto = false;
            panel.style.display = 'none';
            pintarBotones(false);
        }
        if (botonesFiltro.length && panel) {
            var abierto = panel.style.display !== 'none';
            valoresIniciales = serializarFiltro();
            botonesFiltro.forEach(function (btn) {
                btn.addEventListener('click', function () {
                    if (!abierto) {
                        // Abre el panel de filtros
                        abierto = true;
                        panel.style.display = '';
                        pintarBotones(true);
                        actualizarDegradeFiltro();
                    } else if (btn.getAttribute('data-cerrar-sin-cambios') === 'true'
                               && serializarFiltro() === valoresIniciales) {
                        // No se modificó ningún valor: solo se cierra el panel
                        cerrarPanel();
                    } else {
                        // Aplica los filtros elegidos (refresca solo la grilla)
                        sincronizarModoFiltro();
                        refrescarResultado('filtro');
                    }
                });
            });
            // Cierra el panel de filtros sin aplicar cambios
            var btnCerrar = document.getElementById('btnCerrarFiltros');
            if (btnCerrar) {
                btnCerrar.addEventListener('click', cerrarPanel);
            }
        }
        if (filtroScroll) {
            filtroScroll.addEventListener('scroll', actualizarDegradeFiltro);
            window.addEventListener('resize', actualizarDegradeFiltro);
        }

        // Desplegables con casillas (multi-valor): cada campo admite varias
        // opciones marcadas a la vez y el botón muestra cuántas hay.
        function refrescarMultiselects() {
            document.querySelectorAll('.mp-multiselect').forEach(function (ms) {
                var texto = ms.querySelector('.mp-multiselect-texto');
                var menu = ms.querySelector('.mp-multiselect-menu');
                if (!texto || !menu) return;
                var marcadas = Array.prototype.filter.call(
                    menu.querySelectorAll('input[type="checkbox"]'),
                    function (c) { return c.checked; });
                var todos = texto.getAttribute('data-texto') || 'Todos';
                if (marcadas.length === 0) {
                    texto.textContent = todos;
                } else if (marcadas.length === 1) {
                    texto.textContent = marcadas[0].parentNode.textContent.trim();
                } else {
                    texto.textContent = marcadas.length + ' seleccionados';
                }
            });
        }
        document.querySelectorAll('.mp-multiselect').forEach(function (ms) {
            var btn = ms.querySelector('.mp-multiselect-btn');
            var menu = ms.querySelector('.mp-multiselect-menu');
            if (!btn || !menu) return;
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                var abierto = ms.classList.contains('abierto');
                document.querySelectorAll('.mp-multiselect.abierto').forEach(function (otro) {
                    otro.classList.remove('abierto');
                    var b = otro.querySelector('.mp-multiselect-btn');
                    if (b) b.setAttribute('aria-expanded', 'false');
                });
                if (!abierto) {
                    ms.classList.add('abierto');
                    btn.setAttribute('aria-expanded', 'true');
                }
            });
            menu.addEventListener('click', function (e) { e.stopPropagation(); });
            menu.addEventListener('change', refrescarMultiselects);
        });
        // Un clic fuera de un desplegable lo cierra.
        document.addEventListener('click', function () {
            document.querySelectorAll('.mp-multiselect.abierto').forEach(function (ms) {
                ms.classList.remove('abierto');
                var b = ms.querySelector('.mp-multiselect-btn');
                if (b) b.setAttribute('aria-expanded', 'false');
            });
        });
        refrescarMultiselects();

        // Botón "Limpiar" del panel de filtros: desmarca todo y aplica.
        var btnLimpiar = document.getElementById('btnLimpiarFiltros');
        if (btnLimpiar) {
            btnLimpiar.addEventListener('click', function () {
                var panel = document.getElementById('filtroPanel');
                if (!panel) return;
                panel.querySelectorAll('input[type="checkbox"]').forEach(function (c) { c.checked = false; });
                panel.querySelectorAll('input[type="number"]').forEach(function (n) { n.value = ''; });
                panel.querySelectorAll('.mp-toggle-hidden').forEach(function (i) {
                    if (i.parentNode) i.parentNode.removeChild(i);
                });
                document.querySelectorAll('.mp-toggle-btn.on').forEach(function (b) { b.classList.remove('on'); });
                refrescarMultiselects();
                sincronizarModoFiltro();
                refrescarResultado('filtro');
            });
        }

        // Ajusta el top sticky del buscador para que no se solape con el header fijo
        var searchbarWrapTop = document.querySelector('.mp-searchbar-wrap');
        var headerEl = document.querySelector('.mp-header');
        var pedirAjusteToggleScrollGlobal = null;
        function ajustarTopSearchbar() {
            if (searchbarWrapTop && headerEl) {
                var headerHeight = headerEl.offsetHeight;
                document.documentElement.style.setProperty('--mp-header-height', headerHeight + 'px');
            }
            if (typeof pedirAjusteToggleScrollGlobal === 'function') {
                pedirAjusteToggleScrollGlobal();
            }
        }
        if (searchbarWrapTop && headerEl) {
            ajustarTopSearchbar();
            // Also run immediately to be safe
            setTimeout(ajustarTopSearchbar, 0);
            window.addEventListener('resize', ajustarTopSearchbar);
            window.addEventListener('load', ajustarTopSearchbar);
            var observer = new ResizeObserver(ajustarTopSearchbar);
            observer.observe(headerEl);
            observer.observe(searchbarWrapTop);
            // Al terminar la transición del `top` del buscador (cuando el header
            // aparece o se oculta), el sidebar se recalcula para quedar apoyado
            // en la posición final del buscador.
            searchbarWrapTop.addEventListener('transitionend', function (e) {
                if (e.propertyName === 'top' && typeof pedirAjusteToggleScrollGlobal === 'function') {
                    pedirAjusteToggleScrollGlobal();
                }
            });
        }

        // Sidebar de filtros (desktop): el alto del contenedor sticky se
        // ajusta a lo que queda por debajo de su posición real en el viewport,
        // así sus últimas opciones siempre quedan visibles tanto arriba del
        // todo (cuando el sticky aún no se ha clavado) como al llegar al fondo,
        // donde la griesa (.mp-shop-grid) ya no lo deja estirarse.
        // Se actualiza cada frame con requestAnimationFrame (anda igual de
        // rápido que lento) y, además, se hace una corrección final cuando el
        // scroll se asienta, para que la última medición (que en scroll rápido
        // puede quedar un frame atrás) se aplique al detenerse.
        var toggleScroll = document.querySelector('.mp-togglebar .mp-toggle-scroll');
        if (toggleScroll) {
            var mqDesktop = window.matchMedia('(min-width: 992px)');
            var gridToggle = toggleScroll.closest('.mp-shop-grid');
            var togglebar = toggleScroll.parentElement;
            var searchbarWrap = document.querySelector('.mp-searchbar-wrap');
            var ultimaAltura = null;
            var ultimoTop = null;
            var encoladoFrame = false;
            var timerSettle = null;
            function ajustarAlturaToggleScroll() {
                if (togglebar.classList.contains('mp-togglebar-oculto')) {
                    togglebar.style.maxHeight = '';
                    if (!mqDesktop.matches) {
                        // En móvil el botón usa la posición CSS (abajo a la derecha):
                        // se limpia cualquier top inline del sticky de escritorio.
                        togglebar.style.top = '';
                        ultimaAltura = null;
                        ultimoTop = null;
                        return;
                    }
                    // Escritorio: el botón flotante queda "empujado" por el buscador.
                    // Su tope sigue la posición actual del ojo (mp-searchbar-toggle)
                    // dentro de mp-searchbar-filtro-movil, así comparte su altura y se
                    // desliza con él cuando el header aparece u oculta.
                    var ojoTogglebar = document.querySelector('.mp-searchbar-filtro-movil .mp-searchbar-toggle');
                    if (document.body.classList.contains('mp-searchbar-oculto')) {
                        // Buscador oculto: el botón sigue al ojo del buscador (fijo).
                        if (ojoTogglebar) {
                            togglebar.style.top = Math.round(ojoTogglebar.getBoundingClientRect().top) + 'px';
                        }
                    } else if (searchbarWrap) {
                        // Buscador VISIBLE: el botón nunca debe tocarlo; se ubica
                        // unos pixeles debajo de su fondo mientras se scrollea.
                        togglebar.style.top = Math.floor(searchbarWrap.getBoundingClientRect().bottom + 12) + 'px';
                    }
                    ultimaAltura = null;
                    ultimoTop = null;
                    return;
                }
                if (!mqDesktop.matches) {
                    togglebar.style.maxHeight = '';
                    togglebar.style.top = '';
                    ultimaAltura = null;
                    ultimoTop = null;
                    return;
                }
                // Header, buscador y sidebar se "empujan" entre sí:
                //   - el header al aparecer/ocultarse mueve al buscador (top),
                //   - el buscador visible arrastra al sidebar a quedar justo
                //     debajo de él (top = su fondo + 12),
                //   - si el buscador está OCULTO (ojo cerrado), el sidebar
                //     sube a ocupar el hueco que dejó, apoyado en el header.
                var headerOcultoBar = document.body.classList.contains('mp-header-oculto');
                var searchbarOcultoBar = document.body.classList.contains('mp-searchbar-oculto');
                var headerAltoBar = headerEl ? headerEl.offsetHeight : 104;
                var topeSticky;
                if (searchbarOcultoBar) {
                    // Buscador oculto: ocupa el espacio vacío, justo debajo del
                    // header (o del tope si el header también está oculto).
                    topeSticky = (headerOcultoBar ? 10 : headerAltoBar) + 12;
                } else if (searchbarWrap) {
                    // Buscador visible: el sidebar queda pegado a su fondo.
                    topeSticky = Math.max(100, Math.floor(searchbarWrap.getBoundingClientRect().bottom + 12));
                } else {
                    topeSticky = 100;
                }
                if (topeSticky !== ultimoTop) {
                    ultimoTop = topeSticky;
                    togglebar.style.top = topeSticky + 'px';
                }
                var limiteInferior = window.innerHeight - 8;
                if (gridToggle) {
                    limiteInferior = Math.min(limiteInferior, gridToggle.getBoundingClientRect().bottom);
                }
                // Cuando el footer entra en visión, el fondo de la grilla sube
                // y el sticky es empujado hacia arriba por su contenedor: si
                // medimos el alto desde esa posición ya subida (y que además
                // en scroll rápido queda un frame atrás) la barra nunca vuelve
                // a su tope y queda oculta bajo la searchbar. Por eso se usa
                // el MÍNIMO entre el alto medido por posición real y el que
                // corresponde al tope fijo: así la barra encoge para quedarse
                // apoyada en el tope y llegar justo al fondo de la grilla.
                var altoPorPosicion = Math.floor(limiteInferior - togglebar.getBoundingClientRect().top - 16);
                var altoPorTope = Math.floor(limiteInferior - topeSticky - 16);
                var altoDisponible = Math.max(160, Math.min(altoPorPosicion, altoPorTope));
                if (altoDisponible !== ultimaAltura) {
                    ultimaAltura = altoDisponible;
                    togglebar.style.maxHeight = altoDisponible + 'px';
                }
            }
            function pedirAjusteToggleScroll() {
                if (!encoladoFrame) {
                    encoladoFrame = true;
                    requestAnimationFrame(function () {
                        encoladoFrame = false;
                        ajustarAlturaToggleScroll();
                    });
                }
                window.clearTimeout(timerSettle);
                timerSettle = window.setTimeout(ajustarAlturaToggleScroll, 120);
            }
            pedirAjusteToggleScrollGlobal = pedirAjusteToggleScroll;
            ajustarAlturaToggleScroll();
            window.addEventListener('scroll', pedirAjusteToggleScroll, { passive: true });
            window.addEventListener('resize', pedirAjusteToggleScroll);
            window.addEventListener('load', pedirAjusteToggleScroll);
            if (mqDesktop.addEventListener) {
                mqDesktop.addEventListener('change', pedirAjusteToggleScroll);
            }
        }

        // Notificaciones: al abrir la bandeja se marcan todas como leídas
        var notifDropdown = document.getElementById('notifDropdown');
        if (notifDropdown) {
            notifDropdown.addEventListener('shown.bs.dropdown', function () {
                var btn = notifDropdown.querySelector('.mp-icon-btn');
                var badge = btn ? btn.querySelector('.mp-badge') : null;
                var headers = csrfHeaders();
                fetch('/notificaciones/leer', { method: 'POST', headers: headers })
                    .then(function (r) { if (r.ok && badge) badge.remove(); })
                    .catch(function () {});
            });

            // Eliminar una notificación sin recargar
            notifDropdown.addEventListener('submit', function (e) {
                var form = e.target.closest('form[action*="/notificaciones/"][action$="/eliminar"]');
                if (!form) return;
                var url = form.getAttribute('action');
                var row = form.closest('.mp-notif-row');
                e.preventDefault();
                var btn = form.querySelector('button[type="submit"]');
                if (btn) btn.disabled = true;
                fetch(url, {
                    method: 'POST',
                    headers: csrfHeaders(),
                    body: new FormData(form)
                })
                    .then(function (r) {
                        if (!r.ok) throw new Error('HTTP ' + r.status);
                        if (row) row.remove();
                        actualizarToolbarNotificaciones();
                    })
                    .catch(function () {
                        form.submit();
                    })
                    .finally(function () {
                        if (btn) btn.disabled = false;
                    });
            });

            // Silenciar/activar o eliminar todas sin recargar
            notifDropdown.addEventListener('click', function (e) {
                var btn = e.target.closest('.mp-notif-toolbar button[type="submit"]');
                if (!btn) return;
                var form = btn.closest('form');
                if (!form) return;
                if (form.action.indexOf('/notificaciones/silenciar') === -1 && form.action.indexOf('/notificaciones/eliminar-todas') === -1) return;
                e.preventDefault();
                var url = form.getAttribute('action');
                if (btn.disabled) return;
                if (url.indexOf('/eliminar-todas') !== -1) {
                    if (!window.confirm('¿Eliminar todas las notificaciones?')) {
                        return;
                    }
                }
                btn.disabled = true;
                fetch(url, {
                    method: 'POST',
                    headers: csrfHeaders(),
                    body: new FormData(form)
                })
                    .then(function (r) {
                        if (!r.ok) throw new Error('HTTP ' + r.status);
                        if (url.indexOf('/eliminar-todas') !== -1) {
                            notifDropdown.querySelectorAll('.mp-notif-row').forEach(function (row) {
                                row.remove();
                            });
                            actualizarToolbarNotificaciones();
                        } else if (url.indexOf('/silenciar') !== -1) {
                            var icono = notifDropdown.querySelector('.mp-icon-btn > i');
                            var badge = notifDropdown.querySelector('.mp-icon-btn .mp-badge');
                            var label = btn.querySelector('span');
                            var iconoBoton = btn.querySelector('i');
                            if (icono) {
                                var estabaSilenciado = icono.classList.contains('bi-bell-slash');
                                if (estabaSilenciado) {
                                    icono.classList.remove('bi-bell-slash');
                                    icono.classList.add('bi-bell');
                                    if (label) label.textContent = 'Silenciar notificaciones';
                                    if (iconoBoton) {
                                        iconoBoton.classList.remove('bi-bell-fill');
                                        iconoBoton.classList.add('bi-bell-slash');
                                    }
                                } else {
                                    icono.classList.remove('bi-bell');
                                    icono.classList.add('bi-bell-slash');
                                    if (label) label.textContent = 'Activar notificaciones';
                                    if (iconoBoton) {
                                        iconoBoton.classList.remove('bi-bell-slash');
                                        iconoBoton.classList.add('bi-bell-fill');
                                    }
                                    if (badge) badge.remove();
                                }
                            }
                            notifDropdown.querySelectorAll('.mp-notif-row.unread').forEach(function (row) {
                                row.classList.remove('unread');
                            });
                        }
                    })
                    .catch(function () {
                        form.submit();
                    })
                    .finally(function () {
                        btn.disabled = false;
                    });
            });
        }

        function actualizarToolbarNotificaciones() {
            var dropdown = document.getElementById('notifDropdown');
            if (!dropdown) return;
            var rows = dropdown.querySelectorAll('.mp-notif-row').length;
            var vacio = dropdown.querySelector('.mp-notif-empty');
            if (rows === 0) {
                if (!vacio) {
                    vacio = document.createElement('p');
                    vacio.className = 'mp-notif-empty px-3 py-2 text-muted small mb-0';
                    vacio.textContent = 'No tenés notificaciones por ahora.';
                    var toolbar = dropdown.querySelector('.mp-notif-toolbar');
                    if (toolbar) {
                        dropdown.querySelector('.dropdown-menu').insertBefore(vacio, toolbar);
                    } else {
                        dropdown.querySelector('.dropdown-menu').appendChild(vacio);
                    }
                }
                var formEliminarTodas = dropdown.querySelector('form[action*="/notificaciones/eliminar-todas"]');
                if (formEliminarTodas) formEliminarTodas.remove();
            }
        }

        // Carrito: muestra el degradé del scroll solo cuando hay más elementos hacia abajo
        var cartMenu = document.querySelector('.mp-cart-menu');
        if (cartMenu) {
            var cartToggle = cartMenu.closest('.dropdown');
            function actualizarDegradeCarrito() {
                var scroll = cartMenu.querySelector('.mp-cart-scroll');
                if (!scroll) { cartMenu.classList.remove('has-more'); return; }
                var alFondo = scroll.scrollTop + scroll.clientHeight >= scroll.scrollHeight - 4;
                var hayMas = scroll.scrollHeight > scroll.clientHeight + 4 && !alFondo;
                cartMenu.classList.toggle('has-more', hayMas);
            }
            if (cartToggle) {
                cartToggle.addEventListener('shown.bs.dropdown', actualizarDegradeCarrito);
            }
            // El menú puede reemplazarse vía AJAX: escucho el scroll con delegación
            cartMenu.addEventListener('scroll', function (e) {
                if (e.target && e.target.classList.contains('mp-cart-scroll')) actualizarDegradeCarrito();
            });
            window.addEventListener('resize', actualizarDegradeCarrito);
        }

        // Evita que se cierre el desplegable de filtros avanzados al tocar check/radios/botones
        document.addEventListener('click', function (e) {
            var menu = e.target.closest('.mp-filtros-avanzados-menu');
            if (!menu) return;
            if (e.target.closest('.mp-toggle-check') ||
                e.target.closest('.mp-toggle-radio') ||
                e.target.closest('.mp-toggle-btn') ||
                e.target.closest('.mp-sf-head-movil') ||
                e.target.closest('.mp-toggle-acumular-movil')) {
                e.stopPropagation();
            }
            if (e.target.closest('.mp-filtro-modo')) {
                e.stopPropagation();
            }
        });
        document.addEventListener('change', function (e) {
            var menu = e.target.closest('.mp-filtros-avanzados-menu');
            if (!menu) return;
            var modoRadio = e.target.closest('input[name="modoFiltro"]');
            if (modoRadio) {
                sincronizarModoFiltro();
                refrescarResultado('filtro');
                return;
            }
            if (e.target.closest('.mp-toggle-check') || e.target.closest('.mp-toggle-acumular-movil')) {
                e.stopPropagation();
            }
        });

        // Confirmación para quitar desde el menú desplegable del carrito
        var quitarCarritoModalEl = document.getElementById('mpQuitarCarritoModal');
        if (quitarCarritoModalEl && window.bootstrap) {
            var quitarCarritoModal = new bootstrap.Modal(quitarCarritoModalEl);
            var quitarFormPendiente = null;
            var quitarNombreEl = document.getElementById('mpQuitarPlantaNombre');
            var quitarConfirmarBtn = document.getElementById('mpQuitarCarritoConfirmar');

            document.addEventListener('submit', function (e) {
                var form = e.target;
                if (!form.classList.contains('js-cart-remove')) return;
                e.preventDefault();
                quitarFormPendiente = form;
                if (quitarNombreEl) {
                    var nombre = form.getAttribute('data-planta-nombre') || 'esta planta';
                    quitarNombreEl.textContent = nombre;
                }
                quitarCarritoModal.show();
            });

            if (quitarConfirmarBtn) {
                quitarConfirmarBtn.addEventListener('click', function () {
                    if (!quitarFormPendiente) return;
                    var form = quitarFormPendiente;
                    quitarCarritoModal.hide();
                    quitarFormPendiente = null;
                    var headers = csrfHeaders();
                    headers['X-Requested-With'] = 'fetch';
                    fetch(form.action, { method: 'POST', headers: headers })
                        .then(function (r) {
                            if (!r.ok) throw new Error('HTTP ' + r.status);
                            return r.json();
                        })
                        .then(function (datos) {
                            actualizarBadgeCarrito(datos.cantidad);
                            actualizarPaginaCarrito(datos);
                            refrescarMenuCarrito();
                        })
                        .catch(function () {
                            form.submit();
                        });
                });
            }

            quitarCarritoModalEl.addEventListener('hidden.bs.modal', function () {
                quitarFormPendiente = null;
            });
        }

        // Headers de CSRF para los POST vía fetch
        function csrfHeaders() {
            var token = document.querySelector('meta[name="_csrf"]');
            var header = document.querySelector('meta[name="_csrf_header"]');
            var headers = {};
            if (token && header && header.content) {
                headers[header.content] = token.content;
            }
            return headers;
        }

        // Filtros rápidos (toggle sin iconos): al hacer clic se agrega el valor
        // al campo y se envía el formulario (GET).
        // En las páginas donde el panel solo tiene precios (subrutas de tienda),
        // el campo del toggle no existe: se crea un input oculto por valor.
        // El modo toggle (acumulable) depende del check "filtros acumulables":
        // activado, se pueden dejar varias opciones del mismo campo marcadas.
        function inputOculto(campo, valor) {
            var input = document.createElement('input');
            input.type = 'hidden';
            input.className = 'mp-toggle-hidden';
            input.setAttribute('data-campo', campo);
            input.name = campo;
            input.value = valor;
            return input;
        }
        function valoresDelCampo(panel, campo) {
            return Array.prototype.filter.call(
                panel.querySelectorAll('input.mp-toggle-hidden[data-campo="' + campo + '"]'),
                function (i) { return i.value; });
        }
        // Sincroniza los toggles ya activos desde servidor con inputs ocultos
        // en el formulario (para que se mantengan al recargar/conservar).
        if (panel) {
            document.querySelectorAll('.mp-toggle-btn.on').forEach(function (btn) {
                var campo = btn.getAttribute('data-campo');
                var valor = btn.getAttribute('data-valor');
                if (!campo || !valor) return;
                var ya = valoresDelCampo(panel, campo).some(function (i) { return i.value === valor; });
                if (!ya) {
                    panel.appendChild(inputOculto(campo, valor));
                }
            });
        }
        document.addEventListener('click', function (e) {
            var btn = e.target.closest('.mp-toggle-btn');
            if (!btn) return;
            var campo = btn.getAttribute('data-campo');
            var valor = btn.getAttribute('data-valor');
            if (!campo || !valor) return;
            var panel = document.getElementById('filtroPanel');
            if (!panel) return;
            var acumular = document.getElementById('mpToggleAcumular');
            var modoToggle = !acumular || acumular.checked;
            var yaEsta = valoresDelCampo(panel, campo).some(function (i) { return i.value === valor; });
            // En modo acumulable + Excluyente, ciertas categorías admiten una sola opción.
            var radioModo = document.querySelector('input[name="modoFiltro"]:checked');
            var excluyente = radioModo && radioModo.value === 'EXCLUYENTE';
            var camposUnicos = ['luces', 'nivelesCuidado', 'tiposPlanta', 'tamanos'];
            var unico = modoToggle && excluyente && camposUnicos.indexOf(campo) !== -1;
            if (unico) {
                // Solo una opción de este campo: si ya estaba, se quita; si no, reemplaza.
                valoresDelCampo(panel, campo).forEach(function (i) {
                    if (i.parentNode) i.parentNode.removeChild(i);
                });
                document.querySelectorAll('.mp-toggle-btn[data-campo="' + campo + '"]').forEach(function (b) {
                    b.classList.remove('on');
                });
                if (!yaEsta) {
                    panel.appendChild(inputOculto(campo, valor));
                    btn.classList.add('on');
                }
            } else if (modoToggle) {
                // Acumulable: la opción tocada se agrega o se quita.
                if (yaEsta) {
                    valoresDelCampo(panel, campo).forEach(function (i) {
                        if (i.value === valor && i.parentNode) i.parentNode.removeChild(i);
                    });
                    btn.classList.remove('on');
                } else {
                    panel.appendChild(inputOculto(campo, valor));
                    btn.classList.add('on');
                }
            } else {
                // Sin acumular: cada toque limpia las demás selecciones del mismo campo
                // o de todos los toggles y aplica solo la elegida. Al tocar el ya activado
                // se desactiva (sin filtros de ese campo).
                if (yaEsta) {
                    valoresDelCampo(panel, campo).forEach(function (i) {
                        if (i.parentNode) i.parentNode.removeChild(i);
                    });
                    btn.classList.remove('on');
                } else {
                    // Limpia todos los toggles para modo no acumulable
                    document.querySelectorAll('.mp-toggle-btn').forEach(function (b) { b.classList.remove('on'); });
                    panel.querySelectorAll('.mp-toggle-hidden').forEach(function (i) {
                        if (i.parentNode) i.parentNode.removeChild(i);
                    });
                    panel.appendChild(inputOculto(campo, valor));
                    btn.classList.add('on');
                }
            }
            sincronizarModoFiltro();
            refrescarResultado('filtro');
        });

        // Botones de ordenamiento (toggle): si el botón tocado ya está activo se
        // desactiva (vuelve al orden por defecto = ventas desc); si no, lo activa.
        // Se refresca solo la grilla vía AJAX conservando la URL navegable.
        document.addEventListener('click', function (e) {
            var btn = e.target.closest('.mp-ordenbtn');
            if (!btn) return;
            var orden = btn.getAttribute('data-orden');
            if (!orden) return;
            var form = document.getElementById('filtroPanel');
            var actual = form ? new URLSearchParams(new FormData(form)).get('orden') : null;
            var activo = (actual === orden) || (!actual && orden === 'ventas-desc');
            var panel = document.getElementById('filtroPanel');
            if (panel) {
                var oculto = panel.querySelector('input.mp-orden-hidden');
                if (!oculto) {
                    oculto = document.createElement('input');
                    oculto.type = 'hidden';
                    oculto.className = 'mp-orden-hidden';
                    oculto.name = 'orden';
                    panel.appendChild(oculto);
                }
                if (activo) {
                    oculto.value = '';
                } else {
                    oculto.value = orden;
                }
            }
            var cont = document.getElementById('mp-resultado');
            if (!cont || !panel) {
                var url = new URL(window.location.href);
                if (activo) url.searchParams.delete('orden');
                else url.searchParams.set('orden', orden);
                url.searchParams.delete('page');
                window.location.href = url.toString();
                return;
            }
            // Refresca solo la grilla; el input oculto ya lleva el orden elegido.
            var pageHidden = panel.querySelector('input[name="page"]');
            if (pageHidden) pageHidden.value = '1';
            refrescarResultado('orden');
            actualizarBotonesOrden(activo, orden);
        });

        // Alterna el estado visual de los botones de ordenamiento según el elegido.
        function actualizarBotonesOrden(desactivar, orden) {
            if (desactivar) {
                document.querySelectorAll('.mp-ordenbtn[data-orden="ventas-desc"]').forEach(function (b) {
                    b.classList.add('active');
                });
                document.querySelectorAll('.mp-ordenbtn').forEach(function (b) {
                    if (b.getAttribute('data-orden') !== 'ventas-desc') b.classList.remove('active');
                });
                return;
            }
            document.querySelectorAll('.mp-ordenbtn').forEach(function (b) {
                b.classList.toggle('active', b.getAttribute('data-orden') === orden);
            });
        }

        // Refresca SOLO el bloque de resultados (grilla + paginación) sin recargar
        // la página. Serializa el formulario de filtros y pide el fragmento a la
        // ruta ".../resultado". Si no hay bloque de resultados (otras páginas) o
        // el fetch falla, hace el submit normal como respaldo.
        var refrescando = false;
        function refrescarResultado(origen) {
            var form = document.getElementById('filtroPanel');
            var cont = document.getElementById('mp-resultado');
            if (!form || !cont) {
                if (form) form.submit();
                return;
            }
            if (refrescando) return;
            refrescando = true;
            cont.classList.add('mp-resultado-cargando');

            var action = form.getAttribute('action') || window.location.pathname;
            var urlFetch = action.replace(/\/$/, '') + '/resultado';
            var params = new URLSearchParams(new FormData(form)).toString();

            fetch(urlFetch + '?' + params, {
                headers: { 'X-Requested-With': 'fetch' }
            })
                .then(function (r) {
                    if (!r.ok) throw new Error('HTTP ' + r.status);
                    return r.text();
                })
                .then(function (html) {
                    cont.outerHTML = html;
                    var nuevo = document.getElementById('mp-resultado');
                    if (nuevo) nuevo.classList.remove('mp-resultado-cargando');
                    // Actualiza la barra de direcciones para conservar el estado compartible.
                    var base = action.split('?')[0];
                    var qs = params ? ('?' + params) : '';
                    if (window.history && window.history.replaceState) {
                        window.history.replaceState(null, '', base + qs);
                    }
                    actualizarCartelFiltros();
                    try { window.scrollTo({ top: 0, behavior: 'smooth' }); } catch (err) {}
                })
                .catch(function () {
                    // Respaldo: recarga completa con los filtros elegidos.
                    if (origen === 'orden') {
                        window.location.href = urlFetch.replace(/\/resultado$/, '') + '?' + params;
                    } else {
                        form.submit();
                    }
                })
                .finally(function () { refrescando = false; });
        }

        // Sincroniza el cartel "Filtros aplicados" con los datos del bloque de
        // resultados recién refrescado (lo muestra/oculta y actualiza el texto).
        function actualizarCartelFiltros() {
            var cont = document.getElementById('mp-resultado');
            var cartel = document.getElementById('mpFiltrosCartel');
            var plantilla = document.getElementById('mpFiltrosCartelPlantilla');
            if (!cont || !plantilla) return;
            var activo = cont.getAttribute('data-filtro-activo') === 'true';
            if (activo) {
                var resumen = cont.getAttribute('data-filtros-resumen') || '-';
                var total = cont.getAttribute('data-total') || '0';
                if (!cartel) {
                    cartel = plantilla.cloneNode(true);
                    cartel.id = 'mpFiltrosCartel';
                    cartel.hidden = false;
                    plantilla.parentNode.insertBefore(cartel, plantilla);
                }
                cartel.hidden = false;
                var r = cartel.querySelector('#mpFiltrosResumenTpl') || cartel.querySelector('#mpFiltrosResumen');
                var t = cartel.querySelector('#mpFiltrosTotalTpl') || cartel.querySelector('#mpFiltrosTotal');
                if (r) { r.id = 'mpFiltrosResumen'; r.textContent = resumen; }
                if (t) { t.id = 'mpFiltrosTotal'; t.textContent = total; }
            } else if (cartel) {
                cartel.hidden = true;
            }
        }

        // Paginación dentro del bloque de resultados: también sin recargar.
        document.addEventListener('click', function (e) {
            var link = e.target.closest('#mp-resultado .mp-page-btn');
            if (!link || !link.getAttribute('href')) return;
            var form = document.getElementById('filtroPanel');
            var cont = document.getElementById('mp-resultado');
            if (!form || !cont) return;
            e.preventDefault();
            var destino = link.getAttribute('href');
            var pageValor = null;
            try {
                pageValor = new URL(destino, window.location.origin).searchParams.get('page');
            } catch (err) {}
            var pageHidden = form.querySelector('input[name="page"]');
            if (!pageHidden) {
                pageHidden = document.createElement('input');
                pageHidden.type = 'hidden';
                pageHidden.name = 'page';
                form.appendChild(pageHidden);
            }
            pageHidden.value = pageValor || '1';
            refrescarResultado('page');
        });

        // Check "filtros acumulables": habilita/deshabilita el modo toggle y la nota
        function actualizarModoFiltro(activo, contexto) {
            var modo = contexto ? contexto.querySelector('.mp-filtro-modo') : null;
            if (!modo) modo = document.getElementById('mpFiltroModo');
            if (modo) modo.hidden = !activo;
            var nota = contexto ? contexto.querySelector('.mp-toggle-nota') : null;
            if (!nota) nota = document.getElementById('mpToggleNota');
            if (nota) nota.hidden = !activo;
        }
        // Vuelca el modo elegido (Incluyente/Excluyente) como input oculto del
        // formulario de filtros, así viaja en cada submit (filtros, precio, etc.).
        function sincronizarModoFiltro() {
            var panel = document.getElementById('filtroPanel');
            if (!panel) return;
            var radio = document.querySelector('input[name="modoFiltro"]:checked');
            if (!radio) {
                var radios = document.querySelectorAll('input[name="modoFiltro"]');
                if (radios.length > 0) {
                    radios[0].checked = true;
                    radio = radios[0];
                } else {
                    return;
                }
            }
            var oculto = panel.querySelector('input.mp-modo-hidden');
            if (!oculto) {
                oculto = document.createElement('input');
                oculto.type = 'hidden';
                oculto.className = 'mp-modo-hidden';
                oculto.name = 'modoFiltro';
                panel.appendChild(oculto);
            }
            oculto.value = radio.value || 'INCLUYENTE';
        }
        document.addEventListener('change', function (e) {
            var modoRadio = e.target.closest('input[name="modoFiltro"]');
            if (modoRadio) {
                sincronizarModoFiltro();
                refrescarResultado('filtro');
                return;
            }
            var caja = e.target.closest('#mpToggleAcumular, .mp-toggle-acumular-movil');
            if (!caja) return;
            var activo = caja.checked;
            var lab = caja.closest('.mp-toggle-check');
            if (lab) lab.classList.toggle('on', activo);
            var contexto = caja.closest('.dropdown-menu, .mp-togglebar, form');
            actualizarModoFiltro(activo, contexto);
            if (activo) {
                var radiosModoCtx = contexto ? contexto.querySelectorAll('input[name="modoFiltro"]') : document.querySelectorAll('input[name="modoFiltro"]');
                if (radiosModoCtx && radiosModoCtx.length > 0) {
                    var algunoMarcado = Array.prototype.some.call(radiosModoCtx, function (r) { return r.checked; });
                    if (!algunoMarcado) {
                        radiosModoCtx[0].checked = true;
                    } else {
                        // asegurar que el marcado esté sincronizado
                    }
                } else {
                    var radiosModo = document.querySelectorAll('input[name="modoFiltro"]');
                    if (radiosModo.length > 0 && !Array.prototype.some.call(radiosModo, function (r) { return r.checked; })) {
                        radiosModo[0].checked = true;
                    }
                }
                sincronizarModoFiltro();
            }
            try {
                localStorage.setItem('mp-acumulable', activo ? '1' : '0');
            } catch (err) {}
        });

        // Asegura que siempre haya un modo de filtro seleccionado por defecto
        var radiosModo = document.querySelectorAll('input[name="modoFiltro"]');
        if (radiosModo.length > 0) {
            var algunoMarcado = Array.prototype.some.call(radiosModo, function (r) { return r.checked; });
            if (!algunoMarcado) {
                radiosModo[0].checked = true;
            }
        }

        // Restaura el estado guardado del check "filtros acumulables" al recargar
        var acu = document.getElementById('mpToggleAcumular');
        if (acu) {
            var acuActivo = false;
            try { acuActivo = localStorage.getItem('mp-acumulable') === '1'; } catch (err) {}
            acu.checked = acuActivo;
            var acuLab = acu.closest('.mp-toggle-check');
            if (acuLab) acuLab.classList.toggle('on', acuActivo);
            actualizarModoFiltro(acuActivo, acu.closest('.mp-togglebar'));
            sincronizarModoFiltro();
        }
        document.querySelectorAll('.mp-toggle-acumular-movil').forEach(function (caja) {
            var acuActivo = false;
            try { acuActivo = localStorage.getItem('mp-acumulable') === '1'; } catch (err) {}
            caja.checked = acuActivo;
            var acuLab = caja.closest('.mp-toggle-check');
            if (acuLab) acuLab.classList.toggle('on', acuActivo);
            actualizarModoFiltro(acuActivo, caja.closest('.dropdown-menu'));
        });
        sincronizarModoFiltro();

        // Heart de favoritos: alterna sin recargar la página
        document.addEventListener('submit', function (e) {
            var form = e.target;
            if (!form.classList.contains('js-wishlist-form')) return;
            e.preventDefault();
            var plantId = form.getAttribute('data-planta-id');
            var btn = form.querySelector('button[type="submit"]');
            if (!plantId || !btn || btn.disabled) return;
            btn.disabled = true;
            fetch('/wishlist/toggle/' + plantId, { method: 'POST', headers: csrfHeaders() })
                .then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
                .then(function (res) {
                    if (!res || res.favorito === undefined) return;
                    form.classList.toggle('active', res.favorito);
                    btn.classList.toggle('active', res.favorito);
                    var icon = btn.querySelector('i');
                    if (icon) {
                        icon.classList.toggle('bi-heart-fill', res.favorito);
                        icon.classList.toggle('bi-heart', !res.favorito);
                    }
                    if (!res.favorito && window.location.pathname.indexOf('/wishlist') === 0) {
                        window.location.reload();
                    }
                })
                .catch(function () {
                    // Si falla el toggle (por ejemplo, por un CSRF vencido),
                    // se envía el formulario normal con el token oculto.
                    form.submit();
                })
                .finally(function () { btn.disabled = false; });
        });

        // Carrito: al agregar se hace vía AJAX y se actualizan el badge y los totales
        // sin recargar la página. Si algo falla, se reenvía como POST normal.
        function actualizarBadgeCarrito(n) {
            var cont = document.getElementById('cartDropdown');
            if (!cont) return;
            var badge = cont.querySelector('.mp-badge');
            if (n > 0) {
                if (!badge) {
                    badge = document.createElement('span');
                    badge.className = 'mp-badge';
                    var boton = cont.querySelector('.mp-icon-btn');
                    if (boton) boton.appendChild(badge);
                }
                badge.textContent = n;
            } else if (badge) {
                badge.remove();
            }
        }
        function actualizarPaginaCarrito(datos) {
            var total = document.getElementById('cartTotal');
            if (total && datos.total != null) total.textContent = datos.total;
            var cuenta = document.getElementById('cartCount');
            if (cuenta && datos.cantidad != null) cuenta.textContent = datos.cantidad + ' ítems en tu carrito';
            (datos.items || []).forEach(function (item) {
                var fila = document.querySelector('tr[data-planta-id="' + item.plantId + '"]');
                if (!fila) return;
                var qty = fila.querySelector('.cart-qty');
                if (qty) qty.textContent = item.cantidad;
                var sub = fila.querySelector('.cart-subtotal');
                if (sub) sub.textContent = '$' + item.subtotal;
            });
        }
        function refrescarMenuCarrito() {
            var cuerpo = document.querySelector('#cartDropdown .mp-cart-cuerpo');
            if (!cuerpo) return;
            fetch('/carrito/menu')
                .then(function (r) { return r.text(); })
                .then(function (html) { cuerpo.innerHTML = html; })
                .catch(function () {});
        }
        document.addEventListener('submit', function (e) {
            var form = e.target;
            if (!form.classList.contains('js-cart-add')) return;
            e.preventDefault();
            var btn = form.querySelector('button[type="submit"]');
            if (btn) btn.disabled = true;
            var headers = csrfHeaders();
            headers['X-Requested-With'] = 'fetch';
            fetch(form.action, { method: 'POST', headers: headers, body: new FormData(form) })
                .then(function (r) {
                    if (!r.ok) throw new Error('HTTP ' + r.status);
                    return r.json();
                })
                .then(function (datos) {
                    actualizarBadgeCarrito(datos.cantidad);
                    actualizarPaginaCarrito(datos);
                    refrescarMenuCarrito();
                })
                .catch(function () {
                    if (btn) btn.disabled = false;
                    form.submit();
                })
                .finally(function () { if (btn) btn.disabled = false; });
        });

        function restaurarScrollCarrito() {
            var y = sessionStorage.getItem('mp-restore-scroll');
            if (y === null) return;
            sessionStorage.removeItem('mp-restore-scroll');
            var objetivo = parseInt(y, 10) || 0;
            if ('scrollRestoration' in window.history) {
                window.history.scrollRestoration = 'manual';
            }
            window.scrollTo(0, objetivo);
        }
        window.addEventListener('load', restaurarScrollCarrito);
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', restaurarScrollCarrito);
        } else {
            restaurarScrollCarrito();
        }

        // Promos: el carrusel solo se activa en móvil; en escritorio los carteles quedan estáticos
        var promoCarousel = document.getElementById('promoCarousel');
        if (promoCarousel && window.matchMedia && window.bootstrap) {
            var mqMovil = window.matchMedia('(max-width: 767.98px)');
            function iniciarPromos() {
                if (mqMovil.matches) {
                    var inst = bootstrap.Carousel.getInstance(promoCarousel);
                    if (inst) {
                        inst.cycle();
                    } else {
                        new bootstrap.Carousel(promoCarousel, { interval: 5000, touch: true, ride: 'carousel' });
                    }
                } else {
                    promoCarousel.removeAttribute('data-bs-ride');
                    promoCarousel.removeAttribute('data-bs-interval');
                    var inst2 = bootstrap.Carousel.getInstance(promoCarousel);
                    if (inst2) inst2.pause();
                }
            }
            iniciarPromos();
            if (mqMovil.addEventListener) {
                mqMovil.addEventListener('change', iniciarPromos);
            }
        }

        // Contador de la Oferta del día: regresivo hasta la medianoche (se renueva a las 00:00)
        var contador = document.getElementById('contadorOferta');
        if (contador) {
            var cHoras = document.getElementById('c-horas');
            var cMin = document.getElementById('c-min');
            var cSeg = document.getElementById('c-seg');
            function pad2(n) { return (n < 10 ? '0' : '') + n; }
            function tickContador() {
                var ahora = new Date();
                var fin = new Date(ahora.getFullYear(), ahora.getMonth(), ahora.getDate(), 23, 59, 59, 999);
                var restante = Math.max(0, fin.getTime() - ahora.getTime());
                if (cHoras) cHoras.textContent = pad2(Math.floor(restante / 3600000));
                if (cMin) cMin.textContent = pad2(Math.floor(restante % 3600000 / 60000));
                if (cSeg) cSeg.textContent = pad2(Math.floor(restante % 60000 / 1000));
                if (restante === 0) {
                    // Medianoche: refrescar para mostrar las ofertas nuevas
                    window.location.reload();
                }
            }
            tickContador();
            setInterval(tickContador, 1000);
        }

        // Carruseles horizontales con flechas
        function carouselDe(btn) {
            var carousel = btn.closest('.mp-carousel');
            if (!carousel) {
                var sec = btn.closest('.mp-section');
                if (sec) {
                    carousel = sec.querySelector('.mp-carousel');
                }
            }
            return carousel;
        }

        function hayMas(track) {
            return track.scrollLeft + track.clientWidth < track.scrollWidth - 4;
        }

        function actualizarFlechas(carousel) {
            var track = carousel.querySelector('.mp-carousel-track');
            if (!track) return;
            var prev = carousel.querySelector('.mp-carousel-btn.prev');
            var next = carousel.querySelector('.mp-carousel-btn.next');
            var sec = carousel.closest('.mp-section');
            var headPrev = sec ? sec.querySelector('.mp-section-head .prev') : null;
            var headNext = sec ? sec.querySelector('.mp-section-head .next') : null;
            var atStart = track.scrollLeft <= 4;
            var atEnd = !hayMas(track);
            carousel.classList.toggle('has-more', hayMas(track));
            [prev, headPrev].forEach(function (b) { if (b) b.style.visibility = atStart ? 'hidden' : 'visible'; });
            [next, headNext].forEach(function (b) { if (b) b.style.visibility = atEnd ? 'hidden' : 'visible'; });
        }

        document.addEventListener('click', function (e) {
            var btn = e.target.closest('.mp-carousel-btn');
            if (!btn) return;
            var carousel = carouselDe(btn);
            if (!carousel) return;
            var track = carousel.querySelector('.mp-carousel-track');
            var slide = track.querySelector('.mp-carousel-slide');
            var step = slide ? slide.offsetWidth + 12 : track.clientWidth;
            var dir = btn.classList.contains('next') ? 1 : -1;
            track.scrollBy({ left: dir * step, behavior: 'smooth' });
            setTimeout(function () { actualizarFlechas(carousel); }, 400);
        });

        document.querySelectorAll('.mp-carousel').forEach(function (carousel) {
            var track = carousel.querySelector('.mp-carousel-track');
            if (!track) return;
            track.addEventListener('scroll', function () { actualizarFlechas(carousel); });
            actualizarFlechas(carousel);
        });
        window.addEventListener('resize', function () {
            document.querySelectorAll('.mp-carousel').forEach(actualizarFlechas);
        });

        // Toggle del buscador móvil: el ojo abierto muestra el bloque completo,
        // el ojo cerrado lo oculta dejando visible solo el botón.
        var toggleBuscar = document.getElementById('mpSearchbarToggle');
        if (toggleBuscar) {
            toggleBuscar.addEventListener('click', function () {
                var oculto = document.body.classList.toggle('mp-searchbar-oculto');
                var icono = toggleBuscar.querySelector('.bi');
                icono.className = 'bi ' + (oculto ? 'bi-eye-slash-fill' : 'bi-eye-fill');
                toggleBuscar.setAttribute('aria-expanded', String(!oculto));
                toggleBuscar.setAttribute('title', oculto ? 'Mostrar buscador' : 'Ocultar buscador');
                // Recalcular el top del sidebar de filtros: al ocultar el bus­
                // cador el sidebar sube a su tope base (100px) y al mostrarlo
                // vuelve a quedar justo debajo del buscador.
                if (typeof pedirAjusteToggleScrollGlobal === 'function') {
                    pedirAjusteToggleScrollGlobal();
                }
            });
        }

        // Toggle de la barra lateral de filtros: embudo cuando está desplegada
        // (mp-filtros-apagado=off lo tacha con una barra diagonal).
        var toggleFiltros = document.getElementById('mpTogglebarToggle');
        if (toggleFiltros) {
            var togglebarFiltros = toggleFiltros.closest('.mp-togglebar');
            var filtrosStorage = null;
            try { filtrosStorage = window.sessionStorage; } catch (err) {}
            // Aplica el estado (oculto o visible) al togglebar y guarda la
            // preferencia para que siga oculta al cambiar de página.
            function aplicarFiltrosOcultos(oculto) {
                togglebarFiltros.classList.toggle('mp-togglebar-oculto', oculto);
                var icono = toggleFiltros.querySelector('.bi');
                icono.className = 'bi bi-filter' + (oculto ? ' mp-filtros-apagado' : '');
                toggleFiltros.setAttribute('aria-expanded', String(!oculto));
                toggleFiltros.setAttribute('title', oculto ? 'Mostrar filtros' : 'Ocultar filtros');
                if (filtrosStorage) {
                    filtrosStorage.setItem('mp-filtros-ocultos', oculto ? '1' : '0');
                }
                if (typeof pedirAjusteToggleScrollGlobal === 'function') {
                    // Al colapsar limpia el top inline del sticky (para que mande el
                    // top:188px del estado oculto); al expandir recalcula con los
                    // valores actuales.
                    pedirAjusteToggleScrollGlobal();
                }
            }
            // Al volver a una página cargada, los filtros siguen ocultos si el
            // usuario los ocultó al navegar esta sesión.
            if (filtrosStorage && filtrosStorage.getItem('mp-filtros-ocultos') === '1') {
                aplicarFiltrosOcultos(true);
            }
            toggleFiltros.addEventListener('click', function () {
                aplicarFiltrosOcultos(!togglebarFiltros.classList.contains('mp-togglebar-oculto'));
            });
        }

        // Botón volver arriba: solo aparece al bajar el scroll y sube al tope.
        var scrollTopBtn = document.getElementById('scrollTopBtn');
        if (scrollTopBtn) {
            var UMBRAL_SCROLL = 400;
            function actualizarScrollTop() {
                var y = window.scrollY || document.documentElement.scrollTop;
                scrollTopBtn.classList.toggle('visible', y > UMBRAL_SCROLL);
            }
            window.addEventListener('scroll', actualizarScrollTop, { passive: true });
            scrollTopBtn.addEventListener('click', function () {
                window.scrollTo({ top: 0, behavior: 'smooth' });
            });
            actualizarScrollTop();
        }

        // El header se oculta al bajar el scroll y reaparece al subir o al volver
        // al tope. Solo en desktop: el header es sticky y se desliza con transform,
        // por lo que nunca empuja ni mueve los elementos del contenido.
        var mpHeader = document.querySelector('.mp-header');
        if (mpHeader) {
            var headerDesktop = window.matchMedia('(min-width: 768px)');
            var ultimoY = window.scrollY || document.documentElement.scrollTop;
            var headerOculto = false;
            var scrollEnCurso = false;
            var timerHeaderTop = null;
            function reubicarStickyConHeader() {
                // El header cambió de estado: se actualiza --mp-header-height y se
                // recalcula el top del buscador y del sidebar de filtros, primero
                // al toque y de nuevo al terminar la transición (0.35s) para
                // quedar apoyado en la posición final.
                ajustarTopSearchbar();
                window.clearTimeout(timerHeaderTop);
                timerHeaderTop = window.setTimeout(ajustarTopSearchbar, 380);
            }
            function ocultarHeaderAlScroll() {
                if (!headerDesktop.matches) return;
                var y = window.scrollY || document.documentElement.scrollTop;
                var bajando = y > ultimoY + 4;
                var subiendo = y < ultimoY - 4;
                if (subiendo) ultimoY = y;
                if (bajando) {
                    ultimoY = y;
                    if (y > 160 && !headerOculto) {
                        headerOculto = true;
                        mpHeader.classList.add('mp-header-hidden');
                        document.body.classList.add('mp-header-oculto');
                        reubicarStickyConHeader();
                    }
                } else if (subiendo && headerOculto) {
                    headerOculto = false;
                    mpHeader.classList.remove('mp-header-hidden');
                    document.body.classList.remove('mp-header-oculto');
                    reubicarStickyConHeader();
                }
            }
            window.addEventListener('scroll', function () {
                if (scrollEnCurso) return;
                scrollEnCurso = true;
                requestAnimationFrame(function () {
                    ocultarHeaderAlScroll();
                    scrollEnCurso = false;
                });
            }, { passive: true });
        }
    // Efecto máquina de escribir en los párrafos de las tarjetas (portada).
        // Tipean al entrar en pantalla; un clic muestra el texto completo.
        var parrafosTipo = document.querySelectorAll('.mp-tag-row-txt');
        function tipear(el) {
            if (el.dataset.tipeoIniciado) return;
            el.dataset.tipeoIniciado = '1';
            var texto = el.textContent;
            el.textContent = '';
            var i = 0;
            function mostrarTodo() {
                el.textContent = texto;
                i = texto.length;
            }
            el.addEventListener('click', mostrarTodo);
            el.addEventListener('keydown', function (e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    mostrarTodo();
                }
            });
            (function tipearLetra() {
                i = Math.min(i + 2, texto.length);
                el.textContent = texto.slice(0, i);
                if (i < texto.length) {
                    setTimeout(tipearLetra, 22);
                }
            })();
        }
        if ('IntersectionObserver' in window && parrafosTipo.length) {
            var observarTipo = new IntersectionObserver(function (entries) {
                entries.forEach(function (entry) {
                    if (entry.isIntersecting) {
                        tipear(entry.target);
                    }
                });
            }, { threshold: 0.4 });
            parrafosTipo.forEach(function (el) {
                el.setAttribute('role', 'button');
                el.setAttribute('tabindex', '0');
                observarTipo.observe(el);
            });
        } else {
            parrafosTipo.forEach(function (el) {
                el.textContent = el.textContent;
            });
        }
    // Efectos de las tarjetas del hero al scrollear (portada):
        // Las cartas completas (.mp-tag-card-img) rotan en orden,
        // igual en todas las vistas (PLANTAS izq, MACETA der, SOPORTES izq).
        var filasTags = document.querySelectorAll('.mp-tag-row');
        var angulosTags = [-12, 12, -12];
        var cardsTags = [];
        var cardsTagsLen = 0;
        filasTags.forEach(function (f) {
            cardsTags.push(f.querySelector('.mp-tag-card-img'));
        });
        cardsTagsLen = cardsTags.length;
        function rotarCardsTags() {
            if (!cardsTagsLen) return;
            var vh = window.innerHeight;
            filasTags.forEach(function (fila, i) {
                var r = fila.getBoundingClientRect();
                var cy = r.top + r.height / 2;
                var t = (vh - cy) / vh;
                t = Math.max(0, Math.min(1, t));
                var card = cardsTags[i];
                if (card) card.style.transform = 'rotate(' + (angulosTags[i] * t).toFixed(2) + 'deg)';
            });
        }
        if (cardsTagsLen) {
            window.addEventListener('scroll', rotarCardsTags, { passive: true });
            window.addEventListener('resize', rotarCardsTags);
            rotarCardsTags();
        }

        // Parallax del fondo del hero (imagen): la capa .mp-hero-bg sobresale 120% arriba
        // y ese exceso es TODO el recorrido disponible: no se frena hasta pasar
        // el hero entero). La imagen acompaña el scroll en el MISMO sentido (al bajar se
        // desplaza hacia abajo, al subir hacia arriba) a 0.6x la velocidad, con rAF, y con
        // prefers-reduced-motion se cancela.
        var heroBg = document.querySelector('.mp-hero .mp-hero-bg');
        if (heroBg) {
            var heroPar = heroBg.parentElement;
            var HERO_PARALLAX_FACTOR = 0.6;
            var heroTopInicialPar = null;
            var heroShiftMaxPar = 0;
            var enRafPar = false;
            function medirParallax() {
                heroShiftMaxPar = Math.max(0, -heroBg.offsetTop);
                heroTopInicialPar = heroPar.getBoundingClientRect().top;
            }
            function aplicarParallax() {
                if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
                var top = heroPar.getBoundingClientRect().top;
                var shift = Math.max(0, Math.min(heroShiftMaxPar, (heroTopInicialPar - top) * HERO_PARALLAX_FACTOR));
                heroBg.style.transform = 'translate3d(0,' + shift.toFixed(1) + 'px,0)';
            }
            function pedirFrameParallax() {
                if (enRafPar) return;
                enRafPar = true;
                requestAnimationFrame(function () {
                    enRafPar = false;
                    aplicarParallax();
                });
            }
            medirParallax();
            aplicarParallax();
            window.addEventListener('scroll', pedirFrameParallax, { passive: true });
            window.addEventListener('resize', function () { medirParallax(); aplicarParallax(); });
            window.addEventListener('load', medirParallax);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', iniciar);
    } else {
        iniciar();
    }
})();