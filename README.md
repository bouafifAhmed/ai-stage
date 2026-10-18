# Gestion des stages — catalogue des prompts

Ce fichier décrit les prompts qui correspondent au code déjà présent dans le dépôt. Le dépôt ne conserve pas les conversations d’origine (les commits sont `first commit`, `a` et `gal3et el 3tach vs elbaten`). Chaque prompt ci-dessous est reconstruit à partir des modules, routes, rôles, champs et règles métier réellement codés. On peut les recopier tels quels pour refaire, expliquer ou étendre une partie du projet.

Pour s’en servir : coller un seul prompt, puis préciser « ne change pas les autres modules ». Les noms de champs, d’enums et d’URL ci-dessous sont ceux du code.

## Architecture visée par tous les prompts

Trois applications :

| Application | Dossier | Technologie | Port |
|---|---|---|---|
| API | `imed_backend` | Spring Boot 4.1, Java 17, JPA, Security, JWT | 8081, préfixe `/api` |
| Interface | `gestion-stages-frontend` | Angular 18, composants standalone | 4200 |
| Recommandations | `recommandation-service` | FastAPI, scikit-learn | 8000 |

Héritage JPA : `Utilisateur` est la racine (`InheritanceType.JOINED`, table `utilisateurs`). Il implémente `UserDetails`. L’autorité Spring est `ROLE_` plus le nom de l’enum. Un compte entreprise est un utilisateur de rôle `ENTREPRISE` lié à une entité `Entreprise`.

Base par défaut : MySQL `gestion_stages` sur `localhost:3306`, utilisateur `root`, mot de passe vide, `createDatabaseIfNotExist=true`, `ddl-auto=update`. Profil `local` : H2 en mémoire, console `/h2-console`. Le frontend appelle `http://localhost:4200` vers `http://localhost:8081/api`. CORS n’autorise que cette origine, méthodes GET POST PUT PATCH DELETE OPTIONS, en-têtes `Authorization` et `Content-Type`, credentials autorisés.

Fichiers uploadés, hors du web public :

- CV : `app.upload.cv-dir`, défaut `uploads/cv`, PDF, 20 Mo (`spring.servlet.multipart.max-file-size=20MB`, requête 21 Mo).
- Pièces de tâches : `app.upload.tache-dir`, défaut `uploads/taches-assignees`.
- Signatures de clôture : chemins stockés sur la candidature.

## Vocabulaire à respecter dans tous les prompts

| Enum | Valeurs |
|---|---|
| `Role` | `ETUDIANT`, `ENTREPRISE`, `CHEF_DEPT_STAGE`, `CHEF_DEPT_PEDAGOGIQUE`, `SUPER_ADMIN` |
| `TailleEntreprise` | `TPE`, `PME`, `ETI`, `GRANDE_ENTREPRISE` |
| `StatutValidation` (entreprise) | `EN_ATTENTE`, `VALIDEE`, `REJETEE` |
| `StatutOffre` | `BROUILLON`, `PUBLIEE`, `CLOTUREE`, `ARCHIVEE` |
| `ModeTravail` | `PRESENTIEL`, `HYBRIDE`, `DISTANCIEL` |
| `StatutCandidature` | `EN_ATTENTE`, `ACCEPTEE`, `REFUSEE` |
| `StatutStage` | `ACTIF`, `CLOTURE` |
| `StatutApprobation` (ligne de journal) | `EN_ATTENTE`, `APPROUVEE`, `REJETEE` |
| `StatutTacheAssignee` | `ASSIGNEE`, `EN_COURS`, `TERMINEE`, `VALIDEE`, `REJETEE` |
| `TypeNotification` | `CANDIDATURE_ACCEPTEE`, `TACHE_ASSIGNEE` |
| `TypeReclamation` | `NOTE`, `DOCUMENT`, `EVALUATION`, `AUTRE` |
| `StatutReclamation` | `OUVERTE`, `EN_TRAITEMENT`, `RESOLUE`, `CLOTUREE`, `REOUVERTE` |
| `TypeQuestion` | `CHOIX_UNIQUE`, `CHOIX_MULTIPLE`, `VRAI_FAUX` |

