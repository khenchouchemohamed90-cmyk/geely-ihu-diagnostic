#!/usr/bin/env python3
"""Garde-fou statique de revue des sources, pas une preuve de sûreté du binaire.

La version 0.9.6 n'admet que le seul appel A/C manuel, explicite et cadré
dans ManualAcTrial. Tout autre point d'écriture reste interdit.

Détecte les noms d'écritures ECARX connus, en appel direct, référence de méthode
ou chaîne réflexive (y compris échappements et concaténations littérales).
Les commentaires sont ignorés. Un nom construit dynamiquement ou du code natif
nécessite toujours une revue ; ce script ne remplace pas l'essai sur appareil.
"""
from pathlib import Path
import argparse
import re

FORBIDDEN = frozenset('''
setFunctionValue setCustomizeFunctionValue setFunctionValueById
setFunctionValueWithRetry setAcOn setClimateSystemOn setAcAuto setAcEcoOn
setAcMax setAirRecirculation setBlowingMode setFanAuto setFanSpeed
setTemperature setAllWindows setAllWindowsOneCommand setWindow setSunroof
setSunshade setSeatHeating setSeatVentilation setSteeringHeating
setDefrostFront setDefrostFrontMax setDefrostRear setElectricDefrostFront
setWindscreenHeat setDrivingMode setRearMirrorsFolded setMirrorAutoFolding
setExteriorLightAutomatic setExteriorLightLowBeam setExteriorLightMode
setExteriorLightOff setExteriorLightPositionLights setRearFogLamps
setInteriorLight setInteriorLightAll setInteriorLightDriver
setInteriorLightFront setInteriorLightPassenger setInteriorLightRear
setInteriorLightRearLeft setInteriorLightRearRight setAtmosphereLamps
setAmbienceLightMusicLink setMonjaroAmbienceColor setMonjaroAmbienceEffect
setMonjaroAmbienceEnabled setMonjaroAmbienceFunctionValue setParkingSensors
setProjector setWiperServiceModeAll setWiperServiceModeFront
setWiperServiceModeRear setAutoFanSetting
setDisplayAndInstrumentBrightnessAtlasPreface setDisplayBrightnessAutoAtlasPreface
'''.split())

LEXER = re.compile(
    r'(?P<comment>//[^\n]*|/\*[\s\S]*?\*/)'
    r'|(?P<string>"(?:\\[\s\S]|[^"\\])*")'
    r"|(?P<char>'(?:\\[\s\S]|[^'\\])*')"
    r'|(?P<identifier>[A-Za-z_$][A-Za-z0-9_$]*)'
    r'|(?P<space>\s+)'
    r'|(?P<symbol>::|.)'
)
ESCAPES = {'b': '\b', 't': '\t', 'n': '\n', 'f': '\f', 'r': '\r',
           '"': '"', "'": "'", '\\': '\\'}


def unescape_literal(token):
    return re.sub(r'\\([0-7]{1,3}|[btnfr"\'\\])',
                  lambda m: chr(int(m[1], 8)) if m[1][0] in '01234567' else ESCAPES[m[1]],
                  token[1:-1])


def violations(source):
    # Java traduit les échappements Unicode avant de lire les commentaires.
    source = re.sub(r'\\u+([0-9A-Fa-f]{4})', lambda m: chr(int(m[1], 16)), source)
    tokens = [(m.lastgroup, m.group(), m.start()) for m in LEXER.finditer(source)
              if m.lastgroup not in ('comment', 'space')]
    found = set()
    for i, (kind, value, offset) in enumerate(tokens):
        if kind == 'identifier' and value in FORBIDDEN:
            previous = tokens[i - 1][1] if i else None
            following = tokens[i + 1][1] if i + 1 < len(tokens) else None
            if following == '(' or previous == '::':
                found.add((source.count('\n', 0, offset) + 1, value))
        if kind == 'string':
            name = unescape_literal(value)
            j = i + 1
            # Un nom réflexif entier reste interdit même hors getMethod().
            if name in FORBIDDEN:
                found.add((source.count('\n', 0, offset) + 1, name))
            while j + 1 < len(tokens) and tokens[j][1] == '+' and tokens[j + 1][0] == 'string':
                name += unescape_literal(tokens[j + 1][1])
                j += 2
                if name in FORBIDDEN:
                    found.add((source.count('\n', 0, offset) + 1, name))
    return sorted(found)


