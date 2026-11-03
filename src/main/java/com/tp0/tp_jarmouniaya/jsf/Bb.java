package com.tp0.tp_jarmouniaya.jsf;

import com.tp0.tp_jarmouniaya.llm.JsonAdapterPourGemini;
import com.tp0.tp_jarmouniaya.llm.LlmInteraction;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Backing bean pour la page JSF index.xhtml.
 * Portée view pour conserver l'état de la conversation qui dure pendant plusieurs requêtes HTTP.
 * La portée view nécessite l'implémentation de Serializable.
 */
@Named
@ViewScoped
public class Bb implements Serializable {

    /**
     * Rôle "système" envoyé au LLM au début de la conversation.
     */
    private String roleSysteme;

    /**
     * Quand le rôle est choisi par l'utilisateur, il n'est plus modifiable
     * (voir la page JSF), sauf pour un nouveau chat.
     */
    private boolean roleSystemeChangeable = true;

    /**
     * Liste de tous les rôles prédéfinis.
     */
    private List<SelectItem> listeRolesSysteme;

    /**
     * Dernière question posée par l'utilisateur.
     */
    private String question;

    /**
     * Dernière réponse du LLM.
     */
    private String reponse;

    /**
     * La conversation depuis le début.
     */
    private StringBuilder conversation = new StringBuilder();

    /**
     * Mode debug : affiche le JSON échangé avec le LLM.
     */
    private boolean debug;

    /**
     * JSON de la dernière requête envoyée au LLM (affiché en mode debug).
     */
    private String texteRequeteJson;

    /**
     * JSON de la dernière réponse du LLM (affiché en mode debug).
     */
    private String texteReponseJson;

    /**
     * Adaptateur JSON : construit les requêtes pour Gemini, lit les réponses
     * et conserve l'historique de la conversation.
     */
    @Inject
    private JsonAdapterPourGemini jsonAdapter;

    /**
     * Contexte JSF. Utilisé pour afficher un message d'erreur dans le formulaire.
     */
    @Inject
    private FacesContext facesContext;

    /**
     * Obligatoire pour un bean CDI.
     */
    public Bb() {
    }

    public String getRoleSysteme() {
        return this.roleSysteme;
    }

    public void setRoleSysteme(String roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public boolean isRoleSystemeChangeable() {
        return this.roleSystemeChangeable;
    }

    public String getQuestion() {
        return this.question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return this.reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getConversation() {
        return conversation.toString();
    }

    public void setConversation(String conversation) {
        this.conversation = new StringBuilder(conversation);
    }

    public boolean isDebug() {
        return this.debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getTexteRequeteJson() {
        return this.texteRequeteJson;
    }

    public void setTexteRequeteJson(String texteRequeteJson) {
        this.texteRequeteJson = texteRequeteJson;
    }

    public String getTexteReponseJson() {
        return this.texteReponseJson;
    }

    public void setTexteReponseJson(String texteReponseJson) {
        this.texteReponseJson = texteReponseJson;
    }

    /**
     * Envoie la question au LLM et affiche sa réponse.
     *
     * @return null pour rester sur la même page.
     */
    public String envoyer() {
        if (this.question == null || this.question.isBlank()) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Texte question vide", "Il manque le texte de la question");
            facesContext.addMessage(null, message);
            return null;
        }

        // Début de la conversation : on fige le rôle et on l'envoie à l'adaptateur
        if (this.conversation.isEmpty()) {
            this.roleSystemeChangeable = false;
            this.jsonAdapter.setSystemRole(this.roleSysteme);
        }

        try {
            LlmInteraction interaction = jsonAdapter.envoyerRequete(question);
            this.reponse = interaction.reponseExtraite();
            this.texteRequeteJson = interaction.questionJson();
            this.texteReponseJson = interaction.reponseJson();
        } catch (Exception e) {
            this.reponse = "Erreur : " + e.getMessage();
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Problème de connexion avec l'API du LLM",
                    "Problème de connexion avec l'API du LLM" + e.getMessage() + "\n"
                            + Arrays.toString(e.getStackTrace()));
            facesContext.addMessage(null, message);
        }

        afficherConversation();
        return null;
    }

    /**
     * Pour un nouveau chat : retourne "index" pour terminer la portée view
     * et obtenir une nouvelle instance du backing bean.
     *
     * @return "index"
     */
    public String nouveauChat() {
        return "index";
    }

    /**
     * Ajoute la dernière question et la dernière réponse à la conversation affichée.
     */
    private void afficherConversation() {
        this.conversation.append("== User:\n").append(question)
                .append("\n== Serveur:\n").append(reponse).append("\n");
    }

    /**
     * Rôles système prédéfinis, affichés dans la liste déroulante.
     */
    public List<SelectItem> getRolesSysteme() {
        if (this.listeRolesSysteme == null) {
            this.listeRolesSysteme = new ArrayList<>();

            // 1er argument : la valeur du rôle, 2ème argument : le libellé du rôle
            String role = """
                    You are a helpful assistant. You help the user to find the information they need.
                    If the user type a question, you answer it.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Assistant"));

            role = """
                    You are an interpreter. You translate from English to French and from French to English.
                    If the user type a French text, you translate it into English.
                    If the user type an English text, you translate it into French.
                    If the text contains only one to three words, give some examples of usage of these words in English.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Traducteur Anglais-Français"));

            role = """
                    Your are a travel guide. If the user type the name of a country or of a town,
                    you tell them what are the main places to visit in the country or the town
                    are you tell them the average price of a meal.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Guide touristique"));

            role = """
                    You are a Moroccan chef named Chef Hassan. Whatever the user asks,
                    you answer correctly but you explain everything with cooking metaphors
                    (ingredients, spices, slow cooking, tajine...).
                    Answer in French, in a warm and humorous tone,
                    and always end your answer with a short Moroccan proverb about food.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Chef cuisinier marocain"));
        }

        return this.listeRolesSysteme;
    }
}