# Jarvis FR 0.5.0 — diagnostic après essai réel IHU007B

## Ce qui est confirmé par votre essai V4

L’APK démarre sur la vraie tablette Android 9, son interface française fonctionne et la saisie « Ouvre la navigation » lance Amap. La voix ne déclenche aucune action et aucune transcription n’est affichée ; la synthèse est déformée ; l’autonomie renvoie `notavailable`, alors que Jarvis russe sait la lire.

Ces observations sont des validations utilisateur de V4. Les corrections ci-dessous sont testées sur machine de développement et en CI, **pas encore sur la tablette**. La disposition, les boutons et le champ de saisie existants sont conservés. Le parseur français et `AndroidActions` n’ont pas été modifiés : le chemin écrit déjà opérationnel d’Amap reste le même.

## Corrections de reconnaissance

- Remplacement de la capture Vosk Android fixe par une capture PCM16 instrumentée. En automatique, la source standard MIC est essayée avant VOICE_RECOGNITION et DEFAULT ; les formats sont 16 kHz, 48 kHz puis 44,1 kHz. Le repli se fait sur échec réel d’initialisation/démarrage, jamais sur la simple présence de silence.
- Si une autre fréquence est ouverte, conversion continue vers 16 kHz avant Vosk. Aucune modification du modèle fourni, de sa configuration ou de ses empreintes.
- Lecture audio non bloquante, délai mural de 15 secondes, erreurs de lecture explicites et finalisation Vosk avant le signal d’expiration. Un appel natif propriétaire déjà bloqué ne peut pas être interrompu de force.
- Hypothèses partielles affichées dans le résultat existant, sans exécuter de commande. Seuls les résultats finalisés et reconnus peuvent déclencher le même routeur que la saisie. Une phrase inconnue à un premier endpoint ne consomme plus prématurément la session. Les négations, commandes composites et `[unk]` restent refusés.
- Reprise de l’écoute demandée après autorisation du microphone si l’application est toujours au premier plan ; aucune écoute permanente ni reprise générale en arrière-plan.
- Annulation non bloquante ; le worker ferme son AudioRecord et son Recognizer, puis autorise la libération du modèle. Plusieurs activations rapides ne ferment pas les ressources encore utilisées.
- Journalisation du format ouvert, de la source, du périphérique réellement routé et des microphones actifs quand le système les expose, des niveaux RMS/crête, des échantillons nuls/saturés, des erreurs et de chaque étape de transcription.

Une source peut être initialisée avec succès tout en recevant des zéros à cause du routage constructeur, de la confidentialité ou d’un autre assistant actif. Cela reste à diagnostiquer sur IHU007B. Le journal indique aussi l’état muet système, sans le modifier.

## Synthèse française et silence

Le mode silencieux est activé à chaque démarrage. Le contrôleur inspecte les voix déclarées installées, choisit explicitement une voix locale française `fr-FR` ou `fr` sans dépendance réseau, vérifie la voix effectivement renvoyée et fixe hauteur/débit à 1,0 avec sortie MEDIA/SPEECH. Il n’utilise plus un simple `setLanguage` avec repli implicite vers une autre voix.

Un échantillon doit se terminer sans erreur du moteur, puis l’utilisateur doit demander explicitement l’activation des réponses. Cette confirmation porte sur ce que vous avez entendu : un retour logiciel réussi ne prouve pas une bonne qualité sonore. Si aucune voix admissible n’est installée, l’application reste silencieuse. Aucune installation de moteur, modification du volume, du mode audio système ou des permissions système n’est effectuée.

## Commandes de diagnostic dans le champ existant

Ces commandes se saisissent comme « Ouvre la navigation », sans nouveau bouton. Les réglages audio ne sont jamais activés à partir d’une hypothèse vocale.

| Saisie exacte | Effet |
| --- | --- |
| `Diagnostic vocal` | Affiche autorisation, modèle, dernière hypothèse, état audio et voix choisie |
| `Microphone automatique` | Essais MIC / VOICE_RECOGNITION / DEFAULT sur erreurs d’ouverture |
| `Microphone standard` | Source MIC uniquement, fréquences compatibles possibles |
| `Microphone reconnaissance` | Source VOICE_RECOGNITION uniquement |
| `Microphone système` | Source DEFAULT uniquement |
| `Tester la voix française` | Joue un échantillon explicite, garde les réponses silencieuses |
| `Activer les réponses vocales` | Votre accord après un test terminé ; aucune activation automatique |
| `Mode silencieux` | Arrête la voix et garde les réponses affichées |
| `Diagnostic ECARX` | Inventorie les classes, permissions de notre paquet et signatures disponibles, sans lire ni écrire de consigne |
| `Exporter le diagnostic` | Prépare une copie locale et ouvre le partage Android à votre demande |

