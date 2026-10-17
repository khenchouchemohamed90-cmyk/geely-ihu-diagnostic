#!/usr/bin/env python3
"""Génère à la compilation des réponses françaises PCM sans moteur TTS Android.
Le contenu émis est lié à des réponses validées, jamais à une écriture ECARX.
"""
from pathlib import Path
import shutil
import subprocess
import sys

PHRASES = {
    "test": "Bonjour. Je suis Jarvis, votre assistant vocal français.",
    "reponse_affichee": "Je vous ai répondu en français à l’écran. La synthèse vocale connectée est indisponible.",
    "bonjour": "Bonjour. Je suis à votre écoute.",
    "identite": "Je suis Jarvis F R, votre assistant embarqué sur Geely Emgrand.",
    "etat": "Je vais bien, merci. Je suis prêt à vous aider.",
    "merci": "Avec plaisir.",
    "aide": "Je peux ouvrir la navigation, contrôler la musique, lire les capteurs disponibles et répondre à quelques questions simples. Les commandes mécaniques non validées restent désactivées.",
    "blague": "Pourquoi les voitures aiment-elles les histoires? Parce qu'elles ont toujours un bon moteur!",
    "inconnu": "Je n'ai pas compris cette demande. Reformulez en une seule phrase.",
    "micro": "Je vous écoute en français.",
    "navigation": "J'ouvre la navigation, si elle est disponible.",
    "navigateur": "J'ouvre le navigateur, si une application est installée.",
    "musique": "Commande de musique transmise.",
    "pause": "Commande de pause transmise.",
    "suivante": "Passage au morceau suivant demandé.",
    "precedente": "Retour au morceau précédent demandé.",
    "telephone": "Par sécurité, l'ouverture du téléphone est désactivée sur cette tablette.",
    "vehicule_bloque": "Cette fonction de la voiture est reconnue, mais reste désactivée pour des raisons de compatibilité.",
    "clim_bloquee": "J'ai reconnu la demande de climatisation, mais la commande constructeur n'est pas encore validée.",
    "capteur_absent": "Je n'arrive pas à obtenir cette donnée depuis le système de la voiture.",
    "capteur_disponible": "J'ai reçu une valeur du véhicule. Elle est affichée à l'écran, avec ses limites de fiabilité.",
    "autonomie_absente": "L'autonomie n'est pas disponible dans le capteur actuel.",
    "temperature_absente": "Le capteur de température extérieure ne donne pas de valeur fiable.",
    "vitesse_absente": "Le capteur de vitesse ne donne pas de valeur fiable.",
    "clim_absente": "L'état de la climatisation n'est pas disponible par cette interface.",
    "carburant_absent": "Le niveau de carburant n'est pas encore disponible dans cette version.",
    "occupe": "Une autre opération est en cours. Veuillez réessayer.",
    "horloge": "L'heure actuelle est affichée sur le tableau de bord.",
    "date": "La date du jour est disponible sur le système Android.",
    "annulation": "Commande annulée.",
}
def main():
    tool = shutil.which("espeak")
    if not tool:
        sys.exit("ERREUR: espeak introuvable. Le paquet vocal est obligatoire.")
    root = Path(__file__).resolve().parents[1]
    out = root/"android"/"app"/"src"/"main"/"assets"/"jarvis_replies"
    out.mkdir(parents=True, exist_ok=True)
    for key, phrase in PHRASES.items():
        dest = out/(key+".wav")
        subprocess.run([tool, "-v", "fr", "-s", "145", "-p", "44", "-w", str(dest), phrase], check=True)
        if not dest.is_file() or dest.stat().st_size < 200:
            sys.exit("ERREUR sortie vocale absente : "+key)
    print(f"Paroles intégrées : {len(PHRASES)} fichiers WAV français ; moteur sur tablette non requis")
if __name__ == "__main__":
    main()
