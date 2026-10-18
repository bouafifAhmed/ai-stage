"""
Évaluation hors ligne du moteur de recommandation.

1. Classement TF-IDF : configuration d'origine vs configuration retenue,
   sur 8 profils étudiants x 10 offres annotés à la main
   (2 = offre très pertinente, 1 = pertinente, 0 = hors sujet).
   Mesures : Top-1, Précision@3, MRR, nDCG@5, et marge = score de la meilleure offre très
   pertinente moins score de la meilleure offre hors sujet (plus elle est grande, moins le
   bon classement tient au hasard).

2. Correspondance des compétences : ancienne comparaison par sous-chaîne,
   comparaison par mots + synonymes, et LLM (option --ia, nécessite ANTHROPIC_API_KEY ;
   chaque paire déclenche un appel facturé).

Limites : jeu de données petit et construit à la main par l'équipe, pour couvrir des cas
réalistes (fautes d'accents, sigles, technologies aux noms proches). Il compare des
variantes entre elles ; il ne mesure pas la qualité en production.

Usage :
    python evaluation.py
    python evaluation.py --ia
"""

import argparse
import asyncio
import math
import statistics
import time
from typing import Dict, List, Tuple

from models import EtudiantProfilDTO, OffreEntrepriseDTO
from recommandation import CONFIG_TFIDF, CONFIG_TFIDF_INITIALE, calculer_recommandations, competences_correspondent


def _offre(id_, entreprise, secteur, titre, description, competences):
    # Même construction que RecommandationClientService côté Spring : sujet = titre + description
    return OffreEntrepriseDTO(id=id_, nomEntreprise=entreprise, secteurActivite=secteur,
                              sujetOffre=f"{titre} {description}", competencesRequises=competences)


OFFRES = [
    _offre(1, "Softia", "Informatique", "Développement d'une API REST en Java",
           "Vous participerez à la conception d'une API REST avec Spring Boot et à l'écriture de tests unitaires.",
           ["Java", "Spring Boot", "SQL", "Git"]),
    _offre(2, "Pixel", "Informatique", "Développeur front-end Angular",
           "Refonte de l'interface web de notre application de gestion en Angular et TypeScript, intégration de maquettes Figma.",
           ["Angular", "TypeScript", "HTML", "CSS"]),
    _offre(3, "Predicta", "Data / IA", "Data scientist - prévision des ventes",
           "Construction de modèles de machine learning pour prévoir les ventes, analyse exploratoire des données avec Python et pandas.",
           ["Python", "Machine Learning", "Pandas", "SQL"]),
    _offre(4, "Embedia", "Systèmes embarqués", "Développement de firmware embarqué",
           "Développement de pilotes en C et C++ pour microcontrôleurs STM32, tests sur banc.",
           ["C", "C++", "Linux embarqué", "Git"]),
    _offre(5, "Nuage", "Cloud", "Ingénieur DevOps - CI/CD et Kubernetes",
           "Mise en place de pipelines CI/CD, conteneurisation Docker et déploiement sur Kubernetes.",
           ["Docker", "Kubernetes", "Linux", "CI/CD"]),
    _offre(6, "Bouclier", "Cybersécurité", "Analyste cybersécurité SOC",
           "Surveillance des alertes de sécurité, analyse des journaux et réponse aux incidents dans un SOC.",
           ["Réseaux", "SIEM", "Linux", "Sécurité"]),
    _offre(7, "Finexa", "Finance", "Assistant contrôle de gestion",
           "Assistance au contrôleur de gestion : reporting mensuel, suivi budgétaire et analyse des écarts sous Excel.",
           ["Excel", "Comptabilité", "Analyse financière"]),
    _offre(8, "Clic", "Marketing", "Chargé de marketing digital",
           "Gestion des campagnes sur les réseaux sociaux, analyse des performances et création de contenus.",
           ["SEO", "Réseaux sociaux", "Google Analytics"]),
    _offre(9, "Webly", "Informatique", "Développeur full-stack JavaScript",
           "Développement d'une plateforme web en Node.js et React, base de données MongoDB.",
           ["JavaScript", "Node.js", "React", "MongoDB"]),
    _offre(10, "Insight", "Data / IA", "Business Intelligence - tableaux de bord",
           "Conception de tableaux de bord Power BI et modélisation d'un entrepôt de données SQL.",
           ["SQL", "Power BI", "Excel"]),
]


