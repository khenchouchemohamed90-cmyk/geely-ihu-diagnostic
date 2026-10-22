#!/usr/bin/env python3
"""Contrôles statiques de l'APK de diagnostic ; aucun test d'installation réel."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import zipfile

BASE = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, default=BASE / 'android/app/build/outputs/apk/debug/app-debug.apk')
    args = parser.parse_args()
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk:
        raise ValueError('ANDROID_HOME ou ANDROID_SDK_ROOT doit désigner le SDK installé')
    tools = Path(sdk) / 'build-tools/34.0.0'

    def run(*cmd):
        return subprocess.check_output([str(x) for x in cmd], text=True, stderr=subprocess.STDOUT).strip()

    apk = args.apk.resolve()
    badging = run(tools / 'aapt', 'dump', 'badging', apk)
    for expected in ("package: name='com.aztech.jarvisfr.xiaoka.acmanual547'", "sdkVersion:'28'", "targetSdkVersion:'28'"):
        if expected not in badging:
            raise ValueError('Déclaration APK inattendue : ' + expected)
    alignment = run(tools / 'zipalign', '-c', '-v', '4', apk)
    signature = run(tools / 'apksigner', 'verify', '--verbose', '--min-sdk-version', '28', '--print-certs', apk)
    with zipfile.ZipFile(apk) as archive:
        if len(archive.namelist()) != len(set(archive.namelist())):
            raise ValueError('Entrées dupliquées dans l’APK')
        libs = sorted(name for name in archive.namelist() if name.startswith('lib/') and name.endswith('.so'))
        abis = sorted({name.split('/')[1] for name in libs})
        if abis != ['arm64-v8a', 'armeabi-v7a']:
            raise ValueError('Architectures natives inattendues : ' + str(abis))
        for abi in abis:
            for lib in ('libvosk.so', 'libjnidispatch.so'):
                if 'lib/' + abi + '/' + lib not in libs:
                    raise ValueError('Bibliothèque native manquante : ' + abi + '/' + lib)
        checked = 0
        for line in (BASE / 'analysis/SHA256_MODELE_FR.txt').read_text().splitlines():
            if not line.strip():
                continue
            expected, name = line.split(maxsplit=1)
            relative = name.split('/assets/model-fr/', 1)[1]
            actual = hashlib.sha256(archive.read('assets/model-fr/' + relative)).hexdigest()
            if actual != expected:
                raise ValueError('Modèle altéré dans l’APK : ' + relative)
            checked += 1
        if checked != 15:
            raise ValueError('Le modèle embarqué doit contenir les 15 fichiers attendus')
    print(json.dumps({
        'ok': True, 'apk': str(apk), 'bytes': apk.stat().st_size,
        'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
        'min_sdk': 28, 'target_sdk': 28, 'model_files_verified': checked,
        'native_abis': abis, 'native_libraries': libs,
        'zipalign': alignment.splitlines()[-1], 'signature_verification': signature,
        'manifest_badging': badging, 'installation_tablette_verifiee': False,
    }, ensure_ascii=False, indent=2))
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError, zipfile.BadZipFile, subprocess.CalledProcessError) as error:
        print('ERREUR : ' + str(error), file=sys.stderr)
        if isinstance(error, subprocess.CalledProcessError) and error.output:
            print(error.output, file=sys.stderr)
        sys.exit(1)
