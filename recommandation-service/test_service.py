import json
from types import SimpleNamespace

import anthropic
import httpx2
import pytest
from fastapi.testclient import TestClient

import analyse_ia
from main import app
from recommandation import (
    CONFIG_TFIDF_INITIALE,
    calculer_recommandations,
    competences_correspondent,
    construire_texte_etudiant,
    construire_texte_offre,
)
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


# --- Correspondance des compétences et TF-IDF -------------------------------

@pytest.mark.parametrize("requise, possedee", [
    ("Java", "JavaScript"),
    ("JavaScript", "Java"),
    ("R", "React"),
    ("C", "C++"),
    ("SQL", "NoSQL"),
])
def test_competences_voisines_ne_correspondent_pas(requise, possedee):
    assert not competences_correspondent(requise, possedee)


@pytest.mark.parametrize("requise, possedee", [
    ("Machine Learning", "ML"),
    ("Sécurité", "securite"),
    ("Node.js", "NodeJS"),
    ("Spring Boot", "Spring"),
    ("Python", "Python 3"),
])
def test_synonymes_accents_et_variantes_correspondent(requise, possedee):
    assert competences_correspondent(requise, possedee)


def test_tfidf_garde_cpp_et_ignore_les_accents():
    etudiant = EtudiantProfilDTO(id=1, filiere="Systemes embarques", niveau="L3",
                                 competences=["C", "C++"], motsClesSujetSouhaite="Systemes embarques")
    offres = [
        OffreEntrepriseDTO(id=1, nomEntreprise="Embedia", secteurActivite="Systèmes embarqués",
                           sujetOffre="Pilotes en C et C++", competencesRequises=["C", "C++"]),
        OffreEntrepriseDTO(id=2, nomEntreprise="Webly", secteurActivite="Informatique",
                           sujetOffre="Plateforme web", competencesRequises=["React"]),
    ]
    origine = calculer_recommandations(etudiant, offres, config_tfidf=CONFIG_TFIDF_INITIALE)
    retenue = calculer_recommandations(etudiant, offres)
    assert origine[0].score == 0.0  # « C++ » et « Systèmes » invisibles pour la configuration d'origine
    assert retenue[0].offreId == 1
    assert retenue[0].score > 0.3


# --- Analyse d'adéquation par LLM (client Anthropic simulé, aucun appel réel) ---

PAYLOAD_ADEQUATION = {
    "etudiant": {
        "id": 42,
        "filiere": "Data science",
        "niveau": "M1",
        "competences": ["Python", "ML", "MySQL"],
        "motsClesSujetSouhaite": "Data science"
    },
    "offre": {
        "id": 7,
        "nomEntreprise": "Predicta",
        "secteurActivite": "Data / IA",
        "sujetOffre": "Prévision des ventes par machine learning",
        "competencesRequises": ["Python", "Machine Learning", "SQL", "Docker"]
    }
}


class _FauxMessages:
    def __init__(self, reponse=None, erreur=None):
        self.reponse, self.erreur, self.appels = reponse, erreur, []

    async def create(self, **kwargs):
        self.appels.append(kwargs)
        if self.erreur:
            raise self.erreur
        return self.reponse


def _reponse_llm(contenu, stop_reason="end_turn"):
    return SimpleNamespace(
        stop_reason=stop_reason,
        model="claude-opus-5-5",
        content=[SimpleNamespace(type="text", text=json.dumps(contenu))],
        usage=SimpleNamespace(input_tokens=600, output_tokens=250),
    )


@pytest.fixture
def faux_llm(monkeypatch):
    def installer(reponse=None, erreur=None):
        messages = _FauxMessages(reponse, erreur)
        monkeypatch.setattr(analyse_ia, "_client", SimpleNamespace(beta=SimpleNamespace(messages=messages)))
        monkeypatch.setattr(analyse_ia, "ACTIVE", True)
        monkeypatch.setattr(analyse_ia, "_sans_identifiants", False)
        monkeypatch.setattr(analyse_ia, "_cache", analyse_ia.OrderedDict())
        return messages
    return installer


