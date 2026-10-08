"""
Microservice FastAPI de recommandation étudiant-entreprise.

Point d'entrée principal. Expose :
- GET  /health                 : Vérification que le service tourne
- POST /recommander            : Calcule les offres recommandées pour un étudiant

À lancer avec :
    uvicorn main:app --reload --port 8000

Ou en production :
    uvicorn main:app --host 0.0.0.0 --port 8000
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import logging

from models import (
    RecommandationRequestDTO,
    RecommandationResponseDTO,
    HealthCheckResponse
)
from recommandation import calculer_recommandations


# Configuration du logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# Instanciation de l'application FastAPI
app = FastAPI(
    title="Service de Recommandation - Gestion de Stages",
    description="Microservice de recommandation basé sur TF-IDF et similarité cosinus",
    version="1.0.0"
)


# Configuration CORS : autoriser les appels depuis le backend Spring Boot
# Le frontend Angular ne doit PAS appeler ce service directement,
# c'est le backend Spring qui fait l'intermédiaire
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8081"],  # Backend Spring Boot uniquement
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health", response_model=HealthCheckResponse)
async def health_check():
    """
    Endpoint de vérification de l'état du service.
    
    Utilisé pour tester que le microservice Python est actif et accessible.
    Appelé généralement par le backend Spring Boot avant d'envoyer les vraies requêtes.
    
    Returns:
        HealthCheckResponse: {"status": "ok"}
    
    Example:
        curl http://localhost:8000/health
    """
    logger.info("Health check appelé")
    return HealthCheckResponse(status="ok")


@app.post("/recommander", response_model=RecommandationResponseDTO)
async def recommander(request: RecommandationRequestDTO):
    """
    Endpoint principal : calcule les offres recommandées pour un étudiant.
    
    Reçoit :
    - Le profil complet d'un étudiant (filière, niveau, compétences, souhaits)
    - La liste COMPLÈTE des offres disponibles en base de données
    
    Retourne :
    - Une liste triée des meilleures offres (top 5 par défaut) avec leurs scores de pertinence
    
    Processus :
    1. Construit une représentation textuelle du profil étudiant
    2. Construit une représentation textuelle de chaque offre
    3. Applique TF-IDF pour vectoriser les textes
    4. Calcule la similarité cosinus entre l'étudiant et chaque offre
    5. Trie par score et retourne le top N
    
    Args:
        request (RecommandationRequestDTO): 
            {
                "etudiant": {
                    "id": 123,
                    "filiere": "Informatique",
                    "niveau": "L3",
                    "competences": ["Python", "Java", "SQL"],
                    "motsClesSujetSouhaite": "Data Science Machine Learning"
                },
                "offres": [
                    {
                        "id": 1,
                        "nomEntreprise": "TechCorp",
                        "secteurActivite": "Informatique",
                        "sujetOffre": "Développement backend Python",
                        "competencesRequises": ["Python", "Django", "PostgreSQL"]
                    },
                    ...
                ]
            }
    
    Returns:
        RecommandationResponseDTO: 
            {
                "recommandations": [
                    {
                        "offreId": 1,
                        "nomEntreprise": "TechCorp",
                        "score": 0.87
                    },
                    {
                        "offreId": 3,
                        "nomEntreprise": "DataLabs",
                        "score": 0.65
                    },
                    ...
                ]
            }
    
    Raises:
        HTTPException: 400 si la requête est invalide (validation Pydantic)
        HTTPException: 500 si une erreur interne se produit
    
    Example:
        curl -X POST http://localhost:8000/recommander \\
             -H "Content-Type: application/json" \\
             -d '{
                   "etudiant": {"id": 1, "filiere": "Info", "niveau": "L3", 
                                "competences": ["Python"], "motsClesSujetSouhaite": "Data"},
                   "offres": [...]
                 }'
    """
    try:
        logger.info(f"Requête de recommandation reçue pour l'étudiant {request.etudiant.id}")
        logger.info(f"Nombre d'offres à analyser : {len(request.offres)}")
        
        # Valider qu'il y a au moins une offre
        if not request.offres:
            logger.warning(f"Aucune offre fournie pour l'étudiant {request.etudiant.id}")
            return RecommandationResponseDTO(recommandations=[])
        
        # Appeler le moteur de recommandation
        recommandations = calculer_recommandations(
            etudiant=request.etudiant,
            offres=request.offres,
            top_n=5  # Retourner les 5 meilleures offres
        )
        
        logger.info(f"{len(recommandations)} recommandations calculées pour l'étudiant {request.etudiant.id}")
        
        # Retourner la réponse
        return RecommandationResponseDTO(recommandations=recommandations)
    
    except Exception as e:
        logger.error(f"Erreur lors du calcul des recommandations : {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Erreur interne lors du calcul des recommandations : {str(e)}"
        )


# Point d'entrée pour lancer le serveur
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
