document.addEventListener('DOMContentLoaded', async () => {
    const contenedorLista = document.getElementById('cotizacion-lista');
    const selectCasa = document.getElementById('conversor-casa');
    const inputArs = document.getElementById('conversor-monto-ars');
    const inputUsd = document.getElementById('conversor-monto-usd');
    const estadoConversor = document.getElementById('conversor-estado');

    if (!contenedorLista) return;

    const token = localStorage.getItem('token');
    if (!token) return;

    let cotizaciones = [];
    let bloquearEventos = false;

    function formatearARS(valor) {
        return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', minimumFractionDigits: 2 }).format(Number(valor) || 0);
    }

    function casaSeleccionada() {
        return cotizaciones.find((c) => c.casa === selectCasa.value) || cotizaciones[0];
    }

    async function cargarCotizaciones() {
        try {
            const response = await fetch('/api/cotizacion/dolar', {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) {
                contenedorLista.innerHTML = '<p class="text-xs text-center text-red-400 py-3">No se pudo obtener la cotización.</p>';
                if (estadoConversor) estadoConversor.textContent = 'Conversor no disponible.';
                return;
            }

            cotizaciones = await response.json();
            renderLista();
            renderSelector();
        } catch (error) {
            console.error('Error al obtener cotización del dólar:', error);
            contenedorLista.innerHTML = '<p class="text-xs text-center text-red-400 py-3">Error de conexión al obtener la cotización.</p>';
        }
    }

    function renderLista() {
        if (!Array.isArray(cotizaciones) || cotizaciones.length === 0) {
            contenedorLista.innerHTML = '<p class="text-xs text-center text-gray-500 py-3">Sin datos disponibles.</p>';
            return;
        }

        // Priorizamos Oficial y Blue si están presentes, el resto se agrega después.
        const orden = ['oficial', 'blue'];
        const ordenadas = [...cotizaciones].sort((a, b) => orden.indexOf(a.casa) - orden.indexOf(b.casa));

        contenedorLista.innerHTML = ordenadas.slice(0, 4).map((c) => `
            <div class="bg-[#0D0B14] rounded-xl p-3 border border-gray-800/80">
                <p class="text-[11px] text-gray-400 uppercase tracking-wide">${c.nombre || c.casa}</p>
                <div class="flex justify-between items-baseline mt-1">
                    <span class="text-[11px] text-gray-500">Compra</span>
                    <span class="text-sm font-semibold text-white">${formatearARS(c.compra)}</span>
                </div>
                <div class="flex justify-between items-baseline">
                    <span class="text-[11px] text-gray-500">Venta</span>
                    <span class="text-sm font-bold text-turquoiseNeon">${formatearARS(c.venta)}</span>
                </div>
            </div>
        `).join('');
    }

    function renderSelector() {
        if (!selectCasa) return;
        selectCasa.innerHTML = cotizaciones
            .map((c) => `<option value="${c.casa}">${c.nombre || c.casa}</option>`)
            .join('');
        recalcularDesdeArs();
    }

    function recalcularDesdeArs() {
        const casa = casaSeleccionada();
        if (!casa || !inputArs || !inputUsd) return;
        const ars = parseFloat(inputArs.value);
        bloquearEventos = true;
        inputUsd.value = (!isNaN(ars) && casa.venta > 0) ? (ars / casa.venta).toFixed(2) : '';
        bloquearEventos = false;
    }

    function recalcularDesdeUsd() {
        const casa = casaSeleccionada();
        if (!casa || !inputArs || !inputUsd) return;
        const usd = parseFloat(inputUsd.value);
        bloquearEventos = true;
        inputArs.value = (!isNaN(usd)) ? (usd * casa.venta).toFixed(2) : '';
        bloquearEventos = false;
    }

    if (inputArs) inputArs.addEventListener('input', () => { if (!bloquearEventos) recalcularDesdeArs(); });
    if (inputUsd) inputUsd.addEventListener('input', () => { if (!bloquearEventos) recalcularDesdeUsd(); });
    if (selectCasa) selectCasa.addEventListener('change', recalcularDesdeArs);

    
    const btnComprarUsd = document.getElementById('btn-comprar-usd');
    const btnVenderUsd = document.getElementById('btn-vender-usd');

    async function ejecutarCanje(monedaOrigen, monedaDestino, montoOrigen) {
        if (!token) return;
        
        // Asumiendo que dashboard.js tiene accesible las notificaciones...
        const showNotif = typeof window.mostrarNotificacionDashboard === 'function' 
            ? window.mostrarNotificacionDashboard 
            : (msg) => alert(msg);

        const m = parseFloat(montoOrigen);
        if (isNaN(m) || m <= 0) {
            showNotif('Ingrese un monto válido para operar.', 'error');
            return;
        }

        try {
            const resp = await fetch('/api/conversiones', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    monedaOrigen: monedaOrigen,
                    monedaDestino: monedaDestino,
                    monto: m
                })
            });

            if (resp.ok) {
                showNotif(`¡Conversión exitosa de ${m.toFixed(2)} ${monedaOrigen} a ${monedaDestino}!`, 'exito');
                // Refrescar el dashboard
                if (typeof window.sincronizarDashboard === 'function') {
                    window.sincronizarDashboard();
                }
                if (inputArs) inputArs.value = '';
                if (inputUsd) inputUsd.value = '';
            } else {
                const err = await resp.json().catch(() => null);
                showNotif(err?.mensaje || err?.message || 'Error al procesar el cambio.', 'error');
            }
        } catch (e) {
            console.error('Error al operar divisas', e);
            showNotif('Error de conexión al servidor.', 'error');
        }
    }

    if (btnComprarUsd) btnComprarUsd.addEventListener('click', () => {
        ejecutarCanje('ARS', 'USD', inputArs.value);
    });

    if (btnVenderUsd) btnVenderUsd.addEventListener('click', () => {
        ejecutarCanje('USD', 'ARS', inputUsd.value);
    });

    await cargarCotizaciones();
});
