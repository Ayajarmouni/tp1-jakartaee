package com.tp0.tp_jarmouniaya.llm;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class LlmClientPourGemini {

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent";

    private String key;
    private Client client;

    @PostConstruct
    void init() {
        this.key = System.getenv("GEMINI_KEY");
        this.client = ClientBuilder.newClient();
    }

    @PreDestroy
    void fermer() {
        if (client != null) client.close();
    }

    public String envoyerRequete(String requeteJson) throws RequeteException {
        if (key == null || key.isBlank()) {
            throw new RequeteException("Variable d'environnement GEMINI_KEY absente");
        }
        try (Response r = client.target(URL)
                .queryParam("key", key)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(requeteJson))) {
            String corps = r.readEntity(String.class);
            if (r.getStatus() != 200) {
                throw new RequeteException("Erreur HTTP " + r.getStatus() + " : " + corps);
            }
            return corps;
        }
    }
}