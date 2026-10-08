"""
Logique du moteur de recommandation basée sur TF-IDF et similarité cosinus.

Concept :
- Convertir chaque profil (étudiant, offre) en un vecteur numérique de similarité textuelle
- Utiliser TF-IDF (Term Frequency - Inverse Document Frequency) pour donner plus de poids 
  aux termes rares mais significatifs
- Calculer la similarité cosinus entre le vecteur étudiant et chaque vecteur offre
- Retourner les offres triées par score de pertinence (du plus au moins pertinent)
"""

from typing import List, Dict
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np

from models import (
    EtudiantProfilDTO,
    OffreEntrepriseDTO,
    RecommandationItemDTO,
    AdequationResponseDTO
)


def construire_texte_etudiant(etudiant: EtudiantProfilDTO) -> str:
    """
    Construit une représentation textuelle du profil d'un étudiant.
    
    Concatène :
    - Sa filière et niveau (ex: "Informatique L3")
    - Ses compétences (ex: "Python Java SQL")
    - Ses mots-clés de sujet souhaité (ex: "Data Science Machine Learning")
    
    Args:
        etudiant: Profil étudiant avec filière, niveau, compétences, mots-clés
    
    Returns:
        str: Texte unique représentant le profil de l'étudiant
    
    Exemple:
        "Informatique L3 Python Java SQL Data Science Machine Learning"
    """
    # Base : filière et niveau
    texte = f"{etudiant.filiere} {etudiant.niveau}"
    
    # Ajouter les compétences
    if etudiant.competences:
        texte += " " + " ".join(etudiant.competences)
    
    # Ajouter les mots-clés du sujet souhaité
    if etudiant.motsClesSujetSouhaite:
        texte += " " + etudiant.motsClesSujetSouhaite
    
    return texte


def construire_texte_offre(offre: OffreEntrepriseDTO) -> str:
    """
    Construit une représentation textuelle d'une offre de stage.
    
    Concatène :
    - Le secteur d'activité de l'entreprise (ex: "Informatique")
    - La description/sujet de l'offre (ex: "Développement full-stack Node.js + React")
    - Les compétences requises (ex: "Node.js React MongoDB")
    
    Args:
        offre: Offre avec entreprise, secteur, sujet, compétences requises
    
    Returns:
        str: Texte unique représentant l'offre
    
    Exemple:
        "Informatique Développement full-stack Node.js React Node.js React MongoDB"
    """
    # Base : secteur et description
    texte = f"{offre.secteurActivite} {offre.sujetOffre}"
    
    # Ajouter les compétences requises
    if offre.competencesRequises:
        texte += " " + " ".join(offre.competencesRequises)
    
    return texte