Le journal reste privé à notre paquet. Il contient des métadonnées et transcriptions, **aucun audio brut** et aucun envoi réseau automatique. Taille bornée à deux générations de 256 Kio, avec instantané exporté en lecture seule. Les tags logcat sont `JarvisFR` et `JarvisAudio`. Le partage peut être indisponible si la tablette ne possède aucune application cible ; le journal privé reste alors présent. Les textes saisis et reconnus peuvent figurer dans l’export : vous choisissez sa destination.

## ECARX : correction du diagnostic, autonomie encore à établir

La méthode russe `getEnduranceMileage()` est inventoriée avec retour `float`. Son corps et son identifiant sous-jacent ne figurent pas dans les pièces fournies. L’identifiant V4 `0x00100800` n’est corroboré par aucun extrait brut fourni : il reste un candidat non validé, pas une preuve de lecture correcte d’autonomie. Aucune formule ni unité n’est inventée.

La passerelle journalise désormais les permissions déclarées/accordées à **notre APK**, bibliothèques visibles, classloaders, signatures des interfaces Car et capteurs, identifiant/zone, type et valeur du support, getter choisi et exception. Elle utilise le getter à un argument uniquement si celui à deux arguments est absent, avec préférence pour la surcharge de support correspondante. Il n’y a aucun second essai pour contourner un refus ou une erreur de permission.

`notavailable` reste indisponible, jamais zéro. Le contournement du statut ne corrigerait pas une mauvaise API ou un mauvais identifiant. Pour résoudre la lecture réelle, il faut examiner l’export V5 sur votre tablette ou disposer du corps DEX original de la méthode russe. La réflexion seule ne donne pas les permissions du Jarvis russe et les signatures de capteurs ne prouvent pas leurs identifiants.

`setAcOn()` / `0x10010300` et `setClimateSystemOn()` / `0x10010100` restent des références statiques ; **aucune écriture ECARX n’est ajoutée**, ni pour A/C, ni pour vitres, toit, sièges, coffre ou autres équipements. Jarvis russe, le firmware et les permissions système restent inchangés.

## Validation et prochain essai

Validation locale : **497 vérifications réussies**, compilation réelle `assembleDebug`, Lint sans erreur ni avertissement, signature/alignement API 28 et 15/15 fichiers du modèle dans l’APK contrôlés. Le seul contrôle Lint désactivé reste la politique Google Play sur la cible 28, comme en V4.

Le script `sh tools/build_diagnostic.sh --offline` comprend maintenant les tests du parseur, des sessions, d’ECARX simulé, des actions Android, de la capture simulée, du rééchantillonnage, de la sélection TTS et des journaux. Le workflow GitHub exécute aussi le chargement natif Linux du modèle et le rejet du silence, puis `assembleDebug`, Lint, les contrôles de signature API 28, d’alignement et des 15 fichiers Vosk dans l’APK. Les simulations ne valident pas le microphone réel ou l’acoustique de la voix.

Sur tablette : commencer par « Ouvre la navigation » en saisie, puis « Diagnostic vocal ». Essayer une phrase avec la source automatique, noter le signal et l’hypothèse affichés. Si le signal reste nul ou le texte absent, comparer explicitement les trois sources proposées en relançant une écoute pour chacune. Ne pas désactiver la confidentialité ni modifier les permissions système pour forcer une source. Tester ensuite la voix uniquement à votre demande ; conserver le silence si elle est encore déformée.

Pour ECARX, lancer « Diagnostic ECARX », puis le bouton d’autonomie. Exporter ensuite le journal. Le message exact permettra de distinguer absence de classe, service non prêt, permission refusée, support négatif ou API incorrecte.

L’APK conserve le paquet distinct `com.aztech.jarvisfr.reconstruction`, min/cible Android 9 API 28, avec code de version 6. Les clés de diagnostic générées sur des runners différents peuvent différer : si Android refuse la mise à jour pour signature incompatible, seule l’ancienne application **Jarvis FR** devra être désinstallée, après export des journaux utiles. Ne pas désinstaller ni remplacer Jarvis russe. Le maintien d’une clé de distribution stable sera un travail séparé.