Contraintes répétées : email unique, 190 caractères maximum, format email. Mot de passe entre 8 et 72 caractères. Nom et prénom obligatoires, 100 caractères maximum.

---

## 1. Socle du projet

```text
Crée une application de gestion de stages en trois parties.
Backend Spring Boot Java 17, package com.gestionstages, port 8081, préfixe /api.
Frontend Angular 18 dans gestion-stages-frontend, API dans environment.ts : http://localhost:8081/api.
Microservice Python FastAPI dans recommandation-service, port 8000, sans base de données.
Sécurité stateless JWT (jjwt 0.12.6), mot de passe BCrypt, sessions désactivées, CSRF désactivé.
CORS uniquement depuis http://localhost:4200, en-têtes Authorization et Content-Type, credentials true.
Les erreurs métier renvoient un JSON { instant, status, message, fieldErrors }.
Validation Jakarta : 400 avec une map champ → message.
Email déjà utilisé, entreprise introuvable, statut d’entreprise invalide, étudiant introuvable,
identifiants invalides, compte en attente, rôle non autorisé, accès refusé, règle de stage,
JSON illisible, contrainte d’unicité SQL, fichier trop gros : chacun a son handler.
Au premier démarrage, si app.super-admin.enabled=true, crée un SUPER_ADMIN
admin@imed.com / 123456789 (nom Super, prénom Admin) seulement s’il n’existe pas,
puis le drapeau doit être remis à false.
ddl-auto=update. Ne jamais renvoyer motDePasse dans le JSON (JsonIgnore).
```

## 2. Authentification

```text
Implémente /api/auth, public.
POST /register reçoit nom, prenom, email, motDePasse (8 à 72), role.
Crée un étudiant actif, hash BCrypt, refuse un email déjà utilisé,
et refuse role=SUPER_ADMIN depuis l’inscription publique.
POST /register/entreprise crée l’entité Entreprise (statutValidation EN_ATTENTE par défaut)
et le compte ENTREPRISE inactif, en attente d’approbation.
Champs entreprise : nom (150, obligatoire), adresse (255), ville (100), secteurActivite (150),
taille (TPE PME ETI GRANDE_ENTREPRISE), emailContact unique, telephone (30), siteWeb (255), motDePasse.
POST /login vérifie l’email et le mot de passe.
Si l’entreprise n’est pas VALIDEE, refuse la connexion.
Si les identifiants sont faux, refuse la connexion.
Si la connexion réussit, renvoie un JWT, l’identité et le rôle. Durée 24 h (86400000 ms).
Secret Base64 d’au moins 256 bits, variable JWT_SECRET, valeur de développement seulement en local.
Le filtre JWT s’exécute avant UsernamePasswordAuthenticationFilter.
401 si non authentifié. 403 JSON {"message":"Accès refusé"} si le rôle ne suffit pas.
Le frontend a /login (email, mot de passe, lien créer un compte) et /register.
Après connexion, stocke le jeton et l’utilisateur courant.
Un intercepteur ajoute Authorization: Bearer et redirige vers /login en cas de 401.
Un roleGuard envoie vers /interdit si le rôle ne correspond pas à la route.
Redirections : étudiant /espace-etudiant, entreprise /espace-entreprise,
chefs /espace-administration, super admin /espace-super-admin.
La route vide redirige vers login. Toute route inconnue aussi.
```

## 3. Administration des entreprises

