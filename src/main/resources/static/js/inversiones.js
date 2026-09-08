document.addEventListener('DOMContentLoaded', async () => {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = 'ingresar.html';
        return;
    }

    let monedaActiva = 'ARS';

    const btnArs = document.getElementById('btn-inversion-ars');
    const btnUsd = document.getElementById('btn-inversion-usd');
    const formInvertir = document.getElementById('form-invertir');
    const inputMonto = document.getElementById('monto-invertir');
    const btnInvertir = document.getElementById('btn-invertir');
    const mensajeInvertir = document.getElementById('mensaje-invertir');
    const listaInversiones = document.getElementById('lista-inversiones');
    const saldoDisponible = document.getElementById('saldo-disponible-inversion');

    const formatearMoneda = (valor, moneda) => {
        const simb = moneda === 'USD' ? 'US$' : '$';
        const num = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(valor) || 0);
        return `${simb} ${num}`;
    };

    function refrescarEstilosPestanas() {
        if (!btnArs || !btnUsd) return;
        if (monedaActiva === 'ARS') {
            btnArs.className = 'px-3 py-1 rounded-lg text-xs font-bold transition bg-gradient-to-r from-fuchsiaNeon to-purple-600 text-white shadow-md';
            btnUsd.className = 'px-3 py-1 rounded-lg text-xs font-semibold text-gray-400 hover:text-white transition bg-transparent';
        } else {
            btnUsd.className = 'px-3 py-1 rounded-lg text-xs font-bold transition bg-gradient-to-r from-turquoiseNeon to-blue-500 text-bgMain shadow-md';
            btnArs.className = 'px-3 py-1 rounded-lg text-xs font-semibold text-gray-400 hover:text-white transition bg-transparent';
        }
    }

    async function tieneCuentaEnMoneda(moneda) {
        try {
            const resp = await fetch('/api/cuentas', { headers: { 'Authorization': `Bearer ${token}` } });
            if (resp.ok) {
                const cuentas = await resp.json();
                return cuentas.some(c => c.tipoMoneda === moneda);
            }
        } catch(e) {}
        return false;
    }

    async function cambiarMonedaOpcion(nuevaMoneda) {
        if (monedaActiva === nuevaMoneda) return;
        
        if (nuevaMoneda === 'USD') {
            const existe = await tieneCuentaEnMoneda('USD');
            if (!existe) {
                mostrarMensaje('Aún no tienes cuenta en USD. Ábrela gratuitamente desde el Dashboard.', 'error');
                return;
            }
        }
        
        monedaActiva = nuevaMoneda;
        refrescarEstilosPestanas();
        await Promise.all([cargarSaldo(), cargarInversiones()]);
    }

    if (btnArs) btnArs.addEventListener('click', () => cambiarMonedaOpcion('ARS'));
    if (btnUsd) btnUsd.addEventListener('click', () => cambiarMonedaOpcion('USD'));

    async function cargarSaldo() {
        try {
            const response = await fetch(`/api/cuentas/balance?moneda=${monedaActiva}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (response.ok) {
                const data = await response.json();
                const saldo = data.balance ?? data.saldo ?? data.amount ?? 0;
                if (saldoDisponible) saldoDisponible.textContent = formatearMoneda(saldo, monedaActiva);
            } else {
                if (saldoDisponible) saldoDisponible.textContent = formatearMoneda(0, monedaActiva);
            }
        } catch (error) {
            console.error('Error al obtener el saldo:', error);
        }
    }

    async function cargarInversiones() {
        if (!listaInversiones) return;
        try {
            const response = await fetch(`/api/inversiones?moneda=${monedaActiva}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.status === 401 || response.status === 403) {
                localStorage.removeItem('token');
                window.location.href = 'ingresar.html';
                return;
            }

            if (!response.ok) {
                listaInversiones.innerHTML = '<p class="text-xs text-center text-red-400 py-4">No se pudieron cargar las inversiones.</p>';
                return;
            }

            const inversiones = await response.json();
            renderInversiones(inversiones);
        } catch (error) {
            console.error('Error al cargar inversiones:', error);
            listaInversiones.innerHTML = '<p class="text-xs text-center text-red-400 py-4">Error de conexión al obtener las inversiones.</p>';
        }
    }

    function renderInversiones(inversiones) {
        if (!Array.isArray(inversiones) || inversiones.length === 0) {
            listaInversiones.innerHTML = `<p class="text-xs text-center text-gray-500 py-4">Todavía no tenés inversiones activas en ${monedaActiva}.</p>`;
            return;
        }

        listaInversiones.innerHTML = '';

        inversiones.forEach((inv) => {
            const m = inv.moneda || monedaActiva;
            const tasaPorcentaje = (Number(inv.tasaAnualNominal) * 100).toFixed(1);
            const card = document.createElement('div');
            card.className = 'bg-[#0D0B14] p-4 rounded-xl border border-gray-800/80 hover:border-gray-700 transition';

            card.innerHTML = `
                <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                    <div>
                        <div class="flex items-center gap-2">
                            <span class="text-sm font-semibold text-white">${formatearMoneda(inv.montoInvertido, m)}</span>
                            <span class="text-[11px] text-gray-400 bg-white/5 px-2 py-0.5 rounded-full">TNA ${tasaPorcentaje}%</span>
                            ${inv.activa
                                ? '<span class="text-[11px] text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full">Activa</span>'
                                : '<span class="text-[11px] text-gray-400 bg-white/5 px-2 py-0.5 rounded-full">Rescatada</span>'}
                        </div>
                        <p class="text-xs text-gray-400 mt-1">
                            ${inv.diasTranscurridos} día(s) · Rendimiento simulado:
                            <span class="text-emerald-400 font-semibold">+${formatearMoneda(inv.rendimientoSimulado, m)}</span>
                        </p>
                    </div>
                    <div class="flex items-center gap-3">
                        <div class="text-right">
                            <p class="text-[11px] text-gray-400">Valor actual</p>
                            <p class="text-sm font-bold text-turquoiseNeon">${formatearMoneda(inv.valorActual, m)}</p>
                        </div>
                        ${inv.activa
                            ? `<button data-id="${inv.id}" class="btn-rescatar bg-gray-800 hover:bg-gray-700 text-turquoiseNeon px-4 py-2 rounded-xl text-xs font-bold transition border border-gray-700">Rescatar</button>`
                            : ''}
                    </div>
                </div>
            `;
            listaInversiones.appendChild(card);
        });

        document.querySelectorAll('.btn-rescatar').forEach((boton) => {
            boton.addEventListener('click', () => rescatarInversion(boton.dataset.id, boton));
        });
    }

    async function rescatarInversion(id, boton) {
        boton.disabled = true;
        boton.textContent = 'Procesando...';
        try {
            const response = await fetch(`/api/inversiones/${id}/rescatar`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.ok) {
                await Promise.all([cargarSaldo(), cargarInversiones()]);
            } else {
                boton.disabled = false;
                boton.textContent = 'Rescatar';
                const data = await response.json().catch(() => null);
                alert(data?.mensaje || 'No se pudo rescatar la inversión.');
            }
        } catch (error) {
            console.error('Error al rescatar inversión:', error);
            boton.disabled = false;
            boton.textContent = 'Rescatar';
        }
    }

    if (formInvertir) {
        formInvertir.addEventListener('submit', async (e) => {
            e.preventDefault();
            const monto = parseFloat(inputMonto.value);
            if (isNaN(monto) || monto <= 0) return;

            btnInvertir.disabled = true;
            try {
                // FIXED ENDPOINT TO /api/inversiones/invertir 
                const response = await fetch('/api/inversiones/invertir', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({ monto: monto, moneda: monedaActiva })
                });

                if (response.ok) {
                    mostrarMensaje('¡Inversión realizada con éxito!', 'exito');
                    inputMonto.value = '';
                    await Promise.all([cargarSaldo(), cargarInversiones()]);
                } else {
                    const data = await response.json().catch(() => null);
                    mostrarMensaje(data?.mensaje || 'No se pudo realizar la inversión.', 'error');
                }
            } catch (error) {
                console.error('Error al invertir:', error);
                mostrarMensaje('Error de conexión con el servidor.', 'error');
            } finally {
                btnInvertir.disabled = false;
            }
        });
    }

    function mostrarMensaje(texto, tipo) {
        if (!mensajeInvertir) return;
        mensajeInvertir.textContent = texto;
        mensajeInvertir.classList.remove('hidden', 'text-emerald-400', 'text-red-400');
        mensajeInvertir.classList.add(tipo === 'exito' ? 'text-emerald-400' : 'text-red-400');
        setTimeout(() => mensajeInvertir.classList.add('hidden'), 4000);
    }
    
    refrescarEstilosPestanas();
    await Promise.all([cargarSaldo(), cargarInversiones()]);
});
