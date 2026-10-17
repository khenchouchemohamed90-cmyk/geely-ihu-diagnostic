> Archive historique des vérifications avant compilation réelle. Le bilan courant est dans [RAPPORT_DIAGNOSTIC.md](RAPPORT_DIAGNOSTIC.md).

# Jarvis FR — Vérification technique V4 (17 octobre 2026)

## Résultats effectivement obtenus

- 50 cas de reconnaissance du français : **réussis**.
- 7 cas de suppression des doublons vocaux : **réussis**.
- 6 tests de lectures ECARX avec un service simulé : **réussis**.
- 8 cas de commandes Android standard avec de fausses API : **réussis**.
- Compilation Java de `MainActivity`, `FrenchIntentParser`, `VoiceResultGate` et `EcarxReadOnly` avec des interfaces Android/Vosk simulées : **réussie**.
- Vérification statique de l'absence d'appels directs à `setFunctionValue()`/`setCustomizeFunctionValue()` : **réussie**.

## Corrections V4

- Rejet des commandes composites audio/HVAC/navigation/téléphone : par exemple « Allume la clim et passe au morceau suivant ».
- Rejet de « Mets la climatisation en pause », qui était précédemment mal classé comme une commande musicale.
- Arrêt du microphone lorsqu'on quitte l'écran de l'application (`onPause`).
- Arrêt propre du service vocal en cas d'erreur ou de délai dépassé.

## Ce qui n'a pas été effectué

- **Pas de compilation APK Android réelle** : Gradle et le SDK Android ne sont pas disponibles dans ce conteneur, et son réseau ne permet pas de les télécharger.
- Tentative de créer une branche dans le dépôt GitHub connecté `khenchouchemohamed90-cmyk/geely-ihu-diagnostic` : HTTP **403** « Resource not accessible by integration ». Aucun fichier n'a été modifié dans le dépôt.
- **Aucun test sur la tablette IHU007B**, aucun contrôle de permissions ECARX, aucune validation de la synthèse vocale française et aucun essai automobile.

## Suite de travail (critères avant toute APK)

1. Compiler sous JDK 17 + Android SDK 34 avec Gradle 8.7 (`cd android && gradle :app:assembleDebug`).
2. Examiner le journal de compilation et résoudre les éventuelles incompatibilités Vosk/Gradle.
3. Installer **uniquement une APK de diagnostic en lecture seule**, avec une identité distincte du Jarvis russe fonctionnel.
4. Confirmer la reconnaissance Vosk française, le TTS, et la connexion ECARX (autonomie/A/C en lecture seule).
5. Puis étudier, fonction par fonction, les capacités réelles. Aucun mouvement de vitre/toit ni action de conduite n'est autorisé par ce prototype.

Les versions source V3 et V4 ne constituent pas le code propriétaire original de Jarvis russe.
