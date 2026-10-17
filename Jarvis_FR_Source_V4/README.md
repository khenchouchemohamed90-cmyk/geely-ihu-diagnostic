# Jarvis 2.8 FR — Reconstruction du projet source (travail de développement)

**IMPORTANT : il ne s'agit PAS du code source original de Jarvis, ni d'une version finale de son APK.**

Ce dossier est la base **recréée en Java** après inspection statique du Jarvis 2.8 Lite fourni par l'utilisateur, de ses ressources Vosk, de l'API ECARX IHU007B et de projets open source comparables. L'APK originale reste intacte.

## Progrès V4 — vérifications supplémentaires (17/10/2026)

- **71 vérifications locales réussies** : 50 cas de phrases françaises, 7 anti-doublons, 6 ECARX simulés et 8 actions Android simulées.
- Java compilé contre des interfaces Android/Vosk **simulées** : cela vérifie des erreurs de source mais ne produit pas d'APK.
- Rejet des ordres composites qui mélangent climatisation et multimédia/navigation/téléphone ; correction de « Mets la climatisation en pause ».
- Arrêt du microphone quand l'application quitte l'avant-plan, et arrêt propre sur erreur vocale.
- Contrôle de l'absence d'écritures ECARX ; les commandes physiques sont toujours **désactivées**.
- Voir `docs/CONTROLES_COMPILATION_V4.md` pour les limites exactes et le blocage de compilation (SDK absent, dépôt GitHub écriture 403).

## Résultats déjà récupérés du DEX d'origine

- 13 989 chaînes indexées dans le DEX ; 2 415 chaînes contenant du cyrillique.
- 167 définitions de classes `org.jarvis` et 1 010 références de méthodes `org.jarvis` dans le DEX.
- `org.jarvis.car.EcarxApiHelper` : méthodes publiques/privées inspectables en inventaire ; notamment `setAcOn(boolean)`, `setClimateSystemOn(boolean)`, `isAcOn()`, `setWindow(int,int)`.
- Les méthodes `setAcOn(boolean)` et `isAcOn()` référencent `0x10010300`; `setClimateSystemOn(boolean)` référence `0x10010100`. Les méthodes de réglage recourent à `setFunctionValue()` (invocations identifiées dans les blocs DEX). L'association de cet identifiant à l'activation observée reste à vérifier : le littéral seul ne prouve pas quelle méthode est invoquée par la phrase vocale.
- L'appel de vitre `BCM_FUNC_WINDOW_POS(16,100)` est apparu dans les logs mais n'a pas provoqué de mouvement physique.

Les tables `analysis/methodes_jarvis.json`, `analysis/classes_jarvis.json` et `analysis/chaines_cyrilliques.tsv` ont été produites en lisant le DEX de l'APK russe, pas en les inventant.

## Progrès V3 : périmètre complet, commandes Android standard et matrice de preuve

- La V2 ne couvrait que l'autonomie et trois commandes climatisation ; la V3 étend la reconnaissance FR aux **médias, navigation, composeur téléphonique, navigateur, température, ventilation, auto HVAC, carburant et température extérieure**.
- Modules **Android standards réellement codés** : lecture/pause/piste suivante/précédente par touches multimédia ; ouverture d'Amap Auto installé, du composeur Android et d'un navigateur. Le résultat sur IHU007B n'est **pas encore testé**.
- **Aucune écriture ECARX** : la commande « Allume la climatisation » reste reconnue mais désactivée dans notre nouveau projet ; les fenêtres, toit, coffre, sièges, rétroviseurs et lumières ne sont pas actionnés.
- Correction du démarrage d'une session Vosk : l'indicateur d'écoute était auparavant toujours `false`, empêchant le traitement du résultat ; réinitialisation du filtre anti-doublons à chaque session.
- 67 vérifications locales réussies : 46 phrases FR, 7 anti-doublons, 6 lectures ECARX simulées, 8 commandes Android simulées. Aucun test sur la voiture.
- **Lire `docs/MATRICE_FONCTIONS_IHU007B_V3.md` pour les niveaux de preuve** : fonction originale confirmée, fonction présente dans le DEX, reconnaissance FR, action Android programmée, effet réel non vérifié.

## Progrès V2 : durcissement du prototype Android en français (non final)

- **Connexion ECARX persistante et asynchrone** : une instance de `Car.create()` est réutilisée ; si `getICarFunction()` renvoie `null`, la lecture attend au maximum 5,5 secondes avant de renvoyer « indisponible », jamais un faux `0`.
- **Statut de support ECARX** : si `isFunctionSupported()` indique une valeur différente de `active`, aucun état véhicule n'est inféré. Les valeurs retournées restent **brutes**, sans extrapolation vers le tableau de bord.
- **Protection anti-doublons vocaux** : le même résultat Vosk ne déclenche pas deux traitements à cause de `onResult` puis `onFinalResult`. En outre, la session n’accepte qu’une seule phrase, même si le résultat final est différent ; la capture micro s’arrête avant de parler, pour éviter une auto-reconnaissance TTS.
- **Tests hors véhicule** : 22 tests du parser français + 6 tests de doublons + 6 tests d'une passerelle ECARX simulée (34 au total), tous réussis au 17 octobre 2026.
- Toujours **zéro appel d'écriture ECARX** dans l'application reconstruite. Pas de test automatique sur voiture.
- Les tests de simulation ne valident **pas** une connexion à l'IHU007B réelle : la présence du framework vendeur dans la classe d'application reste à contrôler sur tablette.

