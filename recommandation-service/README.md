# Service de Recommandation - Gestion de Stages

Microservice Python FastAPI qui calcule les offres de stage recommandées pour un étudiant basé sur la similarité entre son profil et les offres disponibles.

## 📋 Architecture

- **Framework** : FastAPI
- **Algorithme** : TF-IDF + Similarité Cosinus
- **Base de données** : Aucune (stateless) - Spring Boot reste la source unique de vérité
- **Port** : 8000 (par défaut)

## 🛠️ Installation

### Prérequis
- Python 3.8+
- pip

### Étapes

1. **Naviguer vers le dossier du service**
   ```bash
   cd recommandation-service
   ```

2. **Créer un environnement virtuel**
   ```bash
   # Windows
   python -m venv venv
   venv\Scripts\activate
   
   # Linux/Mac
   python -m venv venv
   source venv/bin/activate
   ```

3. **Installer les dépendances**
   ```bash
   pip install -r requirements.txt
   ```

## 🚀 Lancement

### Mode développement (avec rechargement automatique)
```bash
uvicorn main:app --reload --port 8000
```

### Mode production
```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

Le service sera accessible sur `http://localhost:8000`

## 📚 API Endpoints

### 1. Health Check
Vérifie que le service est actif.

**Requête**
```bash
curl http://localhost:8000/health
```

**Réponse (200 OK)**
```json
{
  "status": "ok"
}
```

### 2. Recommander
Calcule les offres recommandées pour un étudiant.

**Requête**
```bash
curl -X POST http://localhost:8000/recommander \
  -H "Content-Type: application/json" \
  -d '{
    "etudiant": {
      "id": 1,
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
        "sujetOffre": "Développement mobile React Native",
        "competencesRequises": ["JavaScript", "React", "Mobile"]
      }
    ]
  }'
```

**Réponse (200 OK)**
```json
{
  "recommandations": [
    {
      "offreId": 2,
      "nomEntreprise": "DataLabs",
      "score": 0.87
    },
    {
      "offreId": 1,
      "nomEntreprise": "TechCorp",
      "score": 0.72
    },
    {
      "offreId": 3,
      "nomEntreprise": "FinanceApp",
      "score": 0.34
    }
  ]
}
```

## 🧮 Algorithme Expliqué

### TF-IDF (Term Frequency - Inverse Document Frequency)

**Concept** : Chaque mot reçoit un poids en fonction de son importance.

- **TF (Fréquence du terme)** : Nombre de fois où un mot apparaît dans le document
  - Exemple : Si "Python" apparaît 3 fois dans le profil étudiant, TF("Python") est élevé
  
- **IDF (Fréquence inverse du document)** : Pénalise les mots trop courants
  - Exemple : Le mot "développement" est commun à beaucoup d'offres, donc IDF faible
  - Le mot "TensorFlow" est rare, donc IDF élevé
  
- **TF-IDF = TF × IDF** : Récompense les mots spécifiques et significatifs

### Similarité Cosinus

**Concept** : Mesure l'angle entre deux vecteurs. 

- Score entre **0 et 1**
  - 0 = aucune similarité (perpendiculaire)
  - 1 = identique (même direction)
  
- **Indépendante de la longueur** : Un profil court et un profil long peuvent avoir une similarité identique s'ils partagent les mêmes termes importants

**Exemple** :
```
Étudiant : "Informatique L3 Python Java SQL Data Science"
Offre 1  : "Big Data Data Science Machine Learning Python SQL"
Offre 2  : "Finance Comptabilité"

Similarité(Étudiant, Offre1) = 0.85 ← Élevée (termes communs)
Similarité(Étudiant, Offre2) = 0.15 ← Faible (peu de termes communs)
```

## 📝 Structure du projet

```
recommandation-service/
├── main.py              # Point d'entrée FastAPI, endpoints
├── models.py            # Schémas Pydantic (DTOs)
├── recommandation.py    # Logique TF-IDF + similarité cosinus
├── requirements.txt     # Dépendances Python
└── README.md            # Cette documentation
```

## 🔗 Intégration avec Spring Boot

Le backend Spring Boot appelle ce service comme suit :

1. **Récupère** l'étudiant connecté depuis la base de données
2. **Récupère** toutes les offres disponibles
3. **Envoie** une requête POST `http://localhost:8000/recommander` avec les données
4. **Affiche** les recommandations au frontend Angular

Voir `RecommandationClientService.java` dans le backend pour les détails.

## 🧪 Tests rapides

### Avec Postman
1. Importer l'endpoint POST `http://localhost:8000/recommander`
2. Copier-coller le JSON de requête ci-dessus
3. Vérifier la réponse

### Avec Pytest (optionnel)
```bash
pip install pytest httpx
pytest tests/
```

## 🚨 Troubleshooting

### "Connection refused" depuis Spring Boot
- Vérifier que le service Python tourne sur `localhost:8000`
- Commande : `netstat -ano | findstr "8000"` (Windows)

### "No module named 'sklearn'"
```bash
pip install scikit-learn
```

### "No module named 'fastapi'"
```bash
pip install fastapi uvicorn
```

### Port 8000 déjà utilisé
Lancer sur un port différent :
```bash
uvicorn main:app --reload --port 8001
# Et mettre à jour application.properties du backend
```

## 📈 Performance

- **Temps d'exécution** : ~5-50ms par requête (dépend du nombre d'offres)
- **Mémoire** : ~50MB au lancement, +10-20MB par requête
- **Scalabilité** : Stateless, peut être répliqué horizontalement

## 📄 Licence

Projet académique - 2026

## ✅ Checklist de déploiement

- [ ] Python 3.8+ installé
- [ ] Environnement virtuel créé et activé
- [ ] `pip install -r requirements.txt` exécuté
- [ ] Service lancé sur port 8000
- [ ] Health check répond
- [ ] Backend Spring Boot pointé sur `http://localhost:8000`
- [ ] Logs affichent "Uvicorn running on..."
- [ ] Requête test vers `/recommander` retourne JSON valide
