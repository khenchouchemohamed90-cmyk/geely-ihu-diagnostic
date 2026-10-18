# Jarvis FR 0.9.4 — Xiaoka + diagnostic de liaison SDK ECARX (SS11 build 547)

## Résultats confirmés sur l'IHU007B (0.9.3.1)
- Bouton XIAOKA : ouvre l'assistant OEM.
- Bouton CLIM OEM et commande vocale « Affiche la climatisation » : ouvrent l'interface HVAC d'origine.
- Prévalidation A/C : l'APK n'a ni `CONTROL_CAR_CLIMATE` ni `CAR_VENDOR_EXTENSION`. `ICarFunction` retourne `notavailable` pour `0x10010300` (A/C) et `0x10010100` (HVAC).
- Le succès d'ouverture d'interface n'autorise aucun contrôle automobile.

## Modifications 0.9.4
1. Reconnaissance de « Ouvre l'assistant », « Ouvre l'assistant vocal », « Lance l'assistant Geely », plus le précédent « Ouvre Xiaoka ». Le nom chinois reste accessible depuis le bouton.
2. Bouton explicite « Tester liaison SDK ECARX (sans commande) » dans les réglages (également `tester liaison ecarx` en saisie texte).
3. Uniquement le service SDK public `com.ecarx.sdk.openapi/.ServicePoolService`, repéré sur la tablette. Vérification que le service est exporté et que la permission déclarée, s'il y en a une, est accordée.
4. Test `bindService` avec **flags=0**, sans `BIND_AUTO_CREATE`. Aucun service n'est volontairement démarré, aucune transaction Binder, aucun `getInterfaceDescriptor`, aucun appel de méthode automobile ni écriture. Délai de 3,5 s, libération systématique.
5. Le résultat de liaison est enregistré dans le journal exportable. Une liaison réussie prouverait uniquement qu'un Binder est proposé, **pas** que le véhicule accepte la lecture ou l'écriture ECARX.
6. Identifiant d'application propre : `com.aztech.jarvisfr.xiaoka.probe547`. Coexistence avec Jarvis FR 0.9.3.1.

## Tests, voiture stationnée
- Installer seulement après succès des vérifications GitHub Actions.
- Confirmer « Ouvre l'assistant » et « Affiche la climatisation » au microphone.
- Réglages et diagnostic > « Tester liaison SDK ECARX (sans commande) », puis « Exporter le journal ».
- Ne pas tester une commande d'activation A/C, fenêtre, verrouillage ou conduite dans cette version. Les actions automobiles restent désactivées.

## Interprétation
- `Binder ECARX reçu`: liaison de service possible, autorisation fonctionnelle **non vérifiée**.
- `bindService=false`: service absent/inactif ou liaison refusée ; pas un verdict définitif.
- `SecurityException`: restriction Android.
- Timeout: aucun callback en 3,5 s ; aucun contournement effectué.
- Pas de root, pas de signature OEM, pas de remplacement de Xiaoka.

## Prochain jalon
Inspecter le contrat SDK du ServicePool et trouver une interface explicitement autorisée à ce client, puis seulement créer des tests de lecture sans contrôle physique. Le contrôle A/C n'est **pas** implémenté par ce changement.
