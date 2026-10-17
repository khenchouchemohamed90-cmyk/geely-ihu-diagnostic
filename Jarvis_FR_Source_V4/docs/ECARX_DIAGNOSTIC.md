# Diagnostic ECARX V5 — 17 octobre 2026

L'adaptateur français est en lecture seule. Aucune commande automobile, modification
de firmware ou reprise des droits du paquet Jarvis russe n'est incluse. Les tests
JVM utilisent un faux service : leurs nombres 242, 1 et 0 ne viennent pas d'un véhicule.

## Ce que les archives permettent de conclure

Les résultats d'analyse fournis, notamment `analysis/hvac_references_decompilation.json`,
associent les méthodes russes suivantes aux littéraux DEX :

| Méthode du Jarvis russe | Identifiant observé | Portée de la preuve |
|---|---|---|
| `isAcOn()` / `setAcOn(boolean)` | `0x10010300` | Lecture / consigne A/C dans le code russe |
| `setClimateSystemOn(boolean)` | `0x10010100` | Commande distincte du système de climatisation |
| `setWindow(int,int)` | `0x21030300` | Identifiant de commande de vitre ; absent de l'adaptateur français |

Le graphe conservé dans `analysis/trace_commandes_hvac.json` et expliqué dans
`docs/TRACAGE_AC_SOURCE_RUSSE.md` décrit le trajet `AC` / `handleAc` → `h4/m.l()`
→ `setAcOn(boolean)` → écriture de `0x10010300`. Le nom `INFO_ID_ACON` associé à
`0x10010100` dans d'autres notes ne suffit pas à fusionner ces deux propriétés.
`setAcOn()` retourne `void` ; une confirmation vocale russe ne prouve donc pas le
succès de l'écriture.

Les notes historiques rapportent un voyant A/C et une autonomie observés par le
propriétaire avec le Jarvis russe. Ces observations ne constituent pas une mesure
réalisée pendant cette reconstruction. L'APK russe et ses DEX d'origine ne sont
pas dans les trois archives légères : l'analyse statique d'origine ne peut donc
pas être reproduite ici depuis le binaire. Les tableaux JSON sont des résultats
fournis, et non une nouvelle vérification physique ou une autorisation d'écriture.

## Contrat de l'adaptateur français

- Liste de lecture fermée : `0x00100800` (candidat autonomie), `0x10010300`
  (candidat A/C), `0x10010100` (candidat HVAC global), uniquement zone `0`.
  L'autonomie garde son nom historique `AUTONOMIE_KM` dans le code ; aucune unité
  kilométrique n'est appliquée aux résultats de diagnostic.
- Tout autre ID ou toute autre zone est refusé avant de créer une connexion ECARX.
- La connexion `Car.create(Context)` est réutilisée. `getICarFunction()` est sondé
  pendant environ 5,5 secondes si l'initialisation est asynchrone. Ce délai ne
  limite pas un appel Binder propriétaire déjà bloqué.
- `isFunctionSupported(id, zone)` est privilégié ; à défaut, la surcharge `(id)`
  est acceptée. Il faut explicitement une chaîne ou un nom d'enum `active`, sans
  tenir compte de la casse. Un statut absent, nul, inconnu, numérique, booléen,
  non actif ou une exception bloque la lecture. Aucun mappage de `1` vers
  « compatible » n'est supposé.
- `getFunctionValue(id, zone)` doit retourner un entier. Si cette surcharge est
  absente, la surcharge `(id)` identifiée par le probe de référence est acceptée,
  uniquement pour la zone 0 et après support actif. La surcharge de support qui
  correspond au getter retenu est privilégiée ; l'autre ne sert que si cette
  signature est absente. Aucun repli n'est tenté après
  une exception, un refus de support ou une valeur invalide. `available=true` signifie
  seulement « lecture entière obtenue après statut actif ». Cela ne valide ni
  l'unité, ni la fraîcheur, ni la correspondance avec un équipement physique.
  `available=false` ne signifie jamais « équipement arrêté » ou « autonomie zéro ».
- Les valeurs restent brutes. Carburant, température extérieure et autres IDs
  ne sont pas ajoutés en l'absence de validation.
- Le watcher optionnel gère correctement `equals`, `hashCode` et `toString`.
  Sa référence est conservée. `close()` interdit une reconnexion, annule le
  sondage et tente `unregisterConnectWatcher(watcher)` puis `dispose()` si ces
  méthodes existent. Les erreurs de nettoyage n'empêchent pas de libérer les
  références locales. Les tests de ces méthodes de cycle de vie sont simulés ;
  leur présence exacte dans le firmware reste à vérifier.

Les intentions françaises et les actions Android standard restent séparées de
cet adaptateur. Aucun commutateur de configuration ne peut activer des écritures.
Une future intégration automobile devra fournir sa propre preuve d'API, de droits,
de zone, de valeurs autorisées et de résultat sur l'IHU007B avant d'ajouter une
implémentation de commande distincte. La reconnaissance d'une phrase conserve
un résultat « désactivé » tant que cette implémentation n'existe pas.

## Chargement des classes ECARX sur la tablette

La réflexion supprime la dépendance de compilation au SDK propriétaire ; elle
ne rend pas accessibles des classes absentes du classloader Android. Les fichiers
fournis identifient le paquet Java `com.ecarx.xui.adaptapi`, sans fournir un nom
vérifié de bibliothèque partagée Android. Le manifeste ne déclare donc pas de
`uses-library` deviné. Aucune bibliothèque du Jarvis russe n'est redistribuée.

