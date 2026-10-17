# V5 — contrôleur de synthèse française

## Fichiers ajoutés

- `android/app/src/main/java/com/aztech/jarvisfr/FrenchTtsController.java`
- `android/app/src/main/java/com/aztech/jarvisfr/FrenchVoiceSelector.java`
- `tools/TestFrenchTts.java`, `tools/test_tts.sh`, `tools/fake_tts/`

Aucune modification de MainActivity, des ressources, du parser, des actions Android ou d'ECARX par cet agent.

## API d'intégration

```java
new FrenchTtsController(context, diagnosticLogger, statusCallback);
controller.initialize(); // asynchrone, aucun son
controller.speakTest(); // une seule phrase française fixe, réponses toujours désactivées
controller.approveVoice(); // accord explicite, seulement après onDone du test
controller.setSilent(true); // annule la lecture et désactive les réponses
controller.speakAnswer(answer); // false si silencieux, indisponible, fermé ou texte vide
controller.describe();
controller.stop();
controller.close();
```

`speakTest`, `approveVoice` et `setSilent` renvoient une chaîne de statut française. Les méthodes publiques s'appellent sur le thread d'interface ; les callbacks du moteur sont republiés sur ce thread. `isReady()` et `isSilent()` sont également disponibles.

## Politique

- Chaque instance démarre silencieuse. Aucune approbation persistée et aucune parole à l'initialisation.
- Sélection explicite par `getVoices()` + `setVoice()` ; aucun `setLanguage`, téléchargement ou lancement d'installation.
- Seulement des voix annoncées installées, hors ligne, de langue `fr` et de pays `FR` ou générique. Aucune sélection automatique fr-CA/autre région, russe, autre langue, voix réseau, données manquantes ou caractéristiques nulles.
- Préférence stable : fr-FR, puis fr générique ; qualité, latence, nom départagent les voix comparables.
- Vérification de la voix réellement renvoyée par `getVoice()` après sélection et avant chaque synthèse. Un moteur annonçant SUCCESS tout en conservant une voix russe reste silencieux.
- Débit 1.0, hauteur 1.0, AudioAttributes MEDIA/SPEECH. Aucun volume forcé, changement des réglages système ou permission supplémentaire.
- La fin du test n'active jamais les réponses. Il faut ensuite une demande explicite d'activation. Une erreur de synthèse révoque l'approbation et demande un nouvel échantillon.
- Les callbacks d'une lecture arrêtée ou fermée ne valident jamais un échantillon ultérieur.

## Diagnostic et limites

Le journal reçoit moteur par défaut déclaré, voix initiale, toutes les voix exposées, locale, installation/réseau, qualité/latence, caractéristiques, sélection, réglages, code de speak, début/fin/arrêt/erreurs. `getDefaultEngine()` décrit le moteur configuré : l'API Android publique ne garantit pas l'identité d'un éventuel service de remplacement interne. La voix choisie est vérifiée séparément. Aucun accès par réflexion à une API cachée.

La complétude d'un callback onDone et les métadonnées du moteur ne prouvent pas la qualité acoustique sur IHU007B. Le test doit être réellement écouté ; la qualité reste une validation humaine. Aucune assertion sur l'état physique de la voiture dans l'échantillon.

## Vérifications

- Compilation javac des deux classes contre le vrai `android-34/android.jar`, source/target Java 8 : réussie.
- `sh tools/test_tts.sh` : 74 contrôles JVM réussis avec moteur simulé. Ils couvrent refus réseau/langue/région/données absentes, sélection stable, paramètres, silence initial, approbation après test, erreurs, changement de voix, callbacks périmés et fermeture.
- Ces contrôles ne vérifient ni le son réel, ni le moteur présent sur IHU007B. Les appels Android utilisés sont disponibles à l'API 28.