```text
CRUD /api/admin/entreprises réservé au SUPER_ADMIN (@PreAuthorize hasRole SUPER_ADMIN),
même si /api/admin/** est aussi ouvert aux chefs de département au niveau HttpSecurity.
POST crée une entreprise déjà validée : nom, adresse, ville, secteurActivite, taille,
emailContact, telephone, siteWeb, motDePasse.
Refuser un email de contact déjà utilisé.
PUT /{id} ne modifie que les champs fournis (mise à jour partielle).
DELETE /{id} supprime.
PUT /{id}/validation passe le statut à VALIDEE ou REJETEE. Un rejet peut porter un motifRejet (255).
GET liste avec pagination Spring et filtre.
Page Angular /admin/entreprises, guard SUPER_ADMIN uniquement.
Un CHEF_DEPT_STAGE qui tente de créer une entreprise reçoit 403 « Accès refusé ».
Une entreprise EN_ATTENTE ou REJETEE ne peut pas publier ni gérer des offres.
```

## 4. Administration des étudiants

```text
CRUD /api/admin/etudiants réservé au SUPER_ADMIN.
POST crée un étudiant déjà actif : nom, prenom, email, motDePasse, telephone,
filiere (150), niveauEtudes (100), competences (liste, chaque item 100 caractères).
Refuser un email dupliqué.
PUT /{id} modifie les champs fournis, y compris niveauEtudes s’il est présent.
PUT /{id}/activation active ou désactive le booléen actif.
DELETE /{id} supprime.
GET liste paginée et triée.
Page Angular /admin/etudiants, guard SUPER_ADMIN.
Un CHEF_DEPT_STAGE ne peut ni lister ni créer des étudiants (403).
Le compte désactivé ne doit plus passer isEnabled().
```

## 5. Offres de stage côté entreprise

```text
Sous /api/entreprise, rôle ENTREPRISE seulement.
Une entreprise dont statutValidation n’est pas VALIDEE ne peut pas gérer ses offres.
POST /offres crée une offre liée à l’entreprise du compte connecté.
Corps OffreRequest : titre (180, obligatoire), description (obligatoire), domaine (120),
localisation (180), mode PRESENTIEL|HYBRIDE|DISTANCIEL, typeStage (80), dureeMois positif,
dateDebut, dateFin, dateLimite obligatoire, competences (liste, 100 chacune),
niveauRequis (100), nombrePlaces positif obligatoire, remuneration >= 0, statut obligatoire.
Si statut est omis côté entité, défaut BROUILLON. Dates de création et de modification automatiques.
GET /offres liste les offres de l’entreprise connectée, paginé.
La réponse inclut entrepriseId, entrepriseNom, placesOccupees et placesRestantes
(max(0, nombrePlaces - places occupées)).
GET /offres/{id}, PUT /offres/{id}, DELETE /offres/{id}.
Si l’offre n’a aucune candidature, la suppression est physique.
Si l’offre a des candidatures, ne pas laisser de candidatures orphelines.
On ne peut pas réduire nombrePlaces sous le nombre de candidatures ACCEPTEE.
Compétences dans la table offre_competences.
Pages : /espace-entreprise (liste), /espace-entreprise/offres/nouvelle,
/espace-entreprise/offres/:id/modifier.
```

## 6. Catalogue étudiant, profil et CV

```text
Sous /api/etudiant, rôle ETUDIANT.
GET /offres et GET /offres/{id} : offres consultables par l’étudiant, avec places restantes.
GET /profil renvoie id, nom, prenom, email, telephone, filiere, niveauEtudes, competences,
cvPresent, cvNomFichier, cvDateDepot.
PUT /profil accepte telephone (30), filiere (150), niveauEtudes (100), competences.
Ne pas permettre de changer l’email ou le rôle depuis cet écran.
CV PDF, 20 Mo maximum, dossier privé uploads/cv.
POST /profil/cv en multipart enregistre le fichier, cvNomFichier et cvDateDepot.
POST /profil/cv/extraction lit le PDF avec PDFBox 3 et renvoie telephone, filiere,
niveauEtudes, competences, texteDetecte.
Si le PDF n’a pas de texte, texteDetecte=false.
Si aucune information n’est reconnue, renvoyer des champs vides.
GET /profil/cv renvoie application/pdf. DELETE /profil/cv supprime le fichier et les métadonnées.
Refuser un fichier dont l’en-tête n’est pas un vrai PDF.
Pages : /espace-etudiant (catalogue), /espace-etudiant/offres/:id, /espace-etudiant/profil.
```

