document.addEventListener("DOMContentLoaded", () => {
  // 1. Capturar el formulario de ingreso
  const loginForm = document.getElementById("ingreso");

  if (loginForm) {
    // 2. Agregar evento submit y prevenir la recarga con event.preventDefault()
    loginForm.addEventListener("submit", (event) => {
      event.preventDefault();

      // Captura de inputs y elementos de error
      const emailInput = document.getElementById("Iemail");
      const passwordInput = document.getElementById("Ipassword");

      const errorEmail = document.getElementById("errorEmail");
      const errorPassword = document.getElementById("errorPassword");

      const mensajeExito = document.getElementById("mensajeExito");
      const mensajeError = document.getElementById("mensajeError");

      // Limpiar errores previos
      [errorEmail, errorPassword, mensajeExito, mensajeError].forEach((el) => {
        if (el) {
          el.classList.add("hidden");
          el.style.display = "none";
        }
      });

      let isValid = true;

      // 3. Validar email con expresión regular básica
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailInput.value.trim()) {
        showError(errorEmail, "El correo electrónico es obligatorio.");
        isValid = false;
      } else if (!emailRegex.test(emailInput.value.trim())) {
        showError(errorEmail, "Ingrese un correo electrónico válido.");
        isValid = false;
      }

      // 4. Validar contraseña con longitud mínima (8 caracteres)
      if (!passwordInput.value) {
        showError(errorPassword, "La contraseña es obligatoria.");
        isValid = false;
      } else if (passwordInput.value.length < 8) {
        showError(errorPassword, "Ingrese una contraseña válida.");
        isValid = false;
      }

      // 5. Si es válido, enviar credenciales a la API
      if (isValid) {
        const loginData = {
          email: emailInput.value.trim(),
          password: passwordInput.value,
        };

        const submitButton = loginForm.querySelector('button[type=\"submit\"]');
        if (submitButton) {
          submitButton.disabled = true;
        }

        // Petición POST con Axios al endpoint de login
        axios
          .post("/api/auth/login", loginData, {
            headers: { "Content-Type": "application/json" },
          })
          .then((response) => {
            // 5. Extraer el token JWT y guardarlo en localStorage
            const token = response.data?.token;

            if (token) {
              sessionStorage.clear();
              localStorage.setItem("token", token);
            }

            if (mensajeExito) {
              mensajeExito.textContent =
                "¡Inicio de sesión exitoso! Redirigiendo...";
              mensajeExito.classList.remove("hidden");
              mensajeExito.style.display = "block";
            }

            // Redirigir al dashboard / tablero de control
            setTimeout(() => {
              window.location.href = "tableroDeControl.html";
            }, 1200);
          })
          .catch((error) => {
            console.error("Error de login:", error);

            if (!error.response) {
              showError(
                mensajeError,
                "No se pudo conectar con el servidor. Verifica tu conexión.",
              );
              return;
            }

            const status = error.response.status;

            // 6. Mensaje claro según código de respuesta
            if (status === 401 || status === 403) {
              showError(
                mensajeError,
                "Credenciales inválidas. Verifica tu email y contraseña.",
              );
            } else if (status === 404) {
              showError(
                mensajeError,
                "Usuario no encontrado o servicio no disponible.",
              );
            } else if (status >= 500) {
              showError(
                mensajeError,
                "Error interno del servidor. Intenta de nuevo más tarde.",
              );
            } else {
              showError(mensajeError, "Ocurrió un error al iniciar sesión.");
            }
          })
          .finally(() => {
            if (submitButton) {
              submitButton.disabled = false;
              submitButton.textContent = "Sign in";
            }
          });
      }
    });
  }

  // Función auxiliar para mostrar mensajes de error
  function showError(element, message) {
    if (!element) return;
    element.textContent = message;
    element.classList.remove("hidden");
    element.style.display = "block";
  }
});
