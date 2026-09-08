document.addEventListener('DOMContentLoaded', () => {
    const inputMonto = document.getElementById('monto');
    const btnDepositar = document.getElementById('btn-depositar');
    const formDeposito = document.getElementById('form-deposito');
    const mensajeNotificacion = document.getElementById('mensaje-notificacion');
    const token = localStorage.getItem('token');

    if (btnDepositar) btnDepositar.disabled = true;

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
                const response = await fetch('http://localhost:8080/api/transacciones/deposito', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({ monto: monto })
                });

                if (response.ok) {
                    mostrarMensaje('¡Depósito procesado con éxito!', 'exito');
                    inputMonto.value = '';
                    btnDepositar.disabled = true;
                } else if (response.status === 401 || response.status === 403) {
                    localStorage.removeItem('token');
                    window.location.href = 'ingresar.html';
                } else {
                    mostrarMensaje('No se pudo procesar el depósito. Intente nuevamente.', 'error');
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

    // --- Alternativa: cargar el mismo monto vía Mercado Pago (sandbox) ---
    const btnMercadoPago = document.getElementById('btn-mercadopago');
    if (btnMercadoPago) {
        btnMercadoPago.addEventListener('click', async () => {
            const monto = parseFloat(inputMonto.value);
            const mensajeMp = document.getElementById('mercadopago-mensaje');

            if (isNaN(monto) || monto <= 0) {
                mostrarMensajeMp('Ingresá un monto antes de continuar con Mercado Pago.', 'error');
                return;
            }

            btnMercadoPago.disabled = true;
            btnMercadoPago.textContent = 'Redirigiendo...';

            try {
                const response = await fetch('/api/mercadopago/preferencia', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({ monto })
                });

                const data = await response.json().catch(() => null);

                if (response.ok && data?.checkoutUrl) {
                    window.location.href = data.checkoutUrl;
                } else {
                    mostrarMensajeMp(data?.mensaje || 'Mercado Pago no está disponible en este momento.', 'error');
                    btnMercadoPago.disabled = false;
                    btnMercadoPago.textContent = 'Mercado Pago';
                }
            } catch (error) {
                console.error('Error al iniciar el pago con Mercado Pago:', error);
                mostrarMensajeMp('Error de conexión con el servidor.', 'error');
                btnMercadoPago.disabled = false;
                btnMercadoPago.textContent = 'Mercado Pago';
            }

            function mostrarMensajeMp(texto, tipo) {
                if (!mensajeMp) return;
                mensajeMp.textContent = texto;
                mensajeMp.classList.remove('hidden', 'text-emerald-400', 'text-red-400');
                mensajeMp.classList.add(tipo === 'exito' ? 'text-emerald-400' : 'text-red-400');
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