## 7. Candidatures

```text
L’étudiant postule avec POST /api/etudiant/candidatures.
Contrainte unique uk_candidature_offre_etudiant sur (offre_id, etudiant_id) :
une deuxième candidature à la même offre est refusée.
Champs : offre, etudiant, message libre, dateCandidature automatique,
statut défaut EN_ATTENTE, statutStage défaut ACTIF, encadrant null au départ.
GET /api/etudiant/candidatures liste les siennes.
L’entreprise liste GET /api/entreprise/offres/{offreId}/candidatures,
détail GET /api/entreprise/candidatures/{id} (profil, filière, niveau, compétences, message),
et change le statut avec PATCH /api/entreprise/candidatures/{id}/statut
vers ACCEPTEE ou REFUSEE.
Accepter n’est possible que s’il reste des places.
Quand toutes les places sont prises, refuser une nouvelle acceptation.
Une acceptation crée une notification CANDIDATURE_ACCEPTEE pour l’étudiant.
Pages : /espace-etudiant/candidatures, /espace-entreprise/offres/:id/candidatures,
/espace-entreprise/candidatures/:id.
```

## 8. Notifications

```text
GET /api/etudiant/notifications liste les notifications de l’étudiant connecté.
GET /api/etudiant/notifications/non-lues renvoie le compteur.
PATCH /api/etudiant/notifications/{id}/lue marque une notification comme lue,
sans pouvoir marquer celle d’un autre étudiant.
Types : CANDIDATURE_ACCEPTEE et TACHE_ASSIGNEE.
Plusieurs notifications par stage sont autorisées.
Au démarrage, NotificationSchemaMigration retire la clé étrangère obsolète
puis l’index unique uk_notification_candidature s’il existe encore.
Si l’index a déjà disparu, ne rien faire et ne pas échouer le démarrage.
Page /espace-etudiant/notifications.
Sur le catalogue d’offres, afficher le nombre de non lues et le rafraîchir.
```

## 9. QCM d’admissibilité

```text
Sous /api/etudiant/qcm/admissibilite, rôle ETUDIANT sur toute la classe.
Modèle : Qcm, Question (CHOIX_UNIQUE, CHOIX_MULTIPLE, VRAI_FAUX), OptionReponse, TentativeQcm.
GET renvoie le questionnaire sans indiquer quelles options sont justes.
GET /statut indique si l’étudiant a déjà une tentative valide.
POST /soumettre enregistre la tentative (201).
La tentative n’est valide que si toutes les questions ont les bonnes réponses,
y compris les questions à choix multiple.
Page de passage du QCM branchée sur ces trois endpoints, avec les erreurs HTTP affichées.
```

## 10. Recommandations

