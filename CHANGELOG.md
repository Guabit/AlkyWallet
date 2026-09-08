# AlkyWallet — Etapas 1 y 2

Este paquete contiene **solo los archivos nuevos o modificados**, con la
misma estructura de carpetas que tu proyecto. Para integrarlo: copiá el
contenido de esta carpeta sobre la raíz de tu repo, pisando los archivos
que coincidan. No toca `mvnw`, `.mvn/`, `.gitattributes`, `HELP.md` ni el
histórico de tu repo.

**No pude compilar ni levantar el proyecto acá** (sin red ni Maven en mi
entorno). Revisé cada cambio a mano siguiendo los patrones que ya usa tu
código, pero corré `mvn clean install` y probá todo a mano antes de
darlo por cerrado — sobre todo la parte de Mercado Pago y Ollama, que
dependen de configuración que solo existe en tu máquina.

---

## Etapa 1 (ya entregada antes)

Mostrar/ocultar saldo, cotización + conversor de dólar, QR dinámico para
cobrar, categorización de gastos, inversiones simuladas, página 404,
landing page, Cypress, `.gitignore`. Detalle completo más abajo en
"Backend/Frontend — Etapa 1".

## Etapa 2 (esta entrega)

### 1. Mercado Pago (Sandbox) — funcional, pero requiere que generes tus credenciales

Implementado llamando directo a la REST API de Mercado Pago (Checkout
Pro / API de Preferencias) con `RestClient`, sin agregar el SDK oficial
como dependencia — así no corro el riesgo de darte una coordenada de
Maven que no puedas resolver.

**Flujo:** en Depósitos, el botón "Mercado Pago" crea una preferencia de
pago y te redirige al checkout de Mercado Pago (`sandbox_init_point`).
Cuando el pago se aprueba, Mercado Pago llama a tu webhook
(`/api/mercadopago/webhook`), que consulta el pago real contra su API
(nunca confía en lo que venga en la notificación) y acredita el saldo
usando el mismo `TransaccionService.realizarDepositoPorEmail(...)` que ya
usa el resto de la app. Se guarda cada pago procesado en una tabla nueva
(`pagos_mercado_pago`) para no acreditar dos veces si Mercado Pago
reintenta la notificación.

**Lo que tenés que hacer vos (una sola vez):**
1. Entrá a https://www.mercadopago.com.ar/developers/panel y creá una
   aplicación (elegí "Pagos online" → "Checkout Pro").
2. En la sección "Credenciales de prueba" vas a tener un
   `Access Token` y una `Public Key` que empiezan con `TEST-`.
3. Seteá las variables de entorno antes de levantar el backend:
   ```bash
   export MERCADOPAGO_ACCESS_TOKEN=TEST-xxxxxxxx
   export MERCADOPAGO_PUBLIC_KEY=TEST-xxxxxxxx
   ```
