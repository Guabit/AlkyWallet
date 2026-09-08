package com.alkywallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente mínimo para la API local de Ollama (http://localhost:11434 por
 * defecto). No requiere API key: Ollama corre en la máquina donde se
 * levanta el backend (o en otra máquina de la red si se configura
 * ollama.base-url). Ver CHANGELOG para instrucciones de instalación.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OllamaClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.model:llama3.3:8b}")
    private String modelo;

    /**
     * Envía un prompt a Ollama y devuelve el texto generado. Lanza una
     * excepción si Ollama no está corriendo o el modelo no está descargado;
     * quien llama decide cómo degradar (ver AsistenteIAService).
     */
    public String generar(String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelo);
        body.put("prompt", prompt);
        body.put("stream", false);

        Map<?, ?> respuesta = restClientBuilder.build()
                .post()
                .uri(baseUrl + "/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (respuesta == null || respuesta.get("response") == null) {
            throw new IllegalStateException("Ollama no devolvió una respuesta válida");
        }
        return respuesta.get("response").toString();
    }
}
