# Capture audio V5 — notes d'intégration et validation

## Point de départ vérifié

Le `SpeechService` de Vosk Android 0.3.47 construit un `AudioRecord` avec la source numérique 6 (`VOICE_RECOGNITION`), à la fréquence fournie (16 kHz dans l'activité V4). Son délai dépend du nombre d'échantillons lus. Son arrêt utilise `Thread.join()` sans délai et son `shutdown()` libère seulement `AudioRecord`, sans fermer le `Recognizer`. Le chemin timeout ne livre pas le dernier `getFinalResult()` avant `onTimeout`.

Ces observations expliquent des lacunes du diagnostic V4 ; elles ne prouvent pas la cause exacte constatée sur l'IHU007B.

## API nouvelle

Fichiers ajoutés dans `android/app/src/main/java/com/aztech/jarvisfr/` :

- `DiagnosticSpeechService.java`
- `Pcm16Resampler.java`
- `PcmAudioMetrics.java`

```java
Recognizer recognizer = new Recognizer(model, 16000.0f /*, grammaire optionnelle */);
DiagnosticSpeechService service = new DiagnosticSpeechService(recognizer,
        message -> { /* diagnostic texte, callback thread principal */ },
        DiagnosticSpeechService.InputPreset.AUTO);
// Le service possède désormais recognizer : ne plus appeler recognizer.close() ailleurs.
service.whenStopped(() -> { /* fermeture terminée, callback thread principal */ });
service.startListening(recognitionListener, 15000);
service.cancel(); // non bloquant ; shutdown() est un alias
```

Une instance correspond à une seule écoute. `whenStopped()` fonctionne aussi après un arrêt déjà terminé et après une annulation avant démarrage. Il est livré même si la reconnaissance a été annulée. Toutes les notifications `RecognitionListener` et les diagnostics UI sont livrés sur le thread principal Android et ignorés après annulation.

Le client doit vérifier `RECORD_AUDIO` avant de démarrer. Le service traite également un `SecurityException` tardif en erreur explicite ; il n'essaie pas de contourner les permissions. Une suppression lint `MissingPermission` est limitée à cette méthode d'ouverture, avec ce contrat documenté.

À la destruction de l'activité, attendre via callbacks la fin de **tous** les services créés avant de fermer le `Model`. Une écoute précédemment annulée peut encore terminer son dernier appel natif. Aucun `join`, `AudioRecord.stop/release` ou `Recognizer.close` ne doit être ajouté sur le thread UI par le client.

## Comportement

Les fréquences candidates sont 16 000, 48 000 et 44 100 Hz. Le constructeur à deux arguments conserve le mode `AUTO`. Le constructeur à trois arguments accepte `InputPreset.AUTO`, `MIC`, `VOICE_RECOGNITION` ou `DEFAULT`. En mode `AUTO`, chaque fréquence essaie `MIC`, puis `VOICE_RECOGNITION`, puis `DEFAULT`. Un choix explicite fixe une seule source et conserve uniquement les replis de fréquence ; aucune autre source n'est utilisée silencieusement après ce choix. Le service passe à l'essai suivant exclusivement après un refus réel : fréquence/tampon refusé, recorder non initialisé, exception au démarrage ou état d'enregistrement non actif. Il conserve la première capture démarrée ; il ne change pas silencieusement de microphone sur un simple signal faible.

Les échantillons mono PCM16 sont convertis vers les 16 kHz attendus par le Recognizer du client. La conversion conserve une phase entière entre blocs et effectue une moyenne pondérée des intervalles ; elle ne dépend pas d'une éventuelle conversion automatique du moteur Vosk. C'est un filtre simple de diagnostic vocal, pas un rééchantillonneur musical de haute qualité.

La lecture `READ_NON_BLOCKING` permet de vérifier un délai mural même sans arrivée de données. Les notifications partielles sont limitées à cinq par seconde et ne sont jamais promues en commandes. Au terme du délai, le service demande `getFinalResult()`, libère les ressources sur son worker, puis livre `onFinalResult` avant `onTimeout`. Une annulation supprime ce flush et les callbacks en attente.

Les erreurs de lecture négatives, dont `ERROR_DEAD_OBJECT (-6)`, sont visibles avec source et fréquence. Elles arrêtent l'écoute sans fabriquer de résultat final. Une nouvelle activation manuelle est nécessaire. Les appels natifs propriétaires qui resteraient bloqués ne sont pas interrompus de force ; leur nettoyage attend leur retour sur le worker et ne bloque pas l'interface.

## Diagnostic et confidentialité

Tag logcat : `JarvisAudio`. Après le démarrage puis les premiers échantillons, le service consulte `AudioRecord.getRoutedDevice()` (id, type, produit) et `getActiveMicrophones()` Android 9 (id, type, emplacement, description). Les absences de métadonnées, `SecurityException`, erreurs OEM ou `IOException` sont signalées et n'arrêtent pas la capture. Ce sont exclusivement des lectures : aucun mode AudioManager, Bluetooth SCO, routage ou réglage système n'est modifié.

Les autres messages indiquent les essais refusés, la source/fréquence effective, le tampon en octets, le nombre de blocs, les échantillons capturés et transmis, la crête, le RMS en dBFS, les zéros et les saturations. `AUCUN ÉCHANTILLON` distingue une absence de livraison du signal `SIGNAL NUL` constitué de vrais échantillons à zéro.

Les mesures sont cumulatives pendant l'écoute et publiées une fois par seconde puis à la fermeture. Aucun fichier audio n'est enregistré. Le service ne journalise pas lui-même les transcriptions.

## Vérifications réalisées

Commande : `sh tools/test_audio.sh`, après activation du JDK.

- **24 vérifications PCM/rééchantillonnage** : identité à 16 kHz, moyennes à 48 kHz, durée exacte 44,1→16 kHz, invariance aux découpages, conservation de l'amplitude constante, bornes, crête signée, RMS, silence et saturation.
- **117 vérifications de capture et de cycle de vie simulés** : priorité MIC en mode AUTO, respect des trois sources explicites y compris sur signal nul, absence de changement automatique sur seul silence, repli sur erreurs de source/fréquence, libération de chaque essai, délai mural sans échantillons, signal nul, final avant timeout, échec des neuf routes, `ERROR_DEAD_OBJECT`, annulation avant et après démarrage, callback de fin tardif, métadonnées de route au démarrage/premiers échantillons, refus de métadonnées non bloquant, et annulation non bloquante pendant une inférence native simulée non interruptible. Aucun accès natif après fermeture n'est toléré par le faux Recognizer.
- Compilation `javac` réelle des trois nouvelles classes contre `android-34/android.jar`, le vrai JAR Vosk 0.3.47 et le JAR JNA 5.13 : **réussie**.
- Tentative Gradle depuis le sous-agent bloquée avant les tâches par le sandbox réseau : `Could not determine a usable wildcard IP for this machine.` La compilation APK/lint finale relève de l'invocation Gradle fonctionnelle du parent.

Les doubles de `tools/fake_audio/` servent uniquement aux scénarios JVM et ne sont pas inclus dans l'application Android. Ils ne simulent pas le Looper Android réel et ne valident pas le matériel IHU007B.

## Vérifications nécessaires sur tablette

Vérifier la source choisie et les niveaux réels, la transcription partielle puis finale, le flush après silence, l'arrêt au changement d'activité et plusieurs activations successives. Les replis ne garantissent pas que le micro OEM soit routé vers une application tierce ; le niveau et les transcriptions permettront d'en décider à partir de preuves.

## Précision après retour tablette

Le retour V4 ne montre aucun texte ni transcription : ce constat seul ne permet pas de distinguer absence de permission, absence d'échantillons, route muette, microphone occupé par un autre service OEM, signal trop faible ou résultat Vosk non affiché. Un démarrage réussi suivi de zéros peut correspondre à une route OEM muette ou à une contention du microphone. Le repli sur erreurs d'initialisation ne résout pas ce cas à lui seul, puisqu'Android annonce alors un enregistreur valide. Les présélections explicites permettent le prochain test comparatif MIC / VOICE_RECOGNITION / DEFAULT, sans commutation silencieuse fondée sur le niveau ni modification du firmware. Aucune route précise n'est annoncée corrigée avant ces mesures réelles.