```text
Microservice FastAPI sans base. Le backend Spring reste la seule source de vérité.
POST /recommander reçoit :
etudiant { id, filiere, niveau, competences[], motsClesSujetSouhaite }
offres[] { id, nomEntreprise, secteurActivite, sujetOffre, competencesRequises[] }.
Texte étudiant = « filiere niveau compétences mots-clés ».
Texte offre = « secteur sujet compétences ».
TF-IDF (accents supprimés, mots vides français retirés, C, R, C++, C#, Node.js conservés)
puis similarité cosinus, tri décroissant.
GET /health renvoie {"status":"ok"}.
POST /analyser-adequation score une seule offre et renvoie le détail d’adéquation.
Si ANTHROPIC_API_KEY est définie, Claude (claude-opus-5-5, effort low, délai 7 s, aucun nouvel essai)
juge chaque compétence requise (acquise ou non, avec une justification), rédige une synthèse
et 2 à 4 conseils, en JSON contraint par un schéma. N’envoyer ni l’id ni le nom de l’étudiant.
Ignorer les compétences inventées par le modèle, évaluer les oubliées par règles,
et recalculer le score : 0,65 × part des compétences couvertes + 0,35 × similarité TF-IDF.
Sans clé, ou en cas de délai dépassé, d’erreur, de refus ou de JSON invalide : analyse par règles
(comparaison mot à mot, « Java » ne couvre pas « JavaScript », synonymes ML, JS, TS, K8s, IA).
La réponse porte source (ia | regles | regles-spring), modele, synthese et correspondances[].
Côté Spring, construire le profil ainsi : filiere ou « Général » si vide,
niveauEtudes ou « L3 » si vide, compétences copiées, motsClesSujetSouhaite = filiere.
Ne proposer que les offres PUBLIEE (les autres renvoient 404 côté étudiant).
Pour chaque offre : nom d’entreprise ou « Entreprise », secteur = domaine ou « Général »,
sujet = titre + description.
Appeler http://localhost:8000 avec timeout 10 secondes (connexion et lecture du RestTemplate).
Si le service Python est down, ne pas casser l’API : renvoyer une liste vide et logger l’erreur ;
pour l’adéquation, calcul local par règles (source regles-spring).
Endpoints Spring, étudiant authentifié : GET /api/recommandations,
GET /api/recommandations/adequation/{offreId}, GET /api/recommandations/health.
Page /espace-etudiant/recommandations : état chargement, bannière d’erreur, liste triée, filtre.
Si la liste est vide, interroger /health pour signaler un moteur arrêté plutôt qu’une liste vide.
Page offre : badge « Analyse IA » seulement si source = ia, synthèse, justification en info-bulle,
mention « Analyse générée par IA… elle peut se tromper ».
```

Évaluation et choix de modèles : `recommandation-service/evaluation.py` et
`docs/travail-individuel/` (justification, journal des prompts, gain de productivité).

## 11. Journal de stage et progression

```text
Le stage suivi est la candidature acceptée (stageId = id de candidature).
Sous /api/stages/{stageId}/journal.
Rôles HTTP sur /api/stages/** : ETUDIANT, ENTREPRISE, CHEF_DEPT_STAGE.
POST crée une tâche : date obligatoire, titre (150), description obligatoire, pieceJointe optionnelle.
La date doit être dans [dateDebut, dateFin] de l’offre. Refuser si l’utilisateur n’est pas partie prenante.
Statut d’approbation initial EN_ATTENTE. dateSaisie automatique.
PUT /{tacheId} (UpdateTacheDTO) n’est permis que si la ligne a été REJETEE.
GET liste les tâches du stage. GET /progression :
jours ouvrables entre dateDebut et dateFin, tâches au statut valide, jours couverts, pourcentage.
Exemple attendu par le test : du 6 au 26 octobre 2026, 15 jours ouvrables, 3 tâches valides,
2 jours couverts, environ 13,33 %.
POST /upload et GET /{tacheId}/piece-jointe pour les pièces du journal.
GET /pdf génère le journal avec PDFBox. Un journal sans tâche produit quand même un PDF.
commentaireEncadrant et dateApprobation existent sur la tâche.
Pages : /espace-etudiant/journal/:stageId et /espace-etudiant/stages/:stageId/journal.
Le formulaire affiche le message renvoyé par l’API.
```

## 12. Tâches assignées par l’encadrant

```text
Sous /api/stages/{stageId}/taches-assignees.
POST corps CreateTacheAssigneeDTO : titre (150), description (20 000 max), dateEcheance aujourd’hui ou future.
La première assignation fixe candidature.encadrant et crée la tâche au statut ASSIGNEE.
Un membre de la même entreprise peut assigner même si un autre utilisateur est déjà encadrant.
Un encadrant ne peut pas assigner sur le stage d’une autre entreprise.
Créer une notification TACHE_ASSIGNEE.
PUT /{tacheId}/terminer en multipart : seul l’étudiant du stage peut terminer.
Passer à TERMINEE. Un autre étudiant est refusé.
PUT /{tacheId}/valider : refuser si le statut n’est pas TERMINEE. Corps avec décision de validation.
PUT /{tacheId}/rejeter : commentaire obligatoire, non blanc.
Le rejet passe à REJETEE puis remet la tâche EN_COURS et efface la date de validation.
L’étudiant peut resoumettre une tâche rejetée remise en cours.
GET liste avec filtre de statut. GET /{tacheId}/piece-jointe renvoie le blob.
Stockage privé uploads/taches-assignees. Nom serveur généré, nom client d’origine conservé.
Refuser une extension exécutable.
Pages étudiant : /espace-etudiant/stages/:stageId/taches-assignees.
Pages entreprise : /espace-entreprise/stages/:stageId/taches-assignees/assigner
et .../validation.
```

