document.addEventListener('DOMContentLoaded', async () => {
    const formTransferencia = document.getElementById('form-transferencia');
    const inputDestino = document.getElementById('cuenta-destino');
    const inputMonto = document.getElementById('monto');
    const selectCategoria = document.getElementById('categoria');
    const btnCancelar = document.getElementById('btn-cancelar');
    const btnAll = document.getElementById('btn-all');
    const saldoElement = document.getElementById('saldo-disponible');
    const mensajeNotificacion = document.getElementById('mensaje-notificacion');
    const token = localStorage.getItem('token');

    let saldoActual = 0;

    // Si se llega desde un link/QR de cobro (?to=...&amount=...), precargamos el formulario.
    function precargarDesdeUrl() {
        const params = new URLSearchParams(window.location.search);
        const destino = params.get('to');
        const monto = params.get('amount');

        if (destino && inputDestino) {
            inputDestino.value = destino;
        }
        if (monto && inputMonto) {
            const montoNumerico = parseFloat(monto);
            if (!isNaN(montoNumerico) && montoNumerico > 0) {
                inputMonto.value = montoNumerico.toFixed(2);
            }
        }
    }

    async function cargarSaldo() {
        if (!saldoElement || !token) return;

        try {
            const response = await fetch('/api/cuentas/balance', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                saldoActual = Number(data.balance ?? data.saldo ?? data.amount ?? data) || 0;
                const textoFormateado = new Intl.NumberFormat('es-AR', {
                    style: 'currency',
                    currency: 'ARS',
                    minimumFractionDigits: 2
                }).format(saldoActual);

                if (window.AlkyBalanceVisibility) {
                    window.AlkyBalanceVisibility.render(saldoElement, textoFormateado);
                } else {
                    saldoElement.textContent = textoFormateado;
                }
            }
        } catch (error) {
            console.error('Error al obtener el saldo:', error);
        }
    }

    precargarDesdeUrl();
    await cargarSaldo();

    if (window.AlkyBalanceVisibility) {
        window.AlkyBalanceVisibility.inicializarBoton(
            document.getElementById('btn-toggle-saldo'),
            saldoElement
        );
    }

    if (btnAll && inputMonto) {
        btnAll.addEventListener('click', () => {
            if (saldoActual > 0) {
                inputMonto.value = saldoActual.toFixed(2);
            }
        });
    }

    if (formTransferencia) {
        formTransferencia.addEventListener('submit', async (e) => {
            e.preventDefault();

            const destinatario = inputDestino ? inputDestino.value.trim() : '';
            const monto = inputMonto ? parseFloat(inputMonto.value) : 0;
            const categoria = selectCategoria ? selectCategoria.value : 'TRANSFERENCIA';

            if (!destinatario || isNaN(monto) || monto <= 0) {
                mostrarMensaje('Por favor, ingresá un destinatario y un monto válido.', 'error');
                return;
            }

            if (monto > saldoActual) {
                mostrarMensaje('No tenés saldo suficiente para realizar esta transferencia.', 'error');
                return;
            }

            const transferenciaRequestDTO = {
                destinatario: destinatario,
                monto: monto,
                categoria: categoria
            };

            try {
                const response = await fetch('/api/transacciones/transferencia', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify(transferenciaRequestDTO)
                });

                if (response.ok) {
                    mostrarMensaje('¡Transferencia realizada con éxito!', 'exito');
                    formTransferencia.reset();
                    await cargarSaldo();
                } else if (response.status === 400 || response.status === 404) {
                    const data = await response.json().catch(() => null);
                    const mensajeError = data?.mensaje
                        || (data?.errores ? Object.values(data.errores).join(', ') : null)
                        || 'Saldo insuficiente o datos inválidos.';
                    mostrarMensaje(mensajeError, 'error');
                } else if (response.status === 401 || response.status === 403) {
                    localStorage.removeItem('token');
                    window.location.href = 'ingresar.html';
                } else {
                    mostrarMensaje('Ocurrió un error al procesar la transferencia.', 'error');
                }
            } catch (error) {
                console.error('Error al realizar la transferencia:', error);
                mostrarMensaje('Error de conexión con el servidor.', 'error');
            }
        });
    }

    if (btnCancelar) {
        btnCancelar.addEventListener('click', () => {
            if (formTransferencia) formTransferencia.reset();
            if (mensajeNotificacion) mensajeNotificacion.classList.add('hidden');
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
