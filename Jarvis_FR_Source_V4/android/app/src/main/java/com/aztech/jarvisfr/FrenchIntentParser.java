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
    private static final Pattern NEGATION = Pattern.compile("\\b(?:ne|n|pas|jamais|aucun|sans)\\b");
    private static final Pattern COMPOUND = Pattern.compile("\\b(?:et|ou|puis|ensuite)\\b");
    private static final Pattern INFORMATION_QUESTION = Pattern.compile("^(?:quel|quelle|quels|quelles|comment|pourquoi|quand|combien|est ce)\\b");
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
    private static final Pattern NAVIGATION = Pattern.compile("\\b(?:navigation|gps|cartes)\\b");
    private static final Pattern TELEPHONE = Pattern.compile("\\b(?:telephone|composeur)\\b");
    private static final Pattern BROWSER = Pattern.compile("\\b(?:navigateur internet|navigateur web|browser)\\b");
    private static final Pattern SETTING = Pattern.compile("\\b(?:regle|regler|mets|mettre|fixe|fixer|definis|definir)\\b");
    private static final Pattern NUMBER_OR_DEGREES = Pattern.compile("\\b(?:\\d+|degres?|zero|un|deux|trois|quatre|cinq|six|sept|huit|neuf|dix|seize|vingt|trente|quarante)\\b");
    private static final Pattern MEDIA_REQUEST = Pattern.compile("^(?:reprends|reprendre|joue|jouer|passe|passer)\\b");
    private static final Pattern TERSE_MEDIA = Pattern.compile("^(?:(?:(?:la|le) )?(?:musique|chanson|morceau|piste|lecture) (?:suivant|suivante|precedent|precedente)|pause|reprise(?: de la lecture)?)$");
    private static final Pattern NUMBER_CONTINUATION = Pattern.compile("^\\s*(?:\\d+|et|zero|un|deux|trois|quatre|cinq|six|sept|huit|neuf|dix|onze|douze|treize|quatorze|quinze|seize|vingt|trente|quarante|cinquante|soixante|cents?)\\b");
    private static final Pattern NEGATIVE_NUMBER = Pattern.compile("(?:^|\\s)-\\s*\\d");

    private FrenchIntentParser() { }

    public static String normalize(String raw) {
        if (raw == null) return "";
        return Normalizer.normalize(raw.toLowerCase(Locale.FRENCH), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('’', ' ').replace('\'', ' ')
                .replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    public static Intent parse(String raw) { return parseCommand(raw).intent; }

    public static Command parseCommand(String raw) {
        String s = normalize(raw).replaceFirst("^jarvis\\s+", "")
                .replaceFirst("^(?:s il te plait|merci de|peux tu|pourrais tu|veuillez)\\s+", "");
        if (s.isEmpty()) return of(Intent.INCONNUE);
        // Une négation empêche une action ambiguë, y compris sur des fonctions non automobiles.
        if (NEGATION.matcher(s).find()) return of(Intent.INCONNUE);
        // Une session traite un seul ordre. « vingt et un » reste un nombre unique.
        if (COMPOUND.matcher(s.replaceAll("\\bvingt et un\\b", "vingt un")).find())
            return of(Intent.INCONNUE);
        // Fonctions à mouvement physique : reconnues mais jamais routées vers une écriture.
        if (WINDOW_ROOF_TRUNK.matcher(s).find()) return of(Intent.COMMANDE_VEHICULE_NON_VALIDEE);
        // Ne pas exécuter silencieusement UNE action si l'utilisateur en demande DEUX.
        // En particulier, une phrase mixte véhicule/média doit être clarifiée.
        boolean mentionsOutsideTemperature = TEMPERATURE.matcher(s).find()
                && (s.contains("exterieur") || s.contains("dehors"));
        boolean mentionsClimate = HVAC.matcher(s).find() || FAN.matcher(s).find()
                || TEMPERATURE.matcher(s).find() && !mentionsOutsideTemperature;
        boolean mentionsMusic = MUSIC.matcher(s).find() || s.contains("piste ") || s.contains("morceau ");
        boolean mentionsNavigation = NAVIGATION.matcher(s).find();
        boolean mentionsTelephone = TELEPHONE.matcher(s).find();
        boolean mentionsBrowser = BROWSER.matcher(s).find();
        boolean mentionsRange = s.contains("autonomie") || s.contains("kilometres restants")
                || s.contains("combien de kilometres") || s.contains("rayon d action");
        boolean mentionsFuel = s.contains("niveau de carburant") || s.contains("combien de carburant")
                || s.contains("combien d essence");
        int domains = (mentionsClimate ? 1 : 0) + (mentionsMusic || s.contains("pause") ? 1 : 0)
                + (mentionsNavigation ? 1 : 0) + (mentionsTelephone ? 1 : 0)
                + (mentionsBrowser ? 1 : 0) + (mentionsRange ? 1 : 0)
                + (mentionsFuel ? 1 : 0) + (mentionsOutsideTemperature ? 1 : 0);
        if (domains > 1) {
            return of(Intent.INCONNUE);
        }
        if (mentionsRange) return of(Intent.AUTONOMIE);
        if (mentionsFuel) return of(Intent.NIVEAU_CARBURANT);
        if (mentionsOutsideTemperature) return of(Intent.TEMPERATURE_EXTERIEURE);

        if (MUSIC.matcher(s).find() || s.contains("pause") || s.contains("reprends la lecture")) {
            if (INFORMATION_QUESTION.matcher(s).find()
                    || !isActionRequest(s) && !TERSE_MEDIA.matcher(s).matches()) return of(Intent.INCONNUE);
            boolean next = NEXT.matcher(s).find();
            boolean previous = PREVIOUS.matcher(s).find();
            if (next && previous) return of(Intent.INCONNUE);
            boolean resume = s.contains("reprends") || s.contains("reprise") || s.contains("joue");
            boolean pause = s.contains("pause") || STOP.matcher(s).find();
            if (pause && (resume || START.matcher(s).find() && !s.matches(".*\\b(?:mets|mettez|mettre)\\b.*\\bpause\\b.*")))
                return of(Intent.INCONNUE);
            if ((next || previous) && (pause || resume))
                return of(Intent.INCONNUE);
            if (next) return of(Intent.MUSIQUE_SUIVANTE);
            if (previous) return of(Intent.MUSIQUE_PRECEDENTE);
            if (s.contains("pause") || STOP.matcher(s).find()) return of(Intent.MUSIQUE_PAUSE);
            if (resume || START.matcher(s).find()) return of(Intent.MUSIQUE_LECTURE);
        }
        if (OPEN.matcher(s).find() && isActionRequest(s) && !INFORMATION_QUESTION.matcher(s).find()) {
            if (mentionsNavigation) return of(Intent.OUVRIR_NAVIGATION);
            if (mentionsTelephone) return of(Intent.OUVRIR_TELEPHONE);
            if (mentionsBrowser) return of(Intent.OUVRIR_NAVIGATEUR);
        }
        if (FAN.matcher(s).find() && (SETTING.matcher(s).lookingAt() || s.startsWith("ventilation niveau") || s.startsWith("ventilation vitesse"))) {
            Matcher m = EXPLICIT_LEVEL.matcher(s);
            if (m.find()) {
                int n = parseFrenchNumber(m.group(1));
                if (n >= 1 && n <= 9) return withArg(Intent.VENTILATION_NIVEAU, n);
            }
        }
        if ((TEMPERATURE.matcher(s).find() || HVAC.matcher(s).find() && NUMBER_OR_DEGREES.matcher(s).find())
                && SETTING.matcher(s).lookingAt()) {
            if (s.contains("moins") || raw != null && NEGATIVE_NUMBER.matcher(raw).find())
                return of(Intent.INCONNUE);
            Matcher m = EXPLICIT_TEMP.matcher(s);
            if (m.find()) {
                int value = parseFrenchNumber(m.group(1));
                boolean invalidContinuation = NUMBER_CONTINUATION.matcher(s.substring(m.end())).find();
                if (!invalidContinuation && !m.find()) return withArg(Intent.TEMPERATURE_HABITACLE, value);
            }
            // Une consigne invalide ne doit pas devenir par défaut « allumer la clim ».
            return of(Intent.INCONNUE);
        }
        if (HVAC.matcher(s).find()) {
            if (QUESTION.matcher(s).find() || s.startsWith("donne") && s.contains("etat")
                    || s.contains("est elle activee")) return of(Intent.ETAT_CLIMATISATION);
            if (SHOW_ONLY.matcher(s).find()) return of(Intent.INCONNUE);
            boolean stop = STOP.matcher(s).find();
            boolean start = START.matcher(s).find();
            if (stop && start) return of(Intent.INCONNUE);
            boolean terseAuto = s.equals("climatisation automatique");
            if (!isActionRequest(s) && !terseAuto) return of(Intent.INCONNUE);
            if (stop) return of(Intent.ARRETER_CLIMATISATION);
            if (s.contains("automatique") && (start || terseAuto)) return of(Intent.CLIMATISATION_AUTO);
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

    private static boolean isActionRequest(String phrase) {
        return START.matcher(phrase).lookingAt() || STOP.matcher(phrase).lookingAt()
                || OPEN.matcher(phrase).lookingAt() || MEDIA_REQUEST.matcher(phrase).lookingAt();
    }
}
