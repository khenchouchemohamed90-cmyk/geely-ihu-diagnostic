# Traçage statique Jarvis 2.8 Lite → commande A/C (17 octobre 2026)

## Provenance et limites

Inspection du fichier APK russe `jarvis-v2.8-Lite-build260605.apk` (DEX d'origine), à l'aide de `tools/trace_hvac.py`. Aucune requête au véhicule ni écriture Android n'est effectuée. Les chaînes ci-dessous ont été observées dans le DEX ; les noms de classes `h4/m` sont obfusqués et les pseudonymes `handleAc` sont des noms de règles présents dans l'application. Les résultats ne signifient pas que l'APK française nouvelle a déjà des droits ECARX.

## Chaîne AC retrouvée dans le code compilé

1. La méthode `Lh4/m;->T()->V` enregistre les chaînes **`кондиционер`**, **`кондей`**, **`кондёр`** dans la règle de commande `AC`, liée au nom de gestionnaire **`handleAc`**. Offsets d'instructions DEX (en *code units* de cette méthode) : autour de 147–168. Un autre enregistrement proche active diverses formes impératives ; la syntaxe complète des règles requiert encore une décompilation structurée.
2. `Lh4/m;->h(Li4/a;,Ljava/util/Map;,Ljava/lang/String;)->Ljava/lang/String;` contient les noms de gestionnaires et déclenche notamment `Lh4/m;->l(Ljava/lang/String;)->Ljava/lang/String;` pour le traitement A/C.
3. `h4/m.l(String)` examine `выключ` et `отключ` pour la branche d'arrêt, consulte `EcarxApiHelper.isAcOn()` et appelle `EcarxApiHelper.setAcOn(boolean)` ; chaînes de confirmation présentes : `Включаю кондиционер` et `Выключаю кондиционер`. Le code appelle des méthodes vocales et **ne réévalue pas le retour de succès d'une écriture ECARX** : `setAcOn()` retourne `void`.
4. `EcarxApiHelper.setAcOn(boolean)` utilise `0x10010300` puis appelle `setFunctionValue(int, int)` ; la valeur `boolean` devient une consigne entière (`true` typiquement 1 ; `false` 0).
5. `EcarxApiHelper.setClimateSystemOn(boolean)` utilise séparément `0x10010100`; ce n'est **pas** la voie principale `handleAc` correspondant au mot `кондиционер` dans le routeur identifié.

**Conclusion statique robuste :** le comportement programmé de « Кондиционер » est associé à `handleAc` puis `setAcOn()`, identifié par `0x10010300`. L'utilisateur a observé physiquement l'allumage de son voyant A/C avec Jarvis russe ; la preuve dynamique du message exact sur le bus ECU n'est toutefois pas contenue dans les logs conservés.

## Références à consulter

- `analysis/trace_commandes_hvac.json` : indices des méthodes, références DEX, appels directs, gestionnaires et chaînes.
- `analysis/trace_hvac_log.txt` : sortie compacte reproductible du script d'analyse.
- `tools/trace_hvac.py` : outil de reconstruction du graphe d'appels statique ; ne modifie jamais l'APK.
- `android/app/src/main/java/com/aztech/jarvisfr/FrenchIntentParser.java` : parser français **indépendant de toute écriture ECARX**.

## Décision d'implémentation

Les commandes vocales « Allume la climatisation », « Active la clim » et « Éteins le climatiseur » sont reconnues en intentions, mais leur exécution reste **désactivée** dans la base reconstruite. La mention isolée « Climatisation » reste volontairement *inconnue* pour ne pas déclencher un équipement par erreur. Toute activation future doit nécessiter confirmation de la reconnaissance, compatibilité et état ECARX, et traitement explicite de l'échec. Les vitres et le toit restent désactivés.

## Projets GitHub examinés

- `Schum-io/Geely_Monjaro_Widgets`, MIT : passerelle ECARX `Car.create`, `ICarFunction`, états, source de référence pour la connexion mais non directement compatible avec l'IHU007B.
- `maxim-saplin/zeekr_apk_mod` : interface `IBcm` et constantes, non preuve de compatibilité véhicule.
- `alphacep/vosk-android-demo` : intégration Vosk sur Android.

Les bibliothèques et codes tiers ne doivent pas être redistribués sans vérification de leurs licences et des droits applicables à Jarvis d'origine.
