"""
Logique du moteur de recommandation basée sur TF-IDF et similarité cosinus.

Concept :
- Convertir chaque profil (étudiant, offre) en un vecteur numérique de similarité textuelle
- Utiliser TF-IDF (Term Frequency - Inverse Document Frequency) pour donner plus de poids
  aux termes rares mais significatifs
- Calculer la similarité cosinus entre le vecteur étudiant et chaque vecteur offre
- Retourner les offres triées par score de pertinence (du plus au moins pertinent)

Ce module contient aussi l'analyse d'adéquation « par règles », utilisée seule
quand le LLM est désactivé ou indisponible (voir analyse_ia.py).
"""

import re
import unicodedata
from typing import Dict, List, Optional, Set

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

from models import (
    EtudiantProfilDTO,
    OffreEntrepriseDTO,
    RecommandationItemDTO,
    AdequationResponseDTO,
    CorrespondanceCompetenceDTO,
)


# Mots outils français, déjà en minuscules et sans accents : le vectoriseur
# applique strip_accents AVANT de filtrer les mots vides.
STOP_WORDS_FR = frozenset("""
a afin ainsi au aux avec c ce ces cet cette chez d dans de des du elle elles en
entre est et etre eux il ils j je l la le les leur leurs lui m ma mais me meme mes
moi mon n ne nos notre nous on ou par pas plus pour qu que qui s sa se selon ses
son sont sous sur t ta te tes toi ton tous tout toute toutes tres tu un une vos
votre vous y
""".split())

# Configuration d'origine (avant optimisation), conservée pour l'évaluation comparative.
CONFIG_TFIDF_INITIALE: Dict = {"lowercase": True}

# Configuration retenue après évaluation (voir evaluation.py et docs/travail-individuel) :
# - strip_accents : « Developpement » et « Développement » deviennent le même terme
# - stop_words    : « de », « et », « la »... ne créent plus de similarité artificielle
# - token_pattern : garde les termes d'une lettre et les symboles (C, R, C++, C#, Node.js),
#                   que le motif par défaut de scikit-learn supprime
CONFIG_TFIDF: Dict = {
    "lowercase": True,
    "strip_accents": "unicode",
    "stop_words": sorted(STOP_WORDS_FR),
    "token_pattern": r"(?u)\b\w[\w+#]*(?:\.\w+)*",
}

# Synonymes et abréviations courants, ramenés à une forme canonique.
SYNONYMES_COMPETENCES: Dict[str, str] = {
    "ml": "machine learning",
    "ia": "intelligence artificielle",
    "ai": "intelligence artificielle",
    "artificial intelligence": "intelligence artificielle",
    "js": "javascript",
    "ts": "typescript",
    "nodejs": "node.js",
    "node": "node.js",
    "reactjs": "react",
    "react.js": "react",
    "k8s": "kubernetes",
    "postgres": "postgresql",
    "springboot": "spring boot",
}


def normaliser_texte(texte: Optional[str]) -> str:
    """Minuscules, sans accents, espaces compactés."""
    if not texte:
        return ""
    sans_accents = unicodedata.normalize("NFKD", texte)
    sans_accents = "".join(c for c in sans_accents if not unicodedata.combining(c))
    return " ".join(sans_accents.lower().split())


def normaliser_competence(competence: Optional[str]) -> str:
    """Forme canonique d'une compétence : « ML » -> « machine learning »."""
    forme = normaliser_texte(competence)
    return SYNONYMES_COMPETENCES.get(forme, forme)


def _mots_competence(competence: str) -> Set[str]:
    """Mots significatifs d'une compétence déjà normalisée (C++, C#, Node.js gardés entiers)."""
    return {m for m in re.findall(r"[a-z0-9][a-z0-9+#.]*", competence) if m not in STOP_WORDS_FR}


def competences_correspondent(requise: str, possedee: str) -> bool:
    """
    Vrai si la compétence possédée couvre la compétence requise.

    La comparaison se fait mot à mot et non par sous-chaîne : l'ancienne version
    considérait « Java » comme présent dans « JavaScript », ou « R » dans « React ».
    Une compétence dont les mots sont inclus dans l'autre correspond
    (« Python » / « Python 3 », « Spring » / « Spring Boot »).
    """
    a, b = normaliser_competence(requise), normaliser_competence(possedee)
    if not a or not b:
        return False
    if a == b:
        return True
    mots_a, mots_b = _mots_competence(a), _mots_competence(b)
    if not mots_a or not mots_b:
        return False
    return mots_a <= mots_b or mots_b <= mots_a