## Progrès V1 : traçage de la commande A/C russe

Une analyse du **graphe d'appels du DEX** (et non plus des seuls littéraux) a localisé la règle `AC` / `handleAc` contenant « кондиционер » : `h4/m.T()` → `h4/m.h()` → `h4/m.l()` → `EcarxApiHelper.setAcOn(boolean)` → `setFunctionValue(0x10010300, valeur)`. Le système de climatisation général `0x10010100` est traité distinctement. Voir **`docs/TRACAGE_AC_SOURCE_RUSSE.md`** et `analysis/trace_commandes_hvac.json`.

Le parser vocal français comporte **22 tests** de reconnaissance et de rejet prudent ; les phrases sans intention explicite ou à la forme négative n'activent rien. **Aucune écriture ECARX dans le projet Android** ; il reste à compiler et valider ce projet sur l'IHU007B.

## Base Android en français

`android/` contient un projet Android **minimal écrit proprement**, compilable en principe pour Android 9 (`minSdk=28`) :
- moteur Vosk français via `com.alphacephei:vosk-android:0.3.47` et modèle `assets/model-fr/` ;
- écran principal français ; reconnaissance et synthèse françaises ;
- table d'intentions françaises `FrenchIntentParser.java` ;
- adaptateur ECARX réflexif **strictement en lecture seule**, avec état A/C `0x10010300`, système de climatisation `0x10010100` et autonomie `0x00100800` ;
- aucun écriture ECU, aucune vitre ni toit actionné ; aucune tentative d'accorder des permissions système ;
- nom de paquet propre `com.aztech.jarvisfr.reconstruction` pour éviter toute substitution accidentelle à Jarvis 2.8 russe.

Cette base ne reproduit PAS les fonctions riches du Jarvis original. C'est une fondation maintenable, en complément de l'inventaire du DEX. Les commandes A/C **ne sont pas encore autorisées** dans cette base, malgré leur reconnaissance en français. Une erreur de détection ne doit pas activer une fonction automobile.

## Décompilation complète à effectuer ensuite

Installer `JADX` (outil officiel https://github.com/skylot/jadx) et `apktool` (https://github.com/iBotPeaches/Apktool) sur un poste de développement, puis utiliser :

```sh
jadx -j 4 -d jarvis-original-java jarvis-v2.8-Lite-build260605.apk
apktool d jarvis-v2.8-Lite-build260605.apk -o jarvis-original-resources
```

Le DEX est bytecode optimisé et parfois obscurci. L'export JADX reconstitue du Java **approximatif**, qui n'est pas garanti recompilable sans réécriture. Il faut ensuite étudier `EcarxApiHelper`, `VoiceService`, le routeur de commandes et les ressources Android, et réimplémenter proprement les fonctionnalités vérifiées.

## Construire / tester la base reconstruite

1. Ouvrir `android/` dans Android Studio (JDK 17, SDK 34) ;
2. Construire `:app:assembleDebug`, ou utiliser `.github/workflows/build-source.yml` depuis GitHub ;
3. Le modèle français est inclus ; taille du paquet de développement élevée ;
4. Pour valider uniquement le routeur **sans Android** : `sh tools/test_parser.sh`.

Aucune compilation Android réelle n'a été lancée dans cet environnement faute de SDK Android local. Ne pas installer ni publier sans compilation et validation sur l'IHU007B. La présence de `isFunctionSupported` n'implique pas les droits de `setFunctionValue`.

## Projets comparés

- https://github.com/Schum-io/Geely_Monjaro_Widgets — licence MIT ; architecture ECARX `compileOnly`, connexion `Car.create`, état support, monitoring ; environnement Monjaro OS 2.0+ différent d'IHU007B Android 9. Nous n'avons pas copié son code.
- https://github.com/maxim-saplin/zeekr_apk_mod — référence SMALI `IBcm` et identifiant `BCM_FUNC_WINDOW_POS = 0x21030300`. Compatibilité Emgrand non démontrée.
- https://github.com/alphacep/vosk-android-demo — exemple de reconnaissance hors-ligne Android/Vosk.
- https://github.com/skylot/jadx — récupération du code Java approximatif à partir du DEX.

Respecter les licences tierces. Le dépôt source et les conditions de redistribution du Jarvis russe exact n'ont pas été retrouvés lors de cette recherche. Cette archive n'inclut pas l'APK originale et n'est pas la redistribution d'un code source Jarvis officiel.
