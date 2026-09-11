// 1. Proteger la página: si no hay token, fuera
const token = localStorage.getItem('token');
if (!token) {
    window.location.href = 'ingresar.html';
}

// 2. Función y evento para cerrar sesión
function cerrarSesion() {
    localStorage.removeItem('token');
    localStorage.removeItem('user_display_name');
    sessionStorage.removeItem('alky_chat_historial');
    sessionStorage.removeItem('alky_chat_abierto');
    window.location.href = 'ingresar.html';
}

document.addEventListener('DOMContentLoaded', () => {
    const btnSalir = document.getElementById('btn-salir');
    if (btnSalir) {
        btnSalir.addEventListener('click', (e) => {
            e.preventDefault();
            cerrarSesion();
        });
    }
});

// 3. Carga y validación real de la sesión contra el backend
document.addEventListener('DOMContentLoaded', async () => {
    const email = obtenerEmailUsuario();

    // En la tarjeta de depósitos SIEMPRE va el email, sin importar el nombre
    const emailVinculado = document.getElementById('email-usuario');
    if (emailVinculado && email) {
        emailVinculado.textContent = email;
    }

    // Nombre en caché para evitar parpadeos visuales
    const nombreEnCache = localStorage.getItem('user_display_name');
    actualizarNombreVisual(nombreEnCache || email);

    if (!token) return;

    try {
        const response = await fetch('/api/usuarios/me', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        // Si el usuario no existe en la BD o el token es inválido (cualquier código != 200)
        if (!response.ok) {
            cerrarSesion();
            return;
        }

        const usuario = await response.json();
        const nombre = (usuario.nombre || '').trim();
        const apellido = (usuario.apellido || '').trim();
        const nombreCompleto = `${nombre} ${apellido}`.trim();

        const displayName = nombreCompleto !== '' ? nombreCompleto : usuario.email;

        if (displayName) {
            localStorage.setItem('user_display_name', displayName);
            actualizarNombreVisual(displayName);
        }

        if (emailVinculado && usuario.email) {
            emailVinculado.textContent = usuario.email;
        }
    } catch (e) {
        console.error('Error al verificar sesión:', e);
    }
});

// Actualiza los textos de bienvenida y avatar (excluyendo el campo de depósito)
function actualizarNombreVisual(texto) {
    if (!texto) return;
    document.querySelectorAll('.user-email').forEach(el => {
        if (el.id !== 'email-usuario') {
            el.textContent = texto;
        }
    });
}

// Obtener el email decodificando el JWT del localStorage
function obtenerEmailUsuario() {
    const token = localStorage.getItem('token');
    if (!token) return null;
    try {
        const payloadBase64 = token.split('.')[1];
        const payloadJson = JSON.parse(atob(payloadBase64));
        return payloadJson['sub'];
    } catch (e) {
        console.error('Error al decodificar token:', e);
        return null;
    }
}
