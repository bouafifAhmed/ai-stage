"""
Script de test du service de recommandation Python.
Permet de tester l'API sans avoir besoin du backend Spring Boot.

Usage:
    python test_example.py

Prérequis:
    - Le service FastAPI doit être en cours d'exécution (uvicorn main:app --reload --port 8000)
"""

import requests
import json

# URL du service
BASE_URL = "http://localhost:8000"

# Exemple de requête
EXEMPLE_REQUETE = {
    "etudiant": {
        "id": 1,
        "filiere": "Informatique",
        "niveau": "L3",
        "competences": ["Python", "Java", "SQL", "Machine Learning"],
        "motsClesSujetSouhaite": "Data Science Deep Learning TensorFlow"
    },
    "offres": [
        {
            "id": 1,
            "nomEntreprise": "TechCorp",
            "secteurActivite": "Informatique",
            "sujetOffre": "Développement backend Python et FastAPI",
            "competencesRequises": ["Python", "FastAPI", "PostgreSQL"]
        },
        {
            "id": 2,
            "nomEntreprise": "DataLabs",
            "secteurActivite": "Big Data",
            "sujetOffre": "Data Science avec Machine Learning et TensorFlow",
            "competencesRequises": ["Python", "Machine Learning", "TensorFlow", "SQL"]
        },
        {
            "id": 3,
            "nomEntreprise": "FinanceApp",
            "secteurActivite": "Finance",
            "sujetOffre": "Développement mobile React Native et JavaScript",
            "competencesRequises": ["JavaScript", "React", "Mobile"]
        },
        {
            "id": 4,
            "nomEntreprise": "CloudSystems",
            "secteurActivite": "Cloud Infrastructure",
            "sujetOffre": "DevOps et gestion d'infrastructure Kubernetes",
            "competencesRequises": ["Docker", "Kubernetes", "Python", "Linux"]
        },
        {
            "id": 5,
            "nomEntreprise": "SecurityPlus",
            "secteurActivite": "Cybersécurité",
            "sujetOffre": "Sécurité réseau et pentesting",
            "competencesRequises": ["Python", "Linux", "Networking"]
        },
        {
            "id": 6,
            "nomEntreprise": "WebDesign Studio",
            "secteurActivite": "Design Web",
            "sujetOffre": "UI/UX et développement frontend Angular",
            "competencesRequises": ["Angular", "TypeScript", "CSS", "Design"]
        }
    ]
}


def test_health_check():
    """Teste l'endpoint health check."""
    print("\n" + "="*70)
    print("TEST 1 : Health Check")
    print("="*70)

    try:
        response = requests.get(f"{BASE_URL}/health")
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.json()}")

        if response.status_code == 200 and response.json().get("status") == "ok":
            print("✅ Health check réussi")
            return True
        else:
            print("❌ Health check échoué")
            return False
    except Exception as e:
        print(f"❌ Erreur : {str(e)}")
        return False


