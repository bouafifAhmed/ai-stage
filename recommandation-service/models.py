"""
Schémas Pydantic pour les requêtes et réponses du service de recommandation.
Miroir exact des DTOs du backend Spring Boot pour assurer la compatibilité JSON.
"""

from pydantic import BaseModel, Field
from typing import List, Optional


class EtudiantProfilDTO(BaseModel):
    """
    Profil d'un étudiant envoyé par le backend Spring Boot.
    Utilisé pour construire le vecteur textuel côté Python (TF-IDF).
    """
    id: int
    filiere: str = Field(..., description="Filière d'études de l'étudiant")
    niveau: str = Field(..., description="Niveau d'études (ex: L1, L2, L3, M1, M2)")
    competences: List[str] = Field(default_factory=list, description="Liste des compétences de l'étudiant")
    motsClesSujetSouhaite: Optional[str] = Field(default=None, description="Mots-clés décrivant le sujet/domaine souhaité pour le stage")


class OffreEntrepriseDTO(BaseModel):
    """
    Une offre de stage d'une entreprise.
    Utilisé pour construire le vecteur textuel de chaque offre (TF-IDF).
    """
    id: int
    nomEntreprise: str = Field(..., description="Nom de l'entreprise")
    secteurActivite: str = Field(..., description="Secteur d'activité (ex: IT, Finance, Santé)")
    sujetOffre: str = Field(..., description="Description textuelle du sujet/poste de stage")
    competencesRequises: List[str] = Field(default_factory=list, description="Compétences requises pour ce stage")


class RecommandationRequestDTO(BaseModel):
    """
    Requête du backend Spring Boot au microservice Python.
    Contient l'étudiant ET la liste complète des offres disponibles.
    Spring Boot reste la source unique de vérité ; le service Python ne stocke rien.
    """
    etudiant: EtudiantProfilDTO
    offres: List[OffreEntrepriseDTO] = Field(..., description="Toutes les offres disponibles à comparer")


class RecommandationItemDTO(BaseModel):
    """
    Une recommandation individuelle : une offre avec son score de pertinence.
    """
    offreId: int
    nomEntreprise: str
    score: float = Field(..., description="Score de similarité cosinus entre 0 et 1, arrondi à 2 décimales")


class RecommandationResponseDTO(BaseModel):
    """
    Réponse du service Python au backend Spring Boot.
    Liste des offres triées par score de pertinence (décroissant).
    """
    recommandations: List[RecommandationItemDTO] = Field(..., description="Offres recommandées triées par score décroissant")


class HealthCheckResponse(BaseModel):
    """Simple health check response."""
    status: str = "ok"
