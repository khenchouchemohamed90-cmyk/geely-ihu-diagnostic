# Jarvis FR 0.9.7 — prototype audio français → Xiaoka chinois (IHU007B/SS11/547)

## Point de départ prouvé sur la voiture
- Xiaoka comprend et exécute **打开空调** (activation A/C) et **关闭空调** (arrêt A/C) quand le propriétaire les fait prononcer en mandarin depuis un téléphone.
- Jarvis FR français reconnaît « Ouvre l'assistant », peut ouvrir Xiaoka et libère correctement son microphone avant l'activité constructeur (0.9.4.1).
- L'écriture directe par `ICarFunction.setFunctionValue(0x10010300,0,1)` depuis l'APK Jarvis 0.9.6 a rendu `FALSE`. Aucun contrôle physique réussi depuis le paquet tiers.
- Il reste à valider **une nouvelle condition** : Xiaoka peut-il reconnaître une phrase en mandarin *émise par les haut-parleurs de la même tablette* ?

## Nouveauté 0.9.7
- Ajout, dans « Réglages, audio et diagnostic », de deux boutons avec confirmation séparée : « Test relais Xiaoka : A/C MARCHE (chinois) » et « A/C ARRÊT (chinois) ».
- Suite à la confirmation : arrêt du micro et des réponses vocales de Jarvis, attente du callback AudioRecord.release() si nécessaire, vérification qu'une voix chinoise existe via Android TextToSpeech, ouverture de l'activité OEM Xiaoka, puis prononciation **une seule fois** de la phrase chinoise après 1,9 seconde.
- Aucune commande constructeur directe : ni SDK ECARX, ni Binder, ni méthode ICarFunction, ni injection de paquet Xiaoka, ni changement de fichier système ou de permissions.
- Retour détaillé au journal sous `MANDARIN_RELAY` : disponibilité de la voix chinoise Android, lancement Xiaoka, entrée/sortie TTS. **La synthèse TTS terminée ne signifie pas que Xiaoka a entendu ou exécuté l'ordre**.
- Les commandes vocales françaises d'activation/désactivation restent désactivées dans cette preuve de concept. L'utilisateur effectue un test manuel explicite.

## Étapes de test à l'arrêt
1. Installer l'APK `Jarvis FR 0.9.7 - Relais` en parallèle de 0.9.5/0.9.6, véhicule stationné, frein serré. Éviter de laisser le moteur tourner en milieu non ventilé.
2. Ouvrir Jarvis, puis « Réglages, audio et diagnostic ».
3. L'A/C étant initialement éteinte, toucher « Test relais Xiaoka : A/C MARCHE (chinois) » ; lire et confirmer la fenêtre.
4. Observer **si Xiaoka s'ouvre**, si la **voix chinoise est audible via les haut-parleurs de la tablette**, si Xiaoka transcrit **打开空调**, et si l'A/C s'active physiquement. Décrire ces 4 résultats séparément.
5. Si A/C réellement activée, le bouton A/C ARRÊT peut être testé après confirmation, sinon utiliser la commande constructeur habituelle. Ne pas multiplier les essais en cas d'échec.
6. Revenir à Jarvis pour voir la section RÉPONSE ou exporter le journal `MANDARIN_RELAY`.

## Limitations à respecter
- Android standard TTS peut ne **pas** exposer de voix chinoise même si Xiaoka parle mandarin via un moteur propriétaire. Un refus ne signifie pas que la passerelle est impossible, seulement qu'il faudra fournir des phrases audio localement ou trouver l'API TTS OEM autorisée.
- L'annulation d'écho, les réglages AudioFocus, le routage des flux et la prise du micro peuvent empêcher Xiaoka de capter les propres haut-parleurs de l'IHU. Aucun contournement de droits ni injection forcée n'est tenté.
- **L'objectif final français → intention NLU constructeur** demanderait la preuve d'une interface d'injection ouverte à Jarvis ; non prouvé à cette étape.
- Ne jamais annoncer « A/C activée » d'après la seule fin TTS. Le conducteur valide la commande visuellement.

## Identité
Application ID `com.aztech.jarvisfr.xiaoka.relay547`, version `0.9.7-xiaoka-audio-bridge-547`, Android 9.
