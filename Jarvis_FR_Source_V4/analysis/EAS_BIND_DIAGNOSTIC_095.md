# Jarvis FR 0.9.5 — Diagnostic liaison EASFrameworkService (Xiaoka / SS11 / build 547)

## État de départ déjà validé sur la tablette
- Jarvis FR 0.9.4.1 : ordre français « Ouvre l'assistant » -> Xiaoka ; le microphone est libéré avant l'ouverture.
- Jarvis FR 0.9.4 : connexion à `com.ecarx.sdk.openapi.ServicePoolService` et réception d'un Binder (sans invocation AIDL).
- Les traces constructeur indiquent une connexion de `com.ecarx.xiaoka.carcontrol` à `com.ecarx.sdk.openapi.EASFrameworkService` ; cette autre interface mérite un test distinct.
- L'APK n'a pas les permissions de contrôle `CONTROL_CAR_CLIMATE` ni `CAR_VENDOR_EXTENSION`. La réception d'un Binder n'octroie aucune de ces permissions.

## Nouveauté précise
Dans les réglages, le bouton « Tester liaison Xiaoka / EAS (sans commande) » exécute un **bindService explicite** vers
`com.ecarx.sdk.openapi/com.ecarx.sdk.openapi.EASFrameworkService`.
La commande écrite `tester liaison xiaoka` appelle la même fonction, sans que l'écoute vocale puisse la déclencher involontairement.
Un seul `OemServiceProbe` gère les tests SDK (SERVICE_POOL) et EAS (EAS_FRAMEWORK), un à la fois.

## Limitations fortes
- Demande uniquement un Binder par `Context.bindService(..., flags=0)`.
- `flags=0` : **aucun BIND_AUTO_CREATE**, aucun démarrage volontaire du service.
- **Aucune** méthode de ce Binder n'est invoquée ; **aucun** `transact` ni lecture de données véhicule à ce stade.
- Aucun envoi d'intention NLU, aucune modification de Xiaoka ni commande A/C, vitres, portes, coffre ou conduite.
- Vérifie uniquement que le service est visible, exporté et que les éventuelles permissions de manifeste sont accordées ; les contrôles internes restent inconnus.
- Libère le lien à la fin, y compris sur délai de 3,5 s et fermeture de l'application.
- Les résultats de réussite signifient seulement que la communication de base est possible, **pas** que Jarvis a accès aux fonctions.

## Test sur IHU007B, véhicule stationné
1. Installer `Jarvis FR 0.9.5 - EAS` en parallèle de 0.9.4.1.
2. Ouvrir « Réglages, audio et diagnostic » et appuyer « Tester liaison Xiaoka / EAS (sans commande) ».
3. Faire défiler jusqu'à « RÉPONSE ». Noter : Binder reçu, refus Android, service inactif ou délai dépassé.
4. « Exporter le journal » et communiquer le résultat. Le mot-clé dans le journal est `XIAOKA_EAS_BIND`.
5. Ne pas lancer d'actions physiques. Ne pas déclencher de broadcast `VR_PLUGIN_START`, réservé au démarrage de composants constructeur.

## Étape suivante dépendant de ce résultat
Reconstituer, dans le code firmware 547, le contrat AIDL `IEASFrameworkService` et les points de contrôle d'autorisation pour une API **documentée et de lecture seule**, puis décider s'il est raisonnable de l'utiliser depuis un paquet tiers. Ne pas inventer d'ID, ni confondre service exporté et permission accordée.

## Packaging et régression
- package Android distinct : `com.aztech.jarvisfr.xiaoka.eas547`
- version : `0.9.5-xiaoka-eas-readonly-547`
- Tests explicites du parseur `DiagnosticCommands`, garde-fou d'absence d'écritures, validation Android 9 et signature via CI.
