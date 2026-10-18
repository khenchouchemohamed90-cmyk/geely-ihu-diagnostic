# SS11 / IHU007B build 547 — trace physique HVAC A/C du 18 octobre 2026

**Source:** logcat fourni par l'utilisateur le 18 octobre 2026, extrait de `/sdcard/Download/Jarvis_AC_547.log` (~26 Mo), pendant activation/désactivation manuelles de la climatisation d'origine. Journal complet non fourni ; cette note s'appuie sur l'extrait transmis dans le chat.

## Séquence réellement observée

| Heure Android | UI OEM | Appel interne | Retour |
|---|---|---|---|
| 18:23:59.352–.449 | `AirConditionPresenter.setA_C status=true` | `CarFuncManager.setFunctionValue(function=268501760, funcValue=1)`; `CarFunctionImpl.setFunctionValue(function=268501760, zone=0, funcValue=1)` | `HvacModule.setFunctionValue(funId=0x10010300, propId=INFO_ID_AC_KEY)`; `onFunctionValueChanged(funcValue=0x00000001,propValue(1))`; `AcView.setAcFunction status=true` |
| 18:24:08.011–.048 | `setA_C status=false` | même fonction, `funcValue=0` | `onFunctionValueChanged(funcValue=0x00000000,propValue(0))`; `AcView.setAcFunction status=false` |
| 18:24:16.168–.347 | `setA_C status=true` | même fonction, `funcValue=1` | `onFunctionValueChanged(funcValue=0x00000001,propValue(1))`; `AcView.setAcFunction status=true` |

La ligne `HvacModule.setFunctionValue(... propValue(1))` apparaît **même au moment de l'extinction** (`funcValue=0`). **Ne pas interpréter `propValue(1)` dans l'appel comme l'état final du compresseur**. Le callback `onFunctionValueChanged` et `AcView.setAcFunction` confirment le passage à 0, puis à 1.

Enregistrement OEM confirmé pour la commande `INFO_ID_AC_KEY = 0x10010300`, `zone=0`, `valeur d'activation=1`, `valeur de désactivation=0`.

## Pourquoi Jarvis FR 0.9.2 affichait "notavailable"

`EcarxReadOnly.read(id,zone)` exécute `isFunctionSupported(id,zone)`, puis **arrête localement** l'exécution si le statut n'est pas exactement `active`. Son `getFunctionValue()` n'a alors pas été appelé pour ces deux fonctions. Ce résultat **ne démontre ni l'absence de climatisation ni un refus d'écriture**.

L'ID `0x10010300` correspond dans la trace à `INFO_ID_AC_KEY` ; il peut s'agir d'une fonction de commande et de notification plutôt que d'un getter d'état persistant. La trace ne démontre pas qu'un `getFunctionValue(0x10010300)` renvoie l'état A/C fiable.

`0x10010100` est distinct (système HVAC global) : il n'a pas été actionné séparément dans cette capture.

Pour une mesure fiable de l'état A/C, **identifier et valider l'enregistrement du callback/watcher** `ICarFunction.registerFunctionValueWatcher` ou une propriété d'état adaptée, sans extrapoler depuis `getFunctionValue` d'une touche/commande.

## Valeurs annexes effectivement lues par XCHvac

- Ventilation : `getFunctionValue(funId=0x10020100)`, `INFO_ID_BLOWER_LEVEL` -> `0x10020104` (UI : niveau 4).
- Orientation : `0x10070100`, `INFO_ID_FLOWMODE` -> `0x10070101` (UI : soufflage visage).
- Température : `getCustomizeFunctionValue(0x10060100, zone=1)` ; `convertToFloatValue(10060100,17.0)` ; UI 17,0 °C.
- Bi-zone : `isFunctionSupported(0x10060500)` -> `notavailable`, `not supported dual!` ; observation compatible avec une finition mono-zone, pas une preuve générale sur tous les véhicules.

## Décisions de développement

1. Marquer la commande A/C OEM **confirmée**, mais l'écriture par notre APK **non validée**.
2. Adapter les diagnostics pour séparer support déclaré, droits du paquet, existence du getter, résultat du callback et état physique.
3. Rechercher les protections d'accès de l'API ECARX depuis UID 10107 (`com.aztech.jarvisfr.xiaoka.audit`) ; UID 1000 de XCHvac n'est pas transférable.
4. Avant un test de commande depuis Jarvis FR : confirmation utilisateur explicite, véhicule stationné, accès officiellement autorisé, contrôle d'état avant/après, aucune commande réitérée automatiquement.
5. Ne pas utiliser `0x10010100` comme synonyme de `0x10010300` ; ne pas tester de vitres, toit, coffre.

**Aucune instruction de ce fichier n'autorise un essai A/C depuis une APK tierce sans avoir d'abord validé droits et état.**
