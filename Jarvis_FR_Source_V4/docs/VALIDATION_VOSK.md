# Vérification Vosk français — 17 octobre 2026

Les 15 fichiers du modèle fusionné sont présents, pour **68 483 828 octets** (65,31 Mio). Les 15 SHA-256 correspondent au manifeste V4 fourni, `analysis/SHA256_MODELE_FR.txt` ; il ne s'agit pas d'une attestation indépendante de l'origine du modèle. Aucun fichier supplémentaire n'est présent dans `model-fr`. `ivector/online_cmvn.conf` est intentionnellement vide : son empreinte attendue est celle d'un fichier vide. Le fichier `uuid` contient `fr-0.22-standalone-1`. La configuration MFCC demande 16 000 Hz, 40 coefficients / 40 bandes ; les graphes HCLr/Gr et l'extracteur i-vector se chargent réellement.

## Résultats obtenus

- `python3 tools/verify_model.py` : code 0, `ok: true`, 15/15 empreintes conformes. Le script utilise un SHA-256 par blocs, compatible Python 3.8+.
- Vérification négative en copie temporaire : modification de `uuid` détectée comme empreinte différente ; suppression de `uuid` détectée comme fichier absent. Les fichiers du projet n'ont pas été altérés.
- `python3 -m py_compile tools/verify_model.py tools/test_vosk_host.py` : code 0.
- Python Vosk **0.3.45**, bibliothèque native Linux x86_64 du wheel PyPI : chargement réel réussi ; trois secondes PCM16 de silence donnent un texte vide, en modes libre et guidé.
- Bibliothèque chargée : `libvosk.so`, SHA-256 `85c4654de3acdeb99abab86eeb2a6e603927d37089597c0fcc33d8638dc2ccaf`. L'API n'expose pas un numéro de version natif distinct ; la version indiquée est celle du paquet Python qui la fournit.
- Android utilise `vosk-android:0.3.47` : les essais Linux ne valident **pas** cette bibliothèque JNI Android, le dépaquetage Android, ni le microphone IHU007B.

## Corpus synthétique : comparaison des intentions

Douze phrases ont été synthétisées par **eSpeak NG 1.51**, voix `fr-fr`, six à 140 mots/min et six à 175 mots/min, puis converties par FFmpeg en PCM16 mono 16 kHz, avec une seconde de silence final. Onze de ces phrases correspondent à une intention prise en charge par l'analyseur ; « monte le volume » est hors fonctionnalités et reste inconnue. Dix autres phrases contrôlent les négations, les commandes multiples et des paroles sans rapport avec les fonctions de l'application.

Les transcriptions sont passées au **vrai `FrenchIntentParser.java`** compilé avec le JDK 17, via un harnais Java indépendant ; aucune copie Python de ses règles n'a été utilisée. Une sortie guidée contenant `[unk]` est traitée comme inconnue avant l'analyse, conformément au routage Android.

| Mesure sur le même corpus | Mode libre | Mode guidé |
| --- | ---: | ---: |
| Intention positive correcte (11 phrases) | 2/11 | 7/11 |
| Transcription positive exactement conforme | 0/11 | 5/11 |
| Négations / commandes composites rejetées | 7/7 | 7/7 |
| Paroles hors domaine rejetées | 3/3 | 2/3 |
| Silence de 3 s rejeté | oui | oui |

**Limite observée du mode guidé :** « bonjour comment vas tu » devient « mon autonomie » et produit une demande de lecture AUTONOMIE. Ce faux positif n'est ni corrigé artificiellement par l'ajout de cette phrase au vocabulaire, ni masqué. Le guidage reste une aide au diagnostic et doit être essayé avec la voix de l'utilisateur. Les tests ne démontrent aucune fiabilité universelle ; aucune commande physique ECARX n'a été exécutée. Les demandes de navigation et téléphone ne sont pas validées par ces échantillons synthétiques, car leur début est mal transcrit.

| Phrase attendue | Intention attendue | Transcription libre | Transcription guidée | Intention guidée |
| --- | --- | --- | --- | --- |
| ouvre la navigation | OUVRIR_NAVIGATION | a la barbie martiaux | à la navigation | INCONNUE |
| lance la musique | MUSIQUE_LECTURE | l'ensemble munich | lance la musique | MUSIQUE_LECTURE |
| monte le volume | INCONNUE | montrez-moi | mon mon | INCONNUE |
| ouvre le téléphone | OUVRIR_TELEPHONE | le téléphone | à ouvre le téléphone | INCONNUE |
| autonomie restante | AUTONOMIE | l'autonomie restante | mon autonomie restante | AUTONOMIE |
| éteins la climatisation | ARRETER_CLIMATISATION | c'est un art les mardis martiaux | à éteins la climatisation | INCONNUE |
| pause | MUSIQUE_PAUSE | paul | pause | MUSIQUE_PAUSE |
| piste suivante | MUSIQUE_SUIVANTE | piste alimente | piste suivante | MUSIQUE_SUIVANTE |
| piste précédente | MUSIQUE_PRECEDENTE | pistes pour nantes | piste précédente | MUSIQUE_PRECEDENTE |
| ouvre le navigateur internet | OUVRIR_NAVIGATEUR | dans le navigateur internet | à le navigateur internet | INCONNUE |
| quelle est mon autonomie restante | AUTONOMIE | kaname l'autonomie restante | quelle est mon autonomie restante | AUTONOMIE |
| quel est le niveau de carburant | NIVEAU_CARBURANT | quels aliments le carburant | quelle est le niveau de carburant | NIVEAU_CARBURANT |
| ne lance pas la musique | INCONNUE | d'abord par l'armée | lance pas la musique | INCONNUE |
| n'allume pas la climatisation | INCONNUE | harlem par la climatisation | allume pas la climatisation | INCONNUE |
| ne mets pas la musique en pause | INCONNUE | le par la munich en pause | le mets pas la musique en pause | INCONNUE |
| je ne veux pas de musique | INCONNUE | je n'aimais par dominique | je ne mets pas de musique | INCONNUE |
| allume la climatisation et éteins la climatisation | INCONNUE | parle-moi l'intégration et les mardis martiaux | allume la climatisation et éteins la climatisation | INCONNUE |
| ouvre la navigation et le téléphone | INCONNUE | a la barbie martiaux et le téléphone | à la navigation et le téléphone | INCONNUE |
| lance la musique puis ouvre la navigation | INCONNUE | à munich les beaux arts martiaux | lance la musique puis ouvre la navigation | INCONNUE |
| bonjour comment vas tu | INCONNUE | bonjour laurent normal | mon autonomie | AUTONOMIE |
| je cherche une station service | INCONNUE | je cherche une tension fermé | je [unk] [unk] | INCONNUE |
| je vais manger demain | INCONNUE | jamais mangé de ma | je mets [unk] | INCONNUE |