def self_test():
    cases = [
        ('fn.setFunctionValue(1, 0);', True),
        ('fn . setCustomizeFunctionValue /* détail */ (1, 0);', True),
        ('writer = fn::setFunctionValue;', True),
        ('fn.getClass().getMethod("setFunctionValue", int.class);', True),
        ('fn.getClass().getMethod("set" + "FunctionValue", int.class);', True),
        (r'fn.getClass().getMethod("set\u0046unctionValue", int.class);', True),
        (r'fn.getClass().getMethod("set\106unctionValue", int.class);', True),
        ('helper.setAcOn(true);', True),
        ('/* fn.setFunctionValue(1, 0); */ getFunctionValue(1, 0);', False),
        ('// fn.setFunctionValue(1, 0);\nview.setText("Lecture seule");', False),
        ('String url = "https://example.test"; fn.getFunctionValue(1, 0);', False),
        ('fn.getClass().getMethod("getFunctionValue", int.class);', False),
        ('String label = "aucun appel setFunctionValue dans V4";', False),
    ]
    for source, expected in cases:
        if bool(violations(source)) != expected:
            raise AssertionError('Garde-fou incorrect : ' + source)
    print(f'{len(cases)} tests du garde-fou statique PASSÉS')


def validate_manual_ac_trial(source):
    """Contrôle statique STRICT du seul point d'écriture explicitement accepté."""
    errors = []
    exact = {
        'signature setFunctionValue': '"setFunctionValue"',
        'ID A/C constructeur': 'static final int AC_ID = 0x10010300;',
        'zone constructeur': 'static final int ZONE = 0;',
        'appel unique A/C': 'write.invoke(function, AC_ID, ZONE, value)',
        'garde de présence en premier plan': 'guard.canProceed()',
        'vérification des refus': 'InvocationTargetException',
    }
    for label, marker in exact.items():
        if source.count(marker) != 1 and label != 'garde de présence en premier plan':
            errors.append(label + ' absent ou répété dans ManualAcTrial')
        if label == 'garde de présence en premier plan' and source.count(marker) < 2:
            errors.append('garde de présence insuffisante dans ManualAcTrial')
    forbidden = [(line, name) for line, name in violations(source)
                 if name != 'setFunctionValue']
    errors += ['écriture supplémentaire dans ManualAcTrial ligne ' + str(line) + ': ' + name
               for line, name in forbidden]
    if sum(1 for line, name in violations(source) if name == 'setFunctionValue') != 1:
        errors.append('setFunctionValue doit être référencé exactement une fois dans ManualAcTrial')
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    if args.self_test:
        self_test()
    root = Path(__file__).resolve().parent.parent / 'android/app/src/main/java'
    files = sorted(root.rglob('*.java'))
    if not files:
        raise SystemExit('ERREUR : aucun fichier Java à contrôler')
    errors = []
    for file in files:
        source = file.read_text(encoding='utf-8')
        if file.name == 'ManualAcTrial.java':
            errors.extend(str(file) + ': ' + issue for issue in validate_manual_ac_trial(source))
        else:
            for line, name in violations(source):
                errors.append(f'{file}:{line}: écriture ECARX potentielle ({name})')
    if errors:
        raise SystemExit('ERREUR :\\n' + '\\n'.join(errors))
    print(f'Contrôle statique : 1 essai A/C manuel limité ; aucune autre écriture ECARX dans {len(files)} sources Java')


if __name__ == '__main__':
    main()
