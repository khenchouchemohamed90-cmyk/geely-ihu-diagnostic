# Jarvis FR — reconstruction fonctionnelle et préparation à une distribution commerciale

Établi le 17 octobre 2026 sur la base des APK Jarvis 2.8 Lite, Jarvis FR 0.5.0, AdapterAPI.jar/AdapterAPIImpl.jar, des journaux IHU007B et des analyses du code DEX. **Ce document est un cahier de validation, pas une attestation de produit fini.**

## Objectif produit
Créer une application originale Android 9/API 28 pour le système **Geely Emgrand IHU007B**, en français, hors ligne pour la reconnaissance, qui reprenne toutes les fonctionnalités réellement accessibles à une APK non privilégiée. L'application doit préserver le Jarvis russe installé, disposer d'une signature de distribution stable et désactiver automatiquement les fonctions non prouvées pour le profil et le firmware détectés.

Le prototype 0.5.0 de diagnostic ne constitue **pas** cette application : il ne gère pas l'ensemble des familles Jarvis, ne peut pas contrôler physiquement l'A/C et ne dispose pas d'un assistant système/éveil validé.

## Provenance — comparaison des comportements

| Domaine | Preuve de l'APK russe | Preuve IHU007B | État Jarvis FR 0.5.0 | Exigence produit |
|---|---|---|---|---|
| Autonomie | `getEnduranceMileage()->float` appelle `ISensor.getSensorLatestValue(0x00100800)` | 242 km lus et observés précédemment ; la V5 utilisait à tort ICarFunction | **Chemin ISensor corrigé sur branche dédiée**, revalidation physique requise | Valeur réelle, erreur explicite, aucun zéro inventé |
| A/C marche/arrêt | `setAcOn(boolean)` → `setFunctionValue(0x10010300, valeur)` | Commande « Кондиционер » allume réellement le voyant A/C | Intention FR reconnue, action physique désactivée | Appel autorisé uniquement après contrôle droits, résultat et retour d'état, essais à l'arrêt |
| Mise en marche HVAC global | `setClimateSystemOn(boolean)` → `0x10010100` | Pas d'essai spécifique isolé | Désactivée | Ne pas confondre avec compresseur A/C |
| Température / ventilation / auto | `setTemperature`, `setFanSpeed`, `setAcAuto`, `getTemperature`, `getFanSpeed` | Non validés séparément | Intention FR reconnue, aucune écriture | Vérifier unités, limites, consignes, retour et sécurité |
| Température extérieure | `getAmbientTemperature` appelle `ISensor.getSensorLatestValue(0x00100B00)` | Lecture russe parfois discordante | Indisponible | Calibration et comparaison du tableau de bord |
| Vitesse | `getCarSpeed` appelle `ISensor.getSensorLatestValue(0x00100100)` | ID vu dans logs constructeur | Non intégré | Lecture seulement ; unités/vérification |
| Niveau de carburant | `getFuelLevel` lit notamment `ISensor(0x00100600)` et `ISensor(0x00404500)` | Jarvis russe a répondu « niveau indéterminé » | Indisponible | Ne pas commercialiser cette valeur tant qu'elle n'est pas vérifiée |
| Navigation | Règles Jarvis russe | « Ouvre la navigation » écrit ouvre Amap sur IHU007B | Fonctionne par texte | Voix, destinations, échecs, sans distraction |
| Multimédia | Player, lecture, pause, piste, volume, applications | Non validé au-delà du code | Actions Android de base | Tester avec chaque lecteur réellement installé |
| Téléphone/contacts | Modules et règles dans Jarvis russe | Pas de validation sur ce firmware | Ouverture éventuelle du dialer seulement | Permissions explicites, confirmation d'appel, Bluetooth |
| Mot d'éveil / arrière-plan | Interface et options dans Jarvis 2.8 | Droits système ECARX non prouvés | Pas de service d'assistant complet | Réveil fiable et contrôle de consommation, sans capture non consentie |
| Vitres | `setWindow(0x21030300,zone,position)` ; zone pilote 0x10 | Le BCM a reçu l'appel ; **vitre immobile** | Désactivé | Pas de commande disponible avant preuve physique sûre |
| Toit ouvrant / rideau | Méthodes présentes | Non testé / profil « Other » retombe sur Atlas | Désactivé | Blocage par défaut et profil Emgrand vérifié |
| Coffre, sièges, éclairage, rétroviseurs, dégivrage, essuie-glaces | Méthodes dans `EcarxApiHelper` | Compatibilité Emgrand non établie | Désactivés | Essais spécifiques, permissions et garde-fous, sans présomption |

