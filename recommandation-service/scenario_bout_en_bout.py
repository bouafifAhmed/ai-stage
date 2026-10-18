"""
Scénario de bout en bout de la section 10 (Recommandations) : Angular n'est pas utilisé,
on appelle l'API Spring comme le ferait le frontend.

Prérequis :
- Spring en profil local (H2, super admin admin@imed.com / 123456789) :
      mvnw spring-boot:run -Dspring-boot.run.profiles=local
- Service Python sur le port 8000.

Usage :
    python scenario_bout_en_bout.py actif     # service Python démarré
    (arrêter le service Python)
    python scenario_bout_en_bout.py arrete    # vérifie le repli côté Spring

Chaque exécution crée une entreprise, trois offres et un étudiant avec des emails uniques.
"""
import json
import sys
import tempfile
import time
import urllib.error
import urllib.request
from datetime import date, timedelta
from pathlib import Path

API = "http://127.0.0.1:8081/api"
ETAT = Path(tempfile.gettempdir()) / "scenario_recommandation.json"
SUFFIXE = str(int(time.time()))


def appel(methode, chemin, corps=None, jeton=None):
    donnees = json.dumps(corps).encode("utf-8") if corps is not None else None
    req = urllib.request.Request(API + chemin, data=donnees, method=methode)
    req.add_header("Content-Type", "application/json; charset=utf-8")
    if jeton:
        req.add_header("Authorization", f"Bearer {jeton}")
    debut = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            texte = r.read().decode("utf-8")
            statut = r.status
    except urllib.error.HTTPError as e:
        texte, statut = e.read().decode("utf-8", "replace"), e.code
    duree = time.perf_counter() - debut
    try:
        contenu = json.loads(texte) if texte else None
    except json.JSONDecodeError:
        contenu = texte
    return statut, contenu, duree


def verifier(condition, message):
    print(("OK   " if condition else "ECHEC") + " " + message)
    if not condition:
        verifier.echecs += 1


verifier.echecs = 0


def connexion(email, mdp):
    statut, rep, _ = appel("POST", "/auth/login", {"email": email, "motDePasse": mdp})
    assert statut == 200, (statut, rep)
    return rep["token"]