def _etudiant(id_, filiere, niveau, competences):
    # Même construction que Spring : motsClesSujetSouhaite = filière
    return EtudiantProfilDTO(id=id_, filiere=filiere, niveau=niveau,
                             competences=competences, motsClesSujetSouhaite=filiere)


# (profil, {id_offre: pertinence}) ; les offres absentes valent 0
PROFILS: List[Tuple[EtudiantProfilDTO, Dict[int, int]]] = [
    (_etudiant(1, "Génie logiciel", "M1", ["Java", "Spring", "SQL", "Git", "Docker"]), {1: 2, 5: 1, 10: 1}),
    (_etudiant(2, "Data science", "M2", ["Python", "ML", "pandas", "SQL", "statistiques"]), {3: 2, 10: 1}),
    (_etudiant(3, "Systemes embarques", "L3", ["C", "C++", "Linux", "electronique"]), {4: 2, 5: 1}),
    (_etudiant(4, "Informatique", "L3", ["JavaScript", "React", "Node.js", "HTML", "CSS"]), {9: 2, 2: 1}),
    (_etudiant(5, "Réseaux et sécurité", "M1", ["réseaux", "Linux", "sécurité", "Wireshark"]), {6: 2, 5: 1}),
    (_etudiant(6, "Gestion et finance", "L3", ["Excel", "comptabilite", "analyse financiere"]), {7: 2, 10: 1}),
    (_etudiant(7, "Developpement web", "L2", ["Angular", "Typescript", "html", "css"]), {2: 2, 9: 1}),
    (_etudiant(8, "Marketing digital", "M1", ["SEO", "réseaux sociaux", "Google Analytics", "Canva"]), {8: 2}),
]

# (compétence requise par l'offre, compétence de l'étudiant, verdict attendu)
PAIRES_COMPETENCES: List[Tuple[str, str, bool]] = [
    ("Java", "Java", True),
    ("Java", "JavaScript", False),
    ("JavaScript", "Java", False),
    ("R", "React", False),
    ("C", "C++", False),
    ("C#", "C", False),
    ("SQL", "NoSQL", False),
    ("Go", "Google Analytics", False),
    ("Réseaux", "Réseaux sociaux", False),
    ("Machine Learning", "ML", True),
    ("Node.js", "NodeJS", True),
    ("TypeScript", "TS", True),
    ("Kubernetes", "K8s", True),
    ("PostgreSQL", "Postgres", True),
    ("Intelligence artificielle", "IA", True),
    ("Sécurité", "securite", True),
    ("Spring Boot", "Spring", True),
    ("Python", "Python 3", True),
    ("Excel", "Microsoft Excel", True),
    ("Linux", "Linux embarqué", True),
    ("Docker", "Conteneurisation Docker", True),
    ("SQL", "MySQL", True),
    ("Machine Learning", "Apprentissage automatique", True),
    ("Méthodes agiles", "Scrum", True),
    ("Base de données", "PostgreSQL", True),
]


def ancienne_correspondance(requise: str, possedee: str) -> bool:
    """Comparaison par sous-chaîne de la version d'origine."""
    a, b = requise.strip().lower(), possedee.strip().lower()
    return a == b or a in b or b in a


def ndcg(pertinences: List[int], k: int) -> float:
    dcg = sum((2 ** r - 1) / math.log2(i + 2) for i, r in enumerate(pertinences[:k]))
    ideal = sorted(pertinences, reverse=True)
    idcg = sum((2 ** r - 1) / math.log2(i + 2) for i, r in enumerate(ideal[:k]))
    return dcg / idcg if idcg else 0.0


