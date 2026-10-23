#!/usr/bin/env python3
"""Contrôle de sécurité statique du prototype audio chinois vers Xiaoka."""
from pathlib import Path

base = Path(__file__).resolve().parents[1] / 'android/app/src/main/java/com/aztech/jarvisfr'
relay = (base / 'MandarinRelay.java').read_text(encoding='utf-8')
main = (base / 'MainActivity.java').read_text(encoding='utf-8')
checks = [
    (relay.count('"打开空调"') == 1, 'une seule phrase A/C marche'),
    (relay.count('"关闭空调"') == 1, 'une seule phrase A/C arrêt'),
    (relay.count('tts.speak(') == 1, 'une seule émission vocale par test'),
    ('TextToSpeech.LANG_AVAILABLE' in relay, 'vérification de la langue installée'),
    ('startActivity' not in relay, 'ouverture OEM via action Android existante'),
    ('setFunctionValue' not in relay and 'transact(' not in relay and 'getICarFunction' not in relay,
     'pas de commande constructeur depuis le relais'),
    ('confirmMandarinRelay(boolean enable)' in main, 'confirmation explicite'),
    ('setPositiveButton("CONFIRMER LE TEST"' in main, 'validation après confirmation'),
    ('capture.whenStopped' in main, 'libération du microphone'),
    ('mandarinRelay.start(enable' in main, 'lancement volontaire uniquement'),
    ('case ACTIVER_CLIMATISATION:\n            case ARRETER_CLIMATISATION:\n                answer = getString(R.string.hvac_disabled);' in main,
     'commandes physiques vocales toujours bloquées'),
]
for ok, description in checks:
    print(('OK' if ok else 'ECHEC') + ' - ' + description)
if not all(flag for flag, _ in checks):
    raise SystemExit(1)
print('Contrôle prototype audio chinois terminé.')
