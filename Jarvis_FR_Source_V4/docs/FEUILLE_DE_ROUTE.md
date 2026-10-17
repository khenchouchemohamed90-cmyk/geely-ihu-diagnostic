# Feuille de route vers un Jarvis FR réellement complet

## 1. Rétro-analyse de Jarvis 2.8 (APK original)
- Reconstituer avec JADX la logique de `org.jarvis.VoiceService`, du routeur vocal, du dictionnaire, de la synthèse et de `EcarxApiHelper` ;
- Association statique **identifiée** : `Кондиционер` -> `handleAc` -> `setAcOn()` -> `0x10010300`. L’utilisateur confirme que le voyant A/C s’est allumé physiquement sans ouverture de l’écran HVAC ;
- Ajouter la vérification de réponse aux commandes et interdire tout faux succès ;
- Déterminer les éléments encore cachés dans les `resources.arsc` et layouts.

## 2. Source français stable
- Implémenter tous les écrans en français, menus, modules et paramètres ;
- Modèle Vosk FR + dictionnaire FR et grammaire FR ;
- Synthèse vocale française ;
- Mapping d'intentions séparé des actions techniques ; éviter les faux positifs dangereux ;
- Gestion de l'écoute au démarrage et en arrière-plan uniquement après contrôle des permissions Android 9.

## 3. Compatibilité ECARX IHU007B
- Consulter `isFunctionSupported` avec le service connecté, ne pas interpréter "0" comme succès systématiquement ;
- Lecture d'autonomie et de statut A/C comme tests de base ;
- N'activer l'écriture A/C qu'après avoir identifié le chemin exact et confirmé une validation de résultat ;
- Conserver les commandes vitres/toit DÉSACTIVÉES (échec confirmé des vitres, toit non validé) ;
- Pas de modifications du firmware, d'ADB permanent, ni d'élévation de privilèges.

## 4. Fin de projet
- APK signée avec l'identité propre du projet ; test de démarrage, reconnaissance, TTS, retour de service ECARX ;
- Test utilisateur sur véhicule stationné uniquement ;
- N'annoncer "Jarvis FR complet" qu'après audits d'interface, commandes et compatibilité sur IHU007B.


## Extension aux fonctions compatibles (ajout V3)
1. Tester la compilation réelle et le démarrage, corriger les dépendances ou permissions au besoin.
2. Vérifier sur tablette les fonctions Android non destructives : musique, ouverture navigation, navigateur, composeur (sans appel).
3. Vérifier uniquement en lecture l'état de support ECARX : A/C, HVAC global, ventilateur, température ; interpréter les unités via le DEX.
4. Autoriser individuellement une commande ECARX **uniquement après validation de son retour** et essai stationné. Ne jamais faire d'activation générale par simple correspondance du nom d'une méthode.
5. Ajouter ensuite les fonctions réelles constatées disponibles. Ne pas présenter les fonctions non disponibles comme des commandes actives.
