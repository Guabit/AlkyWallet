// Módulo de Reporte y Distribución de Movimientos Multimoneda (ARS / USD)
(function () {
    let monedaActual = 'ARS';

    function formatearMoneda(valor, moneda = monedaActual) {
        const num = Math.abs(Number(valor) || 0);
        const formateado = new Intl.NumberFormat('es-AR', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(num);
        const simbolo = (moneda === 'USD') ? 'US$' : '$';
        return `${simbolo} ${formateado}`;
    }

    async function cargarReportes(moneda = 'ARS') {
        monedaActual = moneda;
        const token = localStorage.getItem('token');
        if (!token) {
            window.location.href = 'ingresar.html';
            return;
        }

        await Promise.all([
            cargarReportePorTipo(token, monedaActual),
            cargarReportePorCategoria(token, monedaActual)
        ]);
    }

    async function cargarReportePorTipo(token, moneda) {
        const contenedor = document.getElementById('reporte-gastos-contenedor');
        const badgeTotal = document.getElementById('reporte-total-gastos');

        if (!contenedor) return;

        try {
            const response = await fetch(`/api/transacciones/reporte-gastos?moneda=${moneda}`, {
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
                contenedor.innerHTML = '<p class="text-xs text-center text-red-400 py-4">Error al cargar el reporte de gastos.</p>';
                return;
            }

            const reporte = await response.json();
            contenedor.innerHTML = '';

            if (!Array.isArray(reporte) || reporte.length === 0) {
                contenedor.innerHTML = `<p class="text-xs text-center text-gray-500 py-4">Aún no hay transacciones en ${moneda} para generar un reporte.</p>`;
                if (badgeTotal) badgeTotal.textContent = `Total: ${formatearMoneda(0, moneda)}`;
                return;
            }

            const granTotal = reporte.reduce((acumulado, item) => acumulado + (Number(item.total) || 0), 0);

            if (badgeTotal) {
                badgeTotal.textContent = `Total operado: ${formatearMoneda(granTotal, moneda)}`;
            }

            reporte.forEach(item => {
                const monto = Number(item.total) || 0;
                const porcentaje = granTotal > 0 ? Math.round((monto / granTotal) * 100) : 0;
                const tipo = String(item.tipoTransaccion || '').toUpperCase();
                const config = obtenerConfiguracionTipo(tipo);

                renderBarra(contenedor, config.icono, config.etiqueta, config.colorTexto, config.gradienteBarra, monto, porcentaje, moneda);
            });

        } catch (error) {
            console.error('Error al generar el reporte de gastos:', error);
            contenedor.innerHTML = '<p class="text-xs text-center text-red-400 py-4">Error de conexión al obtener el reporte.</p>';
        }
    }

    async function cargarReportePorCategoria(token, moneda) {
        const contenedor = document.getElementById('reporte-categorias-contenedor');
        if (!contenedor) return;

        try {
            const response = await fetch(`/api/transacciones/reporte-categorias?moneda=${moneda}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                }
            });

            if (!response.ok) {
                contenedor.innerHTML = '<p class="text-xs text-center text-red-400 py-4">Error al cargar las categorías.</p>';
                return;
            }

            const reporte = await response.json();
            contenedor.innerHTML = '';

            if (!Array.isArray(reporte) || reporte.length === 0) {
                contenedor.innerHTML = `<p class="text-xs text-center text-gray-500 py-4">Todavía no hay movimientos categorizados en ${moneda}.</p>`;
                return;
            }

            const granTotal = reporte.reduce((acumulado, item) => acumulado + (Number(item.total) || 0), 0);

            [...reporte]
                .sort((a, b) => (Number(b.total) || 0) - (Number(a.total) || 0))
                .forEach(item => {
                    const monto = Number(item.total) || 0;
                    const porcentaje = granTotal > 0 ? Math.round((monto / granTotal) * 100) : 0;
                    const categoria = String(item.categoria || '').toUpperCase();
                    const config = obtenerConfiguracionCategoria(categoria);

                    renderBarra(contenedor, config.icono, config.etiqueta, config.colorTexto, config.gradienteBarra, monto, porcentaje, moneda);
                });

        } catch (error) {
            console.error('Error al generar el reporte por categoría:', error);
            contenedor.innerHTML = '<p class="text-xs text-center text-red-400 py-4">Error de conexión al obtener las categorías.</p>';
        }
    }

    function renderBarra(contenedor, icono, etiqueta, colorTexto, gradienteBarra, monto, porcentaje, moneda) {
        const itemWrapper = document.createElement('div');
        itemWrapper.className = 'bg-[#0D0B14] p-4 rounded-xl border border-gray-800/80 hover:border-gray-700 transition';

        const header = document.createElement('div');
        header.className = 'flex justify-between items-center mb-2 text-xs';

        const titulo = document.createElement('span');
        titulo.className = 'font-semibold text-white flex items-center gap-2';
        titulo.innerHTML = `${icono} ${etiqueta}`;

        const infoMonto = document.createElement('div');
        infoMonto.className = 'flex items-center space-x-2';

        const textoMonto = document.createElement('span');
        textoMonto.className = `font-bold ${colorTexto}`;
        textoMonto.textContent = formatearMoneda(monto, moneda);

        const textoPorcentaje = document.createElement('span');
        textoPorcentaje.className = 'text-gray-400 text-[11px] bg-white/5 px-2 py-0.5 rounded-full';
        textoPorcentaje.textContent = `${porcentaje}%`;

        infoMonto.appendChild(textoMonto);
        infoMonto.appendChild(textoPorcentaje);

        header.appendChild(titulo);
        header.appendChild(infoMonto);

        const barraTrack = document.createElement('div');
        barraTrack.className = 'w-full h-2.5 bg-gray-800/90 rounded-full overflow-hidden';

        const barraFill = document.createElement('div');
        barraFill.className = `h-full rounded-full transition-all duration-700 ease-out ${gradienteBarra}`;
        barraFill.style.width = '0%';

        barraTrack.appendChild(barraFill);

        itemWrapper.appendChild(header);
        itemWrapper.appendChild(barraTrack);
        contenedor.appendChild(itemWrapper);

        requestAnimationFrame(() => {
            setTimeout(() => {
                barraFill.style.width = `${porcentaje}%`;
            }, 50);
        });
    }

    function obtenerConfiguracionTipo(tipo) {
        switch (tipo) {
            case 'DEPOSITO':
                return {
                    etiqueta: 'Depósitos',
                    icono: '💰',
                    colorTexto: 'text-emerald-400',
                    gradienteBarra: 'bg-gradient-to-r from-emerald-500 to-teal-400'
                };
            case 'INGRESO':
                return {
                    etiqueta: 'Transferencias Recibidas',
                    icono: '📥',
                    colorTexto: 'text-turquoiseNeon',
                    gradienteBarra: 'bg-gradient-to-r from-cyan-400 to-turquoiseNeon'
                };
            case 'EGRESO':
                return {
                    etiqueta: 'Transferencias Enviadas / Pagos',
                    icono: '📤',
                    colorTexto: 'text-rose-400',
                    gradienteBarra: 'bg-gradient-to-r from-rose-500 to-fuchsiaNeon'
                };
            default:
                return {
                    etiqueta: tipo || 'Otros',
                    icono: '💳',
                    colorTexto: 'text-gray-300',
                    gradienteBarra: 'bg-gradient-to-r from-purple-500 to-indigo-500'
                };
        }
    }

    function obtenerConfiguracionCategoria(categoria) {
        const mapa = {
            COMIDA: { etiqueta: 'Comida', icono: '🍔', colorTexto: 'text-amber-400', gradienteBarra: 'bg-gradient-to-r from-amber-500 to-orange-400' },
            TRANSPORTE: { etiqueta: 'Transporte', icono: '🚌', colorTexto: 'text-sky-400', gradienteBarra: 'bg-gradient-to-r from-sky-500 to-cyan-400' },
            SERVICIOS: { etiqueta: 'Servicios', icono: '🧾', colorTexto: 'text-indigo-400', gradienteBarra: 'bg-gradient-to-r from-indigo-500 to-purple-400' },
            ENTRETENIMIENTO: { etiqueta: 'Entretenimiento', icono: '🎮', colorTexto: 'text-fuchsiaNeon', gradienteBarra: 'bg-gradient-to-r from-fuchsiaNeon to-purple-500' },
            SALUD: { etiqueta: 'Salud', icono: '💊', colorTexto: 'text-emerald-400', gradienteBarra: 'bg-gradient-to-r from-emerald-500 to-teal-400' },
            EDUCACION: { etiqueta: 'Educación', icono: '📚', colorTexto: 'text-turquoiseNeon', gradienteBarra: 'bg-gradient-to-r from-cyan-400 to-turquoiseNeon' },
            INVERSION: { etiqueta: 'Inversiones', icono: '📈', colorTexto: 'text-emerald-400', gradienteBarra: 'bg-gradient-to-r from-emerald-500 to-turquoiseNeon' },
            TRANSFERENCIA: { etiqueta: 'Transferencias', icono: '🔄', colorTexto: 'text-rose-400', gradienteBarra: 'bg-gradient-to-r from-rose-500 to-fuchsiaNeon' },
            OTROS: { etiqueta: 'Otros', icono: '💳', colorTexto: 'text-gray-300', gradienteBarra: 'bg-gradient-to-r from-gray-500 to-gray-400' }
        };
        return mapa[categoria] || mapa.OTROS;
    }

    // Exponer API global
    window.AlkyReporteGastos = {
        cargarReportes,
        formatearMoneda
    };

    // Carga inicial por defecto
    document.addEventListener('DOMContentLoaded', () => {
        cargarReportes('ARS');
    });
})();
