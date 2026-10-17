# Transfert Jarvis FR V4 vers Codex (archives sous 32 Mio)

La version complète avait une taille de 40,4 Mio, incompatible avec la limite de transfert de Codex.
Les fichiers ont été repartis en trois ZIP sans suppression du code ni des donnees du modele.

1. Joindre cette archive SOURCES_LEGER et les deux archives MODELE_VOSK_FR_1 / MODELE_VOSK_FR_2 a une meme tache Codex.
2. Dans Codex, extraire les trois ZIP dans le MEME repertoire de travail, sans changer les chemins et en fusionnant le dossier Jarvis_FR_Source_V4 commun.
3. Verifier que le modele est present sous Jarvis_FR_Source_V4/android/app/src/main/assets/model-fr/ ; ne pas commencer la validation vocale tant que les deux ZIP du modele ne sont pas extraits.
4. Depuis Jarvis_FR_Source_V4/android, verifier Gradle (environnement deja configure) et lancer : gradle :app:assembleDebug.
5. Si compilation impossible, corriger le code et le build; ne jamais annoncer les actions physiques ECARX comme validees sur la voiture.
6. Consulter analysis/SHA256_MODELE_FR.txt pour verifier l'integrite des fichiers reconstruits.

Le projet est une RECONSTRUCTION PARTIELLE, pas le code source original de Jarvis 2.8 Lite. Toutes les ecritures ECARX restent desactivees.