def evaluer_classement(config: Dict) -> Dict[str, float]:
    top1, p3, rr, n5, marges = [], [], [], [], []
    for etudiant, verite in PROFILS:
        classement = calculer_recommandations(etudiant, OFFRES, top_n=None, config_tfidf=config)
        pertinences = [verite.get(r.offreId, 0) for r in classement]
        top1.append(1.0 if pertinences[0] == 2 else 0.0)
        p3.append(sum(1 for r in pertinences[:3] if r > 0) / 3)
        rang = next(i for i, r in enumerate(pertinences) if r == 2) + 1
        rr.append(1 / rang)
        n5.append(ndcg(pertinences, 5))
        marges.append(max(r.score for r in classement if verite.get(r.offreId, 0) == 2)
                      - max(r.score for r in classement if verite.get(r.offreId, 0) == 0))
    return {"Top-1": statistics.mean(top1), "P@3": statistics.mean(p3),
            "MRR": statistics.mean(rr), "nDCG@5": statistics.mean(n5),
            "Marge": statistics.mean(marges), "Marge min": min(marges)}


def evaluer_paires(fonction) -> Tuple[float, List[str]]:
    erreurs = []
    for requise, possedee, attendu in PAIRES_COMPETENCES:
        if fonction(requise, possedee) != attendu:
            erreurs.append(f"{requise} / {possedee} (attendu {'oui' if attendu else 'non'})")
    exactitude = 1 - len(erreurs) / len(PAIRES_COMPETENCES)
    return exactitude, erreurs


async def evaluer_paires_ia() -> Tuple[float, List[str], List[float]]:
    from analyse_ia import analyser_adequation_ia, ia_disponible
    if not ia_disponible():
        raise SystemExit("IA désactivée (RECO_IA_ACTIVE=0).")
    erreurs, durees = [], []
    for i, (requise, possedee, attendu) in enumerate(PAIRES_COMPETENCES):
        etudiant = _etudiant(1000 + i, "Informatique", "M1", [possedee])
        offre = _offre(1000 + i, "Test", "Informatique", "Stage", "Stage de test.", [requise])
        debut = time.perf_counter()
        resultat = await analyser_adequation_ia(etudiant, offre)
        if resultat is None:
            raise SystemExit("L'appel au LLM a échoué : vérifiez ANTHROPIC_API_KEY (détails dans les logs).")
        durees.append(time.perf_counter() - debut)
        if resultat.correspondances[0].acquise != attendu:
            erreurs.append(f"{requise} / {possedee} (attendu {'oui' if attendu else 'non'})")
    return 1 - len(erreurs) / len(PAIRES_COMPETENCES), erreurs, durees


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--ia", action="store_true", help="évaluer aussi la correspondance par LLM (appels facturés)")
    args = parser.parse_args()

    print(f"## Classement TF-IDF ({len(PROFILS)} profils x {len(OFFRES)} offres)\n")
    print("| Configuration | Top-1 | P@3 | MRR | nDCG@5 | Marge moyenne | Marge minimale |")
    print("|---|---|---|---|---|---|---|")
    for nom, config in [("Origine", CONFIG_TFIDF_INITIALE), ("Retenue", CONFIG_TFIDF)]:
        m = evaluer_classement(config)
        print(f"| {nom} | {m['Top-1']:.2f} | {m['P@3']:.2f} | {m['MRR']:.2f} | {m['nDCG@5']:.2f} "
              f"| {m['Marge']:.2f} | {m['Marge min']:.2f} |")

    print(f"\n## Correspondance des compétences ({len(PAIRES_COMPETENCES)} paires)\n")
    print("| Méthode | Exactitude | Erreurs |")
    print("|---|---|---|")
    for nom, fonction in [("Sous-chaîne (origine)", ancienne_correspondance),
                          ("Mots + synonymes", competences_correspondent)]:
        exactitude, erreurs = evaluer_paires(fonction)
        print(f"| {nom} | {exactitude:.0%} | {'; '.join(erreurs) or '-'} |")

    if args.ia:
        from analyse_ia import MODELE
        exactitude, erreurs, durees = asyncio.run(evaluer_paires_ia())
        print(f"| LLM ({MODELE}) | {exactitude:.0%} | {'; '.join(erreurs) or '-'} |")
        durees.sort()
        p95 = durees[min(len(durees) - 1, math.ceil(0.95 * len(durees)) - 1)]
        print(f"\nLatence LLM : médiane {statistics.median(durees):.1f} s, p95 {p95:.1f} s, max {durees[-1]:.1f} s")


if __name__ == "__main__":
    main()
