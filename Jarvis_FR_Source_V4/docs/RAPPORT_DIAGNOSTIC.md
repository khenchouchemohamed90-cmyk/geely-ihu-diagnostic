# Rapport de diagnostic — 17 octobre 2026

## Résultat livré

APK `Jarvis_FR_IHU007B_0.4.1_diagnostic.apk`, version `0.4.1-diagnostic-fr` / code 5, paquet `com.aztech.jarvisfr.reconstruction`, 61 135 805 octets (58,30 Mio).

SHA-256 : `8352938829dd573bde72eafecd9ae18d3b409a27efa6e6f60f1dfe9af5ae5780`.

Configuration déclarée et contrôlée : Android 9 API 28 minimum et cible 28, compilation SDK 34, ARM `armeabi-v7a` et `arm64-v8a`. Signature de diagnostic APK v2 vérifiée avec `apksigner --min-sdk-version 28`, et alignement vérifié. Ce résultat établit une APK signée et structurellement compatible API 28 ; **l’installation et le démarrage sur la tablette n’ont pas été exécutés**.

Certificat public de diagnostic SHA-256 : `f742acccc12d80db4b6fd97e790cf09e5dd4c66423fc95d4437b65c3b95368ce`. La clé privée n’est pas livrée dans le dépôt.

## Conservation du projet

Trois ZIP utilisateur contrôlés par CRC, fusionnés sous `Jarvis_FR_Source_V4`. Le seul nom de fichier commun avec un contenu différent était `MODELE_PARTIE_INFO.txt` ; la seconde note est conservée sous `MODELE_PARTIE_INFO_2.txt`. Les sources et le modèle V4 reçus sont conservés au commit d’import `ac04c8c`. Le développement se fait sur `codex/jarvis-fr-v4-diagnostic`, dans une copie de travail distincte. Le commit de départ de `main` est `8ae639802174e09790c96c776f070872ec6ff6b4`.

Aucun fichier de la branche principale n’a été remplacé. Aucun firmware ni permission système n’a été modifié. Les documents inclus sont des données et des analyses de référence ; les travaux ont suivi les demandes de l’utilisateur.

## Compilation et tests réellement exécutés

- JDK Temurin 17.0.17, Gradle 8.7 / wrapper vérifié, AGP 8.5.2, SDK Platform 34 et Build Tools 34.0.0.
- `sh tools/build_diagnostic.sh --offline` : code 0, **BUILD SUCCESSFUL** ; Java, ressources Android, DEX, APK et Lint réels.
- **220 vérifications JVM/statique réussies** : 121 cas d’intentions françaises, 7 anti-doublons historiques, 12 isolations de sessions, 38 assertions ECARX simulées, 29 actions Android simulées, 13 cas du garde-fou contre les écritures. Aucun de ces tests ne s’exécute dans un véhicule.
- Lint final : **0 erreur, 0 avertissement**, après l’exception ciblée `ExpiredTargetSdkVersion` pour l’installation privée avec cible 28. Les autres contrôles Lint, notamment d’API, restent actifs.
- 15/15 SHA-256 du modèle fusionné et 15/15 des mêmes fichiers extraits de l’APK conformes au manifeste utilisateur ; modèle de 68 483 828 octets.
- `zipalign` et `apksigner` : succès ; présence de `libvosk.so` et `libjnidispatch.so` dans les deux architectures ARM.
- Vosk natif Linux : modèle chargé et silence rejeté. Sur un corpus synthétique eSpeak NG, 2/11 intentions positives reconnues en mode libre et 7/11 en mode guidé ; 7/7 négations/composites rejetés. En guidé, un essai hors domaine devient une demande d’autonomie. Voir [le détail intégral](VALIDATION_VOSK.md).
- Aucun test instrumenté Android : ni tablette attachée, ni émulateur API 28 disponible. La bibliothèque de test Linux est Vosk Python 0.3.45 ; l’APK utilise Vosk Android 0.3.47.

Les premiers essais ont rencontré une restriction de sockets du sandbox Gradle, une dépendance Lint absente hors ligne et la règle Google Play sur la cible 28. La compilation finale utilise le réseau autorisé pour les sockets locaux, puis les dépendances en cache ; elle réussit hors ligne. Les avertissements de métriques Android non inscriptibles et de version XML des métadonnées SDK restent dans le journal, sans échec ni avertissement Lint.