Si le diagnostic renvoie `ClassNotFoundException`, il faudra consulter sur la
tablette, en lecture seule, `adb shell pm list libraries`, la configuration de
bibliothèques partagées système et le manifeste original disponible sur l'appareil.
Ajouter une éventuelle dépendance partagée optionnelle ne doit se faire qu'après
identification de son nom exact. Un ajout de permission ne confère pas une
permission système réservée à une autre signature.

## Vérification locale et essais encore nécessaires

`tools/TestEcarxReadOnly.java` vérifie les lectures autorisées, l'absence d'écriture,
les statuts ambigus ou refusés, les exceptions, l'initialisation retardée, le délai,
le watcher et sa fermeture. Les tests V5 couvrent aussi les métadonnées sans
connexion, les permissions déclarées et accordées, les classes invisibles, les
surcharges du getter, les journaux de refus et les valeurs flottantes rejetées. `tools/verify_readonly.py --self-test` vérifie aussi le
garde-fou sur appels directs, références de méthode, réflexion, échappements,
concaténations littérales et commentaires. Ce garde-fou ne prouve pas l'absence
absolue d'une écriture dans un binaire ou dans une dépendance : tout changement de
réflexion dynamique ou ajout de bibliothèque nécessite une revue.

Sur l'IHU007B, il reste à relever la présence des classes, les erreurs de droits,
le statut et la valeur brute de chaque ID autorisé, puis à comparer la valeur à
l'état affiché par les interfaces d'origine. Aucune comparaison physique n'a été
faite ici. La connexion au service, sa fermeture et la reprise après retour à
l'application doivent aussi être essayées. La procédure de cette version ne
comporte aucun essai d'écriture, ni de firmware.


## Retour tablette et diagnostic de l'autonomie en V5

Le nouveau retour utilisateur établit que l'application démarre et peut ouvrir
Amap via sa commande écrite ; la lecture d'autonomie renvoie `notavailable`,
alors que le Jarvis russe annonce une autonomie. Ce statut n'est ni une autonomie
zéro, ni la preuve que le matériel n'expose jamais l'information. Il concerne le
chemin `ICarFunction` et le candidat d'ID choisi dans la reconstruction.

La vérification des pièces donne une raison précise de ne pas annoncer ce chemin
comme validé :

- `analysis/methodes_jarvis.json`, méthode `12334`, identifie
  `EcarxApiHelper.getEnduranceMileage()->F` : un flottant, sans argument.
- `analysis/trace_commandes_hvac.json`, routeur `h4/m.h`, offsets `5171`, `5175`,
  `5181` et `5189`, appelle ce helper, arrondit par `Math.round(float)` et construit
  la réponse russe « Запас хода … километров ».
- Le corps du helper n'est pas fourni. Aucun `getDistanceToEmpty`, `getSensor`,
  `getICarInfo` ou identifiant de capteur d'autonomie n'est démontré par les
  inventaires disponibles.
- `0x00100800` n'apparaît pas dans les instructions JSON ou journaux bruts fournis.
  Les mentions proviennent de la documentation historique et du prototype/faux
  service. Le journal d'origine cité dans la matrice n'est pas présent.

La reconstruction ne peut donc pas affirmer que
`ICarFunction.getFunctionValue(0x00100800, 0)` équivaut au helper russe. Passer
outre `notavailable`, convertir son résultat en zéro, tester tous les IDs ou
appeler un autre sous-système au hasard masquerait le problème. V5 n'effectue
aucune de ces opérations. La réussite de l'autonomie reste à établir.

`inspectEnvironment()` produit désormais un rapport de métadonnées : nom du
paquet, version, UID/PID, API Android, nom du processus déclaré, permissions
actuelles, fichiers de bibliothèques du paquet, noms des bibliothèques système,
classloaders et signatures publiques en lecture du SDK ECARX. Les interfaces de
sous-systèmes sont découvertes depuis les types de retour des accesseurs publics
de `Car`, sans invocation de ces accesseurs. Les noms/types des constantes
contenant autonomie/distance/capteur/carburant sont inventoriés ; leurs valeurs
ne sont pas lues pour éviter toute initialisation statique de classe OEM pendant
l'inspection. Les données ne comprennent pas les contacts ou les destinations.
Ce rapport ne crée pas de service ECARX.

Chaque essai de lecture écrit séparément l'ID hexadécimal, la zone, la classe de
service, les signatures disponibles, le type et le texte exact du statut, le
getter choisi, la valeur brute éventuellement reçue et l'exception éventuelle.
`getCustomizeFunctionValue` peut être inventorié comme signature, sans être
appelé. Les flottants reçus ne sont pas tronqués et restent indisponibles dans
l'API actuelle ; leur type et leur représentation brute sont consignés pour
identifier le chemin à corriger. Aucune unité n'est déduite.

Le constructeur accepte un `EcarxReadOnly.Logger` pour intégrer ces lignes au
journal de diagnostic. Sans injection, le tag Android est `JarvisFR`. Un échec
du journal ne modifie pas le résultat d'une lecture. Les prochaines pièces
utiles sont le rapport réel de la V5 et, si accessible légalement, le corps
`getEnduranceMileage()` de l'APK russe. Aucun `uses-library`, changement de droits
ou appel de capteur inventé n'est ajouté en attendant ces preuves.
