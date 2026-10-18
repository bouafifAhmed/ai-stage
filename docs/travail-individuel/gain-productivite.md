# Gain de productivité et argumentation des choix

## Méthode

Un gain de productivité ne se déclare pas, il se mesure. Ce document sépare :

- les **faits vérifiables** (fichiers, tests, défauts trouvés), relevés pendant la session ;
- les **temps**, que seul l'étudiant peut estimer honnêtement (colonnes à compléter).

Pour estimer « sans IA » : se demander combien de temps il aurait fallu pour la même chose, à son
niveau, documentation comprise. Pour « avec IA » : temps réel, **relecture et vérification comprises**.

## Tâches de la session du 18/10/2026

| Tâche | Ce que l'IA a fait | Vérification humaine attendue | Sans IA (estimé) | Avec IA (réel) |
|---|---|---|---|---|
| Analyse de l'écart avec la grille | Lecture de la grille et du code, verdict par critère | Relire le verdict | | |
| Compréhension de l'existant (3 projets) | Lecture du service Python, du client Spring, du contrôleur, des DTO et des composants Angular | Savoir réexpliquer le flux | | |
| Intégration de l'API Claude | `analyse_ia.py` : appel, schéma, garde-fous, repli, cache, journalisation | Relire, tester avec une vraie clé | | |
| Correction de la correspondance des compétences | Comparaison mot à mot + synonymes (Python et Java) | Vérifier les cas pièges | | |
| Optimisation TF-IDF + évaluation | `evaluation.py`, jeu annoté, métriques, analyse des résultats | Valider les étiquettes du jeu | | |
| Corrections Spring | Offres publiées seulement, délai 10 s, repli local, sécurité de `/health` | Relire les modifications | | |
| Interface Angular | Badge selon la source, synthèse, info-bulles, bannière « moteur indisponible » | Tester à l'écran | | |
| Tests | 19 tests Python, 9 tests Java, 8 tests Angular | Lire les assertions | | |
| Synchronisation et vérification (2e session) | `git fetch` et lecture du commit entrant avant `git pull`, `.venv` local, contrôle de compatibilité Python 3.11, scénario de bout en bout sur les vrais serveurs | Rejouer le scénario | | |
| Documentation | README du service, catalogue racine, ce dossier | Relire, compléter les parties « à compléter » | | |
| **Total** | | | | |

## Faits mesurés

| Indicateur | Valeur |
|---|---|
| Fichiers touchés | 31 (Python 9, Spring 8, Angular 7, documentation 6, `.gitignore` 1) |
| Nouveau code Python | `analyse_ia.py` (261 lignes), `evaluation.py` (211 lignes), `recommandation.py` réécrit (344 lignes), `scenario_bout_en_bout.py` |
| Tests backend Spring | 58 → 67, tous au vert |
| Tests du service Python | 4 → 23, tous au vert, sans aucun appel payant |
| Tests Angular | 16 → 24, tous au vert ; build réussi |
| Scénario de bout en bout | 16 vérifications au vert, rejoué deux fois de suite |
| Exactitude de la correspondance des compétences | 32 % → 80 % (règles) |

### Défauts trouvés par l'IA en lisant le code

Aucun n'était demandé explicitement. Chacun est corrigé et couvert par un test (sauf UI, à tester à l'écran).

1. « Java » reconnu dans « JavaScript », « R » dans « React », « C » dans « C++ » (Python et Spring).
2. Les offres en brouillon ou archivées étaient recommandées ; le lien menait à une page 404.
3. Sans offre, Spring envoyait les **identifiants d'entreprise** comme identifiants d'offre :
   « Voir l'offre » ouvrait une autre offre.
4. La propriété `recommandation.service.timeout-seconds=10` n'était lue nulle part : aucun délai,
   un service Python bloqué bloquait la requête de l'étudiant.
5. La bannière d'erreur Angular ne pouvait jamais s'afficher (erreurs transformées en liste vide).
6. Le badge « 🤖 Analyse IA » s'affichait même quand aucune IA n'était utilisée.
7. TF-IDF supprimait `C`, `R`, `C++`, `C#` et distinguait les mots avec ou sans accents.
8. `/api/recommandations/health` n'exigeait pas le rôle étudiant, contrairement à la spécification.

## Limites de l'IA constatées

- **Elle se trompe** : un cas d'erreur non géré (`CredentialsError`), une coquille dans du Java,
  un environnement Python mal diagnostiqué au départ. Les tests et la relecture les ont attrapés
  (détail dans [journal-prompts.md](journal-prompts.md)).
- **Elle propose les choix, l'étudiant les assume** : à l'oral, il faut pouvoir défendre chacun
  (pourquoi 65/35 dans le score, pourquoi 7 s, pourquoi Opus par défaut). Voir
  [justification-modeles.md](justification-modeles.md).
- **La relecture prend du temps** : 31 fichiers modifiés en deux sessions, c'est rapide à produire et
  long à vérifier. Le gain réel est net de ce temps.
- **Coût d'usage** : l'IA intégrée à l'application coûte à chaque analyse (voir l'estimation dans
  la justification), d'où le cache et le choix possible de Haiku.

## Où le gain est le plus fort

1. **Lecture d'un code existant écrit par d'autres** : relier Angular → Spring → Python et trouver
   les défauts 2 à 5, qui ne se voient qu'en lisant les trois couches ensemble.
2. **Tests** : les cas de repli (délai, coupure, refus, JSON invalide, absence de clé) sont
   fastidieux à écrire à la main et rarement testés sans aide.
3. **Documentation de l'API d'un fournisseur** : paramètres exacts du SDK (sortie structurée, délais,
   exceptions), vérifiés dans la documentation plutôt que de mémoire.

Le gain est plus faible sur les **décisions** (quel modèle, quel compromis coût/qualité) : l'IA les
argumente, mais elles demandent des mesures (`evaluation.py --ia`) et un jugement humain.