def construire_texte_etudiant(etudiant: EtudiantProfilDTO) -> str:
    """
    Construit une représentation textuelle du profil d'un étudiant.

    Concatène :
    - Sa filière et niveau (ex: "Informatique L3")
    - Ses compétences (ex: "Python Java SQL")
    - Ses mots-clés de sujet souhaité (ex: "Data Science Machine Learning")

    Exemple:
        "Informatique L3 Python Java SQL Data Science Machine Learning"
    """
    texte = f"{etudiant.filiere} {etudiant.niveau}"

    if etudiant.competences:
        texte += " " + " ".join(etudiant.competences)

    if etudiant.motsClesSujetSouhaite:
        texte += " " + etudiant.motsClesSujetSouhaite

    return texte


def construire_texte_offre(offre: OffreEntrepriseDTO) -> str:
    """
    Construit une représentation textuelle d'une offre de stage.

    Concatène :
    - Le secteur d'activité de l'entreprise (ex: "Informatique")
    - Le sujet de l'offre, c'est-à-dire titre + description côté Spring
    - Les compétences requises (ex: "Node.js React MongoDB")

    Exemple:
        "Informatique Développement full-stack Node.js React Node.js React MongoDB"
    """
    texte = f"{offre.secteurActivite} {offre.sujetOffre}"

    if offre.competencesRequises:
        texte += " " + " ".join(offre.competencesRequises)

    return texte


def calculer_recommandations(
    etudiant: EtudiantProfilDTO,
    offres: List[OffreEntrepriseDTO],
    top_n: Optional[int] = 5,
    config_tfidf: Optional[Dict] = None,
) -> List[RecommandationItemDTO]:
    """
    Calcule la similarité entre un étudiant et un ensemble d'offres.

    Étapes :
    1. Construire le texte de l'étudiant et celui de chaque offre
    2. Vectoriser le corpus [étudiant, offre_1, ..., offre_n] avec TF-IDF
    3. Calculer la similarité cosinus entre le vecteur étudiant et chaque vecteur offre
    4. Trier par score décroissant et retourner le top N (toutes les offres si top_n=None)

    TF-IDF :
    - TF (Term Frequency) : fréquence d'un terme dans un document
    - IDF (Inverse Document Frequency) : log((1 + n) / (1 + df)) + 1, pénalise les termes
      présents dans beaucoup d'offres (« stage », « développement »...)

    Similarité cosinus :
    - cos(u, v) = u·v / (|u| |v|), entre 0 et 1 ici (poids TF-IDF positifs)
    - Indépendante de la longueur des textes

    Args:
        etudiant: Profil étudiant
        offres: Liste de toutes les offres disponibles
        top_n: Nombre de recommandations à retourner (par défaut 5)
        config_tfidf: Paramètres du TfidfVectorizer (CONFIG_TFIDF par défaut)

    Returns:
        List[RecommandationItemDTO]: Offres recommandées triées par score décroissant
    """
    if not offres:
        return []

    texte_etudiant = construire_texte_etudiant(etudiant).strip()
    textes_offres = [construire_texte_offre(offre).strip() for offre in offres]
    corpus = [texte_etudiant] + textes_offres

    def scores_nuls() -> List[RecommandationItemDTO]:
        return [
            RecommandationItemDTO(offreId=offre.id, nomEntreprise=offre.nomEntreprise, score=0.0)
            for offre in offres[:top_n]
        ]

    if not any(corpus):
        return scores_nuls()

    vectorizer = TfidfVectorizer(**(config_tfidf if config_tfidf is not None else CONFIG_TFIDF))

    try:
        tfidf_matrix = vectorizer.fit_transform(corpus)
    except ValueError:
        # Vocabulaire vide (textes composés uniquement de mots vides, par exemple)
        return scores_nuls()

    # Ligne 0 = étudiant, lignes 1..n = offres
    scores = cosine_similarity(tfidf_matrix[0:1], tfidf_matrix[1:])[0]

    classement = sorted(range(len(offres)), key=lambda i: scores[i], reverse=True)

    return [
        RecommandationItemDTO(
            offreId=offres[i].id,
            nomEntreprise=offres[i].nomEntreprise,
            score=round(float(scores[i]), 2),
        )
        for i in classement[:top_n]
    ]