def test_recommandation():
    """Teste l'endpoint de recommandation."""
    print("\n" + "="*70)
    print("TEST 2 : Endpoint Recommandation")
    print("="*70)

    try:
        print("\n📤 Envoi de la requête...")
        print(f"URL: {BASE_URL}/recommander")
        print(f"\nRequête (partielle):")
        print(f"  - Étudiant: {EXEMPLE_REQUETE['etudiant']['filiere']} {EXEMPLE_REQUETE['etudiant']['niveau']}")
        print(f"  - Compétences: {', '.join(EXEMPLE_REQUETE['etudiant']['competences'])}")
        print(f"  - Nombre d'offres: {len(EXEMPLE_REQUETE['offres'])}")

        response = requests.post(
            f"{BASE_URL}/recommander",
            json=EXEMPLE_REQUETE,
            headers={"Content-Type": "application/json"},
            timeout=10
        )

        print(f"\n📥 Réponse reçue")
        print(f"Status Code: {response.status_code}")

        if response.status_code != 200:
            print(f"❌ Erreur HTTP {response.status_code}")
            print(f"Message: {response.text}")
            return False

        result = response.json()
        print(f"\n📊 Résultats:")
        print(f"Nombre de recommandations: {len(result.get('recommandations', []))}")

        if not result.get('recommandations'):
            print("❌ Aucune recommandation retournée")
            return False

        print("\n🏆 Top 5 recommandations:")
        for i, rec in enumerate(result['recommandations'][:5], 1):
            score_pct = int(rec['score'] * 100)
            print(f"  {i}. {rec['nomEntreprise']:20} - Score: {rec['score']:.2f} ({score_pct}%)")
            print(f"     ID Offre: {rec['offreId']}")

        print("\n✅ Recommandations calculées avec succès")

        # Afficher les résultats détaillés
        print("\n📋 Réponse complète (JSON):")
        print(json.dumps(result, indent=2))

        return True

    except requests.exceptions.ConnectionError:
        print("❌ Impossible de se connecter au service")
        print("   Assurez-vous que le service est lancé : uvicorn main:app --reload --port 8000")
        return False
    except requests.exceptions.Timeout:
        print("❌ Timeout (service trop lent)")
        return False
    except Exception as e:
        print(f"❌ Erreur : {str(e)}")
        import traceback
        traceback.print_exc()
        return False


def test_with_custom_data():
    """Teste avec des données personnalisées."""
    print("\n" + "="*70)
    print("TEST 3 : Données personnalisées")
    print("="*70)

    custom_request = {
        "etudiant": {
            "id": 2,
            "filiere": "Génie Logiciel",
            "niveau": "M1",
            "competences": ["Java", "Scala", "Spring Boot", "DevOps"],
            "motsClesSujetSouhaite": "Architecture microservices cloud"
        },
        "offres": [
            {
                "id": 10,
                "nomEntreprise": "Acme Enterprise",
                "secteurActivite": "Entreprise digitale",
                "sujetOffre": "Architecture microservices avec Spring Boot",
                "competencesRequises": ["Java", "Spring Boot", "Docker", "Kubernetes"]
            },
            {
                "id": 11,
                "nomEntreprise": "Retail Corp",
                "secteurActivite": "E-commerce",
                "sujetOffre": "Développement d'application mobile iOS",
                "competencesRequises": ["Swift", "iOS"]
            }
        ]
    }

    try:
        response = requests.post(
            f"{BASE_URL}/recommander",
            json=custom_request,
            timeout=10
        )

        if response.status_code == 200:
            result = response.json()
            print("\n✅ Requête personnalisée réussie")
            print(f"Recommandations retournées:")
            for rec in result['recommandations']:
                print(f"  - {rec['nomEntreprise']}: {rec['score']:.2f}")
            return True
        else:
            print(f"❌ Erreur {response.status_code}")
            return False

    except Exception as e:
        print(f"❌ Erreur : {str(e)}")
        return False


def main():
    """Exécute tous les tests."""
    print("\n" + "="*70)
    print("🧪 TESTS DU SERVICE DE RECOMMANDATION")
    print("="*70)

    # Test 1 : Health check
    health_ok = test_health_check()

    if not health_ok:
        print("\n⚠️  Service non accessible. Impossible de continuer.")
        return

    # Test 2 : Recommandation
    rec_ok = test_recommandation()

    # Test 3 : Données personnalisées
    custom_ok = test_with_custom_data()

    # Résumé
    print("\n" + "="*70)
    print("📊 RÉSUMÉ DES TESTS")
    print("="*70)
    print(f"Health Check:       {'✅' if health_ok else '❌'}")
    print(f"Recommandation:     {'✅' if rec_ok else '❌'}")
    print(f"Données Perso:      {'✅' if custom_ok else '❌'}")

    if health_ok and rec_ok and custom_ok:
        print("\n🎉 Tous les tests sont passés avec succès!")
    else:
        print("\n⚠️  Certains tests ont échoué. Vérifiez les logs.")

    print("\n" + "="*70)


if __name__ == "__main__":
    main()