## 13. Échéances, clôture et signatures

```text
GET /api/etudiant/echeances regroupe les dates limites d’offres et les échéances de tâches de l’étudiant.
GET /api/stages/{stageId}/echeances fait la même chose pour un stage, côté étudiant ou entreprise.
Calendrier Angular : /espace-etudiant/calendrier (data.role ETUDIANT),
/espace-etudiant/stages/:stageId/calendrier,
/espace-entreprise/stages/:stageId/calendrier (data.role ENTREPRISE).
GET /api/stages/{stageId}/cloture résume statutStage, présence des deux signatures et dateCloture.
POST /api/stages/{stageId}/signature/etudiant et /signature/encadrant.
Stocker un PNG, chemin dans signatureEtudiantPath ou signatureEncadrantPath,
et la date correspondante. Refuser un fichier vide.
Quand les deux signatures sont présentes, statutStage peut passer à CLOTURE et dateCloture est posée.
GET /api/stages/{stageId}/rapport-pdf renvoie le rapport PDF du stage.
Pages : /espace-etudiant/stages/:stageId/cloture et /espace-entreprise/stages/:stageId/cloture.
```

## 14. Réclamations

```text
POST /api/stages/{stageId}/reclamations ouvre une réclamation.
Corps : typeReclamation obligatoire (NOTE, DOCUMENT, EVALUATION, AUTRE),
objet obligatoire (150 max), messageInitial obligatoire.
Statut initial OUVERTE. Le message initial devient le premier MessageReclamation.
GET /api/stages/{stageId}/reclamations liste celles du stage.
GET /api/entreprise/reclamations liste celles de l’entreprise connectée.
GET /api/reclamations/{id} renvoie le détail et le fil de messages.
POST /api/reclamations/{id}/messages ajoute un message (texte obligatoire).
PUT /resoudre passe à RESOLUE.
PUT /cloturer passe à CLOTUREE.
PUT /rouvrir passe à REOUVERTE.
Un message peut faire passer OUVERTE vers EN_TRAITEMENT.
Pages étudiant : /espace-etudiant/reclamations et /reclamations/:id.
Pages entreprise : /espace-entreprise/reclamations et /reclamations/:id.
Le fil affiche l’auteur, la date, et les boutons résoudre / clôturer / rouvrir selon le rôle.
Afficher le message d’erreur de l’API sur chaque action.
```

## 15. Tests

