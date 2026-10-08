@echo off
REM Script de lancement du microservice Python sur Windows

echo.
echo ===============================================
echo Service de Recommandation - Lancement
echo ===============================================
echo.

REM Vérifier que Python est installé
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python n'est pas installé ou pas dans le PATH
    pause
    exit /b 1
)

echo [1/4] Python détecté
python --version
echo.

REM Créer l'environnement virtuel s'il n'existe pas
if not exist "venv" (
    echo [2/4] Création de l'environnement virtuel...
    python -m venv venv
    echo Environnement virtuel créé
) else (
    echo [2/4] Environnement virtuel existant détecté
)
echo.

REM Activer l'environnement virtuel
echo [3/4] Activation de l'environnement virtuel...
call venv\Scripts\activate.bat
echo Environnement activé
echo.

REM Installer les dépendances
echo [4/4] Installation des dépendances...
pip install -q -r requirements.txt
if errorlevel 1 (
    echo ERROR: Installation des dépendances échouée
    pause
    exit /b 1
)
echo Dépendances installées
echo.

REM Lancer le service
echo ===============================================
echo Lancement du service sur http://localhost:8000
echo Health check: http://localhost:8000/health
echo Documentation: http://localhost:8000/docs
echo ===============================================
echo.
echo Appuyez sur CTRL+C pour arrêter le service
echo.

uvicorn main:app --reload --port 8000

pause
