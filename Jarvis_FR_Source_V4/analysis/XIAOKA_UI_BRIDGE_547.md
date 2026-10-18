# Jarvis FR 0.9.3 — Première passerelle avec les interfaces Xiaoka (IHU007B / SS11 / 547)

## Portée exacte
- `ouvre xiaoka` : demande Android explicite d'ouverture de `com.ecarx.xiaokagui/com.ecarx.xiaoka.XKMainActivity`. Activité confirmée par le `dumpsys package` de la tablette et exécutée antérieurement avec `monkey`.
- `affiche la climatisation` / `ouvre la climatisation` : demande d'ouverture de l'écran OEM `ecarx.hvac.app/ecarx.hvac.app.MainActivity`. Exportation identifiée dans la copie du manifeste du firmware 547 ; ouverture depuis Jarvis FR non encore validée physiquement.
- Deux boutons directs « XIAOKA » et « CLIM OEM » permettent de valider l'envoi sans passer d'abord par la reconnaissance vocale.
- Le pipeline Vosk français et les réponses françaises existants sont réutilisés. Aucun changement de firmware, Xiaoka, bases NLG, système ou modèles ASR constructeur.

## Sécurité et limites
- Ouvrir le panneau HVAC NE signifie PAS activer la climatisation.
- Les phrases `active la climatisation`, `ouvre la vitre` restent dans la politique d'actions physiques bloquées.
- Aucune insertion d'intention au NLU Xiaoka, aucune commande Binder non documentée, aucun appel `setFunctionValue`, aucun service constructeurs démarré arbitrairement.
- Une Activity `exported` peut quand même refuser `startActivity` ; Jarvis capture le refus.
- La confirmation « ouverture demandée » atteste seulement de l'envoi Android, pas d'une action sur le véhicule.
- La communication bidirectionnelle des commandes automobiles en français n'est PAS réalisée par cette étape.

## Validation à l'arrêt, après build CI réussi
1. Vérifier que Xiaoka d'origine fonctionne inchangé.
2. Installer seulement une nouvelle APK test si l'utilisateur approuve l'essai.
3. Appuyer sur « XIAOKA » : vérifier la présence du panneau vocal OEM, puis revenir manuellement à Jarvis.
4. Appuyer sur « CLIM OEM » : vérifier l'ouverture de l'interface HVAC, **sans changer de consigne ni commander un accessoire**.
5. Tester « ouvre Xiaoka » et « affiche la climatisation » avec Vosk français après avoir vérifié le micro.
6. Tester la séparation : « allume la climatisation » doit rester bloqué, « ouvre la vitre » ne doit pas produire de mouvement.
7. Exporter le diagnostic via l'option intégrée en cas de message « Interface constructeur introuvable » ou « Autorisation Android refusée ».
8. Aucun test lorsque le véhicule se déplace.

## Suite : contrôle constructeur, non inclus
Cartographier uniquement des interfaces officiellement exportées et permises ou identifier un protocole de transmission d'intentions clairement autorisé. Valider d'abord les lectures, puis chaque écriture non critique véhicule arrêté, après accord explicite, avec retour réel et vérification des équipements de la finition.
