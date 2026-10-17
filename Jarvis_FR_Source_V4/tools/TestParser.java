import com.aztech.jarvisfr.FrenchIntentParser;
public class TestParser {
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
  System.out.println("50 tests du parser FR PASSÉS (zéro écriture automobile)");
 }
 private static void expectArg(String input, FrenchIntentParser.Intent expected,int arg){
  FrenchIntentParser.Command got=FrenchIntentParser.parseCommand(input);
  if (got.intent!=expected||got.argument!=arg) throw new AssertionError(input+" : "+got.intent+"/"+got.argument);
 }
 private static void expect(String input, FrenchIntentParser.Intent expected){
  var actual=FrenchIntentParser.parse(input);
  if (actual!=expected) throw new AssertionError(input+" : "+actual+" au lieu de "+expected);
 }
}