def score_textuel(etudiant: EtudiantProfilDTO, offre: OffreEntrepriseDTO) -> float:
    """Similarité TF-IDF entre un étudiant et une seule offre."""
    recs = calculer_recommandations(etudiant, [offre], top_n=1)
    return recs[0].score if recs else 0.0


def score_combine(nb_acquises: int, nb_requises: int, similarite_texte: float) -> float:
    """
    Score d'adéquation : 65 % compétences couvertes, 35 % similarité textuelle.
    Sans compétence requise, seule la similarité textuelle compte.
    """
    if nb_requises:
        score = 0.65 * (nb_acquises / nb_requises) + 0.35 * similarite_texte
    else:
        score = similarite_texte
    return max(0.0, min(1.0, score))


def correspondances_par_regles(
    etudiant: EtudiantProfilDTO,
    offre: OffreEntrepriseDTO,
) -> List[CorrespondanceCompetenceDTO]:
    """Pour chaque compétence requise, cherche une compétence de l'étudiant qui la couvre."""
    resultat = []
    for requise in offre.competencesRequises or []:
        couverte_par = next(
            (p for p in (etudiant.competences or []) if competences_correspondent(requise, p)),
            None,
        )
        resultat.append(CorrespondanceCompetenceDTO(
            competenceRequise=requise,
            acquise=couverte_par is not None,
            justification=(
                f"Couverte par « {couverte_par} » dans votre profil."
                if couverte_par else "Absente de votre profil."
            ),
        ))
    return resultat


def conseils_par_regles(
    etudiant: EtudiantProfilDTO,
    offre: OffreEntrepriseDTO,
    score_pct: int,
    competences_manquantes: List[str],
) -> List[str]:
    """Conseils génériques choisis selon des seuils (utilisés sans LLM)."""
    conseils = []
    if score_pct >= 70:
        conseils.append("Excellente adéquation ! Votre profil correspond étroitement aux attentes de l'entreprise.")
    elif score_pct >= 40:
        conseils.append("Bonne correspondance globale. Mettez en valeur vos projets pratiques dans votre candidature.")
    else:
        conseils.append("Cette offre présente des défis stimulants. Préparez-vous en découvrant les bases des compétences demandées.")

    if competences_manquantes:
        conseils.append(f"Compétences à explorer ou valoriser en entretien : {', '.join(competences_manquantes[:3])}.")

    secteur = normaliser_texte(offre.secteurActivite)
    sujet = normaliser_texte(offre.sujetOffre)
    filiere = normaliser_texte(etudiant.filiere)
    if filiere and (filiere in secteur or filiere in sujet or (secteur and secteur in filiere)):
        conseils.append(f"Votre filière ({etudiant.filiere}) s'aligne directement avec le domaine du stage.")

    return conseils


def construire_reponse_adequation(
    offre: OffreEntrepriseDTO,
    correspondances: List[CorrespondanceCompetenceDTO],
    similarite_texte: float,
    conseils: List[str],
    source: str,
    synthese: Optional[str] = None,
    modele: Optional[str] = None,
) -> AdequationResponseDTO:
    """Assemble la réponse ; le score est toujours calculé ici, jamais fourni par le LLM."""
    acquises = [c.competenceRequise for c in correspondances if c.acquise]
    manquantes = [c.competenceRequise for c in correspondances if not c.acquise]
    score = score_combine(len(acquises), len(correspondances), similarite_texte)

    return AdequationResponseDTO(
        offreId=offre.id,
        score=round(score, 2),
        scorePourcentage=int(round(score * 100)),
        competencesAcquises=acquises,
        competencesManquantes=manquantes,
        conseils=conseils,
        correspondances=correspondances,
        synthese=synthese,
        source=source,
        modele=modele,
    )


def analyser_adequation(
    etudiant: EtudiantProfilDTO,
    offre: OffreEntrepriseDTO,
) -> AdequationResponseDTO:
    """
    Analyse l'adéquation détaillée et l'écart de compétences (Skill Gap)
    entre un profil étudiant et une offre spécifique, sans LLM.
    """
    correspondances = correspondances_par_regles(etudiant, offre)
    similarite = score_textuel(etudiant, offre)

    manquantes = [c.competenceRequise for c in correspondances if not c.acquise]
    score_pct = int(round(score_combine(len(correspondances) - len(manquantes), len(correspondances), similarite) * 100))

    return construire_reponse_adequation(
        offre=offre,
        correspondances=correspondances,
        similarite_texte=similarite,
        conseils=conseils_par_regles(etudiant, offre, score_pct, manquantes),
        source="regles",
    )
