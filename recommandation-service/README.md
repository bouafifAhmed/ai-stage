# Service de Recommandation - Gestion de Stages

Microservice Python FastAPI qui calcule les offres de stage recommandées pour un étudiant basé sur la similarité entre son profil et les offres disponibles.

## 📋 Architecture

- **Framework** : FastAPI
- **Algorithme** : TF-IDF + Similarité Cosinus
- **Base de données** : Aucune (stateless) - Spring Boot reste la source unique de vérité
- **Port** : 8000 (par défaut)

## 🛠️ Installation

### Prérequis
- Python 3.10+ (exigé par anthropic, fastapi et pydantic ; testé avec Python 3.14)
- pip

### Étapes

1. **Naviguer vers le dossier du service**
   ```bash
   cd recommandation-service
   ```

2. **Créer un environnement virtuel**

   Le dossier `venv/` est versionné : il pointe vers le Python de la personne qui l'a commité et
   ne démarre pas forcément sur votre poste. Dans ce cas, créez un `.venv` local, ignoré par git,
   au lieu de modifier `venv/` :
   ```powershell
   py -m venv .venv
   .venv\Scripts\python -m pip install -r requirements.txt
   .venv\Scripts\python -m uvicorn main:app --host 127.0.0.1 --port 8000
   ```

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

### Prétraitement retenu (après évaluation)

- accents supprimés (`strip_accents="unicode"`) : « Developpement » = « Développement » ;
- mots vides français retirés (« de », « et », « la »...) ;
- motif de découpage qui garde `C`, `R`, `C++`, `C#`, `Node.js` (le motif par défaut de scikit-learn les supprime).

`python evaluation.py` compare cette configuration à celle d'origine sur un petit jeu annoté.

## 🤖 Analyse d'adéquation par IA (Claude)

`POST /analyser-adequation` appelle Claude (API Anthropic) quand une clé est configurée :

| Étape | Rôle |
|---|---|
| Prompt | profil (filière, niveau, compétences, souhait) + offre (secteur, sujet, compétences). Ni id ni nom envoyés |
| Sortie | JSON contraint par schéma : verdict + justification par compétence requise, synthèse, 2 à 4 conseils |
| Contrôle | compétences inventées ignorées, compétences oubliées évaluées par les règles |
| Score | recalculé par le service : 65 % compétences couvertes + 35 % similarité TF-IDF |
| Repli | sans clé, délai dépassé (7 s), erreur, refus ou JSON invalide → analyse par règles (`source: "regles"`) |
| Cache | 256 analyses en mémoire, une même paire profil/offre n'est facturée qu'une fois |

```bash
# Windows (PowerShell)
$env:ANTHROPIC_API_KEY = "sk-ant-..."
# Optionnel
$env:RECO_IA_MODELE = "claude-opus-5-5"   # ou claude-haiku-5-5, moins cher et plus rapide
$env:RECO_IA_EFFORT = "low"               # low | medium | high
$env:RECO_IA_TIMEOUT = "7"                # secondes, doit rester sous le délai de 10 s de Spring
$env:RECO_IA_ACTIVE = "0"                 # forcer le mode règles
```

La réponse indique `source` (`ia` ou `regles`) et `modele`. Le frontend affiche « Analyse IA »
uniquement si `source` vaut `ia`.

## 📝 Structure du projet

```
recommandation-service/
├── main.py              # Point d'entrée FastAPI, endpoints
├── models.py            # Schémas Pydantic (DTOs)
├── recommandation.py    # TF-IDF + similarité cosinus, analyse d'adéquation par règles
├── analyse_ia.py        # Analyse d'adéquation par Claude, garde-fous et repli
├── evaluation.py        # Évaluation hors ligne (classement, correspondance des compétences)
├── scenario_bout_en_bout.py  # Scénario réel Spring + service Python (actif puis arrêté)
├── test_service.py      # Tests pytest (LLM simulé, aucun appel réel)
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

### Avec Pytest
```bash
pytest test_service.py
```

### Évaluation
```bash
python evaluation.py         # sans appel payant
python evaluation.py --ia    # ajoute le LLM (nécessite ANTHROPIC_API_KEY, ~25 appels)
```

### Bout en bout (Spring + service Python)
```bash
# Spring : mvnw spring-boot:run -Dspring-boot.run.profiles=local ; service Python sur 8000
python scenario_bout_en_bout.py actif    # 13 vérifications
# arrêter le service Python, puis :
python scenario_bout_en_bout.py arrete   # 3 vérifications du repli côté Spring
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