## Vocabulaire guidé livré

`FrenchVoiceGrammar.json()` retourne une liste déterministe de **57 phrases / 65 mots distincts**, incluant `[unk]`, des négations et des ordres composites afin de conserver leurs mots de garde. Les **65 mots sont tous présents dans le dictionnaire du modèle** : zéro mot ignoré par Vosk. Le vocabulaire couvre la lecture, pause et changement de piste ; les ouvertures navigation, composeur et navigateur ; les lectures autonomie, carburant, température extérieure et état de climatisation ; la reconnaissance des consignes climatisation, températures de 16 à 30 degrés, ventilation de 1 à 9 et demandes vitre/coffre. Ces dernières intentions restent soumises aux interdictions ECARX déjà présentes : le vocabulaire ne leur confère aucune capacité d'écriture.

Ce vocabulaire est volontairement plus limité que toutes les formulations acceptées par l'analyseur textuel : le mode libre et la saisie texte restent utiles pour d'autres paraphrases. Le mode guidé peut produire des combinaisons nouvelles de mots ; ce n'est pas une liste d'autorisations exacte. Le routage doit continuer à rejeter `[unk]`, les négations, commandes composites et actions automobiles non validées.

## Reproduction

Depuis `Jarvis_FR_Source_V4` :

```sh
python3 tools/verify_model.py
python3 -m venv /workspace/work/model-validation-venv
/workspace/work/model-validation-venv/bin/pip install vosk==0.3.45
/workspace/work/model-validation-venv/bin/python tools/test_vosk_host.py --report /workspace/work/model-validation-host-silence.json
/workspace/work/model-validation-venv/bin/python tools/test_vosk_host.py --case '/workspace/work/model-validation-audio/navigation.wav=ouvre la navigation' --case '/workspace/work/model-validation-audio/musique.wav=lance la musique' --case '/workspace/work/model-validation-audio/volume.wav=monte le volume' --case '/workspace/work/model-validation-audio/telephone.wav=ouvre le téléphone' --case '/workspace/work/model-validation-audio/autonomie.wav=autonomie restante' --case '/workspace/work/model-validation-audio/climatisation.wav=éteins la climatisation' --report /workspace/work/model-validation-host-speech.json
```

Ajouter `--grammar /workspace/work/model-validation-grammar.json` pour le mode guidé. La liste JSON exacte exportée depuis `FrenchVoiceGrammar.json()` est jointe aux preuves ; `GrammarProbe.java` est le harnais d'export, `ParserProbe.java` le harnais de l'analyseur, `model-validation-grammar-run.py` reproduit les 22 entrées audio avec la grammaire. Les JSON `*-parser.json` conservent les intentions réelles et les transcriptions d'origine. `model-validation-summary.json` fournit les compteurs ci-dessus.

Exemple de création d'un WAV lorsque `espeak-ng` est installé :

```sh
espeak-ng -v fr-fr -s 140 -w /tmp/navigation-22050.wav 'ouvre la navigation'
ffmpeg -nostdin -y -i /tmp/navigation-22050.wav -ar 16000 -ac 1 -c:a pcm_s16le -af apad=pad_dur=1 /tmp/navigation.wav
python tools/test_vosk_host.py --case '/tmp/navigation.wav=ouvre la navigation'
```

Pour cette session, les paquets eSpeak NG ont été téléchargés depuis les dépôts Ubuntu autorisés puis extraits dans `/workspace/work/model-validation-espeak/root` sans installation système : `espeak-ng_1.51+dfsg-12build1_amd64.deb`, `espeak-ng-data_1.51+dfsg-12build1_amd64.deb`, `libespeak-ng1_1.51+dfsg-12build1_amd64.deb`, `libpcaudio0_1.3-1build1_amd64.deb`, `libsonic0_0.2.0-13build2_amd64.deb`. Les chemins des commandes décrivent cet espace de travail et sont à adapter sur une autre machine.

## Preuves et limites

`model-validation-evidence.zip` rassemble ce rapport, les JSON et logs natifs, les WAV finaux, les phrases et les harnais. Les essais synthétiques sont des tests logiciels de développement ; ils ne représentent pas un essai de parole réelle dans l'habitacle. Un essai sur la tablette reste nécessaire pour la JNI ARM, l'extraction des assets, l'autorisation microphone, le TTS français installé et les commandes prononcées. Aucune fonction ECARX physique n'a été appelée, validée ou activée pendant ces essais.
