#!/bin/sh
# JDK 17 + SDK Android 34. Aucun accès à la tablette ni au véhicule.
set -eu
BASE="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
python3 "$BASE/tools/verify_model.py"
sh "$BASE/tools/test_parser.sh"
sh "$BASE/tools/test_audio.sh"
sh "$BASE/tools/test_tts.sh"
sh "$BASE/tools/test_diagnostics.sh"
cd "$BASE/android"
"${JARVIS_GRADLE:-./gradlew}" --no-daemon "$@" :app:assembleDebug :app:lintDebug
python3 "$BASE/tools/verify_apk.py"