```text
Backend JUnit 5. Logique métier avec Mockito, sans lever tout le contexte.
Sécurité avec MockMvc et @SpringBootTest seulement là où c’est nécessaire.
AuthServiceTest : hash à l’inscription, email dupliqué, SUPER_ADMIN public refusé,
entreprise inactive en attente, login refusé tant que non approuvée, mauvais mot de passe, jeton si succès.
EntrepriseServiceTest : création déjà validée, email de contact dupliqué, mise à jour partielle.
AdminEtudiantServiceTest : création active, email dupliqué, désactivation, activation, modification.
StageServicesTest : double candidature, suppression d’offre avec et sans candidatures,
entreprise non validée, acceptation, places épuisées, places sous le nombre d’acceptées,
compteur de notifications non lues.
CvExtractionServiceTest : texte reconnu, champs vides, PDF sans texte.
CvStorageServiceTest : PDF valide relu, fichier qui n’est pas un PDF refusé.
SignatureStorageServiceTest : PNG relu, signature vide refusée.
TacheAttachmentStorageServiceTest : nom serveur et nom client, extension exécutable refusée.
JournalPdfServiceTest : PDF avec tâches et PDF sans tâches.
RecommandationClientServiceTest : profil par défaut, offres PUBLIEE seulement, liste vide si Python
est down, adéquation locale si Python est down, offre non publiée refusée, Java ≠ JavaScript.
TacheServiceTest : création, pas le propriétaire, hors dates, modification si non rejetée, progression.
TacheAssigneeServiceTest : autre entreprise, autre étudiant, valider une tâche non terminée,
rejet sans commentaire, collègue de la même entreprise, première assignation,
rejet qui remet EN_COURS, resoumission.
QcmEtudiantServiceTest : toutes les bonnes réponses valident.
NotificationSchemaMigrationTest : suppression FK puis index ; no-op si l’index n’existe plus.
SuperAdminInitializerTest : création si le compte n’existe pas.
UtilisateurTest : autorité ROLE_ + enum.
AdminEntrepriseSecurityTest et AdminEtudiantSecurityTest : CHEF_DEPT_STAGE reçoit 403.
TacheControllerSecurityTest : anonyme 401, étudiant autorisé sur le journal.
RecommandationControllerSecurityTest : anonyme 401 sur les trois endpoints, entreprise 403, étudiant 200 sur /health.
ImedBackendApplicationTests : le contexte de test charge.
Frontend Karma, HttpClientTestingModule :
app.component.spec.ts crée l’application.
auth.service.spec.ts stocke le jeton et l’utilisateur après login.
entreprise.service.spec.ts demande une page filtrée.
entreprise-stage.service.spec.ts : page d’offres, création, mise à jour, candidatures et détail.
etudiant-stage.service.spec.ts : offres, détail, profil, CV (upload, extraction, download, delete),
candidatures, notifications lues.
tache-assignee.service.spec.ts : liste filtrée, assignation, multipart, validation, refus avec stageId dans l’URL, blob.
recommandation.service.spec.ts : pourcentage de score, erreurs propagées, health à false en cas d’erreur, adéquation ou null.
recommandations.component.spec.ts : liste triée et filtre, bannière d’erreur, moteur arrêté, liste vide moteur actif.
Lancer : mvnw test dans imed_backend (67 tests).
Service Python : pytest test_service.py dans recommandation-service (23 tests, client Claude simulé).
npx ng test --watch=false --browsers=ChromeHeadless dans gestion-stages-frontend (24 tests).
Bout en bout (Spring profil local + service Python lancés) : python scenario_bout_en_bout.py actif,
puis arrêter le service Python et lancer python scenario_bout_en_bout.py arrete.
Ne pas laisser Karma ouvert : le port 9876 n’est pas l’application.
```

## 16. Lancement

```text
Démarre les trois processus sans toucher au code.
Frontend : dans gestion-stages-frontend, npm install puis npm start. URL http://localhost:4200/login.
API sans MySQL installé : dans imed_backend,
mvnw spring-boot:run -Dspring-boot.run.profiles=local
(H2 mémoire, données perdues à l’arrêt, super admin admin@imed.com / 123456789).
API avec MySQL : mvnw spring-boot:run, base gestion_stages créée si absente, utilisateur root, mot de passe vide.
Recommandations : dans recommandation-service, python -m venv venv,
venv\Scripts\python -m pip install -r requirements.txt,
puis venv\Scripts\python -m uvicorn main:app --host 127.0.0.1 --port 8000.
Pour l’analyse par IA, définir ANTHROPIC_API_KEY avant de lancer uvicorn ; sans clé, le service reste en mode règles.
Le dossier venv/ est versionné et pointe vers le Python de la dernière personne qui l’a commité :
s’il ne démarre pas, créer un .venv local (ignoré par git) avec py -m venv .venv, sans toucher à venv/.
Vérifier GET http://localhost:8000/health.
Ne pas ouvrir http://localhost:9876 : c’est Karma, il s’arrête à la fin des tests.
```

