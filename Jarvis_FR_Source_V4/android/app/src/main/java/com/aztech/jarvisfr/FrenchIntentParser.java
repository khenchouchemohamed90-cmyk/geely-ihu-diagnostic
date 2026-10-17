package com.aztech.jarvisfr;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpréteur francophone sans effet de bord : détecte des intentions, jamais
 * d'écriture automobile. La politique d'exécution est dans MainActivity.
 */
public final class FrenchIntentParser {
    public enum Intent {
        AUTONOMIE, ETAT_CLIMATISATION, ACTIVER_CLIMATISATION,
        ARRETER_CLIMATISATION, TEMPERATURE_HABITACLE, VENTILATION_NIVEAU,
        CLIMATISATION_AUTO, NIVEAU_CARBURANT, TEMPERATURE_EXTERIEURE,
        MUSIQUE_LECTURE, MUSIQUE_PAUSE, MUSIQUE_SUIVANTE, MUSIQUE_PRECEDENTE,
        OUVRIR_NAVIGATION, OUVRIR_TELEPHONE, OUVRIR_NAVIGATEUR,
        COMMANDE_VEHICULE_NON_VALIDEE, INCONNUE
    }

    /** argument=-1 si aucune valeur numérique n'est associée à la phrase. */
    public static final class Command {
        public final Intent intent;
        public final int argument;
        private Command(Intent intent, int argument) { this.intent = intent; this.argument = argument; }
    }

    private static final Pattern HVAC = Pattern.compile("\\b(?:climatisation|climatiseur|clim|air conditionne|a c)\\b");
    private static final Pattern QUESTION = Pattern.compile("^(?:est ce que|est ce|quel est|quelle est|comment est|la clim est|le climatiseur est|la climatisation est)\\b");
    private static final Pattern STOP = Pattern.compile("\\b(?:eteins|eteindre|arrete|arreter|coupe|couper|desactive|desactiver|stoppe|stopper)\\b");
    private static final Pattern START = Pattern.compile("\\b(?:allume|allumer|active|activer|demarre|demarrer|enclenche|enclencher|lance|lancer|mets|mettez|mettre)\\b");
    private static final Pattern NEGATION = Pattern.compile("\\b(?:ne|pas|jamais|aucun|sans)\\b");
    private static final Pattern SHOW_ONLY = Pattern.compile("\\b(?:ouvre|ouvrir|affiche|afficher|montre|montrer|ecran|menu|page|parametres|reglages)\\b");
    private static final Pattern FAN = Pattern.compile("\\b(?:ventilation|ventilateur|soufflerie|soufflage)\\b");
    private static final Pattern TEMPERATURE = Pattern.compile("\\b(?:temperature|degres?|chauffage)\\b");
    private static final Pattern EXPLICIT_LEVEL = Pattern.compile("\\b(?:niveau|vitesse|sur)\\s+(\\d+|un|deux|trois|quatre|cinq|six|sept|huit|neuf)\\b");
    private static final Pattern EXPLICIT_TEMP = Pattern.compile("\\b(1[6-9]|2[0-9]|30|dix six|seize|dix sept|dix huit|dix neuf|vingt(?: et un| un| deux| trois| quatre| cinq| six| sept| huit| neuf)?|trente)\\s*(?:degres?)?\\b");
    private static final Pattern WINDOW_ROOF_TRUNK = Pattern.compile("\\b(?:vitre|vitres|fenetre|fenetres|toit ouvrant|pare soleil|store du toit|coffre|retroviseurs?|phares?|essui?e?s? glaces?)\\b");
    private static final Pattern MUSIC = Pattern.compile("\\b(?:musique|chanson|morceau|piste|lecture|lecteur)\\b");
    private static final Pattern NEXT = Pattern.compile("\\b(?:suivant|suivante|prochain|prochaine)\\b");
    private static final Pattern PREVIOUS = Pattern.compile("\\b(?:precedent|precedente|retour)\\b");
    private static final Pattern OPEN = Pattern.compile("\\b(?:ouvre|ouvrir|lance|lancer|demarre|demarrer|affiche|afficher)\\b");

    private FrenchIntentParser() { }