## Fonctions et niveau de validation

| Fonction | Implémentation livrée | Vérification acquise | À vérifier sur IHU007B |
| --- | --- | --- | --- |
| Interface et messages français | Écran défilant, saisie de commande, erreurs et statuts français | Compilation et ressources Android | Affichage, clavier et confort sur l’écran |
| Compréhension des commandes écrites | Analyseur séparé ; rejet prudent des négations, mélanges et contradictions | 121 tests JVM | Ergonomie et formulations réelles |
| Vosk français hors ligne | Modèle embarqué, copie initiale, modes guidé/libre, écoute manuelle 15 s | Intégrité + moteur Linux + corpus synthétique imparfait | Chargement JNI ARM, micro, précision en habitacle |
| Sécurité des sessions | Une commande par activation, jetons de session, fermeture du Recognizer, arrêt à la sortie | 12 tests d’isolation + revue de cycle de vie | Enchaînements réels, permissions et interruptions |
| Synthèse française | API TTS Android et contrôle de disponibilité du français | Code compilé | Moteur et données vocales françaises installés |
| Lecture/pause/pistes suivante et précédente | Touches multimédias Android DOWN/UP | Simulations des requêtes exactes | Lecteur actif et réaction du système ECARX |
| Navigation | Ouverture Amap Auto installé, sinon Google Maps | Simulation lancement/absence/échec | Paquet réellement installé ; aucun calcul d’itinéraire intégré |
| Téléphone | Ouverture du composeur `ACTION_DIAL` | Simulation ; aucun appel direct | Existence du composeur, intégration Bluetooth éventuelle |
| Navigateur | Demande d’ouverture HTTPS dans une app installée | Simulation | Navigateur et connectivité de la tablette |
| Autonomie et A/C | Lectures brutes candidates, zone 0, support `active` obligatoire | 38 assertions ECARX simulées ; aucune preuve véhicule | Classes ECARX, droits du nouveau paquet, support, valeurs et unités |
| HVAC global | Identifiant candidat conservé dans l’adaptateur en lecture seule | Simulation de la liste autorisée | Pas de bouton ni de commande dédiée dans l’interface actuelle |
| Climatisation marche/arrêt, auto, consigne température, ventilation | Intention reconnue, message explicite de désactivation | Tests du routeur et garde-fou | **Écriture désactivée**, aucune mise en service physique |
| Vitres, toit, coffre, sièges, rétroviseurs, éclairage | Rejet ou réponse de commande non validée | Aucun chemin d’écriture livré | **Désactivés** ; aucun mouvement revendiqué |
| Carburant et température extérieure | Intention reconnue, réponse lecture indisponible | Analyseur testé | Lecture fiable encore non implémentée |
| Volume, appel direct/contact, itinéraires vocaux, mot de réveil, écoute permanente | Non implémentés | Aucune revendication | Travaux futurs selon compatibilité démontrée |

## Méthodes ECARX examinées

Les tables et le traçage fournis relient statiquement le gestionnaire russe A/C à `setAcOn(boolean)` puis `setFunctionValue(0x10010300, valeur)`. `setClimateSystemOn(boolean)` emploie distinctement `0x10010100`. Les archives légères ne contiennent pas l’APK/DEX russe original : cette session a examiné les analyses dérivées fournies et n’a pas reproduit la décompilation d’origine.

Cette association ne démontre ni les permissions de cette nouvelle APK, ni l’effet sur l’équipement. Le projet ne contient aucune API d’écriture ECARX activable par un réglage. La liste de lectures est limitée à `0x00100800`, `0x10010300` et `0x10010100`, zone 0, avec valeurs brutes et support explicite ; tout identifiant arbitraire ou statut incertain est rejeté.

La connexion du service est attendue au plus environ 5,5 s. Un appel Binder propriétaire déjà engagé peut dépasser ce délai et ne peut pas être interrompu de force ; les lectures et la fermeture ECARX sont hors du thread d’interface. Aucun nom de bibliothèque partagée système n’a été inventé dans le manifeste : un éventuel `ClassNotFoundException` doit être diagnostiqué sur l’appareil avant intégration d’un SDK ou d’un `uses-library` attesté.
