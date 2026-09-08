// Utilidad compartida para mostrar/ocultar montos sensibles (botón "ojo").
// Se carga ANTES que dashboard.js / tranferencia.js para que esos scripts
// puedan usar window.AlkyBalanceVisibility al renderizar el saldo, en vez
// de escribir el texto directamente con textContent.
(function () {
    const STORAGE_KEY = 'alkywallet_saldo_oculto';
    const MASCARA = '••••••••';

    const ICONO_OJO = '<svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>';

    const ICONO_OJO_TACHADO = '<svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" viewBox="0 0 24 24"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';

    // Recuerda el último texto "real" mostrado en cada elemento para poder
    // restaurarlo al des-ocultar, sin volver a pedirle nada al backend.
    const ultimoValorPorElemento = new WeakMap();

    function estaOculto() {
        return localStorage.getItem(STORAGE_KEY) === 'true';
    }

    function guardarEstado(oculto) {
        localStorage.setItem(STORAGE_KEY, oculto ? 'true' : 'false');
    }

    // Llamar desde cada página en vez de "elemento.textContent = texto".
    function render(elemento, textoFormateado) {
        if (!elemento) return;
        ultimoValorPorElemento.set(elemento, textoFormateado);
        elemento.textContent = estaOculto() ? MASCARA : textoFormateado;
    }

    function actualizarIcono(boton) {
        if (!boton) return;
        boton.innerHTML = estaOculto() ? ICONO_OJO_TACHADO : ICONO_OJO;
        boton.setAttribute('aria-label', estaOculto() ? 'Mostrar saldo' : 'Ocultar saldo');
    }

    // Engancha el botón "ojo" a un elemento de saldo específico.
    function inicializarBoton(boton, elemento) {
        if (!boton || !elemento) return;
        actualizarIcono(boton);
        boton.addEventListener('click', () => {
            guardarEstado(!estaOculto());
            actualizarIcono(boton);
            const valorGuardado = ultimoValorPorElemento.get(elemento);
            elemento.textContent = estaOculto() ? MASCARA : (valorGuardado ?? elemento.textContent);
        });
    }

    window.AlkyBalanceVisibility = { render, inicializarBoton, estaOculto };
})();
