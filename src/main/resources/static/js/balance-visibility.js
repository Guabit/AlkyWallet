// Utilidad compartida para mostrar/ocultar montos sensibles (botón "ojo").
// Se carga ANTES que dashboard.js / tranferencia.js para que esos scripts
// puedan usar window.AlkyBalanceVisibility al renderizar el saldo, en vez
// de escribir el texto directamente con textContent.
(function () {
    const STORAGE_KEY = 'alkywallet_saldo_oculto';
    const MASCARA = '•••••••';

    const ICONO_OJO = '<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/></svg>';
    const ICONO_OJO_TACHADO = '<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21"/></svg>';

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
