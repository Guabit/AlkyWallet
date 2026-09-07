// 1. Proteger la página: si no hay token, fuera
const token = localStorage.getItem('token');
if (!token) {
    window.location.href = 'ingresar.html';
}

// 2. Función y evento para cerrar sesión
function cerrarSesion() {
    localStorage.removeItem('token');
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

document.addEventListener('DOMContentLoaded', () => {
    const email = obtenerEmailUsuario();
    if (email) {
        // Llena todos los elementos de texto con clase .user-email
        document.querySelectorAll('.user-email').forEach(el => {
            el.textContent = email;
        });
    }
});

// Obtener el email del usuario decodificando el JWT del localStorage
function obtenerEmailUsuario() {
    const token = localStorage.getItem('token');
    if (!token) return null;
    try {
        // La parte [1] es el payload en base64
        const payloadBase64 = token.split('.')[1];
        const payloadJson = JSON.parse(atob(payloadBase64));
        return payloadJson['sub'];
    } catch (e) {
        console.error('Error al decodificar token:', e);
        return null;
    }
}