def test_adequation_ia_correspondance_semantique(faux_llm):
    messages = faux_llm(_reponse_llm({
        "correspondances": [
            {"competenceRequise": "Python", "acquise": True, "justification": "Présent dans le profil."},
            {"competenceRequise": "Machine Learning", "acquise": True, "justification": "ML = Machine Learning."},
            {"competenceRequise": "SQL", "acquise": True, "justification": "MySQL est un SGBD SQL."},
            {"competenceRequise": "Kubernetes", "acquise": True, "justification": "Inventée par le LLM."},
        ],
        "synthese": "Profil très proche de l'offre.",
        "conseils": ["Mettez en avant un projet de prévision.", "Découvrez Docker.", "  "],
    }))

    data = client.post("/analyser-adequation", json=PAYLOAD_ADEQUATION).json()

    assert data["source"] == "ia"
    assert data["modele"] == "claude-opus-5-5"
    assert data["competencesAcquises"] == ["Python", "Machine Learning", "SQL"]
    # Docker oublié par le LLM : évalué par les règles. Kubernetes inventé : ignoré.
    assert data["competencesManquantes"] == ["Docker"]
    assert [c["competenceRequise"] for c in data["correspondances"]] == ["Python", "Machine Learning", "SQL", "Docker"]
    assert data["conseils"] == ["Mettez en avant un projet de prévision.", "Découvrez Docker."]
    assert data["synthese"] == "Profil très proche de l'offre."
    # Le score est recalculé par le service : 0.65 * 3/4 + 0.35 * similarité textuelle
    assert 0.48 <= data["score"] <= 0.84

    message_envoye = messages.appels[0]["messages"][0]["content"]
    assert "42" not in message_envoye and "Predicta" not in message_envoye
    assert messages.appels[0]["output_config"]["format"]["type"] == "json_schema"


def test_adequation_ia_cache(faux_llm):
    messages = faux_llm(_reponse_llm({"correspondances": [], "synthese": "Ok.", "conseils": ["Postulez."]}))
    client.post("/analyser-adequation", json=PAYLOAD_ADEQUATION)
    client.post("/analyser-adequation", json=PAYLOAD_ADEQUATION)
    assert len(messages.appels) == 1


_REQUETE_API = httpx2.Request("POST", "https://api.anthropic.com/v1/messages")


@pytest.mark.parametrize("reponse, erreur", [
    (None, anthropic.APITimeoutError(request=_REQUETE_API)),
    (None, anthropic.APIConnectionError(request=_REQUETE_API)),
    (_reponse_llm({}, stop_reason="refusal"), None),
    (_reponse_llm({"correspondances": "pas une liste"}), None),
], ids=["delai-depasse", "api-injoignable", "refus", "json-invalide"])
def test_adequation_repli_sur_les_regles(faux_llm, reponse, erreur):
    faux_llm(reponse, erreur)
    response = client.post("/analyser-adequation", json=PAYLOAD_ADEQUATION)
    assert response.status_code == 200
    data = response.json()
    assert data["source"] == "regles"
    assert data["modele"] is None
    # Les règles reconnaissent ML grâce aux synonymes, mais pas MySQL pour SQL
    assert data["competencesAcquises"] == ["Python", "Machine Learning"]
    assert data["competencesManquantes"] == ["SQL", "Docker"]


@pytest.mark.parametrize("erreur", [
    TypeError("Could not resolve authentication method. Expected one of api_key, auth_token, or credentials to be set."),
    anthropic.CredentialsError("Config file not found"),
], ids=["sans-cle", "profil-invalide"])
def test_adequation_sans_identifiants_reste_en_mode_regles(faux_llm, erreur):
    messages = faux_llm(erreur=erreur)

    premiere = client.post("/analyser-adequation", json=PAYLOAD_ADEQUATION).json()
    seconde = client.post("/analyser-adequation", json=PAYLOAD_ADEQUATION).json()

    assert premiere["source"] == seconde["source"] == "regles"
    assert analyse_ia.ia_disponible() is False
    assert len(messages.appels) == 1  # plus aucune tentative une fois l'absence de clé constatée
