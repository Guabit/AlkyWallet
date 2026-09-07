document.addEventListener('DOMContentLoaded', async () => {
    const token = localStorage.getItem('token');
    if (!token) return;

    const emailSpan = document.getElementById('email-usuario');
    const dniSpan = document.getElementById('dni-usuario');
    const nombreInput = document.getElementById('nombre');
    const apellidoInput = document.getElementById('apellido');
    const nombreHeader = document.getElementById('nombre-cliente');
    const formPerfil = document.getElementById('form-perfil');
    const btnGuardar = document.getElementById('btn-guardar');
    const mensajePerfil = document.getElementById('mensaje-perfil');

    // 1. Cargar datos del usuario desde el backend al entrar a la pantalla
    try {
        const response = await fetch('/api/usuarios/me', {
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

        if (response.ok) {
            const usuario = await response.json();
            if (emailSpan) emailSpan.textContent = usuario.email || '';
            if (dniSpan) dniSpan.textContent = usuario.dni || '';
            if (nombreInput) nombreInput.value = usuario.nombre || '';
            if (apellidoInput) apellidoInput.value = usuario.apellido || '';

            actualizarNombreHeader(usuario.nombre, usuario.apellido, usuario.email);
        }
    } catch (error) {
        console.error('Error al obtener perfil:', error);
    }

    // 2. Guardar cambios de nombre y apellido al enviar el formulario
    if (formPerfil) {
        formPerfil.addEventListener('submit', async (e) => {
            e.preventDefault();

            const nuevoNombre = nombreInput ? nombreInput.value.trim() : '';
            const nuevoApellido = apellidoInput ? apellidoInput.value.trim() : '';

            if (btnGuardar) {
                btnGuardar.disabled = true;
                btnGuardar.textContent = 'Guardando...';
            }

            try {
                const response = await fetch('/api/usuarios/me', {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({
                        nombre: nuevoNombre,
                        apellido: nuevoApellido
                    })
                });

                if (response.ok) {
                    const usuarioActualizado = await response.json();
                    actualizarNombreHeader(usuarioActualizado.nombre, usuarioActualizado.apellido, usuarioActualizado.email);
                    mostrarMensaje('¡Perfil actualizado con éxito!', 'exito');
                } else {
                    mostrarMensaje('No se pudo actualizar el perfil. Intenta nuevamente.', 'error');
                }
            } catch (error) {
                console.error('Error al guardar perfil:', error);
                mostrarMensaje('Error de conexión con el servidor.', 'error');
            } finally {
                if (btnGuardar) {
                    btnGuardar.disabled = false;
                    btnGuardar.textContent = 'Guardar';
                }
            }
        });
    }

    // Función auxiliar para actualizar el título arriba del avatar
    function actualizarNombreHeader(nombre, apellido, email) {
        if (!nombreHeader) return;
        if (nombre && apellido) {
            nombreHeader.textContent = `${nombre} ${apellido}`;
        } else if (nombre) {
            nombreHeader.textContent = nombre;
        } else {
            nombreHeader.textContent = email || 'Cuenta Personal';
        }
    }

    // Función auxiliar para mostrar avisos en pantalla
    function mostrarMensaje(texto, tipo) {
        if (!mensajePerfil) return;
        mensajePerfil.textContent = texto;
        mensajePerfil.classList.remove('hidden', 'text-emerald-400', 'text-red-400');

        if (tipo === 'exito') {
            mensajePerfil.classList.add('text-emerald-400');
        } else {
            mensajePerfil.classList.add('text-red-400');
        }

        setTimeout(() => {
            mensajePerfil.classList.add('hidden');
        }, 3000);
    }
});