def calculer_recommandations(
    etudiant: EtudiantProfilDTO,
    offres: List[OffreEntrepriseDTO],
    top_n: int = 5
) -> List[RecommandationItemDTO]:
    """
    Calcule la similarité entre un étudiant et un ensemble d'offres.
    
    Étapes :
    1. Construire le vecteur textuel de l'étudiant
    2. Construire le vecteur textuel de chaque offre
    3. Combiner tous les textes pour créer un corpus
    4. Appliquer TF-IDF sur le corpus (vectorisation numérique)
    5. Calculer la similarité cosinus entre le vecteur étudiant et chaque vecteur offre
    6. Trier les offres par score décroissant
    7. Retourner le top N
    
    TF-IDF Expliqué :
    - TF (Term Frequency) : nombre de fois où un terme apparaît dans un document
    - IDF (Inverse Document Frequency) : pénalise les termes trop courants
    - Résultat : les termes spécifiques à une offre reçoivent un poids plus élevé
    
    Similarité cosinus :
    - Mesure l'angle entre deux vecteurs dans l'espace vectoriel
    - Score entre 0 et 1 (0 = aucune similarité, 1 = identique)
    - Indépendante de la longueur des vecteurs (normalisée)
    
    Args:
        etudiant: Profil étudiant
        offres: Liste de toutes les offres disponibles
        top_n: Nombre de recommandations à retourner (par défaut 5)
    
    Returns:
        List[RecommandationItemDTO]: Offres recommandées triées par score décroissant
    
    Exemple de retour:
        [
            RecommandationItemDTO(offreId=3, nomEntreprise="TechCorp", score=0.87),
            RecommandationItemDTO(offreId=1, nomEntreprise="DataLabs", score=0.65),
            ...
        ]
    """
    
    # Guard si aucune offre n'est fournie
    if not offres:
        return []

    # Étape 1 & 2 : Construire les textes
    texte_etudiant = construire_texte_etudiant(etudiant).strip()
    textes_offres = [construire_texte_offre(offre).strip() for offre in offres]
    
    # Étape 3 : Créer le corpus (étudiant en premier, puis toutes les offres)
    corpus = [texte_etudiant] + textes_offres

    # Si tous les textes sont vides
    if not any(bool(t) for t in corpus):
        return [
            RecommandationItemDTO(
                offreId=offre.id,
                nomEntreprise=offre.nomEntreprise,
                score=0.0
            )
            for offre in offres[:top_n]
        ]
    
    # Étape 4 : Appliquer TF-IDF
    vectorizer = TfidfVectorizer(
        stop_words=None,
        lowercase=True,
        max_features=None
    )
    
    # Vectoriser le corpus : chaque texte devient un vecteur numérique
    try:
        tfidf_matrix = vectorizer.fit_transform(corpus)
    except ValueError:
        # En cas de vocabulaire vide ou problème d'analyse
        return [
            RecommandationItemDTO(
                offreId=offre.id,
                nomEntreprise=offre.nomEntreprise,
                score=0.0
            )
            for offre in offres[:top_n]
        ]
    
    # Étape 5 : Calculer la similarité cosinus
    # tfidf_matrix[0] = vecteur de l'étudiant
    # tfidf_matrix[1:] = vecteurs des offres
    vecteur_etudiant = tfidf_matrix[0:1]  # Garder comme matrice 2D pour cosine_similarity
    vecteurs_offres = tfidf_matrix[1:]
    
    # scores[i] = similarité cosinus entre l'étudiant et l'offre i
    # Retour : matrice de shape (1, n_offres)
    scores = cosine_similarity(vecteur_etudiant, vecteurs_offres)[0]
    
    # Étape 6 : Créer une liste de tuples (indice_offre, score)
    # et trier par score décroissant
    offres_avec_scores = [
        (i, scores[i])
        for i in range(len(offres))
    ]
    offres_avec_scores.sort(key=lambda x: x[1], reverse=True)
    
    # Étape 7 : Construire la réponse (top N seulement)
    recommandations = []
    for indice_offre, score in offres_avec_scores[:top_n]:
        offre = offres[indice_offre]
        recommandations.append(
            RecommandationItemDTO(
                offreId=offre.id,
                nomEntreprise=offre.nomEntreprise,
                score=round(float(score), 2)  # Arrondir à 2 décimales
            )
        )
    
    return recommandations


def analyser_adequation(
    etudiant: EtudiantProfilDTO,
    offre: OffreEntrepriseDTO
) -> AdequationResponseDTO:
    """
    Analyse l'adéquation détaillée et l'écart de compétences (Skill Gap)
    entre un profil étudiant et une offre spécifique.
    """
    etudiant_skills_norm = {s.strip().lower(): s.strip() for s in (etudiant.competences or [])}
    offre_skills = offre.competencesRequises or []

    competences_acquises = []
    competences_manquantes = []

    for req in offre_skills:
        req_norm = req.strip().lower()
        matched = False
        for es_norm in etudiant_skills_norm.keys():
            if req_norm == es_norm or req_norm in es_norm or es_norm in req_norm:
                matched = True
                break
        if matched:
            competences_acquises.append(req)
        else:
            competences_manquantes.append(req)

    # Calcul de similarité textuelle
    recs = calculer_recommandations(etudiant, [offre], top_n=1)
    text_score = recs[0].score if recs else 0.0

    # Score composite pondéré
    if offre_skills:
        skill_ratio = len(competences_acquises) / len(offre_skills)
        combined_score = 0.65 * skill_ratio + 0.35 * text_score
    else:
        combined_score = text_score

    combined_score = max(0.0, min(1.0, combined_score))
    score_pct = int(round(combined_score * 100))

    # Génération des conseils personnalisés
    conseils = []
    if score_pct >= 70:
        conseils.append("Excellente adéquation ! Votre profil correspond étroitement aux attentes de l'entreprise.")
    elif score_pct >= 40:
        conseils.append("Bonne correspondance globale. Mettez en valeur vos projets pratiques dans votre candidature.")
    else:
        conseils.append("Cette offre présente des défis stimulants. Préparez-vous en découvrant les bases des compétences demandées.")

    if competences_manquantes:
        skills_str = ", ".join(competences_manquantes[:3])
        conseils.append(f"Compétences à explorer ou valoriser en entretien : {skills_str}.")

    secteur = (offre.secteurActivite or "").lower()
    sujet = (offre.sujetOffre or "").lower()
    filiere = (etudiant.filiere or "").lower()
    if filiere and (filiere in secteur or filiere in sujet or secteur in filiere):
        conseils.append(f"Votre filière ({etudiant.filiere}) s'aligne directement avec le domaine du stage.")

    return AdequationResponseDTO(
        offreId=offre.id,
        score=round(combined_score, 2),
        scorePourcentage=score_pct,
        competencesAcquises=competences_acquises,
        competencesManquantes=competences_manquantes,
        conseils=conseils
    )

