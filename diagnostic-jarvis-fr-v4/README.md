# Jarvis FR IHU007B — APK de diagnostic

[**Télécharger l’APK Android 9**](Jarvis_FR_IHU007B_0.4.1_diagnostic.apk?raw=true) — version 0.4.1, 58,30 Mio, ARM 32/64 bits, signée pour diagnostic.

SHA-256 : `8352938829dd573bde72eafecd9ae18d3b409a27efa6e6f60f1dfe9af5ae5780`.

- [Sources et compilation reproductible](../Jarvis_FR_Source_V4/README.md)
- [Résultats et matrice des fonctions](../Jarvis_FR_Source_V4/docs/RAPPORT_DIAGNOSTIC.md)
- [Procédure d’essais sur tablette](../Jarvis_FR_Source_V4/docs/ESSAIS_TABLETTE.md)
- [Compilation et 220 vérifications](compilation-et-tests.log)
- [Signature, manifeste et intégrité de l’APK](verification-apk.json)
- [Lint final](lint-results-debug.html) : zéro erreur et zéro avertissement
- [Résultats de reconnaissance Vosk](../Jarvis_FR_Source_V4/docs/VALIDATION_VOSK.md) et [preuves avec audio synthétique](preuves-vosk.zip)

Les fonctions Android sont programmées et simulées ; leurs effets et les lectures ECARX restent à tester sur IHU007B. La reconnaissance synthétique guidée donne 7/11 intentions correctes, avec un faux positif de lecture ; elle ne prouve pas la qualité du microphone réel. Les écritures ECARX sont **toutes désactivées**. Aucun firmware modifié.
