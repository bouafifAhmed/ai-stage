# Travail individuel — module Recommandations (IA)

Étudiant : ______________________ Classe : 5SAE____ Projet : Gestion des stages

Périmètre personnel : section 10 du catalogue de prompts (`README.md` à la racine).
Microservice Python de recommandation, client Spring, page Angular des recommandations
et analyse d'adéquation affichée sur la page d'une offre.

## Correspondance avec la grille (travail individuel, 8 pts)

| Critère | Preuves |
|---|---|
| **Intégration d'une API IA / modèle** (3,5 pts) | Modèle TF-IDF + similarité cosinus pour le classement : [recommandation.py](../../recommandation-service/recommandation.py). API Claude pour l'analyse d'adéquation (sortie JSON contrainte, garde-fous, repli) : [analyse_ia.py](../../recommandation-service/analyse_ia.py). Branchement Spring : [RecommandationClientService.java](../../imed_backend/src/main/java/com/gestionstages/service/RecommandationClientService.java). Affichage : [etudiant-offre-detail.component.ts](../../gestion-stages-frontend/src/app/components/etudiant-offre-detail/etudiant-offre-detail.component.ts) |
| **Maîtrise technique** (1 pt) | [maitrise-technique.md](maitrise-technique.md) : TF-IDF, cosinus, LLM, sortie structurée, dégradation progressive, tests avec LLM simulé |
| **Documentation individuelle : prompts, interactions LLM, justification des modèles** (2 pts) | [journal-prompts.md](journal-prompts.md) (prompts de développement et prompt système de l'application) et [justification-modeles.md](justification-modeles.md) (choix appuyés sur [evaluation.py](../../recommandation-service/evaluation.py)) |
| **Présentation et argumentation, gain de productivité** (1,5 pt) | [gain-productivite.md](gain-productivite.md) et l'argumentaire oral ci-dessous |

## Résultats en un coup d'œil

| Mesure | Avant | Après |
|---|---|---|
| Exactitude de la correspondance des compétences (25 cas pièges) | 32 % | 80 % par règles ; les erreurs restantes sont sémantiques, traitées par le LLM |
| Marge minimale du bon classement (8 profils x 10 offres) | 0,00 (égalité, bon rang par hasard) | 0,19 |
| Tests backend Spring | 58 | 67 |
| Tests du service Python | 4 | 23 (client Claude simulé, aucun appel payant) |
| Tests Angular | 16 | 24 |
| Scénario de bout en bout, Spring et Python réels ([scenario_bout_en_bout.py](../../recommandation-service/scenario_bout_en_bout.py)) | aucun | 16 vérifications, service Python actif puis arrêté |
| Offres non publiées proposées à l'étudiant | oui (lien en 404) | non |
| Délai d'appel du service Python | aucun (propriété ignorée) | 10 s |

## Démo (3 minutes)

1. Lancer le service Python **sans** clé : `uvicorn main:app --port 8000`. Ouvrir une offre côté
   étudiant : badge « ⚙️ Analyse automatique », `source: "regles"`.
2. Arrêter uvicorn, définir `ANTHROPIC_API_KEY`, relancer. Même offre : badge « 🤖 Analyse IA »,
   synthèse rédigée, justification au survol de chaque compétence.
3. Profil avec « ML » et « MySQL », offre demandant « Machine Learning » et « SQL » :
   le LLM les reconnaît, les règles ne reconnaissent que « ML ».
4. Arrêter le service Python : la page des recommandations affiche « moteur indisponible »,
   l'API Spring répond toujours (analyse locale, `source: "regles-spring"`).
5. `python evaluation.py` : tableau avant / après.
6. Option : `python scenario_bout_en_bout.py actif` puis `arrete` pour rejouer les points 1 et 4
   automatiquement.

## Argumentaire oral (5 minutes)

1. **Problème** (30 s) : un étudiant ne lit pas 50 offres ; il faut les classer et expliquer l'écart
   de compétences pour une offre donnée.
2. **Deux besoins, deux modèles** (1 min) : classer toutes les offres doit être rapide, gratuit et
   déterministe, donc TF-IDF. Expliquer une offre demande de comprendre le sens (« ML » =
   « Machine Learning »), donc un LLM, appelé une seule fois par offre consultée et mis en cache.
3. **Garde-fous** (1 min) : JSON imposé par un schéma, score recalculé par le code, compétences
   inventées ignorées, données personnelles non envoyées, repli sur règles, badge honnête dans l'UI.
4. **Mesures** (1 min) : 32 % → 80 % sur les cas pièges, marge de classement, 36 tests ajoutés
   (19 Python, 9 Spring, 8 Angular) et un scénario de bout en bout.
   Limites : jeu de données petit et construit à la main.
5. **IA dans le développement** (1 min 30) : ce que Claude Code a fait (lecture des 3 projets,
   8 défauts trouvés, tests, évaluation), ce que j'ai vérifié, les erreurs de l'IA corrigées
   (voir [journal-prompts.md](journal-prompts.md)) et le temps gagné (voir [gain-productivite.md](gain-productivite.md)).