def phase_service_actif():
    for _ in range(90):
        try:
            jeton_admin = connexion("admin@imed.com", "123456789")
            break
        except (urllib.error.URLError, ConnectionError, AssertionError):
            time.sleep(2)
    else:
        raise SystemExit("Spring ne répond pas sur 8081")

    statut, ent, _ = appel("POST", "/admin/entreprises", {
        "nom": "Predicta", "adresse": "1 rue", "ville": "Tunis", "secteurActivite": "Data",
        "taille": "PME", "emailContact": f"rh{SUFFIXE}@predicta.tn", "telephone": "70000000",
        "siteWeb": "https://predicta.tn", "motDePasse": "motdepasse1"}, jeton_admin)
    verifier(statut in (200, 201), f"entreprise créée par le super admin ({statut})")
    jeton_ent = connexion(f"rh{SUFFIXE}@predicta.tn", "motdepasse1")

    limite = (date.today() + timedelta(days=30)).isoformat()

    def offre(titre, description, domaine, competences, statut_offre):
        s, o, _ = appel("POST", "/entreprise/offres", {
            "titre": titre, "description": description, "domaine": domaine, "localisation": "Tunis",
            "mode": "HYBRIDE", "typeStage": "PFE", "dureeMois": 6, "dateLimite": limite,
            "competences": competences, "niveauRequis": "M1", "nombrePlaces": 2,
            "remuneration": 500, "statut": statut_offre}, jeton_ent)
        assert s in (200, 201), (s, o)
        return o["id"]

    data = offre("Data scientist", "Modèles de machine learning en Python pour prévoir les ventes",
                 "Data / IA", ["Python", "Machine Learning", "SQL", "Docker"], "PUBLIEE")
    web = offre("Développeur front-end", "Interface Angular et TypeScript", "Informatique",
                ["Angular", "TypeScript"], "PUBLIEE")
    brouillon = offre("Data science avancée", "Machine learning Python deep learning",
                      "Data / IA", ["Python", "Machine Learning"], "BROUILLON")

    s, _, _ = appel("POST", "/auth/register", {"nom": "Ben Ali", "prenom": "Sami", "email": f"sami{SUFFIXE}@etud.tn",
                                               "motDePasse": "motdepasse1", "role": "ETUDIANT"})
    verifier(s in (200, 201), f"étudiant inscrit ({s})")
    jeton_etu = connexion(f"sami{SUFFIXE}@etud.tn", "motdepasse1")
    s, _, _ = appel("PUT", "/etudiant/profil", {"telephone": "71000000", "filiere": "Data science",
                                                 "niveauEtudes": "M1",
                                                 "competences": ["Python", "ML", "MySQL", "JavaScript"]}, jeton_etu)
    verifier(s == 200, f"profil étudiant mis à jour ({s})")

    s, recs, d = appel("GET", "/recommandations", jeton=jeton_etu)
    ids = [r["offreId"] for r in recs] if isinstance(recs, list) else []
    score = {r["offreId"]: r["score"] for r in recs} if isinstance(recs, list) else {}
    # Des offres identiques d'exécutions précédentes peuvent être ex aequo : on compare les scores
    verifier(s == 200 and data in score and score[data] == recs[0]["score"] and score.get(web, 0) < score[data],
             f"GET /recommandations : offre data en tête ({len(recs)} offres, {d:.2f} s)")
    verifier(brouillon not in ids, "offre en BROUILLON absente des recommandations")
    verifier(all(recs[i]["score"] >= recs[i + 1]["score"] for i in range(len(recs) - 1)), "tri décroissant")

    s, ad, _ = appel("GET", f"/recommandations/adequation/{data}", jeton=jeton_etu)
    verifier(s == 200 and ad.get("source") == "regles", f"adéquation via Python, source={ad.get('source') if isinstance(ad, dict) else ad}")
    verifier(isinstance(ad, dict) and ad.get("competencesAcquises") == ["Python", "Machine Learning"],
             f"ML reconnu, MySQL non reconnu par les règles : {ad.get('competencesAcquises') if isinstance(ad, dict) else ''}")
    verifier(isinstance(ad, dict) and len(ad.get("correspondances", [])) == 4, "correspondances[] transmises par Spring")

    s, _, _ = appel("GET", f"/recommandations/adequation/{brouillon}", jeton=jeton_etu)
    verifier(s == 404, f"adéquation d'une offre en brouillon refusée ({s})")

    s, sante, _ = appel("GET", "/recommandations/health", jeton=jeton_etu)
    verifier(s == 200 and sante is True, f"health étudiant = {sante}")
    s, _, _ = appel("GET", "/recommandations/health")
    verifier(s == 401, f"health anonyme refusé ({s})")
    s, _, _ = appel("GET", "/recommandations/health", jeton=jeton_ent)
    verifier(s == 403, f"health entreprise refusé ({s})")

    ETAT.write_text(json.dumps({"etu": jeton_etu, "data": data}), encoding="utf-8")


def phase_service_arrete():
    etat = json.loads(ETAT.read_text(encoding="utf-8"))
    s, recs, d = appel("GET", "/recommandations", jeton=etat["etu"])
    verifier(s == 200 and recs == [], f"Python arrêté : 200 et liste vide ({s}, {recs}, {d:.2f} s)")
    s, ad, _ = appel("GET", f"/recommandations/adequation/{etat['data']}", jeton=etat["etu"])
    verifier(s == 200 and isinstance(ad, dict) and ad.get("source") == "regles-spring",
             f"Python arrêté : adéquation locale, source={ad.get('source') if isinstance(ad, dict) else ad}")
    s, sante, _ = appel("GET", "/recommandations/health", jeton=etat["etu"])
    verifier(s == 200 and sante is False, f"Python arrêté : health = {sante}")


if __name__ == "__main__":
    {"actif": phase_service_actif, "arrete": phase_service_arrete}[sys.argv[1]]()
    print(f"\n{verifier.echecs} échec(s)")
    sys.exit(1 if verifier.echecs else 0)
