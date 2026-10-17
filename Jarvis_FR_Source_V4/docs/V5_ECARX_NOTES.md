# ECARX V5 — résultats de recherche et changements

## Diagnostic démontré

Retour utilisateur transmis par le parent : APK V4 démarre, commande écrite de navigation ouvre Amap ; autonomie ECARX renvoie `notavailable`, alors que le Jarvis russe annonce l'autonomie.

Le code V4 choisit `ICarFunction.getFunctionValue(0x00100800,0)->int`. Les sources disponibles ne permettent pas d'établir que ce chemin correspond au code russe d'autonomie.

Preuves exactes, exclusivement dans les résultats d'analyse dérivés fournis :

- `analysis/methodes_jarvis.json:2506` / `analysis/methodes_prioritaires.json:2115` : `Lorg/jarvis/car/EcarxApiHelper;->getEnduranceMileage()->F`, méthode 12334.
- `analysis/trace_commandes_hvac.json:13231` : routeur `h4/m.h(...)`, offset 5171, appelle `getEnduranceMileage()->F`.
- Même fichier, offset 5175 : `Math.round(F)->I` ; 5181 : chaîne « Запас хода » ; 5189 : « километров ».
- Aucun corps du helper n'est fourni. Aucun `getDistanceToEmpty`, `getDrivingRange`, `getRemainingMileage`, `getSensor` ou `getICarInfo` dans ces inventaires.
- `0x00100800` est absent des instructions `analysis/*.json` et des logs d'analyse. Il apparaît dans la documentation historique, la constante du prototype et son double de test renvoyant 242. Le log brut cité par la matrice n'est pas disponible.
- Les ZIP AZVoice présents sont des références de source Android, pas une preuve de l'API d'autonomie russe. `JarvisCompatibilityProbe.java` inspecte les signatures ICarFunction à 1 et 2 arguments, sans les appeler ni prouver leur présence sur la tablette.
- APK russe/DEX et manifeste original absents : pas de reproduction possible de leur analyse à partir des binaires.

Le `notavailable` ne prouve pas une absence de droit ; il peut refléter le mauvais ID ou sous-système. L'absence de SecurityException ne prouve pas non plus un droit métier. Le succès russe ne confère aucun droit au nouveau paquet.

## Changements réellement réalisés

Fichiers propriétaires : EcarxReadOnly.java, tools/TestEcarxReadOnly.java, tools/fake_ecarx/**, docs/ECARX_DIAGNOSTIC.md.

- `EcarxReadOnly(Context, Logger)` avec `Logger { void log(String message); }` ; constructeur précédent conservé, log Android `JarvisFR` par défaut.
- `inspectEnvironment()` : métadonnées uniquement, sans Car.create, getter OEM, valeur de constante ou propriété. Identité du paquet/processus, UID/PID/API, permissions déclarées+accordées, fichiers de bibliothèques du paquet, bibliothèques système, classloaders, signatures Car/ICarFunction et interfaces déclarées comme retours de getters Car. Noms/types des constantes liés aux capteurs/distance/carburant ; aucune lecture de leur valeur statique.
- Événements de lecture : hex ID/zone/preuve, factory/instance/interface/watcher, API et surcharges exposées, statut type+brut+enum.name, getter sélectionné, résultat brut/type ou erreur. Statut active conservé séparément d'une éventuelle exception getter.
- Surcharge `getFunctionValue(int)` acceptée seulement si `(int,int)` est absente et support explicitement actif, toujours zone 0. Aucun repli après refus du support ou exception/getter invalide.
- Support aligné sur la signature du getter structurellement retenu : getter(id,zone) => support(id,zone) prioritaire ; getter(id) seul => support(id) prioritaire. Autre signature uniquement si la préférée est absente. Une seule interrogation de support, aucun repli après statut négatif. Régression explicite getter global seul / support zoné notavailable / support global active.
- Valeur `notavailable` conservée, getter non invoqué, aucune valeur zéro annoncée. Float journalisé mais rejeté dans le contrat entier actuel, sans unité ni troncature.
- Même allowlist de trois candidats, aucun ID/zone ou API de capteur ajouté arbitrairement. Aucune méthode d'écriture, firmware ou permission système modifiée.

## Vérifications

59 assertions JVM ECARX simulées passées ; 13 auto-tests du garde-fou statique passés. Aucun véhicule connecté, aucune nouvelle lecture réelle d'autonomie obtenue.

Cas V5 : métadonnées sans création de service/getter OEM/initialisation statique du faux capteur, bibliothèque et permissions reportées, classes absentes du classloader, notavailable sans faux zéro, fallback d'overload limité, exception zonée sans fallback, statut actif+erreur distincts, float brut journalisé/rejeté, panne du journal sans effet sur lecture.

Les nouveaux imports Android de métadonnées ont leurs fakes sous `tools/fake_ecarx/android/{content/pm,os,util}`. Le runner parent doit compiler tous les `.java` de `tools/fake_ecarx` (pas seulement la liste des anciens fichiers).

## Limites et suite

L'autonomie est encore non opérationnelle sur le retour réel fourni ; V5 apporte un diagnostic précis et un correctif des surcharges, pas une preuve d'autonomie résolue. Le rapport V5 sur tablette ou le corps réel du helper russe sont nécessaires pour choisir un capteur, ID, getter, unité ou permission métier. Ne pas inventer de uses-library à partir d'un nom de paquet Java. Ne pas ajouter de droits système réservés ni essayer des capteurs au hasard.