4. **Para probar el webhook en local** (importante): los servidores de
   Mercado Pago no pueden llamar a `localhost`. Necesitás un túnel, por
   ejemplo con [ngrok](https://ngrok.com/):
   ```bash
   ngrok http 8080
   # copiá la URL https que te da, y seteá:
   export MERCADOPAGO_NOTIFICATION_URL=https://tu-url.ngrok-free.app/api/mercadopago/webhook
   ```
   Sin esto, la preferencia se crea y podés pagar en el sandbox, pero el
   saldo no se va a acreditar solo porque tu máquina nunca recibe el
   aviso.
5. Mercado Pago te da tarjetas de prueba para simular pagos aprobados o
   rechazados en la misma página de credenciales de prueba.

### 2. Asistente IA con Ollama — funcional, pero corre en tu máquina

`AsistenteIAService` no deja que el modelo "decida" qué datos consultar
(poco confiable con modelos chicos corriendo en CPU): primero resuelve la
intención con reglas simples (saldo, gasto del mes, gasto por categoría,
cotización del dólar) consultando tus servicios reales, y recién ahí le
pide a Ollama que redacte la respuesta en una oración a partir de ese
dato ya calculado. Si Ollama no está corriendo, la funcionalidad no se
rompe: devuelve el dato en crudo en vez de la respuesta redactada.

**Lo que tenés que hacer vos:**
```bash
# 1. Instalar Ollama (Windows/Mac/Linux, ver https://ollama.com/download)

# 2. Bajar un modelo liviano y descargar. Recomendado para arrancar:
ollama pull llama3.3:8b
# Si tu máquina no tiene GPU o es más limitada, una alternativa más chica:
ollama pull gemma3:2b

# 3. Ollama queda escuchando solo en http://localhost:11434 (no hace falta "ollama run")
```
Si usás un modelo distinto al configurado por defecto, seteá:
```bash
export OLLAMA_MODEL=gemma3:2b
```
Probalo directo antes de probarlo desde AlkyWallet:
```bash
curl http://localhost:11434/api/generate -d '{"model":"llama3.3:8b","prompt":"Decime hola","stream":false}'
```

Nueva pantalla: **Servicios** (link en el menú), con un chat simple para
preguntarle al asistente.

### 3. PedidosYa y Telepase — mocks, funcionan ya mismo sin configurar nada

`PedidosYaMockService` y `TelepaseMockService` no llaman a ninguna API
externa (no tenemos credenciales), pero sí debitan el saldo real dentro
de AlkyWallet y registran la transacción (categorizada como Comida y
Transporte respectivamente), así se puede demostrar el flujo completo.
Cuando consigan acceso a las APIs reales, el reemplazo es acotado:

```
PedidosYaMockService  →  PedidosYaApiService   (mismo controller, mismo DTO de respuesta)
TelepaseMockService   →  TelepaseApiService     (mismo controller, mismo DTO de respuesta)
```

Nueva pantalla: **Servicios**, con un formulario simple para cada uno.

---

## Backend — archivos nuevos (Etapa 2)

- `entity/PagoMercadoPago.java`
- `dto/MercadoPagoPreferenciaRequestDTO.java`, `dto/MercadoPagoPreferenciaResponseDTO.java`,
  `dto/PagoServicioRequestDTO.java`, `dto/PagoPeajeRequestDTO.java`, `dto/PagoServicioResponseDTO.java`,
  `dto/PreguntaAsistenteDTO.java`, `dto/RespuestaAsistenteDTO.java`
- `repository/PagoMercadoPagoRepository.java`
- `service/MercadoPagoService.java`, `service/PedidosYaMockService.java`,
  `service/TelepaseMockService.java`, `service/OllamaClient.java`, `service/AsistenteIAService.java`
- `controller/MercadoPagoController.java`, `controller/PedidosYaController.java`,
  `controller/TelepaseController.java`, `controller/AsistenteIAController.java`

## Backend — archivos modificados (Etapa 2)

- **`security/SecurityConfig.java`**: se agregó **una sola línea** — `permitAll()`
  para `/api/mercadopago/webhook` — porque a ese endpoint lo llaman los
  servidores de Mercado Pago, no un usuario logueado. Nada más se tocó.
- **`repository/TransaccionRepository.java`** y **`service/TransaccionService.java`**:
  se agregó una consulta/método nuevo (`obtenerTotalGastadoEsteMesPorEmail`)
  para que el asistente pueda responder "cuánto gasté este mes". Nada
  existente se modificó, solo se sumó método nuevo.
- **`application.properties`**: se agregaron las secciones de Mercado Pago
  y Ollama al final. Ningún valor existente se cambió. A propósito, el
  `access-token` de Mercado Pago **no tiene default hardcodeado**: si no
  lo configurás, el endpoint responde un 503 claro en vez de fallar en
  silencio o esconder un secreto en el repo.

## Endpoints nuevos (Etapa 2)

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/api/mercadopago/preferencia` | Sí | Crea la preferencia de pago y devuelve la URL de checkout |
| POST / GET | `/api/mercadopago/webhook` | No (pública) | Notificación de Mercado Pago |
| POST | `/api/pedidosya/pagar` | Sí | Pago simulado, debita saldo real |
| POST | `/api/telepase/pagar` | Sí | Pago simulado, debita saldo real |
| POST | `/api/asistente/preguntar` | Sí | Pregunta en lenguaje natural sobre tu billetera |

## Frontend — nuevo (Etapa 2)

- `static/html/servicios.html` + `static/js/servicios.js` (PedidosYa mock,
  Telepase mock, chat del asistente IA)
- Link "Servicios" agregado al menú en las 6 pantallas existentes
- Botón "Mercado Pago" en Depósitos (`deposito.html` / `deposito.js`)

---

## Backend/Frontend — Etapa 1 (referencia)

Ver más abajo el detalle completo entregado antes: `entity/CategoriaTransaccion.java`,
`entity/Inversion.java`, `dto/GastoPorCategoriaDTO.java`, `dto/InversionDTO.java`,
`dto/InvertirRequestDTO.java`, `dto/CotizacionDolarDTO.java`,
`repository/InversionRepository.java`, `service/InversionService.java`,
`service/CotizacionService.java`, `controller/InversionController.java`,
`controller/CotizacionController.java`, `controller/ErrorPageController.java`,
más las modificaciones aditivas en `Transaccion`, `TransaccionDTO`,
`TransferenciaRequestDTO`, `TransaccionRepository`, `TransaccionService`,
`TransaccionController`, y todo el frontend (landing, 404, inversiones.html,
balance-visibility.js, cobro-qr.js, cotizacion.js, y los cambios en
tableroDeControl/tranferencia/perfil/historial/deposito), más la suite
Cypress y el `.gitignore`.

## ⏳ Todavía pendiente

- **Traducción de variables/funciones a inglés (#11):** sigue siendo el
  último paso, ahora que todas las funcionalidades están cerradas. Es un
  cambio que toca prácticamente todos los archivos (back y front a la
  vez, para no desincronizar nombres de campos JSON), así que lo dejo
  para un mensaje dedicado si querés que lo haga.

## Nota de seguridad (no la toqué)

Seguís teniendo `jwt.secret` y `DB_PASSWORD` con defaults hardcodeados en
`application.properties`. Con Mercado Pago sumamos un secreto más
(`MERCADOPAGO_ACCESS_TOKEN`) pero a ese lo dejé **sin** default, así que
no se agrega el mismo problema. Si en algún momento este proyecto sale de
lo académico, valdría la pena sacar también los otros dos defaults.

---

## Cómo aplicarlo a tu rama

Desde la raíz de tu repo, con el zip descomprimido en, por ejemplo,
`~/Descargas/alkywallet-etapa1-2`:

```bash
git checkout tu-rama          # o: git checkout -b tu-rama si todavía no existe
cp -r ~/Descargas/alkywallet-etapa1-2/* .
git status                    # revisá qué se agregó/modificó antes de commitear
git add .
git commit -m "Integra saldo oculto, cotización, QR, categorías, inversiones, \
Mercado Pago sandbox, mocks de PedidosYa/Telepase, asistente IA con Ollama, \
landing, 404, Cypress y .gitignore"
git push origin tu-rama
```

## Cómo correr Cypress

```bash
npm install
npm run cypress:open   # modo interactivo
npm run cypress:run    # modo headless / CI
```
