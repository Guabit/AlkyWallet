document.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = 'ingresar.html';
        return;
    }

    const formatearMoneda = (valor) =>
        new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', minimumFractionDigits: 2 }).format(Number(valor) || 0);

    function mostrarMensaje(elemento, texto, tipo) {
        if (!elemento) return;
        elemento.textContent = texto;
        elemento.classList.remove('hidden', 'text-emerald-400', 'text-red-400');
        elemento.classList.add(tipo === 'exito' ? 'text-emerald-400' : 'text-red-400');
    }

    // --- PedidosYa (mock) ---
    const formPedidosYa = document.getElementById('form-pedidosya');
    if (formPedidosYa) {
        formPedidosYa.addEventListener('submit', async (e) => {
            e.preventDefault();
            const comercio = document.getElementById('pedidosya-comercio').value.trim();
            const monto = parseFloat(document.getElementById('pedidosya-monto').value);
            const mensajeEl = document.getElementById('pedidosya-mensaje');

            if (!comercio || isNaN(monto) || monto <= 0) {
                mostrarMensaje(mensajeEl, 'Completá el comercio y un monto válido.', 'error');
                mensajeEl.classList.remove('hidden');
                return;
            }

            try {
                const response = await fetch('/api/pedidosya/pagar', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                    body: JSON.stringify({ comercio, monto })
                });
                const data = await response.json().catch(() => null);
                if (response.ok) {
                    mostrarMensaje(mensajeEl, `Pago aprobado (simulado) · ${data.referencia} · ${formatearMoneda(data.monto)}`, 'exito');
                    formPedidosYa.reset();
                } else {
                    mostrarMensaje(mensajeEl, data?.mensaje || 'No se pudo procesar el pago.', 'error');
                }
            } catch (error) {
                mostrarMensaje(mensajeEl, 'Error de conexión con el servidor.', 'error');
            }
            mensajeEl.classList.remove('hidden');
        });
    }

    // --- Telepase (mock) ---
    const formTelepase = document.getElementById('form-telepase');
    if (formTelepase) {
        formTelepase.addEventListener('submit', async (e) => {
            e.preventDefault();
            const patente = document.getElementById('telepase-patente').value.trim();
            const monto = parseFloat(document.getElementById('telepase-monto').value);
            const mensajeEl = document.getElementById('telepase-mensaje');

            if (isNaN(monto) || monto <= 0) {
                mostrarMensaje(mensajeEl, 'Ingresá un monto válido.', 'error');
                mensajeEl.classList.remove('hidden');
                return;
            }

            try {
                const response = await fetch('/api/telepase/pagar', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                    body: JSON.stringify({ monto, patente: patente || null })
                });
                const data = await response.json().catch(() => null);
                if (response.ok) {
                    mostrarMensaje(mensajeEl, `Pago aprobado (simulado) · ${data.referencia} · ${formatearMoneda(data.monto)}`, 'exito');
                    formTelepase.reset();
                } else {
                    mostrarMensaje(mensajeEl, data?.mensaje || 'No se pudo procesar el pago.', 'error');
                }
            } catch (error) {
                mostrarMensaje(mensajeEl, 'Error de conexión con el servidor.', 'error');
            }
            mensajeEl.classList.remove('hidden');
        });
    }
});
