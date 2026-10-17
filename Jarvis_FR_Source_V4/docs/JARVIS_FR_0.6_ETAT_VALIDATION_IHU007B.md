# Jarvis FR 0.6 — état et protocole de validation IHU007B

**Version :** 0.6.0-essai-IHU007B (versionCode 8) — octobre 2026.
**Matériel ciblé :** Geely Emgrand / SS11, tablette IHU007B Android 9. Le firmware extrait du Drive est **298**, celui signalé sur la voiture est **547**.

## Sources techniques comparées

- Jarvis russe 2.8 Lite, DEX : `ISensor.getSensorLatestValue(0x00100800)` (autonomie), `0x00100B00` (température extérieure), `0x00100100` (vitesse).
- VOiSK 2.1.6 : concepts observés de reconnaissance hors ligne, traitement de niveau audio, macros. **Aucun code, modèle russe ou ressource propriétaire VOiSK n'est intégré dans cette APK.**
- Firmware IHU007B v298 : `AdapterAPIImpl.jar`, `com.ecarx.btphone`, `com.ecarx.multimedia`, `com.autonavi.amapauto`, `XCHvac`, `XSFEASVRService`.
- Traces appareils IHU007B v547 : voix russe (autonomie 242 km et A/C activée) et anomalies Jarvis FR précédentes.

## Fonctions de cette APK

| Fonction | État |
|---|---|
| Interface Jarvis FR sombre avec micro, saisie, actions rapides, réglages avancés | Implémentée, compilation passée |
| Reconnaissance de commandes en français, hors ligne, modèle Vosk | Implémentée, test hôte réussi, **micro véhicule à retester** |
| Gain logiciel PCM 16 kHz et source d'entrée sélectionnable | Implémentés, tests hôte ; amplification n'assure pas la suppression du bruit |
| Navigation AutoMap/Amap si installée | Code opérationnel et précédent essai par saisie positif ; test vocal requis |
| Multimédia lecture/pause/suivant | Envoi de touches multimédia Android ; résultat OEM à vérifier |
| Autonomie (ISensor 0x00100800) | Lecture seule ; firmware 547 retourne actuellement une valeur rejetée ; vérifier brute |
| Température extérieure et vitesse ISensor | Nouveaux accès en lecture seule ; unités et compatibilité non validées |
| État brut de climatisation | Lecture seule ; pas d'activation |
| Téléphone via commande | **Désactivé** suite au blocage du composeur OEM observé |
| Climatisation / vitres / toit / chauffage | **Désactivé** : écriture ECARX non validée |
| Réponses vocales | Option uniquement si une vraie voix fr-FR/fr locale est disponible et testée |
| Écoute permanente au mot clé / contacts / destinations / macros | **Non implémentés** |

## Essai sûr, véhicule immobilisé

1. Conserver le Jarvis russe ; ne désinstaller que l'ancien **Jarvis FR** en cas de conflit de signature debug.
2. Installer l'APK 0.6 puis vérifier l'affichage et le chargement du modèle hors ligne.
3. Lancer **Navigation** avec l'écran (ouvrir Amap), revenir à Jarvis ; confirmer le retour.
4. Lire l'autonomie, la température extérieure et la vitesse **au point mort / véhicule à l'arrêt**. Photographier valeurs brutes et messages d'erreur ; comparer aux valeurs du véhicule seulement après confirmation des unités.
5. Appuyer sur **Parler à Jarvis**, tester « Ouvre la navigation », « Autonomie restante » ; ne pas demander d'écriture automobile.
6. Dans **Réglages**, sélectionner successivement les sources AUTO, VOICE_RECOGNITION et MIC si le niveau est trop faible ; noter le résultat de **Diagnostic vocal**. Le gain logiciel peut être décoché pour comparaison.
7. Exporter les journaux avec **Exporter le journal**.

## Captures ADB (lecture seule)

Sur le shell de la tablette, exécuter :

```sh
logcat -d -v time -s JarvisAudio:I JarvisFR:I AndroidRuntime:E '*:S' | tail -n 250
dumpsys activity activities | grep -Ei 'aztech|jarvis|amap|phone|resumed|task'
getprop ro.build.fingerprint
```

## Statut de livraison

**APK d'essai avancé, pas version commerciale certifiée.** Le CI vérifie compilation, signature debug, présence du modèle Vosk et tests hôte simulés ; il ne valide pas la communication ECARX ni les commandes vocales sur un véhicule physique.

N'exécuter aucune action en conduisant. Ne pas flasher le firmware 298 sur un appareil 547.
