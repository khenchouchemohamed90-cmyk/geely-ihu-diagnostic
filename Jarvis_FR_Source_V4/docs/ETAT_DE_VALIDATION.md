# Niveau de preuve au 17 octobre 2026

| Élément | Provenance | Statut |
|---|---|---|
| Vosk FR disponible | archive AZ Voice / ressources fournies | confirmé, embarqué dans cette base |
| Jarvis russe allume la climatisation | observation utilisateur (voyant A/C) | confirmé sur ce véhicule |
| Jarvis appelle BCM_FUNC_WINDOW_POS zone 16 consigne 100 | log `1.txt` | confirmé, sans mouvement |
| Jarvis `setAcOn(boolean)` contient 0x10010300 ; `setClimateSystemOn(boolean)` contient 0x10010100 | inventaire DEX fourni | confirmé ; l'appel réel pour la voix reste non démontré |
| Fonctions `setAcOn`, `isAcOn`, `setFanSpeed` existent | tables DEX | confirmé (signatures) |
| Source Jarvis 2.8 officielle publique | recherches GitHub/web | non retrouvée |
| APK complète FR | — | NON CONSTRUITE |
| Projet source minimal recompilé et testé sur IHU007B | — | NON TESTÉ |

| Route AC originale (`кондиционер`) | graphe DEX `tools/trace_hvac.py` | `handleAc` / `setAcOn` identifié statiquement |
| Parser français : 22 cas | `bash tools/test_parser.sh` (JDK) | 22 réussites, commandes véhicule non exécutées |

## Vérifications V2 (17 octobre 2026)

| Élément | Résultat | Portée |
|---|---|---|
| Analyse d'intentions FR | 22/22 OK | Machine de développement, sans APK |
| Rejets des résultats Vosk en double | 6/6 OK | Machine de développement |
| ECARX simulé : lecture + statut + réutilisation de connexion + interdiction d'écriture | 6/6 OK | **Simulation uniquement**, pas de véhicule |
| Compilation d'une véritable APK Android | Non réalisée | SDK Android indisponible localement |
| Reconnaissance vocale FR sur IHU007B | Non testée | Ne pas conclure |
| Permissions et classes ECARX accessibles au nouvel APK | Non testées | La réflexion peut échouer à l'exécution |
| Activation A/C par l'APK reconstruite | **Désactivée** | L'observation Jarvis russe ne prouve pas les droits de cette APK |

Il ne faut pas distribuer cette base comme « Jarvis complet en français ».


## V3 — extension des intentions et actions Android standard (17 octobre 2026)

| Contrôle | Résultat |
|---|---|
| Reconnaissance d'intentions FR | 46/46 tests locaux OK |
| Anti-doublons Vosk et session d'écoute | 7/7 tests locaux OK ; bug de session corrigé dans MainActivity |
| ECARX lecture seule (service simulé) | 6/6 OK |
| Audio/launch Android simulés | 8/8 OK |
| Test de l'application sur IHU007B | NON EFFECTUÉ |
| Compilation APK Android | NON EFFECTUÉE |
| Fonction automobile nouvelle commandée par V3 | AUCUNE — écriture désactivée |

Les tests ci-dessus n'attestent ni du déclenchement physique de l'A/C par Jarvis FR, ni de la compatibilité des médias/nav sur l'appareil. Voir MATRICE_FONCTIONS_IHU007B_V3.md.
