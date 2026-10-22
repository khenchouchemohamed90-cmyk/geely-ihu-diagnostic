# Jarvis FR 0.9.4.1 : correction du transfert du microphone vers Xiaoka

## Symptom confirmé par la photo utilisateur
Xiaoka affiche : 麦克风被其他应用占领，请稍后再试 (« Le microphone est occupé par une autre application. Réessayez plus tard. »), alors que Jarvis affiche « Ouverture demandée : Xiaoka ».

## Cause plausible établie dans les sources
MainActivity.stopSpeech() envoie une annulation asynchrone au DiagnosticSpeechService (capture en thread séparé). Dans MainActivity.dispatch(), l'ouverture d'Activity OEM était immédiate, avant que AudioRecord.stop()/release() se terminent. Aucun manque de permission OEM n'est impliqué par cette erreur.

## Modification
- Pour l'intention « Ouvre Xiaoka » uniquement, si une capture Jarvis est encore enregistrée, reporter l'ouverture jusqu'aux callbacks DiagnosticSpeechService.whenStopped(), qui s'exécutent après AudioRecord.release().
- Ne pas bloquer le fil UI (ni Thread.sleep ni join).
- Annuler les ouvertures en attente lorsque l'utilisateur arrête la capture, relance l'écoute, quitte l'application ou envoie une autre commande.
- Consigner « MIC_HANDOFF » dans le journal.
- Aucun changement à Xiaoka, aux permissions, aux services ECARX, ni aux actions véhicule.
- APK distincte pour ne pas toucher à Jarvis 0.9.4 déjà installée.

## Validation stationnaire
1. Tester « Ouvre l'assistant » depuis le micro de Jarvis, sans maintenir une deuxième capture.
2. Le journal doit afficher « MIC_HANDOFF AudioRecord libéré » avant l'ouverture de Xiaoka lorsque la capture était active.
3. Xiaoka devrait pouvoir prendre le microphone. Si l'erreur persiste même après fermeture complète de Jarvis, examiner les autres applications accédant au micro.
4. Vérifier que les commandes HVAC physiques sont toujours bloquées et que « Affiche la climatisation » ouvre seulement l'écran.
