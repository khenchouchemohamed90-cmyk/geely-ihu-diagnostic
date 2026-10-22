# Jarvis FR 0.9.6 — essai A/C physique volontaire, SS11 / IHU007B build 547

## Pourquoi l'essai
L'utilisateur a explicitement autorisé une tentative de commande physique sur sa propre Emgrand SS11. L'IHU OEM a déjà actionné le compresseur A/C ; les journaux constructeur du projet ont montré :
- `function=0x10010300`, `zone=0`, `funcValue=1` pour ON et `0` pour OFF ;
- `CarFunctionImpl.setFunctionValue` suivi d'un retour d'état constructeur ;
- Jarvis FR (application tierce) n'a **pas** `CONTROL_CAR_CLIMATE` ni `CAR_VENDOR_EXTENSION`, et `isFunctionSupported()` avait rendu `notavailable` lors d'une prévalidation. L'appel de contrôle peut donc échouer. Cela n'efface pas l'observation sur l'IHU OEM.

## Fonctionnalités de 0.9.6
- Deux boutons séparés dans « Réglages, audio et diagnostic » : « Essai A/C MARCHE » et « Essai A/C ARRÊT ».
- Chaque bouton ouvre une boîte de dialogue de confirmation indiquant qu'il s'agit d'une action physique et rappelant les conditions de stationnement.
- Après validation, le code appelle **exactement une fois** `ICarFunction.setFunctionValue(0x10010300, 0, 1|0)`, via l'interface `com.ecarx.xui.adaptapi.car.Car.create(Context)` disponible en environnement OEM.
- Aucun nouvel ID ou zone n'est deviné. Pas de repli sur des signatures de méthodes inconnues. Pas de `su`, pas de copie de signature, pas de patch système, pas d'injection dans Xiaoka.
- Appel asynchrone et borné en attente de connexion, fenêtre UI ouverte requise avant écriture, un essai à la fois et 5 s d'intervalle entre demandes.
- Aucun contrôle physique depuis la voix, les commandes personnalisées ou le lancement automatique ; toutes les autres actions automobiles restent bloquées.
- Le journal marque `AC_PHYSICAL_TRIAL`, capture la confirmation, le retour `TRUE/FALSE` ou la classe d'exception, **sans prétendre confirmer l'état physique**.
- Une réponse `TRUE` veut seulement dire que l'appel API a été accepté à ce niveau ; elle n'atteste pas que le compresseur est effectivement activé.
- Identifiant de paquet séparé : `com.aztech.jarvisfr.xiaoka.acmanual547`, version `0.9.6-manual-ac-trial-547`. La version Jarvis 0.9.5 installée reste inchangée.

## Procédure de test
1. Voiture sur aire de stationnement sûre, frein de parking serré. Ne pas faire tourner un moteur thermique dans un garage fermé.
2. Ouvrir Jarvis FR 0.9.6 ; faire défiler vers les réglages.
3. Observer d'abord le témoin A/C sur l'interface constructeur. Le test n'exige pas de modification des volets ou vitres.
4. Appuyer une fois sur « Essai A/C MARCHE », lire la confirmation, valider si les conditions sont réunies.
5. Remonter à « RÉPONSE ». Relever le message exact (`TRUE`, `FALSE`, `SecurityException`, `NoSuchMethodException`, `not connected`, etc.). Vérifier l'état réel **manuellement** sur l'écran OEM.
6. Si l'A/C s'est activé, utiliser le bouton « Essai A/C ARRÊT » après 5 secondes et vérifier visuellement, ou utiliser les commandes de climatisation Geely d'origine en cas de difficulté.
7. Exporter le journal et partager la ligne `AC_PHYSICAL_TRIAL`. Ne pas répéter si Android refuse la permission ; analyser le refus plutôt que contourner l'autorisation.

## Règles d'interprétation
- `SecurityException` : restriction d'accès constructeur observée. Aucun nouvel essai de contournement.
- `NoSuchMethodException` : la méthode à 3 paramètres n'est pas exposée dans ce runtime. Ne pas essayer une signature devinée.
- `FALSE` : échec ou refus signalé par l'API.
- `TRUE` : seul le retour logiciel est positif ; vérifier le témoin/climatisation.
- `null` ou retour différent : pas de validation physique ; aucun état supposé.

## Tests CI
Tests Java simulés : un seul appel A/C et arguments exacts, refus, délais, annulation avant écriture et nettoyage de connexion. Garde-fou statique révisé pour autoriser **uniquement** l'appel de cette classe ; toutes les autres commandes voiture demeurent prohibées. L'APK compilée sera livrée seulement après succès complet de GitHub Actions et de la vérification d'identité Android 9.
