// Formateador de moneda en pesos argentinos
const formatCurrency = (amount) => {
    return new Intl.NumberFormat('es-AR', {
        style: 'currency',
        currency: 'ARS',
        minimumFractionDigits: 2
    }).format(amount);
};

// Obtención e inyección del balance en el DOM
async function cargarBalance() {
    const saldoElement = document.getElementById('saldo-disponible');
    if (!saldoElement) return;

    try {
        const response = await fetch('/api/cuentas/balance', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
        });

        if (!response.ok) {
            console.error(`Error HTTP: ${response.status}`);
            saldoElement.textContent = '$ 0,00';
            return;
        }

        const data = await response.json();
        const saldoNumerico = data.balance ?? data.saldo ?? data.amount ?? data;
        saldoElement.textContent = formatCurrency(Number(saldoNumerico));

    } catch (error) {
        console.error('Error al obtener el saldo:', error);
        saldoElement.textContent = '$ 0,00';
    }
}

// Cargar los últimos movimientos reales desde el backend
async function cargarUltimosMovimientos() {
    const listaContainer = document.getElementById('lista-movimientos');
    if (!listaContainer) return;

    const token = localStorage.getItem('token');
    if (!token) return;

    try {
        const response = await fetch('/api/transacciones/historial', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) return;

        const movimientos = await response.json();

        // Si no hay movimientos reales
        if (!Array.isArray(movimientos) || movimientos.length === 0) {
            listaContainer.innerHTML = `
                <p class="text-xs text-center text-gray-500 py-4">
                    No hay movimientos recientes en tu cuenta.
                </p>
            `;
            return;
        }

        // Tomamos los últimos 4 movimientos
        const ultimos = movimientos.slice(0, 4);

        listaContainer.innerHTML = ultimos.map(mov => {
            const tipo = (mov.tipoTransaccion ?? mov.tipo ?? '').toUpperCase();
            const esIngreso = tipo === 'INGRESO' || tipo === 'DEPOSITO';
            const montoFormateado = formatCurrency(Number(mov.monto) || 0);
            const concepto = mov.concepto || (esIngreso ? 'Depósito Recibido' : 'Transferencia Enviada');
            const fecha = mov.fecha ? new Date(mov.fecha).toLocaleDateString('es-AR', {
                day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'
            }) : 'Reciente';

            return `
              <div class="flex items-center justify-between p-3 rounded-xl bg-[#0D0B14] border border-gray-800/60">
                <div class="flex items-center space-x-3">
                  <div class="w-10 h-10 rounded-full ${esIngreso ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' : 'bg-rose-500/10 text-rose-400 border-rose-500/20'} flex items-center justify-center border font-bold">
                    ${esIngreso ? '↓' : '↑'}
                  </div>
                  <div>
                    <p class="text-sm font-semibold text-white">${concepto}</p>
                    <p class="text-xs text-gray-400">${fecha}</p>
                  </div>
                </div>
                <span class="text-sm font-bold ${esIngreso ? 'text-emerald-400' : 'text-rose-400'}">
                  ${esIngreso ? '+' : '-'} ${montoFormateado}
                </span>
              </div>
            `;
        }).join('');

    } catch (error) {
        console.error('Error al cargar movimientos en dashboard:', error);
    }
}

// Ejecutar ambas funciones al cargar la página
document.addEventListener('DOMContentLoaded', () => {
    cargarBalance();
    cargarUltimosMovimientos();
});