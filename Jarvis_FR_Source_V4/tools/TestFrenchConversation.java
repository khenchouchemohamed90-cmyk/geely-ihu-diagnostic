import com.aztech.jarvisfr.FrenchConversation;
public final class TestFrenchConversation {
    private static int count;
    private static void check(boolean ok, String detail) {
        count++;
        if (!ok) throw new AssertionError(detail);
    }
    private static void key(String phrase, String expected) {
        FrenchConversation.Response answer = FrenchConversation.reply(phrase);
        check(answer != null && expected.equals(answer.voiceKey), "Dialogue non reconnu: " + phrase);
        check(answer.text != null && !answer.text.isEmpty(), "Réponse vide: " + phrase);
    }
    public static void main(String[] args) {
        key("Bonjour Jarvis", "bonjour");
        key("Qui es-tu?", "identite");
        key("Comment tu t'appelles ?", "identite");
        key("Comment ça va", "etat");
        key("Merci beaucoup", "merci");
        key("Que sais-tu faire", "aide");
        key("Raconte une blague", "blague");
        key("Quelle heure est-il?", "horloge");
        key("Quelle est la date?", "date");
        for (String forbidden : new String[]{
                "ouvre la vitre", "bonjour et ouvre la vitre",
                "allume la climatisation", "quelle est la température extérieure",
                "appelle mon contact", "démarre la voiture", "merci et ferme la vitre"}) {
            check(FrenchConversation.reply(forbidden) == null, "Action capturée par bavardage: " + forbidden);
        }
        System.out.println(count+" tests de dialogue français sûrs PASSES");
    }
}