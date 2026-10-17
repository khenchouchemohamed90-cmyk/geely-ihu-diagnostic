#!/bin/sh
set -eu
BASE="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
javac -d "$WORK" "$BASE/android/app/src/main/java/com/aztech/jarvisfr/FrenchIntentParser.java" "$BASE/tools/TestParser.java"
java -cp "$WORK" TestParser
javac -encoding UTF-8 -cp "$WORK" -d "$WORK" \
  "$BASE/android/app/src/main/java/com/aztech/jarvisfr/FrenchConversation.java" \
  "$BASE/tools/TestFrenchConversation.java"
java -cp "$WORK" TestFrenchConversation
javac -cp "$WORK" -d "$WORK" "$BASE/android/app/src/main/java/com/aztech/jarvisfr/VoiceResultGate.java" "$BASE/tools/TestVoiceResultGate.java"
java -cp "$WORK" TestVoiceResultGate
javac -cp "$WORK" -d "$WORK" "$BASE/android/app/src/main/java/com/aztech/jarvisfr/VoiceSessionGate.java" "$BASE/tools/TestVoiceSessionGate.java"
java -cp "$WORK" TestVoiceSessionGate
# ECARX: simulation locale sans interaction avec un véhicule réel.
javac -d "$WORK" $(find "$BASE/tools/fake_ecarx" -name '*.java' -type f) "$BASE/android/app/src/main/java/com/aztech/jarvisfr/EcarxReadOnly.java" "$BASE/tools/TestEcarxReadOnly.java"
java -cp "$WORK" TestEcarxReadOnly

# Actions Android standard : simulation pure sans appareil et sans ECARX.
javac -d "$WORK" \
  "$BASE"/tools/fake_android_actions/android/content/Context.java \
  "$BASE"/tools/fake_android_actions/android/content/Intent.java \
  "$BASE"/tools/fake_android_actions/android/content/pm/PackageManager.java \
  "$BASE"/tools/fake_android_actions/android/app/Activity.java \
  "$BASE"/tools/fake_android_actions/android/media/AudioManager.java \
  "$BASE"/tools/fake_android_actions/android/net/Uri.java \
  "$BASE"/tools/fake_android_actions/android/view/KeyEvent.java \
  "$BASE"/android/app/src/main/java/com/aztech/jarvisfr/FrenchIntentParser.java \
  "$BASE"/android/app/src/main/java/com/aztech/jarvisfr/AndroidActions.java \
  "$BASE"/tools/TestAndroidActions.java
java -cp "$WORK" TestAndroidActions

# Aucun appel direct de commande physique n'est autorisé dans cette V4.
python3 "$BASE/tools/verify_readonly.py" --self-test