## 17. Modèle de données

```text
Modélise les tables suivantes, enums en STRING, identifiants IDENTITY.
utilisateurs : nom, prenom, email unique 190, mot_de_passe, role, date_creation, actif,
entreprise_id nullable, telephone 30, filiere 150, niveau_etudes 100, cv_nom_fichier, cv_date_depot.
utilisateur_competences : competence 100.
entreprises : nom 150, adresse, ville, secteur_activite, taille, email_contact unique,
telephone, site_web, statut_validation défaut EN_ATTENTE, date_inscription, motif_rejet.
offres_stage : titre 180, description, domaine, localisation, mode, type_stage, duree_mois,
date_debut, date_fin, date_limite, niveau_requis, nombre_places, remuneration décimal 12,2,
statut défaut BROUILLON, entreprise_id, date_creation, date_modification.
offre_competences.
candidatures : offre_id, etudiant_id, contrainte unique uk_candidature_offre_etudiant,
encadrant_id nullable, message, date_candidature, statut défaut EN_ATTENTE,
statut_stage défaut ACTIF, signature_etudiant_path, signature_encadrant_path,
date_signature_etudiant, date_signature_encadrant, date_cloture.
taches (journal) : stage_id, date, titre, description, piece_jointe,
statut d’approbation EN_ATTENTE, commentaire_encadrant, date_saisie, date_approbation.
taches_assignees : stage, titre, description, date_echeance, statut, pièce jointe, dates de validation.
notifications étudiant : type CANDIDATURE_ACCEPTEE ou TACHE_ASSIGNEE, lue, pas d’unicité par candidature.
reclamations : stage, type, objet 150, statut, dates.
messages de réclamation : auteur, texte, date.
qcm, questions, options, tentatives.
open-in-view=false. Les requêtes passent par les services, pas par les contrôleurs.
```

## 18. Coquille de navigation

```text
Angular standalone. app.routes.ts :
'' et '**' redirigent vers login.
Espaces protégés par authGuard puis roleGuard.
espace-etudiant : offres, offres/:id, profil, candidatures, recommandations, notifications,
reclamations, reclamations/:id, calendrier, stages/:stageId/taches-assignees,
journal/:stageId et stages/:stageId/journal, stages/:stageId/calendrier, stages/:stageId/cloture.
espace-entreprise : offres, reclamations, reclamations/:id, offres/nouvelle, offres/:id/modifier,
offres/:id/candidatures, candidatures/:id, assignation, validation, calendrier, clôture.
espace-administration : CHEF_DEPT_STAGE, CHEF_DEPT_PEDAGOGIQUE, SUPER_ADMIN.
espace-super-admin : SUPER_ADMIN.
admin/entreprises et admin/etudiants : SUPER_ADMIN seul.
interdit : access denied, seulement authentifié.
WorkspaceComponent affiche le titre de l’espace (data.title) et le menu du rôle.
Chaque formulaire affiche les fieldErrors sous le champ et le message global dans une alerte.
```

## 19. Gestion d’erreurs côté écran

```text
Pour chaque écran qui appelle l’API, branche HttpErrorResponse.
400 : afficher fieldErrors sur les contrôles (email déjà utilisé sur le champ email
ou emailContact) et le message général dans serverError.
401 : l’intercepteur vide la session et va sur /login.
403 : message « Accès refusé », lien vers /interdit si on arrive sur une route interdite.
404 : étudiant, entreprise ou offre introuvable, message du corps JSON.
409 ou contrainte d’unicité : candidature déjà envoyée, email déjà pris.
413 : fichier trop gros, rappeler la limite de 20 Mo.
500 : message générique sans stacktrace.
Les listes ont trois états visibles : chargement, erreur avec bouton réessayer, liste vide.
```

## Ce que ce fichier n’est pas

Ce n’est pas une retranscription de conversations passées : elles ne sont pas dans le dépôt. Chaque bloc est le prompt utile pour la fonctionnalité correspondante, aligné sur le code actuel (champs, enums, URL et règles testées).
