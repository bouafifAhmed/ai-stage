# Justification des modèles choisis

Trois choix distincts : le modèle de **classement** des offres, le modèle d'**analyse** d'une offre,
et l'**assistant de développement**.

## 1. Classement des offres : TF-IDF + similarité cosinus

Contraintes : classer *toutes* les offres publiées à chaque ouverture de la page, en moins de 10 s,
sans coût par requête, avec un résultat reproductible et explicable.

| Option | Sens des mots | Coût par requête | Latence (50 offres) | Infrastructure | Verdict |
|---|---|---|---|---|---|
| **TF-IDF + cosinus** | Non (mots exacts) | 0 | Quelques ms | scikit-learn | **Retenu** |
| BM25 | Non | 0 | Quelques ms | Bibliothèque en plus | Pas de gain de sens par rapport à TF-IDF |
| Plongements de phrases (sentence-transformers) | Oui | 0 | ~1 s sur CPU | PyTorch (plusieurs centaines de Mo), modèle à télécharger | Évolution possible si le catalogue grossit |
| LLM qui classe les offres | Oui | Proportionnel au nombre d'offres | Plusieurs secondes, risque de dépasser 10 s | Clé API | Rejeté : coût, latence, résultat non déterministe |

### Optimisation mesurée

`python evaluation.py` (8 profils x 10 offres, pertinence annotée à la main) :

| Configuration | Top-1 | P@3 | MRR | nDCG@5 | Marge moyenne | Marge minimale |
|---|---|---|---|---|---|---|
| Origine | 1.00 | 0.58 | 1.00 | 0.98 | 0.29 | 0.00 |
| Retenue | 1.00 | 0.58 | 1.00 | 0.98 | 0.35 | 0.19 |

Lecture honnête :

- **Les métriques de rang ne bougent pas.** Les offres du jeu sont très différentes les unes des
  autres (finance, marketing, embarqué...), le classement est déjà correct avec la configuration
  d'origine. P@3 plafonne à 0,67 car la plupart des profils n'ont que deux offres pertinentes.
- **La marge change.** La marge est l'écart entre la meilleure offre très pertinente et la meilleure
  offre hors sujet. Pour le profil « Systemes embarques » (tapé sans accents, compétences C et C++),
  l'origine donnait 0,03 à la bonne offre et 0,03 à une offre hors sujet : premier rang gagné à
  l'arrondi près. La configuration retenue donne 0,38 contre 0,03.
- **Effet secondaire observé** : sans accents, « Developpement web » rapproche aussi l'offre de
  firmware (« Développement de pilotes »), qui monte à 0,15. Le bon classement reste inchangé.
- **Limite** : jeu petit, construit par l'équipe. Il compare deux variantes ; il ne prouve pas la
  qualité sur de vraies données. À refaire avec des offres réelles et des choix d'étudiants.

## 2. Analyse d'une offre : LLM (API Claude)

### Pourquoi un LLM ici et pas pour le classement

L'analyse ne porte que sur **une** offre, celle que l'étudiant ouvre. Un appel par consultation,
mis en cache, reste raisonnable. C'est là que comprendre le sens apporte le plus : dire si
« MySQL » couvre « SQL » et rédiger des conseils propres à l'offre.

Mesure sur 25 paires (compétence requise, compétence de l'étudiant) choisies pour couvrir les cas
pièges :

| Méthode | Exactitude | Erreurs restantes |
|---|---|---|
| Sous-chaîne (origine) | 32 % | Java/JavaScript, R/React, C/C++, SQL/NoSQL, ML, K8s, accents... (17 erreurs) |
| Mots + synonymes (repli actuel) | 80 % | Réseaux/Réseaux sociaux, SQL/MySQL, Machine Learning/Apprentissage automatique, Méthodes agiles/Scrum, Base de données/PostgreSQL |
| LLM | _à mesurer : `python evaluation.py --ia`_ | |

Les 5 erreurs restantes des règles sont **sémantiques** : aucune liste de synonymes raisonnable ne
les couvre toutes. C'est l'argument pour le LLM. La ligne LLM reste vide tant qu'elle n'a pas été
mesurée avec une vraie clé : ne pas annoncer de chiffre avant.

### Choix du fournisseur et du modèle

Anthropic (Claude), via le SDK Python officiel `anthropic`, pour :

- la **sortie structurée** (`output_config.format` + schéma JSON) : pas d'analyse fragile de texte libre ;
- le **repli serveur** en cas de refus (`fallbacks: "default"`) ;
- un SDK asynchrone compatible avec FastAPI, avec délais et nouveaux essais configurables.

| Modèle | Prix entrée / sortie (par million de jetons) | Coût estimé par analyse* | Rôle |
|---|---|---|---|
| `claude-opus-5-5` | 4 $ / 20 $ | ~0,016 $ | **Par défaut** : meilleure qualité de jugement et de rédaction en français |
| `claude-haiku-5-5` | 0,10 $ / 0,50 $ | ~0,0004 $ | Alternative 40 fois moins chère, en principe plus rapide |

\* Ordre de grandeur pour ~600 jetons en entrée et ~700 en sortie (réflexion comprise). Les vrais
chiffres sont écrits dans les logs du service à chaque appel (`jetons en entrée`, `en sortie`).

Réglages :

- **`effort: "low"`** : la tâche est courte et bien cadrée ; un effort plus élevé augmenterait la
  latence sans gain attendu, et l'appel doit finir en moins de 7 s.
- **Délai 7 s, aucun nouvel essai** : Spring coupe à 10 s ; un nouvel essai pourrait doubler la durée.
- **Pas de `temperature`** : paramètre refusé par ce modèle ; la reproductibilité vient du schéma
  et du score calculé par le code.

**Décision à prendre après mesure** : lancer `python evaluation.py --ia` deux fois, avec
`RECO_IA_MODELE=claude-opus-5-5` puis `RECO_IA_MODELE=claude-haiku-5-5`. Si Haiku atteint la même
exactitude avec une latence plus faible, il est le meilleur choix en production (coût divisé par 40).
Le changement ne demande qu'une variable d'environnement.

### Limites et éthique

- **Erreurs possibles** : le modèle peut se tromper ; l'interface le dit, et la décision de candidater
  reste à l'étudiant.
- **Données personnelles** : seules la filière, le niveau et les compétences sont envoyés ; ni nom,
  ni email, ni identifiant.
- **Équité** : le score repose sur les compétences déclarées. Un étudiant qui remplit mal son profil
  est désavantagé, quel que soit le modèle. Piste : extraire les compétences du CV (déjà fait côté
  Spring avec PDFBox) et inviter l'étudiant à compléter son profil.
- **Dépendance à un service externe** : sans clé ou sans réseau, l'application continue en mode règles.

## 3. Assistant de développement : Claude Code

| Option | Forces | Limites pour cette tâche |
|---|---|---|
| **Claude Code** (agent, Claude Opus 5.5) | Lit les trois projets, exécute tests et build, modifie plusieurs fichiers de façon cohérente | Doit être relu ; peut se tromper (voir le journal) |
| GitHub Copilot (complétion dans l'éditeur) | Rapide pour du code local, ligne par ligne | Ne lance ni tests ni build ; peu adapté à un changement qui touche Python, Java et Angular à la fois |

La tâche touchait trois langages et un contrat de données commun ; un agent capable d'exécuter les
tests après chaque modification était plus adapté.
