/**
 * asistente-widget.js - Widget flotante global de Asistente IA para AlkyWallet
 * Inyecta un botón flotante y un panel de chat persistente entre navegación de páginas.
 */
document.addEventListener("DOMContentLoaded", () => {
  const token = localStorage.getItem("token");
  if (!token) return; // No mostrar si no hay sesión activa

  // Obtener email del token para aislar el chat por usuario
  function obtenerEmailToken(jwt) {
    try {
      const payloadBase64 = jwt.split(".")[1];
      const payloadJson = JSON.parse(atob(payloadBase64));
      return payloadJson["sub"] || "default";
    } catch (e) {
      return "default";
    }
  }

  const emailUsuario = obtenerEmailToken(token);
  const STORAGE_HISTORIAL = `alky_chat_historial_${emailUsuario}`;
  const STORAGE_ABIERTO = `alky_chat_abierto_${emailUsuario}`;

  // Limpiar claves obsoletas sin scope si existieran
  sessionStorage.removeItem("alky_chat_historial");
  sessionStorage.removeItem("alky_chat_abierto");

  // Inyectar HTML del widget flotante
  const widgetContainer = document.createElement("div");
  widgetContainer.id = "alky-asistente-container";
  widgetContainer.className =
    "fixed bottom-6 right-6 z-50 flex flex-col items-end";

  widgetContainer.innerHTML = `
      <!-- VENTANA DE CHAT FLOTANTE (Oculta por defecto) -->
      <div
        id="alky-chat-panel"
        class="hidden flex-col w-80 sm:w-96 max-w-[calc(100vw-2rem)] h-[460px] max-h-[75vh] bg-[#161221]/95 backdrop-blur-xl border border-turquoiseNeon/30 rounded-2xl shadow-2xl overflow-hidden mb-3 transition-all duration-300 transform scale-95 opacity-0"
      >
        <!-- Header del Chat -->
        <div class="bg-gradient-to-r from-[#1b152b] to-[#161221] border-b border-gray-800 p-3.5 flex items-center justify-between shrink-0">
          <div class="flex items-center space-x-2.5">
            <div class="w-8 h-8 rounded-xl bg-turquoiseNeon/10 border border-turquoiseNeon/30 flex items-center justify-center text-turquoiseNeon">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <div>
              <div class="flex items-center space-x-1.5">
                <h4 class="text-xs font-bold text-white tracking-wide">Asistente Alky</h4>
                <span class="inline-block w-2 h-2 rounded-full bg-emerald-400 animate-pulse" title="En línea"></span>
              </div>
              <p class="text-[10px] text-gray-400">Copiloto Financiero IA</p>
            </div>
          </div>

          <div class="flex items-center space-x-1">
            <!-- Botón limpiar chat -->
            <button
              id="btn-limpiar-chat"
              type="button"
              title="Limpiar conversación"
              class="w-7 h-7 rounded-lg text-gray-400 hover:text-red-400 hover:bg-gray-800/60 flex items-center justify-center transition text-xs"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
            <!-- Botón cerrar -->
            <button
              id="btn-cerrar-asistente"
              type="button"
              aria-label="Cerrar Asistente"
              class="w-7 h-7 rounded-lg text-gray-400 hover:text-white hover:bg-gray-800/60 flex items-center justify-center transition text-sm"
            >
              ✕
            </button>
          </div>
        </div>

        <!-- Contenedor de Mensajes -->
        <div
          id="chat-mensajes-lista"
          class="flex-1 min-h-0 p-3.5 space-y-3 overflow-y-auto text-xs scroll-smooth"
        >
        </div>

        <!-- Footer / Input de Envío -->
        <form
          id="form-chat-flotante"
          class="p-2.5 bg-[#0D0B14]/90 border-t border-gray-800 flex items-center gap-2 shrink-0"
        >
          <input
            type="text"
            id="input-pregunta-flotante"
            placeholder="Escribí una pregunta..."
            autocomplete="off"
            class="flex-1 bg-[#161221] text-white text-xs border border-gray-700/80 rounded-xl px-3 py-2 outline-none focus:border-turquoiseNeon transition placeholder-gray-500"
          />
          <button
            type="submit"
            id="btn-enviar-flotante"
            aria-label="Enviar pregunta"
            class="w-8 h-8 rounded-xl bg-gradient-to-r from-fuchsiaNeon to-turquoiseNeon text-white flex items-center justify-center shadow-lg hover:opacity-90 transition shrink-0 glow-button"
          >
            <svg class="w-4 h-4 transform rotate-90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19V5m0 0l-7 7m7-7l7 7" />
            </svg>
          </button>
        </form>
      </div>

      <!-- BOTÓN FLOTANTE TRIGGER -->
      <button
        id="btn-toggle-asistente"
        type="button"
        title="Consultar al Asistente IA"
        class="w-13 h-13 rounded-2xl bg-cardDark border-2 border-turquoiseNeon/60 hover:border-turquoiseNeon text-white shadow-2xl flex items-center justify-center transition-all duration-300 hover:scale-105 hover:shadow-[0_0_20px_rgba(0,242,254,0.4)] group"
      >
        <div class="relative flex items-center justify-center">
          <svg class="w-6 h-6 text-turquoiseNeon group-hover:rotate-12 transition-transform duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          <span class="absolute -top-1 -right-1 w-2.5 h-2.5 rounded-full bg-fuchsiaNeon border-2 border-[#161221]"></span>
        </div>
      </button>
    `;

  document.body.appendChild(widgetContainer);

  // Referencias a elementos
  const panel = document.getElementById("alky-chat-panel");
  const btnToggle = document.getElementById("btn-toggle-asistente");
  const btnCerrar = document.getElementById("btn-cerrar-asistente");
  const btnLimpiar = document.getElementById("btn-limpiar-chat");
  const form = document.getElementById("form-chat-flotante");
  const input = document.getElementById("input-pregunta-flotante");
  const btnEnviar = document.getElementById("btn-enviar-flotante");
  const lista = document.getElementById("chat-mensajes-lista");

  let abierto = false;
  let historial = [];

  const MENSAJE_BIENVENIDA =
    "¡Hola! Soy tu copiloto financiero. Podés preguntarme tu <strong>saldo actual</strong>, <strong>gastos del mes</strong>, la <strong>cotización del dólar</strong> o dudas sobre tus movimientos.";

  // Cargar historial desde sessionStorage
  function cargarHistorial() {
    try {
      const guardado = sessionStorage.getItem(STORAGE_HISTORIAL);
      historial = guardado ? JSON.parse(guardado) : [];
    } catch (e) {
      historial = [];
    }

    lista.innerHTML = "";

    // Si no hay historial previo, mostrar bienvenida
    if (historial.length === 0) {
      agregarBurbujaDOM(MENSAJE_BIENVENIDA, false, true);
    } else {
      historial.forEach((item) => {
        agregarBurbujaDOM(item.texto, item.esUsuario);
      });
    }
    lista.scrollTop = lista.scrollHeight;
  }

  function guardarEnStorage() {
    try {
      sessionStorage.setItem(STORAGE_HISTORIAL, JSON.stringify(historial));
    } catch (e) {
      console.warn(
        "No se pudo guardar el historial del asistente en sessionStorage",
        e,
      );
    }
  }

  function abrirChat(foco = true) {
    abierto = true;
    sessionStorage.setItem(STORAGE_ABIERTO, "true");
    panel.classList.remove("hidden");
    panel.classList.add("flex");
    requestAnimationFrame(() => {
      panel.classList.remove("scale-95", "opacity-0");
      panel.classList.add("scale-100", "opacity-100");
      if (foco) input.focus();
      lista.scrollTop = lista.scrollHeight;
    });
  }

  function cerrarChat() {
    abierto = false;
    sessionStorage.setItem(STORAGE_ABIERTO, "false");
    panel.classList.remove("scale-100", "opacity-100");
    panel.classList.add("scale-95", "opacity-0");
    setTimeout(() => {
      if (!abierto) {
        panel.classList.remove("flex");
        panel.classList.add("hidden");
      }
    }, 200);
  }

  btnToggle.addEventListener("click", () => {
    if (abierto) cerrarChat();
    else abrirChat(true);
  });

  btnCerrar.addEventListener("click", cerrarChat);

  btnLimpiar.addEventListener("click", () => {
    historial = [];
    sessionStorage.removeItem(STORAGE_HISTORIAL);
    lista.innerHTML = "";
    agregarBurbujaDOM(MENSAJE_BIENVENIDA, false, true);
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && abierto) cerrarChat();
  });

  function agregarBurbujaDOM(texto, esUsuario, esHtml = false) {
    const burbuja = document.createElement("div");
    burbuja.className = esUsuario
      ? "ml-auto max-w-[80%] bg-fuchsiaNeon/20 border border-fuchsiaNeon/30 text-white rounded-2xl rounded-tr-xs px-3.5 py-2 leading-relaxed shadow-sm"
      : "mr-auto max-w-[85%] bg-[#0D0B14] border border-gray-800 text-gray-200 rounded-2xl rounded-tl-xs px-3.5 py-2.5 leading-relaxed shadow-sm";

    if (esHtml) {
      burbuja.innerHTML = texto;
    } else {
      burbuja.innerHTML = texto
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>")
        .replace(/\n/g, "<br>");
    }

    lista.appendChild(burbuja);
    lista.scrollTop = lista.scrollHeight;
  }

  function agregarMensaje(texto, esUsuario) {
    historial.push({ texto, esUsuario });
    guardarEnStorage();
    agregarBurbujaDOM(texto, esUsuario);
  }

  function mostrarCargando() {
    const typing = document.createElement("div");
    typing.id = "asistente-typing-indicator";
    typing.className =
      "mr-auto bg-[#0D0B14] border border-gray-800 text-gray-400 rounded-2xl rounded-tl-xs px-3.5 py-2 flex items-center space-x-1 text-[11px]";
    typing.innerHTML = `
          <span>Pensando</span>
          <span class="animate-bounce">.</span>
          <span class="animate-bounce" style="animation-delay: 0.2s">.</span>
          <span class="animate-bounce" style="animation-delay: 0.4s">.</span>
        `;
    lista.appendChild(typing);
    lista.scrollTop = lista.scrollHeight;
  }

  function ocultarCargando() {
    const typing = document.getElementById("asistente-typing-indicator");
    if (typing) typing.remove();
  }

  // Inicializar estado guardado
  cargarHistorial();

  // Si estaba abierto antes de cambiar de página, reabrir automáticamente
  if (sessionStorage.getItem(STORAGE_ABIERTO) === "true") {
    abrirChat(false);
  }

  // Envío de consulta
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const pregunta = input.value.trim();
    if (!pregunta) return;

    agregarMensaje(pregunta, true);
    input.value = "";
    btnEnviar.disabled = true;
    mostrarCargando();

    try {
      const response = await fetch("/api/asistente/preguntar", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ pregunta }),
      });

      ocultarCargando();

      const data = await response.json().catch(() => null);
      if (response.ok && data?.respuesta) {
        agregarMensaje(data.respuesta, false);
      } else {
        agregarMensaje(
          data?.mensaje || "No pude procesar la respuesta en este momento.",
          false,
        );
      }
    } catch (error) {
      ocultarCargando();
      agregarMensaje(
        "No pude conectar con el asistente. Asegurate de que el backend esté activo.",
        false,
      );
    } finally {
      btnEnviar.disabled = false;
      input.focus();
      lista.scrollTop = lista.scrollHeight;
    }
  });
});
