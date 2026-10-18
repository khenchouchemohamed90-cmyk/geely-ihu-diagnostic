# Jarvis FR — prévalidation A/C du firmware IHU007B 547

## Ce qui est réellement implémenté

Le bouton **Prévalidation climatisation (lecture seule)** (et la commande saisie manuellement `prevalidation climatisation`) déclenche un diagnostic non destructif :

1. Empreinte Android et UID de notre application ;
2. Vérification de ses permissions `CONTROL_CAR_CLIMATE` et `CAR_VENDOR_EXTENSION` ;
3. Versions/UID/droits constructeur et attributs `exported` + `permission` des services XCHvac, XCCarControl, OpenAPI, Xiaoka ;
4. Lecture des **deux identifiants distincts** par `EcarxReadOnly.read(..., 0)` : `0x10010300` = compresseur A/C ; `0x10010100` = système HVAC global ;
5. Statuts explicites, valeurs brutes seulement si disponibles, erreurs et diagnostic exportable.

**Aucun service OEM n'est lié directement par le nouvel audit** et **aucune écriture n'est appelée**. La fonction `EcarxReadOnly.read` utilisée peut déclencher une connexion **en lecture** aux classes SDK ECARX déjà autorisées par l'application. L'existence de cette API ne prouve pas une permission d'écriture.

## Lien avec les preuves OEM

- Dans Jarvis russe 2.8 Lite : `setAcOn(boolean)` utilise `0x10010300` ; activation A/C observée par le propriétaire sur SS11.
- `setClimateSystemOn(boolean)` utilise `0x10010100` ; ne pas confondre les deux.
- Dans XCHvac v547 : `AirConditionPresenter.enableAc/disableAc`, `CarFuncManager` et `ICarFunction`. Les signatures sont réelles mais les corps et le mécanisme d'autorisation complet restent à vérifier.
- OpenAPI v547 : `EASFrameworkService` et `ServicePoolService` sont exportés ; le code contient des indices de vérification de signatures/autorisations.
- Jarvis FR utilise un autre package et ne bénéficie pas de l'UID système 1000.

## Essai sans mouvement sur tablette

1. Véhicule stationné, sans conduire pendant la manipulation.
2. Installer l'APK d'essai `0.9.2-ac-preflight-547` seulement après avoir identifié sa signature et conservé une possibilité de retour à l'ancienne version. Cette APK utilise `com.aztech.jarvisfr.xiaoka.audit`, et n'est pas une modification du logiciel constructeur.
3. Ouvrir l'interface Jarvis FR, descendre à **LECTURES CONSTRUCTEUR**, appuyer sur **Prévalidation climatisation (lecture seule)**.
4. Appuyer sur **Exporter le journal**, puis joindre le rapport complet pour relever les droits, l'état et les erreurs.
5. Ne pas déclarer « climatisation opérationnelle » si `support=active` ou une valeur est retournée ; une vraie commande exigerait une autre version après contrôle des autorisations et retour d'état.

## À faire avant une commande physique

- Revue des instructions appelées par les fonctions OEM, des conditions de support et des valeurs attendues.
- Diagnostic de la protection du service appelé par le package **Jarvis FR**, sans privilèges système empruntés.
- Validation d'une unique commande A/C réversible avec consentement explicite, véhicule immobilisé, et lecture comparative avant/après.
- Ne pas appliquer aveuglément une commande issue d'un profil Atlas à la SS11.
- Les vitres, le toit, le coffre et les fonctions de conduite restent bloqués.

## Limites de cette version

Compilation et tests automatiques vérifient les sources, pas l'installation ni les permissions d'une tablette réelle. Le prédiagnostic peut confirmer un manque de droit ou une interface inaccessible, mais ne peut pas garantir qu'un Binder accepterait les commandes physiques. Aucun remède contournant signatures OEM, permissions protégées ou listes blanches n'est proposé.
