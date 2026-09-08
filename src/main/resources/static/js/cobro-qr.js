document.addEventListener('DOMContentLoaded', () => {
    const btnGenerar = document.getElementById('btn-generar-qr');
    const inputMonto = document.getElementById('monto-cobro');
    const contenedorQr = document.getElementById('qr-cobro-contenedor');
    const imgQr = document.getElementById('qr-cobro-img');
    const inputLink = document.getElementById('link-cobro-texto');
    const btnCopiarLink = document.getElementById('btn-copiar-link-cobro');

    if (!btnGenerar) return;

    function obtenerEmailDesdeToken() {
        const token = localStorage.getItem('token');
        if (!token) return null;
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload['sub'] || null;
        } catch (e) {
            return null;
        }
    }

    btnGenerar.addEventListener('click', () => {
        const email = obtenerEmailDesdeToken();
        if (!email) return;

        const monto = parseFloat(inputMonto.value);
        const params = new URLSearchParams({ to: email });
        if (!isNaN(monto) && monto > 0) {
            params.set('amount', monto.toFixed(2));
        }

        const link = `${window.location.origin}/html/tranferencia.html?${params.toString()}`;
        const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(link)}`;

        if (imgQr) imgQr.src = qrUrl;
        if (inputLink) inputLink.value = link;
        if (contenedorQr) contenedorQr.classList.remove('hidden');
    });

    if (btnCopiarLink) {
        btnCopiarLink.addEventListener('click', () => {
            if (!inputLink || !inputLink.value) return;
            navigator.clipboard.writeText(inputLink.value).then(() => {
                const original = btnCopiarLink.textContent;
                btnCopiarLink.textContent = '¡Copiado!';
                setTimeout(() => { btnCopiarLink.textContent = original; }, 2000);
            }).catch((err) => console.error('Error al copiar el link:', err));
        });
    }
});