    public static String normalize(String raw) {
        if (raw == null) return "";
        return Normalizer.normalize(raw.toLowerCase(Locale.FRENCH), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('’', ' ').replace('\'', ' ')
                .replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    public static Intent parse(String raw) { return parseCommand(raw).intent; }

    public static Command parseCommand(String raw) {
        String s = normalize(raw);
        if (s.isEmpty()) return of(Intent.INCONNUE);
        // Une négation empêche une action ambiguë, y compris sur des fonctions non automobiles.
        if (NEGATION.matcher(s).find()) return of(Intent.INCONNUE);
        // Fonctions à mouvement physique : reconnues mais jamais routées vers une écriture.
        if (WINDOW_ROOF_TRUNK.matcher(s).find()) return of(Intent.COMMANDE_VEHICULE_NON_VALIDEE);
        // Ne pas exécuter silencieusement UNE action si l'utilisateur en demande DEUX.
        // En particulier, une phrase mixte véhicule/média doit être clarifiée.
        boolean mentionsClimate = HVAC.matcher(s).find() || FAN.matcher(s).find();
        boolean mentionsMusic = MUSIC.matcher(s).find() || s.contains("piste ") || s.contains("morceau ");
        boolean mentionsNavigation = s.contains("navigation") || s.matches(".*\\bgps\\b.*");
        boolean mentionsTelephone = s.contains("telephone") || s.contains("composeur");
        if ((mentionsClimate && mentionsMusic)
                || (mentionsClimate && (mentionsNavigation || mentionsTelephone))
                || (mentionsMusic && (mentionsNavigation || mentionsTelephone))
                || (mentionsClimate && s.contains("pause"))) {
            return of(Intent.INCONNUE);
        }
        if (s.contains("autonomie") || s.contains("kilometres restants")
                || s.contains("combien de kilometres") || s.contains("rayon d action")) return of(Intent.AUTONOMIE);
        if (s.contains("niveau de carburant") || s.contains("combien de carburant") || s.contains("combien d essence"))
            return of(Intent.NIVEAU_CARBURANT);
        if (TEMPERATURE.matcher(s).find() && (s.contains("exterieur") || s.contains("dehors")))
            return of(Intent.TEMPERATURE_EXTERIEURE);

        if (MUSIC.matcher(s).find() || s.contains("pause") || s.contains("reprends la lecture")) {
            if (NEXT.matcher(s).find() && !PREVIOUS.matcher(s).find()) return of(Intent.MUSIQUE_SUIVANTE);
            if (PREVIOUS.matcher(s).find() && !NEXT.matcher(s).find()) return of(Intent.MUSIQUE_PRECEDENTE);
            if (s.contains("pause") || STOP.matcher(s).find()) return of(Intent.MUSIQUE_PAUSE);
            if (s.contains("reprends") || s.contains("reprise") || s.contains("joue")
                    || START.matcher(s).find()) return of(Intent.MUSIQUE_LECTURE);
        }
        if (OPEN.matcher(s).find()) {
            if (s.contains("navigation") || s.contains("navigateur gps") || s.matches(".*\\bgps\\b.*")
                    || s.contains("cartes")) return of(Intent.OUVRIR_NAVIGATION);
            if (s.contains("telephone") || s.contains("composeur")) return of(Intent.OUVRIR_TELEPHONE);
            if (s.contains("navigateur internet") || s.contains("navigateur web")
                    || s.contains("browser")) return of(Intent.OUVRIR_NAVIGATEUR);
        }
        if (FAN.matcher(s).find() && (s.contains("regle") || s.contains("mets") || s.contains("vitesse") || s.contains("niveau"))) {
            Matcher m = EXPLICIT_LEVEL.matcher(s);
            if (m.find()) {
                int n = parseFrenchNumber(m.group(1));
                if (n >= 1 && n <= 9) return withArg(Intent.VENTILATION_NIVEAU, n);
            }
        }
        if (TEMPERATURE.matcher(s).find() && (s.contains("regle") || s.contains("mets") || s.contains("fixe") || s.contains("definis"))) {
            Matcher m = EXPLICIT_TEMP.matcher(s);
            if (m.find()) return withArg(Intent.TEMPERATURE_HABITACLE, parseFrenchNumber(m.group(1)));
        }
        if (HVAC.matcher(s).find()) {
            if (QUESTION.matcher(s).find() || s.startsWith("donne") && s.contains("etat")
                    || s.contains("est elle activee")) return of(Intent.ETAT_CLIMATISATION);
            if (SHOW_ONLY.matcher(s).find()) return of(Intent.INCONNUE);
            if (s.contains("automatique") && (START.matcher(s).find() || s.startsWith("climatisation automatique")))
                return of(Intent.CLIMATISATION_AUTO);
            boolean stop = STOP.matcher(s).find();
            boolean start = START.matcher(s).find();
            if (stop && start) return of(Intent.INCONNUE);
            if (stop) return of(Intent.ARRETER_CLIMATISATION);
            if (start) return of(Intent.ACTIVER_CLIMATISATION);
        }
        return of(Intent.INCONNUE);
    }

    private static int parseFrenchNumber(String raw) {
        try { return Integer.parseInt(raw); } catch (NumberFormatException ignored) { }
        String[] ones={"", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf"};
        for (int i=1;i<=9;i++) if (raw.equals(ones[i])) return i;
        if (raw.equals("seize") || raw.equals("dix six")) return 16;
        if (raw.equals("dix sept")) return 17;
        if (raw.equals("dix huit")) return 18;
        if (raw.equals("dix neuf")) return 19;
        if (raw.equals("vingt")) return 20;
        if (raw.equals("trente")) return 30;
        if (raw.equals("vingt et un")) return 21;
        for (int i=1;i<=9;i++) if (raw.equals("vingt "+ones[i])) return 20+i;
        return -1;
    }

    private static Command of(Intent i) { return new Command(i, -1); }
    private static Command withArg(Intent i, int value) { return new Command(i, value); }
}
