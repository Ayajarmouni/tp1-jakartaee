package com.tp0.tp_jarmouniaya.llm;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.json.stream.JsonGenerator;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

@Dependent
public class JsonAdapterPourGemini implements Serializable {

    private String systemRole;
    /** Historique de la conversation : JSON de la dernière requête. null au début. */
    private String requeteJson;

    @Inject
    private LlmClientPourGemini llmClient;

    public void setSystemRole(String systemRole) { this.systemRole = systemRole; }

    public LlmInteraction envoyerRequete(String question) throws RequeteException {
        String aEnvoyer = (requeteJson == null)
                ? creerRequeteJson(systemRole, question)
                : ajouterMessage(requeteJson, "user", question);

        String reponseJson = llmClient.envoyerRequete(aEnvoyer);
        String reponse = extractReponse(reponseJson);

        // Succès : on mémorise la question ET la réponse pour la prochaine requête
        this.requeteJson = ajouterMessage(aEnvoyer, "model", reponse);

        return new LlmInteraction(prettyPrinting(aEnvoyer), prettyPrinting(reponseJson), reponse);
    }

    private String creerRequeteJson(String role, String question) {
        JsonObject req = Json.createObjectBuilder()
                .add("system_instruction", Json.createObjectBuilder()
                        .add("parts", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder().add("text", role))))
                .add("contents", Json.createArrayBuilder().add(message("user", question)))
                .build();
        return req.toString();
    }

    private JsonObjectBuilder message(String role, String texte) {
        return Json.createObjectBuilder()
                .add("role", role)
                .add("parts", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("text", texte)));
    }

    private String ajouterMessage(String json, String role, String texte) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject obj = reader.readObject();
            JsonArray contents = Json.createArrayBuilder(obj.getJsonArray("contents"))
                    .add(message(role, texte)).build();
            return Json.createObjectBuilder(obj).add("contents", contents).build().toString();
        }
    }

    private String extractReponse(String json) throws RequeteException {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject().getJsonArray("candidates").getJsonObject(0)
                    .getJsonObject("content").getJsonArray("parts").getJsonObject(0).getString("text");
        } catch (Exception e) {
            throw new RequeteException("Réponse inattendue du LLM : " + json, e);
        }
    }

    private String prettyPrinting(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            StringWriter sw = new StringWriter();
            try (JsonWriter w = Json.createWriterFactory(
                    Map.of(JsonGenerator.PRETTY_PRINTING, true)).createWriter(sw)) {
                w.write(reader.readObject());
            }
            return sw.toString();
        }
    }
}