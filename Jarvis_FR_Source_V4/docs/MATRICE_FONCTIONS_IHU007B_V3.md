# Matrice de compatibilité Jarvis FR — Geely Emgrand IHU007B (17/10/2026)

## Règle de preuve
- « Original confirmé » = comportement observé physiquement ou sur le tableau de bord par le propriétaire **dans Jarvis russe**. Cela ne confirme PAS la nouvelle APK.
- « Détecté DEX » = méthode repérée par analyse de la version russe ; son existence n'établit ni la compatibilité avec l'Emgrand ni l'autorisation système.
- « Implémenté Android » = une action Android standard est programmée dans le prototype français ; tests avec classes simulées uniquement, **pas sur l'IHU007B**.
- « Reconnu FR » = le parser accepte la commande française ; ce n'est pas une action physique.
- « Désactivé » = aucun appel d'écriture ECARX dans notre code.

| Domaine | Fonctions recherchées | Origine probante | Jarvis FR V3 | Priorité / condition de mise en service |
|---|---|---|---|---|
| Climatisation A/C | marche, arrêt, lecture de l'état | **Marche confirmée dans Jarvis russe**, `setAcOn` et `isAcOn`, 0x10010300 DEX | Marche/arrêt **reconnus FR mais écriture désactivée** ; lecture ECARX sous contrôle de connexion | Haute : compiler, contrôler permissions et résultat du service, tester véhicule stationné |
| HVAC global | marche/arrêt, mode AUTO | `setClimateSystemOn` 0x10010100, `setAcAuto` DEX | AUTO reconnu FR, écriture désactivée | Haute, non validé sur Emgrand |
| Ventilation | niveau, auto, orientation | `setFanSpeed`, `setFanAuto`, `setBlowingMode` DEX | Niveaux 1–9 reconnus FR, écriture désactivée | Haute, plages à valider sur IHU007B |
| Température habitacle | consigne 16–30 °C | `setTemperature` DEX | Consigne entière reconnue FR (chiffres et mots), écriture désactivée | Haute, plage et unités à confirmer avant écriture |
| Autonomie | lecture et réponse vocale | Original russe a annoncé une autonomie correspondant au compteur ; 0x00100800 vu dans les logs | Lecture ECARX brute, voix française à confirmer sur appareil | Haute |
| Carburant / température extérieure | état et mesures | Carburant non déterminé par Jarvis russe ; température extérieure divergente lors d'un essai | Demandes reconnues mais affichées **indisponibles** | Moyenne : trouver sources fiables et unités |
| Musique / média | jouer, pause, suivante, précédente | Android 9 expose AudioManager / touches média | **Implémenté par API Android**, pas testé sur l'IHU007B | Haute : vérifier l'application musicale active |
| Navigation | ouvrir l'application | Paquet `com.autonavi.amapauto` observé installé sur tablette | **Lancement Android implémenté**, non testé sur IHU007B | Haute : vérifier lancement propre et retour utilisateur |
| Téléphone | ouvrir composeur, contacts, appels | Jarvis russe possède `CallsSettingsActivity`, pas de comportement testé ici | **Ouverture du composeur uniquement**, pas d'appel automatique | Moyenne : dépend du composeur installé et de l'appairage BT |
| Navigateur web | ouvrir application web | Fonction classique Android | **Ouverture par Intent Android implémentée**, non testée | Moyenne |
| Vitres | quatre fenêtres, position | Méthodes DEX ; commande conducteur `BCM_FUNC_WINDOW_POS(16,100)` logguée **sans mouvement réel** | Demande reconnue, **écriture interdite** | Bloqué tant que zone/permissions/résultat BCM non validés |
| Toit ouvrant, store, coffre | commandes d'ouverture/fermeture | Méthodes DEX | Demande reconnue, **écriture interdite** | Bloqué : sécurité et compatibilité inconnues |
| Rétroviseurs, sièges, éclairage, essuie-glaces, conduite | fonctions dans `EcarxApiHelper` russe | Méthodes DEX seulement | **Non implémenté** | Étudier disponibilité et risques en lecture seule, pas d'envoi de commandes |
| Assistant par défaut, mot d'activation | service vocal / arrière-plan | Jarvis russe contient `VoiceService` ; Android 9 permet assistants sous conditions | **Non implémenté** | Ultérieurement : intégration et restrictions ECARX |

## Avancées V3 démontrables hors véhicule
- Parser : 46 phrases tests avec commande/interdiction ; fonctions FR A/C, température, ventilation, autonomie, navigation, musique, téléphone, navigateur et commandes de carrosserie reconnues mais verrouillées.
- Événements vocaux : 7 tests anti-doublons, remise à zéro au démarrage d'une nouvelle session ; correction d'un indicateur qui était bloqué à `false`.
- Connexion ECARX : 6 tests simulés en lecture seule.
- Actions Android : 8 tests simulés, avec événements média, lancement de navigation et ouverture du composeur ; la réussite réelle doit être vérifiée sur la tablette.
- **Zéro écriture automobile dans la V3** : aucune commande `setFunctionValue` / `setCustomizeFunctionValue` appelée dans l'application reconstruite.

## Notes techniques et limites
Le code russe identifie une API ECARX, mais l'application reconstruite est un autre paquet et n'hérite pas des permissions de l'original. Les journaux montrent `INFO_ID_ACON` 0x10010100 et les méthodes du code russe fournissent 0x10010300 pour l'A/C. Toute activation réelle doit attendre une compilation APK, un essai en lecture seule de `isFunctionSupported`, une analyse des permissions, puis une validation explicite. Le fait qu'une commande soit reconnue par Vosk ou « envoyée » par Android ne prouve pas l'action sur l'équipement.
