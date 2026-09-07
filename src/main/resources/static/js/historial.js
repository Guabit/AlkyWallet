document.addEventListener('DOMContentLoaded', async () => {
    const token = localStorage.getItem('token');

    const elCargando = document.getElementById('historial-cargando');
    const elVacio = document.getElementById('historial-vacio');
    const elError = document.getElementById('historial-error');
    const elTablaWrapper = document.getElementById('historial-tabla-wrapper');
    const tbody = document.getElementById('historial-tbody');

    if (!token) {
        window.location.href = 'ingresar.html';
        return;
    }

    try {
        const response = await fetch('/api/transacciones/historial', {
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

    function renderMovimientos(movimientos) {
        movimientos.forEach((mov) => {
            const descripcion = mov.concepto ?? '';
            const fecha = formatearFecha(mov.fecha);
            const tipo = (mov.tipoTransaccion ?? mov.tipo ?? '').toUpperCase();
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
            badge.textContent = tipo === 'DEPOSITO' ? 'Depósito' : (esIngreso ? 'Transferencia Recibida' : 'Transferencia Enviada');
            tdTipo.appendChild(badge);

            const tdMonto = document.createElement('td');
            tdMonto.classList.add('py-3.5', 'text-right', esIngreso ? 'historial-monto-ingreso' : 'historial-monto-egreso');
            const signo = esIngreso ? '+' : '-';
            tdMonto.textContent = `${signo}$ ${Math.abs(monto).toLocaleString('es-AR', { minimumFractionDigits: 2 })}`;

            fila.appendChild(tdDescripcion);
            fila.appendChild(tdFecha);
            fila.appendChild(tdTipo);
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
});