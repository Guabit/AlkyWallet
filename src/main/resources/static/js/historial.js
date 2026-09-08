document.addEventListener('DOMContentLoaded', async () => {
    const token = localStorage.getItem('token');

    const elCargando = document.getElementById('historial-cargando');
    const elVacio = document.getElementById('historial-vacio');
    const elError = document.getElementById('historial-error');
    const elTablaWrapper = document.getElementById('historial-tabla-wrapper');
    const tbody = document.getElementById('historial-tbody');
    const btnFiltroArs = document.getElementById('btn-filtro-ars');
    const btnFiltroUsd = document.getElementById('btn-filtro-usd');

    let monedaActual = 'ARS';

    const ETIQUETAS_CATEGORIA = {
        COMIDA: 'Comida',
        TRANSPORTE: 'Transporte',
        SERVICIOS: 'Servicios',
        ENTRETENIMIENTO: 'Entretenimiento',
        SALUD: 'Salud',
        EDUCACION: 'Educación',
        INVERSION: 'Inversión',
        TRANSFERENCIA: 'Transferencia',
        OTROS: 'Otros'
    };

    if (!token) {
        window.location.href = 'ingresar.html';
        return;
    }

    // Verificar si el usuario tiene cuenta en USD para mostrar el botón de filtro
    async function verificarCuentaUsd() {
        try {
            const resp = await fetch('/api/cuentas', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (resp.ok) {
                const cuentas = await resp.json();
                const tieneUsd = cuentas.some(c => c.tipoMoneda === 'USD');
                if (tieneUsd && btnFiltroUsd) {
                    btnFiltroUsd.classList.remove('hidden');
                }
            }
        } catch (e) {
            console.error('Error al verificar cuentas:', e);
        }
    }

    async function cargarHistorial() {
        elCargando.classList.remove('hidden');
        elVacio.classList.add('hidden');
        elError.classList.add('hidden');
        elTablaWrapper.classList.add('hidden');

        try {
            const response = await fetch(`/api/transacciones/historial?moneda=${monedaActual}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.status === 401 || response.status === 403) {
                localStorage.removeItem('token');
                window.location.href = 'ingresar.html';
                return;
            }

            if (!response.ok) {
                elCargando.classList.add('hidden');
                elError.classList.remove('hidden');
                return;
            }

            const movimientos = await response.json();
            elCargando.classList.add('hidden');

            if (!Array.isArray(movimientos) || movimientos.length === 0) {
                elVacio.textContent = `Todavía no tenés movimientos en ${monedaActual}.`;
                elVacio.classList.remove('hidden');
                return;
            }

            renderMovimientos(movimientos);
            elTablaWrapper.classList.remove('hidden');

        } catch (error) {
            console.error('Error al cargar el historial:', error);
            elCargando.classList.add('hidden');
            elError.classList.remove('hidden');
        }
    }

    // Determina la etiqueta precisa del movimiento según tipo, categoría y concepto
    function obtenerTextoTipo(tipo, categoria, concepto) {
        const desc = (concepto || '').toLowerCase();

        if (tipo === 'DEPOSITO') {
            return 'Depósito';
        }

        if (categoria === 'INVERSION' || desc.includes('inversión') || desc.includes('inversion')) {
            return tipo === 'INGRESO' ? 'Rescate Inversión' : 'Inversión';
        }

        if (categoria === 'SERVICIOS' || desc.includes('telepase') || desc.includes('pedidosya') || desc.includes('pago')) {
            return 'Pago de Servicio';
        }

        return tipo === 'INGRESO' ? 'Transferencia Recibida' : 'Transferencia Enviada';
    }

    function renderMovimientos(movimientos) {
        tbody.innerHTML = '';
        movimientos.forEach((mov) => {
            const descripcion = mov.concepto ?? '';
            const fecha = formatearFecha(mov.fecha);
            const tipo = (mov.tipoTransaccion ?? mov.tipo ?? '').toUpperCase();
            const categoria = String(mov.categoria ?? '').toUpperCase();
            const monto = Number(mov.monto) || 0;
            const esIngreso = tipo === 'INGRESO' || tipo === 'DEPOSITO';

            const fila = document.createElement('tr');
            fila.classList.add('historial-fila');

            const tdDescripcion = document.createElement('td');
            tdDescripcion.className = 'py-3.5 font-medium text-white';
            tdDescripcion.textContent = descripcion;

            const tdFecha = document.createElement('td');
            tdFecha.className = 'py-3.5 text-xs text-gray-400';
            tdFecha.textContent = fecha;

            const tdTipo = document.createElement('td');
            tdTipo.className = 'py-3.5';
            const badge = document.createElement('span');
            badge.classList.add('historial-badge', esIngreso ? 'historial-badge-ingreso' : 'historial-badge-egreso');
            badge.textContent = obtenerTextoTipo(tipo, categoria, descripcion);
            tdTipo.appendChild(badge);

            const tdCategoria = document.createElement('td');
            tdCategoria.className = 'py-3.5 text-xs text-gray-400';
            tdCategoria.textContent = ETIQUETAS_CATEGORIA[categoria] || 'Otros';

            const tdMonto = document.createElement('td');
            tdMonto.classList.add('py-3.5', 'text-right', esIngreso ? 'historial-monto-ingreso' : 'historial-monto-egreso');
            const signo = esIngreso ? '+' : '-';
            const simboloMoneda = monedaActual === 'USD' ? 'US$' : '$';
            tdMonto.textContent = `${signo}${simboloMoneda} ${Math.abs(monto).toLocaleString('es-AR', { minimumFractionDigits: 2 })}`;

            fila.appendChild(tdDescripcion);
            fila.appendChild(tdFecha);
            fila.appendChild(tdTipo);
            fila.appendChild(tdCategoria);
            fila.appendChild(tdMonto);

            tbody.appendChild(fila);
        });
    }

    function formatearFecha(fechaISO) {
        if (!fechaISO) return '';
        const fecha = new Date(fechaISO);
        if (isNaN(fecha.getTime())) return fechaISO;

        return fecha.toLocaleDateString('es-AR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    // Botones de filtro de moneda
    btnFiltroArs?.addEventListener('click', () => {
        if (monedaActual === 'ARS') return;
        monedaActual = 'ARS';
        btnFiltroArs.className = 'px-4 py-2 rounded-lg text-xs font-bold transition bg-gradient-to-r from-fuchsiaNeon to-purple-600 text-white shadow-md';
        btnFiltroUsd.className = 'px-4 py-2 rounded-lg text-xs font-semibold text-gray-400 hover:text-white transition';
        cargarHistorial();
    });

    btnFiltroUsd?.addEventListener('click', () => {
        if (monedaActual === 'USD') return;
        monedaActual = 'USD';
        btnFiltroUsd.className = 'px-4 py-2 rounded-lg text-xs font-bold transition bg-gradient-to-r from-turquoiseNeon to-blue-500 text-bgMain shadow-md';
        btnFiltroArs.className = 'px-4 py-2 rounded-lg text-xs font-semibold text-gray-400 hover:text-white transition';
        cargarHistorial();
    });

    await verificarCuentaUsd();
    await cargarHistorial();
});
