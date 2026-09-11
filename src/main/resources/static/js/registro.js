document.addEventListener("DOMContentLoaded", () => {
  // 1. Capturar el formulario de registro
  const registerForm = document.getElementById("registro");

  if (registerForm) {
    // 2. Agregar evento submit y prevenir la recarga con event.preventDefault()
    registerForm.addEventListener("submit", (event) => {
      event.preventDefault();

      // Captura de inputs y elementos de error
      const dniInput = document.getElementById("Rdni");
      const emailInput = document.getElementById("Remail");
      const passwordInput = document.getElementById("Rpassword");
      const confirmPasswordInput = document.getElementById("r-password");

      const errorDni = document.getElementById("errorDni");
      const errorEmail = document.getElementById("errorEmail");
      const errorPassword = document.getElementById("errorPassword");
      const errorConfirmPassword = document.getElementById(
        "errorConfirmarPassword",
      );

      const mensajeExito = document.getElementById("mensajeExito");
      const mensajeError = document.getElementById("mensajeError");

      // Limpiar errores previos
      [
        errorDni,
        errorEmail,
        errorPassword,
        errorConfirmPassword,
        mensajeExito,
        mensajeError,
      ].forEach((el) => {
        if (el) {
          el.classList.add("hidden");
          el.style.display = "none";
        }
      });

      let isValid = true;

      // 3. Validar DNI: no vacío, solo números y entre 7 y 8 dígitos
      const dniRegex = /^[0-9]{7,8}$/;
      const dniValor = dniInput.value.trim();

      if (!dniValor) {
        showError(errorDni, "El DNI es obligatorio.");
        isValid = false;
      } else if (!dniRegex.test(dniValor)) {
        showError(errorDni, "Ingresá un DNI válido (entre 7 y 8 números).");
        isValid = false;
      }

      // 4. Validar email con expresión regular básica
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailInput.value.trim()) {
        showError(errorEmail, "El correo electrónico es obligatorio.");
        isValid = false;
      } else if (!emailRegex.test(emailInput.value.trim())) {
        showError(errorEmail, "Ingresá un correo electrónico válido.");
        isValid = false;
      }

      // 5. Validar contraseña con longitud mínima (8 caracteres)
      if (!passwordInput.value) {
        showError(errorPassword, "La contraseña es obligatoria.");
        isValid = false;
      } else if (passwordInput.value.length < 8) {
        showError(
          errorPassword,
          "La contraseña debe tener al menos 8 caracteres.",
        );
        isValid = false;
      }

      // 6. Validar confirmacion de contraseña
      if (!confirmPasswordInput.value) {
        showError(errorConfirmPassword, "Debes confirmar la contraseña.");
        isValid = false;
      } else if (
        confirmPasswordInput &&
        confirmPasswordInput.value !== passwordInput.value
      ) {
        showError(errorConfirmPassword, "Las contraseñas no coinciden.");
        isValid = false;
      }

      // 7. Si es válido, enviar credenciales a la API
      if (isValid) {
        // Crea objeto con los datos capturados
        const usuarioData = {
          dni: dniInput.value.trim(),
          email: emailInput.value.trim(),
          password: passwordInput.value,
        };

        const submitButton = registerForm.querySelector(
          'button[type=\"submit\"]',
        );
        if (submitButton) {
          submitButton.disabled = true;
        }

        const config = {
          headers: {
            "Content-Type": "application/json",
          },
        };

        // Petición POST con Axios al endpoint de registro
        axios
          .post("/api/usuarios/registrar", usuarioData, config)
          .then((response) => {
            console.log("Respuesta del servidor:", response.data);

            registerForm.reset();
            sessionStorage.clear();

            // Mostrar mensaje de éxito en la tarjeta
            if (mensajeExito) {
              mensajeExito.textContent =
                "¡Registro exitoso! Redirigiendo al login...";
              mensajeExito.classList.remove("hidden");
              mensajeExito.style.display = "block";
            }

            // Redirigir a los 2 segundos a la pantalla de login
            setTimeout(() => {
              window.location.href = "ingresar.html";
            }, 2000);
          })
          .catch((error) => {
            console.error("Error de la peticion:", error);

            if (!error.response) {
              showError(
                mensajeError,
                "No se pudo conectar con el servidor. Verifica tu conexión",
              );
              return;
            }

            const status = error.response.status;

            // Manejo de errores según el código de estado
            if (status === 409) {
              showError(
                mensajeError,
                "El DNI o el correo electrónico ya se encuentran registrados.",
              );
            } else if (status === 400) {
              showError(mensajeError, "Datos de registro inválidos.");
            } else if (status === 404) {
              showError(
                mensajeError,
                "El servicio de registro no está disponible.",
              );
            } else if (status >= 500) {
              showError(
                mensajeError,
                "Error interno del servidor. Intenta de nuevo más tarde.",
              );
            } else {
              showError(
                mensajeError,
                "Ocurrió un error al procesar el registro.",
              );
            }
          })
          .finally(() => {
            if (submitButton) {
              submitButton.disabled = false;
            }
          });
      }
    });
  }

  // Función para mostrar mensajes de error en color rojo debajo de los inputs
  function showError(element, message) {
    if (!element) return;
    element.textContent = message;
    element.classList.remove("hidden");
    element.style.display = "block";
  }
});
