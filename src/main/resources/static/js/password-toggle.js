document.addEventListener('DOMContentLoaded', () => {
    const ICONO_OJO = `<svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>`;
    const ICONO_OJO_TACHADO = `<svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" viewBox="0 0 24 24"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>`;

    document.querySelectorAll('[data-toggle-password]').forEach(boton => {
        const inputId = boton.getAttribute('data-toggle-password');
        const input = document.getElementById(inputId);
        if (!input) return;

        boton.innerHTML = ICONO_OJO_TACHADO;

        boton.addEventListener('click', () => {
            const esPassword = input.getAttribute('type') === 'password';
            input.setAttribute('type', esPassword ? 'text' : 'password');
            boton.innerHTML = esPassword ? ICONO_OJO : ICONO_OJO_TACHADO;
            boton.setAttribute('aria-label', esPassword ? 'Ocultar contraseña' : 'Mostrar contraseña');
        });
    });
});