**Constat important :** `other_test → atlas` dans certaines branches du code russe. Ne jamais reprendre cette correspondance pour appliquer arbitrairement une commande Atlas à une Emgrand.

## Architecture commerciale cible

1. **Noyau de dialogue FR :** moteur hors ligne Vosk, grammaire explicitement sûre, support de variantes des phrases, hypothèses/logs contrôlés et retour texte.
2. **Gestion TTS :** moteur français local vérifié, option muette, sélection et volume utilisateur, erreurs intelligibles.
3. **Service utilisateur :** activation par geste/voix si autorisée, foreground service approprié, notification, pause micro sur appels et contrôle d'audio focus. Assistant Android par défaut seulement si les interfaces Android 9 / ECARX l'autorisent.
4. **Connecteurs indépendants :** navigation Amap, lecteur actif, contacts/téléphone, applications.
5. **Passerelle ECARX originale :** séparation `ISensor` (float, télémétrie) / `ICarFunction` (fonctions), connexion surveillée, vérification de prise en charge, traitement exceptions/permissions, retour de commande et vérification physique.
6. **Profils de compatibilité :** `Emgrand/IHU007B/Android9/variante-firmware`. Toute fonction inconnue reste désactivée. Refus explicite sur autres modèles non certifiés.
7. **Distribution :** clé de signature stable protégée hors du dépôt, versionnage/migrations, contrôles de mises à jour, journal privé borné, politique de confidentialité et consentement micro/contacts, manuel d'installation/désinstallation.

## Critères avant mention « fonctionnel » et vente

- **Reproductibilité :** compilation release reproductible sur GitHub Actions, signature stable, contrôles ZIP/DEX/API28, SBOM et licence des dépendances.
- **Reconnaissance :** tests réels de commandes FR, accents et bruit d'habitacle, faux positifs et négations ; commandes dangereuses jamais déclenchées par la parole mal comprise.
- **Télémétrie :** chaque valeur compare au tableau de bord sur plusieurs sessions, avec unité et borne validées, journalisant les statuts ECARX sans falsification.
- **A/C :** commande volontaire à l'arrêt, allumage réel, confirmation d'état et refus correctement rapportés ; ne jamais annoncer la réussite sur simple appel logiciel.
- **Portabilité :** essais sur plusieurs Emgrand/firmwares, vérification des variantes manuelle/automatique, matrice des fonctions individuellement supportées.
- **Sécurité conducteur :** navigation et téléphonie compatibles sans distraire, contrôles sensibles bloqués durant circulation ou quand état de véhicule inconnu.
- **Licences :** ne pas intégrer ni redistribuer le code/les icônes/les voix de Jarvis russe sans autorisation/licence. Une réimplémentation originale peut être vendue sous réserve de conformité et de droits. Le modèle Vosk `vosk-model-small-fr-0.22` est listé Apache 2.0 ; conserver licences et mentions. Évaluer le nom commercial « Jarvis » avant usage public.
- **Livraison :** APK release complète, notes utilisateur, liste des fonctions par variante, politique confidentialité, procédure support, méthode de restauration.

## Prochaines réalisations techniques

- Corriger en priorité le chemin d'autonomie avec le DEX source vérifié, puis valider sur tablette.
- Reconstruire les modules téléphone/contacts, lecteur, navigation et raccourcis indépendamment des fonctions constructeur.
- Reproduire le comportement A/C contrôlé de Jarvis russe, sans supposer les permissions accordées au nouveau paquet ; ne débloquer l'écriture qu'après procédure de test et contrôles de sécurité.
- Terminer le service de reconnaissance/voix au-delà du mode de diagnostic 15 s.
- Préparer des profils Emgrand et une campagne d'essais sur au moins plusieurs véhicules avant d'autoriser une commercialisation.

## Limite de droits

L'application russe originale est une référence de comportement et d'identifiants, **pas** une bibliothèque à intégrer ou une APK à redistribuer commercialement sans accord. Les éléments spécifiques constructeur sont des interfaces à la disponibilité variable ; leur présence n'autorise pas à contourner les permissions Android.
