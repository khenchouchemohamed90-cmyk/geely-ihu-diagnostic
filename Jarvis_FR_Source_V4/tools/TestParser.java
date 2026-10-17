import com.aztech.jarvisfr.FrenchIntentParser;
public class TestParser {
 private static int assertions;
 public static void main(String[] args) {
  expect("Allume la climatisation",FrenchIntentParser.Intent.ACTIVER_CLIMATISATION);
  expect("Active la clim",FrenchIntentParser.Intent.ACTIVER_CLIMATISATION);
  expect("Mets en marche le climatiseur",FrenchIntentParser.Intent.ACTIVER_CLIMATISATION);
  expect("Démarre l'air conditionné",FrenchIntentParser.Intent.ACTIVER_CLIMATISATION);
  expect("Éteins le climatiseur",FrenchIntentParser.Intent.ARRETER_CLIMATISATION);
  expect("Désactive la climatisation",FrenchIntentParser.Intent.ARRETER_CLIMATISATION);
  expect("Coupe la clim",FrenchIntentParser.Intent.ARRETER_CLIMATISATION);
  expect("Quelle est mon autonomie ?",FrenchIntentParser.Intent.AUTONOMIE);
  expect("Combien de kilomètres me reste-t-il ?",FrenchIntentParser.Intent.AUTONOMIE);
  expect("Est-ce que la climatisation est allumée ?",FrenchIntentParser.Intent.ETAT_CLIMATISATION);
  expect("Quel est l'état du climatiseur ?",FrenchIntentParser.Intent.ETAT_CLIMATISATION);
  expect("La climatisation est-elle activée ?",FrenchIntentParser.Intent.ETAT_CLIMATISATION);
  expect("Climatisation",FrenchIntentParser.Intent.INCONNUE);
  expect("Climatiseur",FrenchIntentParser.Intent.INCONNUE);
  expect("N'allume pas la climatisation",FrenchIntentParser.Intent.INCONNUE);
  expect("Ne coupe pas la clim",FrenchIntentParser.Intent.INCONNUE);
  expect("Je parle de climatisation",FrenchIntentParser.Intent.INCONNUE);
  expect("Ouvre l'écran de climatisation",FrenchIntentParser.Intent.INCONNUE);
  expect("Affiche les paramètres de climatisation",FrenchIntentParser.Intent.INCONNUE);
  expect("Allume puis coupe la clim",FrenchIntentParser.Intent.INCONNUE);
  expect("Ouvre la vitre conducteur",FrenchIntentParser.Intent.COMMANDE_VEHICULE_NON_VALIDEE);
  expect("Ouvre le toit ouvrant",FrenchIntentParser.Intent.COMMANDE_VEHICULE_NON_VALIDEE);
  expect("Lance la musique", FrenchIntentParser.Intent.MUSIQUE_LECTURE);
  expect("Mets de la musique", FrenchIntentParser.Intent.MUSIQUE_LECTURE);
  expect("Mets la musique en pause", FrenchIntentParser.Intent.MUSIQUE_PAUSE);
  expect("Passe au morceau suivant", FrenchIntentParser.Intent.MUSIQUE_SUIVANTE);
  expect("Piste précédente", FrenchIntentParser.Intent.MUSIQUE_PRECEDENTE);
  expect("Ouvre la navigation", FrenchIntentParser.Intent.OUVRIR_NAVIGATION);
  expect("Lance le GPS", FrenchIntentParser.Intent.OUVRIR_NAVIGATION);
  expect("Ouvre le téléphone", FrenchIntentParser.Intent.OUVRIR_TELEPHONE);
  expect("Ouvre le navigateur web", FrenchIntentParser.Intent.OUVRIR_NAVIGATEUR);
  expect("Quel est le niveau de carburant ?", FrenchIntentParser.Intent.NIVEAU_CARBURANT);
  expect("Quelle est la température extérieure ?", FrenchIntentParser.Intent.TEMPERATURE_EXTERIEURE);
  expect("Allume la climatisation automatique", FrenchIntentParser.Intent.CLIMATISATION_AUTO);
  expectArg("Règle la température à 22 degrés", FrenchIntentParser.Intent.TEMPERATURE_HABITACLE,22);
  expectArg("Mets la ventilation niveau 4", FrenchIntentParser.Intent.VENTILATION_NIVEAU,4);
  expect("Mets la ventilation niveau 12", FrenchIntentParser.Intent.INCONNUE);
  expect("Règle la température à 45 degrés", FrenchIntentParser.Intent.INCONNUE);
  expect("N'ouvre pas la navigation", FrenchIntentParser.Intent.INCONNUE);
  expect("Ne mets pas la musique", FrenchIntentParser.Intent.INCONNUE);
  expect("Ferme le coffre", FrenchIntentParser.Intent.COMMANDE_VEHICULE_NON_VALIDEE);
  expect("Ouvre la vitre passager", FrenchIntentParser.Intent.COMMANDE_VEHICULE_NON_VALIDEE);
  expectArg("Mets la ventilation sur quatre", FrenchIntentParser.Intent.VENTILATION_NIVEAU,4);
  expectArg("Règle la température à vingt deux degrés", FrenchIntentParser.Intent.TEMPERATURE_HABITACLE,22);
  expectArg("Règle la température à vingt et un degrés", FrenchIntentParser.Intent.TEMPERATURE_HABITACLE,21);
  expectArg("Règle la climatisation à vingt quatre degrés", FrenchIntentParser.Intent.TEMPERATURE_HABITACLE,24);
  expect("Allume la clim et passe au morceau suivant",FrenchIntentParser.Intent.INCONNUE);
  expect("Coupe la climatisation et lance la navigation",FrenchIntentParser.Intent.INCONNUE);
  expect("Lance la musique et ouvre le téléphone",FrenchIntentParser.Intent.INCONNUE);
  expect("Mets la climatisation en pause",FrenchIntentParser.Intent.INCONNUE);
  // Une commande par phrase, y compris entre domaines Android et lectures véhicule.
  expect("Ouvre la navigation et le téléphone",FrenchIntentParser.Intent.INCONNUE);
  expect("Ouvre le navigateur web puis le GPS",FrenchIntentParser.Intent.INCONNUE);
  expect("Ouvre les cartes ouvre le composeur",FrenchIntentParser.Intent.INCONNUE);
  expect("Ouvre le navigateur web ouvre les cartes",FrenchIntentParser.Intent.INCONNUE);
  expect("Donne mon autonomie ouvre le GPS",FrenchIntentParser.Intent.INCONNUE);
  expect("Quel est mon niveau de carburant et mon autonomie",FrenchIntentParser.Intent.INCONNUE);
  expect("Règle la température à 22 degrés lance la musique",FrenchIntentParser.Intent.INCONNUE);
  expect("Mets de la musique ou ouvre le téléphone",FrenchIntentParser.Intent.INCONNUE);
  expect("Pause puis reprends la musique",FrenchIntentParser.Intent.INCONNUE);
  expect("Musique suivante précédente",FrenchIntentParser.Intent.INCONNUE);
  expect("Mets la musique suivante en pause",FrenchIntentParser.Intent.INCONNUE);
  expect("Arrête reprends la musique",FrenchIntentParser.Intent.INCONNUE);
  expect("Allume coupe la musique",FrenchIntentParser.Intent.INCONNUE);
  expect("N'allume la musique",FrenchIntentParser.Intent.INCONNUE);
  expect("Quelle est la chanson suivante",FrenchIntentParser.Intent.INCONNUE);
  expect("Pourquoi ouvre le téléphone",FrenchIntentParser.Intent.INCONNUE);
  expect("Je parle de lancer la musique",FrenchIntentParser.Intent.INCONNUE);
  expect("Je pense à ouvrir le téléphone",FrenchIntentParser.Intent.INCONNUE);
  expect("La chanson suivante est belle",FrenchIntentParser.Intent.INCONNUE);
  expect("Pause déjeuner",FrenchIntentParser.Intent.INCONNUE);
  expect("Je parle d'allumer la climatisation automatique",FrenchIntentParser.Intent.INCONNUE);
  expect("Allume coupe la climatisation automatique",FrenchIntentParser.Intent.INCONNUE);
  expect("Climatisation automatique coupe",FrenchIntentParser.Intent.INCONNUE);
  expect("Climatisation automatique",FrenchIntentParser.Intent.CLIMATISATION_AUTO);
  expect("Éteins la climatisation automatique",FrenchIntentParser.Intent.ARRETER_CLIMATISATION);
  expect("Jarvis ouvre la navigation",FrenchIntentParser.Intent.OUVRIR_NAVIGATION);
  expect("Peux-tu ouvrir le téléphone",FrenchIntentParser.Intent.OUVRIR_TELEPHONE);
  expect("S'il te plaît lance la musique",FrenchIntentParser.Intent.MUSIQUE_LECTURE);
  expect("Merci de mettre la musique en pause",FrenchIntentParser.Intent.MUSIQUE_PAUSE);
  expect("Pause",FrenchIntentParser.Intent.MUSIQUE_PAUSE);
  expect("Mets en pause",FrenchIntentParser.Intent.MUSIQUE_PAUSE);
  expect("Reprends la musique",FrenchIntentParser.Intent.MUSIQUE_LECTURE);
  expect("Mets le morceau suivant",FrenchIntentParser.Intent.MUSIQUE_SUIVANTE);
  expect("Ouvre les cartes",FrenchIntentParser.Intent.OUVRIR_NAVIGATION);
  expect("Ouvre le navigateur GPS",FrenchIntentParser.Intent.OUVRIR_NAVIGATION);
  expect("Ouvre le composeur",FrenchIntentParser.Intent.OUVRIR_TELEPHONE);
  expect("Règle la climatisation à 45 degrés",FrenchIntentParser.Intent.INCONNUE);
  expect("Mets la climatisation à zéro degré",FrenchIntentParser.Intent.INCONNUE);
  expect("Règle la température à vingt douze degrés",FrenchIntentParser.Intent.INCONNUE);
  expect("Règle la température à 22 24 degrés",FrenchIntentParser.Intent.INCONNUE);
  expect("Règle la température à 22 45 degrés",FrenchIntentParser.Intent.INCONNUE);
  expect("Règle la température à moins vingt degrés",FrenchIntentParser.Intent.INCONNUE);
  expect("Règle la température à -20 degrés",FrenchIntentParser.Intent.INCONNUE);
  expect(null,FrenchIntentParser.Intent.INCONNUE);
  expect(" ",FrenchIntentParser.Intent.INCONNUE);
  for (int temperature=16;temperature<=30;temperature++)
   expectArg("Règle la température à "+temperature+" degrés",FrenchIntentParser.Intent.TEMPERATURE_HABITACLE,temperature);
  for (int level=1;level<=9;level++)
   expectArg("Mets la ventilation niveau "+level,FrenchIntentParser.Intent.VENTILATION_NIVEAU,level);
  expectArg("Règle la température à vingt-et-un degrés",FrenchIntentParser.Intent.TEMPERATURE_HABITACLE,21);
  expectArg("Mets la climatisation à 22 degrés",FrenchIntentParser.Intent.TEMPERATURE_HABITACLE,22);
  System.out.println(assertions+" tests du parser FR PASSÉS (zéro écriture automobile)");
 }
 private static void expectArg(String input, FrenchIntentParser.Intent expected,int arg){
  assertions++;
  FrenchIntentParser.Command got=FrenchIntentParser.parseCommand(input);
  if (got.intent!=expected||got.argument!=arg) throw new AssertionError(input+" : "+got.intent+"/"+got.argument);
 }
 private static void expect(String input, FrenchIntentParser.Intent expected){
  assertions++;
  var actual=FrenchIntentParser.parse(input);
  if (actual!=expected) throw new AssertionError(input+" : "+actual+" au lieu de "+expected);
 }
}
