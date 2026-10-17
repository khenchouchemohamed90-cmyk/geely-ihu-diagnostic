package com.aztech.jarvisfr;

import java.util.Calendar;
import java.util.Locale;

/** Petit dialogue embarqué déterministe en français, sans requêtes Internet. */
public final class FrenchConversation {
    public static final class Response {
        public final String text;
        public final String voiceKey;
        Response(String text, String voiceKey) {
            this.text = text;
            this.voiceKey = voiceKey;
        }
    }

    private FrenchConversation() { }

    /**
     * Une phrase courte complète, jamais un morceau d'une commande automobile.
     * Les requêtes mêlant discussion et action retournent null (traitement sécurisé).
     */
    public static Response reply(String phrase) {
        String s = FrenchIntentParser.normalize(phrase);
        s = s.replaceFirst("^jarvis ", "").trim();
        if (s.matches("(bonjour|salut|bonsoir|coucou|hello)( jarvis)?"))
            return new Response("Bonjour, je suis à votre écoute.", "bonjour");
        if (s.matches("(qui es tu|qui es tu jarvis|comment tu t appelles|quel est ton nom|presente toi|tu es qui)"))
            return new Response("Je suis Jarvis FR, votre assistant embarqué sur Geely Emgrand.", "identite");
        if (s.matches("(comment ca va|comment vas tu|tu vas bien|ca va jarvis|comment allez vous)"))
            return new Response("Je vais bien, merci. Je suis prêt à vous aider.", "etat");
        if (s.matches("(merci|merci jarvis|je te remercie|super merci|merci beaucoup)"))
            return new Response("Avec plaisir.", "merci");
        if (s.matches("(aide|de l aide|que sais tu faire|quelles sont tes fonctions|comment t utiliser|quelles commandes|aide moi|commandes)"))
            return new Response("Je peux ouvrir la navigation, piloter la musique et consulter les capteurs disponibles. Certaines fonctions véhicule restent bloquées jusqu'à leur validation.", "aide");
        if (s.matches("(raconte une blague|dis moi une blague|une blague|fais moi rire)"))
            return new Response("Pourquoi les voitures aiment-elles les histoires ? Parce qu'elles ont toujours un bon moteur !", "blague");
        if (s.matches("(quelle heure est il|donne moi l heure|il est quelle heure|quelle heure|l heure)")) {
            Calendar c = Calendar.getInstance();
            int h = c.get(Calendar.HOUR_OF_DAY), m = c.get(Calendar.MINUTE);
            return new Response(String.format(Locale.FRANCE, "Il est %d h %02d.", h, m), "horloge");
        }
        if (s.matches("(quelle date sommes nous|quel jour sommes nous|la date d aujourd hui|quelle est la date|la date)")) {
            Calendar c = Calendar.getInstance();
            return new Response(String.format(Locale.FRANCE, "Nous sommes le %02d/%02d/%04d.",
                    c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1,
                    c.get(Calendar.YEAR)), "date");
        }
        return null;
    }
}
