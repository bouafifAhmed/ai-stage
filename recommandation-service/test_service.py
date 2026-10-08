import pytest
from fastapi.testclient import TestClient
from main import app
from recommandation import calculer_recommandations, construire_texte_etudiant, construire_texte_offre
from models import EtudiantProfilDTO, OffreEntrepriseDTO

client = TestClient(app)

def test_health():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}

def test_recommander_endpoint():
    payload = {
        "etudiant": {
            "id": 1,
            "filiere": "Informatique",
            "niveau": "L3",
            "competences": ["Python", "Machine Learning"],
            "motsClesSujetSouhaite": "Data Science"
        },
        "offres": [
            {
                "id": 10,
                "nomEntreprise": "Alpha Data",
                "secteurActivite": "Data Science",
                "sujetOffre": "Stage Data Science Python",
                "competencesRequises": ["Python", "Machine Learning"]
            },
            {
                "id": 20,
                "nomEntreprise": "Beta Accounting",
                "secteurActivite": "Comptabilite",
                "sujetOffre": "Audit comptable et finance",
                "competencesRequises": ["Excel", "Finance"]
            }
        ]
    }
    response = client.post("/recommander", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "recommandations" in data
    recs = data["recommandations"]
    assert len(recs) == 2
    assert recs[0]["offreId"] == 10
    assert recs[0]["score"] > recs[1]["score"]

def test_calculer_recommandations_empty_offres():
    etudiant = EtudiantProfilDTO(
        id=1, filiere="Info", niveau="M1", competences=["Java"], motsClesSujetSouhaite=None
    )
    res = calculer_recommandations(etudiant, [], top_n=5)
    assert res == []

def test_analyser_adequation_endpoint():
    payload = {
        "etudiant": {
            "id": 1,
            "filiere": "Informatique",
            "niveau": "L3",
            "competences": ["Python", "FastAPI"],
            "motsClesSujetSouhaite": "Web"
        },
        "offre": {
            "id": 101,
            "nomEntreprise": "DevCorp",
            "secteurActivite": "Informatique",
            "sujetOffre": "Développement API Python et Docker",
            "competencesRequises": ["Python", "FastAPI", "Docker", "PostgreSQL"]
        }
    }
    response = client.post("/analyser-adequation", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["offreId"] == 101
    assert "scorePourcentage" in data
    assert "Python" in data["competencesAcquises"]
    assert "FastAPI" in data["competencesAcquises"]
    assert "Docker" in data["competencesManquantes"]
    assert len(data["conseils"]) > 0

