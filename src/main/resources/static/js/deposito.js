document.addEventListener('DOMContentLoaded', async () => {
    const inputMonto = document.getElementById('monto');
    const btnDepositar = document.getElementById('btn-depositar');
    const formDeposito = document.getElementById('form-deposito');
    const mensajeNotificacion = document.getElementById('mensaje-notificacion');
    const token = localStorage.getItem('token');

    // Elementos del selector y dropdown
    const btnMoneda = document.getElementById('btn-moneda');
    const dropdownMoneda = document.getElementById('dropdown-moneda');
    const flechaMoneda = document.getElementById('flecha-moneda');
    const iconoMonedaActual = document.getElementById('icono-moneda-actual');
    const textoMonedaActual = document.getElementById('texto-moneda-actual');
    const simboloMonto = document.getElementById('simbolo-monto-deposito');
    const checkArs = document.getElementById('check-ars');
    const checkUsd = document.getElementById('check-usd');
    const subtextoUsd = document.getElementById('subtexto-usd');

    let monedaSeleccionada = 'ARS';
    let tieneCuentaUsd = false;

    if (btnDepositar) btnDepositar.disabled = true;

    // Verificar si el usuario tiene cuenta en USD
    if (token) {
        try {
            const resp = await fetch('/api/cuentas', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (resp.ok) {
                const cuentas = await resp.json();
                tieneCuentaUsd = cuentas.some(c => c.tipoMoneda === 'USD');
                if (!tieneCuentaUsd && subtextoUsd) {
                    subtextoUsd.textContent = 'USD (Requiere abrir caja en Dashboard)';
                    subtextoUsd.classList.add('text-amber-400');
                }
            }
        } catch (e) {
            console.error('Error al cargar cuentas:', e);
        }
    }

    // Alternar visibilidad del menú desplegable
    function toggleDropdown(abrir) {
        if (!dropdownMoneda) return;
        const estaAbierto = !dropdownMoneda.classList.contains('hidden');
        const debeAbrir = (typeof abrir === 'boolean') ? abrir : !estaAbierto;

        if (debeAbrir) {
            dropdownMoneda.classList.remove('hidden');
            if (flechaMoneda) flechaMoneda.classList.add('rotate-180');
        } else {
            dropdownMoneda.classList.add('hidden');
            if (flechaMoneda) flechaMoneda.classList.remove('rotate-180');
        }
    }

    if (btnMoneda) {
        btnMoneda.addEventListener('click', (e) => {
            e.stopPropagation();
            toggleDropdown();
        });
    }

    // Cerrar el dropdown al hacer click fuera
    document.addEventListener('click', (e) => {
        if (dropdownMoneda && !dropdownMoneda.contains(e.target) && !btnMoneda.contains(e.target)) {
            toggleDropdown(false);
        }
    });

    // Manejar selección de moneda desde las opciones del dropdown
    document.querySelectorAll('.opcion-moneda').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const monedaElegida = btn.getAttribute('data-moneda');

            if (monedaElegida === 'USD' && !tieneCuentaUsd) {
                mostrarMensaje('Primero debés abrir tu Caja de Ahorro en Dólares desde el Dashboard.', 'error');
                toggleDropdown(false);
                return;
            }

            monedaSeleccionada = monedaElegida;
            actualizarUIMoneda();
            toggleDropdown(false);
        });
    });

    function actualizarUIMoneda() {
        if (monedaSeleccionada === 'USD') {
            if (iconoMonedaActual) {
                iconoMonedaActual.textContent = 'US$';
                iconoMonedaActual.className = 'w-8 h-8 rounded-full bg-turquoiseNeon/20 text-turquoiseNeon font-bold flex items-center justify-center text-xs border border-turquoiseNeon/40';
            }
            if (textoMonedaActual) textoMonedaActual.textContent = 'USD (Dólares)';
            if (simboloMonto) simboloMonto.textContent = 'US$';
            if (btnDepositar) btnDepositar.textContent = 'Depositar Dólares (USD)';
            if (checkUsd) checkUsd.classList.remove('hidden');
            if (checkArs) checkArs.classList.add('hidden');
        } else {
            if (iconoMonedaActual) {
                iconoMonedaActual.textContent = '$';
                iconoMonedaActual.className = 'w-8 h-8 rounded-full bg-emerald-500/20 text-emerald-400 font-bold flex items-center justify-center text-xs border border-emerald-500/40';
            }
            if (textoMonedaActual) textoMonedaActual.textContent = 'ARS (Pesos)';
            if (simboloMonto) simboloMonto.textContent = '$';
            if (btnDepositar) btnDepositar.textContent = 'Depositar Pesos (ARS)';
            if (checkArs) checkArs.classList.remove('hidden');
            if (checkUsd) checkUsd.classList.add('hidden');
        }
    }

    if (inputMonto) {
        inputMonto.addEventListener('input', () => {
            const monto = parseFloat(inputMonto.value);
            btnDepositar.disabled = !(monto > 0);
        });
    }

    if (formDeposito) {
        formDeposito.addEventListener('submit', async (e) => {
            e.preventDefault();

            const monto = parseFloat(inputMonto.value);
            if (isNaN(monto) || monto <= 0) return;

            try {
                const response = await fetch('/api/transacciones/deposito', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({
                        monto: monto,
                        moneda: monedaSeleccionada
                    })
                });

                if (response.ok) {
                    const simboloNotif = monedaSeleccionada === 'USD' ? 'US$' : '$';
                    mostrarMensaje(`¡Depósito de ${simboloNotif} ${monto.toFixed(2)} procesado con éxito!`, 'exito');
                    inputMonto.value = '';
                    btnDepositar.disabled = true;
                } else if (response.status === 401 || response.status === 403) {
                    localStorage.removeItem('token');
                    window.location.href = 'ingresar.html';
                } else {
                    const err = await response.json().catch(() => null);
                    mostrarMensaje(err?.message || err?.mensaje || 'No se pudo procesar el depósito.', 'error');
                }
            } catch (error) {
                console.error('Error al realizar el depósito:', error);
                mostrarMensaje('Error de conexión con el servidor.', 'error');
            }
        });
    }

    const btnCopiar = document.getElementById('btn-copiar-email');
    if (btnCopiar) {
        btnCopiar.addEventListener('click', () => {
            const emailElemento = document.getElementById('email-usuario');
            const emailTexto = emailElemento ? emailElemento.textContent.trim() : '';

            if (emailTexto && emailTexto !== 'cargando...') {
                navigator.clipboard.writeText(emailTexto).then(() => {
                    const original = btnCopiar.textContent;
                    btnCopiar.textContent = '¡Copiado!';
                    btnCopiar.classList.add('text-emerald-400');
                    setTimeout(() => {
                        btnCopiar.textContent = original;
                        btnCopiar.classList.remove('text-emerald-400');
                    }, 2000);
                }).catch(err => {
                    console.error('Error al copiar al portapapeles:', err);
                });
            }
        });
    }

    function mostrarMensaje(texto, tipo) {
        if (!mensajeNotificacion) return;
        mensajeNotificacion.textContent = texto;
        mensajeNotificacion.classList.remove('hidden', 'text-emerald-400', 'text-red-400');

        if (tipo === 'exito') {
            mensajeNotificacion.classList.add('text-emerald-400');
        } else {
            mensajeNotificacion.classList.add('text-red-400');
        }
    }
});
