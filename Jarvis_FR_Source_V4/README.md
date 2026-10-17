# Jarvis FR — Diagnostic IHU007B 0.4.1

Application française reconstruite pour Android 9 : **APK compilée et contrôlée, essais réels sur la tablette encore nécessaires**. Toutes les écritures ECARX restent désactivées. Il s’agit d’une reconstruction partielle, indépendante de l’APK Jarvis russe et de son identité Android.

- APK et journaux : [diagnostic-jarvis-fr-v4](../diagnostic-jarvis-fr-v4/README.md).
- Résultats et matrice des fonctions : [rapport courant](docs/RAPPORT_DIAGNOSTIC.md).
- Essais tablette : [procédure de diagnostic](docs/ESSAIS_TABLETTE.md).
- Reconnaissance : [preuves Vosk](docs/VALIDATION_VOSK.md).
- Analyse des méthodes et lecture ECARX : [diagnostic ECARX](docs/ECARX_DIAGNOSTIC.md).
- Description V4 reçue, conservée : [archive historique](docs/README_V4_ORIGINE.md).

## Compilation reproductible

JDK 17, Android SDK Platform 34 / Build Tools 34.0.0, Python 3.8+, shell POSIX. Définir `ANDROID_HOME` et placer le JDK 17 dans `PATH`. Aucun SDK constructeur ni privilège système requis pour compiler.

```sh
sh tools/build_diagnostic.sh
# Avec les dépendances déjà en cache :
sh tools/build_diagnostic.sh --offline
```

Le script vérifie le modèle, exécute les tests métier et le garde-fou ECARX, construit l’APK, exécute Lint puis contrôle signature, alignement, architectures et modèle embarqué. Sortie : `android/app/build/outputs/apk/debug/app-debug.apk`.

Le wrapper Gradle 8.7 est livré avec l’empreinte SHA-256 officielle de sa distribution. Le build emploie AGP 8.5.2, Vosk Android 0.3.47, du bytecode Java 8, `minSdk=28` et `targetSdk=28`, avec les bibliothèques ARM 32/64 bits. Le contrôle Google Play `ExpiredTargetSdkVersion` est la seule exception Lint : ce diagnostic est destiné à une installation privée sur IHU007B, avec maintien explicite de la cible 28.

Dans l’environnement préparé pour cette session :

```bash
source /workspace/azvoice-tools/activate.sh
cd /workspace/work/geely-ihu-diagnostic-v4/Jarvis_FR_Source_V4
sh tools/build_diagnostic.sh --offline
```

La CI du dépôt parent est `.github/workflows/jarvis-fr-v4.yml`. La variante `.github/workflows/build-source.yml` de ce dossier convient à un dépôt où ce dossier devient la racine. Les workflows ont été préparés ; leur exécution distante n’est pas présumée.

Les tests ECARX et Android sont des simulations. Le chargement natif Linux du modèle ne valide ni la JNI Android, ni le microphone de la voiture. Les limites et les faux positifs constatés sont consignés dans le rapport.
