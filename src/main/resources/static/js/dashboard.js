// Formateador homogéneo de moneda (formato 50.000,00 con espacio después del símbolo)
const formatCurrency = (amount, moneda = 'ARS') => {
    const num = Number(amount) || 0;
    const numeroFormateado = new Intl.NumberFormat('es-AR', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(num);

    const simbolo = moneda === 'USD' ? 'US$' : '$';
    return `${simbolo} ${numeroFormateado}`;
};

let monedaActiva = 'ARS';
let cuentasUsuario = [];

// Obtención de todas las cuentas y balance activo
async function cargarCuentasYBalance() {
    const badgeMoneda = document.getElementById('badge-moneda-activa');
    const btnMonedaArs = document.getElementById('btn-moneda-ars');
    const btnMonedaUsd = document.getElementById('btn-moneda-usd');
    const bannerAbrirUsd = document.getElementById('banner-abrir-usd');
    const token = localStorage.getItem('token');

    if (!token) return;

    try {
        const response = await fetch('/api/cuentas', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            actualizarSaldoEnPantalla('$ 0,00');
            return;
        }

        cuentasUsuario = await response.json();

        const cuentaUsd = cuentasUsuario.find(c => c.tipoMoneda === 'USD');

        // Mostrar o esconder el banner de apertura de cuenta en USD
        if (cuentaUsd) {
            if (bannerAbrirUsd) bannerAbrirUsd.classList.add('hidden');
            if (btnMonedaUsd) {
                btnMonedaUsd.classList.remove('hidden');
            }
        } else {
            if (bannerAbrirUsd) bannerAbrirUsd.classList.remove('hidden');
            if (btnMonedaUsd) {
                btnMonedaUsd.classList.add('hidden');
            }
            monedaActiva = 'ARS';
        }

        // Refrescar estilos de las pestañas
        if (btnMonedaArs && btnMonedaUsd) {
            if (monedaActiva === 'ARS') {
                btnMonedaArs.className = 'px-3 py-1 rounded-lg text-xs font-bold transition bg-gradient-to-r from-fuchsiaNeon to-purple-600 text-white shadow-md';
                btnMonedaUsd.className = 'px-3 py-1 rounded-lg text-xs font-semibold text-gray-400 hover:text-white transition';
            } else {
                btnMonedaUsd.className = 'px-3 py-1 rounded-lg text-xs font-bold transition bg-gradient-to-r from-turquoiseNeon to-blue-500 text-bgMain shadow-md';
                btnMonedaArs.className = 'px-3 py-1 rounded-lg text-xs font-semibold text-gray-400 hover:text-white transition';
            }
        }

        const cuentaActiva = cuentasUsuario.find(c => c.tipoMoneda === monedaActiva);
        const saldo = cuentaActiva ? cuentaActiva.saldo : 0;
        const textoFormateado = formatCurrency(Number(saldo), monedaActiva);

        if (badgeMoneda) {
            badgeMoneda.textContent = monedaActiva;
            badgeMoneda.className = monedaActiva === 'USD'
                ? 'text-[10px] font-bold px-2 py-0.5 rounded-full border border-turquoiseNeon/50 text-turquoiseNeon bg-turquoiseNeon/10'
                : 'text-[10px] font-bold px-2 py-0.5 rounded-full border border-fuchsiaNeon/50 text-fuchsiaNeon bg-fuchsiaNeon/10';
        }

        actualizarSaldoEnPantalla(textoFormateado);

    } catch (error) {
        console.error('Error al obtener el saldo:', error);
        actualizarSaldoEnPantalla('$ 0,00');
    }
}

function actualizarSaldoEnPantalla(textoFormateado) {
    const saldoElement = document.getElementById('saldo-disponible');
    if (!saldoElement) return;

    if (window.AlkyBalanceVisibility) {
        window.AlkyBalanceVisibility.render(saldoElement, textoFormateado);
    } else {
        saldoElement.textContent = textoFormateado;
    }
}

// Cargar los últimos movimientos de la moneda activa
async function cargarUltimosMovimientos() {
    const listaContainer = document.getElementById('lista-movimientos');
    if (!listaContainer) return;

    const token = localStorage.getItem('token');
    if (!token) return;

    try {
        const response = await fetch(`/api/transacciones/historial?moneda=${monedaActiva}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) return;

        const movimientos = await response.json();

        if (!Array.isArray(movimientos) || movimientos.length === 0) {
            listaContainer.innerHTML = `
                <p class="text-xs text-center text-gray-500 py-4">
                    No hay movimientos recientes en tu cuenta de ${monedaActiva}.
                </p>
            `;
            return;
        }

        const ultimos = movimientos.slice(0, 4);

        listaContainer.innerHTML = ultimos.map(mov => {
            const tipo = (mov.tipoTransaccion ?? mov.tipo ?? '').toUpperCase();
            const esIngreso = tipo === 'INGRESO' || tipo === 'DEPOSITO';
            const montoFormateado = formatCurrency(Number(mov.monto) || 0, monedaActiva);
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

// Sincronizar todos los widgets del dashboard con la moneda activa
function sincronizarDashboard() {
    cargarCuentasYBalance();
    cargarUltimosMovimientos();
    if (window.AlkyReporteGastos?.cargarReportes) {
        window.AlkyReporteGastos.cargarReportes(monedaActiva);
    }
}

// Manejador para abrir la caja de ahorro en USD
async function abrirCuentaUsd() {
    const token = localStorage.getItem('token');
    const btnCrear = document.getElementById('btn-crear-cuenta-usd');
    if (!token || !btnCrear) return;

    btnCrear.disabled = true;
    btnCrear.textContent = 'Abriendo cuenta...';

    try {
        const response = await fetch('/api/cuentas', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ "moneda": "USD" })
        });

        if (response.ok || response.status === 409) {
            monedaActiva = 'USD';
            sincronizarDashboard();
        } else {
            mostrarNotificacionDashboard('No se pudo abrir la cuenta en USD. Intente nuevamente.', 'error');
            btnCrear.disabled = false;
            btnCrear.textContent = 'Abrir cuenta en USD gratis';
        }
    } catch (error) {
        console.error('Error al abrir cuenta en USD:', error);
        btnCrear.disabled = false;
        btnCrear.textContent = 'Abrir cuenta en USD gratis';
    }
}

// Configuración de eventos iniciales
document.addEventListener('DOMContentLoaded', () => {
    sincronizarDashboard();

    const btnMonedaArs = document.getElementById('btn-moneda-ars');
    const btnMonedaUsd = document.getElementById('btn-moneda-usd');
    const btnCrearUsd = document.getElementById('btn-crear-cuenta-usd');

    btnMonedaArs?.addEventListener('click', () => {
        if (monedaActiva === 'ARS') return;
        monedaActiva = 'ARS';
        sincronizarDashboard();
    });

    btnMonedaUsd?.addEventListener('click', () => {
        if (monedaActiva === 'USD') return;
        
        const tieneCuentaUsd = cuentasUsuario.some(c => c.tipoMoneda === 'USD');
        if (!tieneCuentaUsd) {
            mostrarNotificacionDashboard('Aún no tienes cuenta en USD. Ábrela gratuitamente desde el banner superior.');
            return;
        }
        
        monedaActiva = 'USD';
        sincronizarDashboard();
    });

    btnCrearUsd?.addEventListener('click', abrirCuentaUsd);

    if (window.AlkyBalanceVisibility) {
        window.AlkyBalanceVisibility.inicializarBoton(
            document.getElementById('btn-toggle-saldo'),
            document.getElementById('saldo-disponible')
        );
    }
});

function mostrarNotificacionDashboard(mensaje, tipo = 'error') {
    const existente = document.getElementById('toast-notificacion');
    if (existente) existente.remove();

    const toast = document.createElement('div');
    toast.id = 'toast-notificacion';
    const esError = tipo === 'error';
    const colorBorder = esError ? 'border-rose-500/50' : 'border-emerald-500/50';
    const colorText = esError ? 'text-rose-400' : (tipo === 'exito' ? 'text-emerald-400' : 'text-turquoiseNeon');
    
    toast.className = `fixed bottom-10 right-1/2 translate-x-1/2 md:bottom-10 md:right-10 md:translate-x-0 z-50 bg-[#161221] border ${colorBorder} ${colorText} px-5 py-3 rounded-xl shadow-2xl flex items-center gap-3 transition-all duration-300`;
    
    const icon = esError 
        ? '<svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>'
        : '<svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>';

    toast.innerHTML = icon + `<span class="text-xs font-semibold">${mensaje}</span>`;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}
