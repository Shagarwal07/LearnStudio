@echo off
title LearnHub – Fresh Setup
color 0A
cls

echo.
echo  ██╗     ███████╗ █████╗ ██████╗ ███╗   ██╗██╗  ██╗██╗   ██╗██████╗
echo  ██║     ██╔════╝██╔══██╗██╔══██╗████╗  ██║██║  ██║██║   ██║██╔══██╗
echo  ██║     █████╗  ███████║██████╔╝██╔██╗ ██║███████║██║   ██║██████╔╝
echo  ██║     ██╔══╝  ██╔══██║██╔══██╗██║╚██╗██║██╔══██║██║   ██║██╔══██╗
echo  ███████╗███████╗██║  ██║██║  ██║██║ ╚████║██║  ██║╚██████╔╝██████╔╝
echo  ╚══════╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝ ╚═════╝ ╚═════╝
echo.
echo  Fresh Environment Setup for Windows
echo  ─────────────────────────────────────────────────────────────────────
echo.

:: ── Check Admin Rights ────────────────────────────────────────────────────
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo  [ERROR] Run this file as Administrator!
    echo  Right-click SETUP.bat → "Run as administrator"
    pause
    exit /b 1
)

:: ── Check winget ──────────────────────────────────────────────────────────
where winget >nul 2>&1
if %errorLevel% neq 0 (
    echo  [ERROR] winget not found.
    echo  Update Windows to 1809+ or install App Installer from Microsoft Store.
    pause
    exit /b 1
)

echo  [1/6] Checking Java 17...
java -version >nul 2>&1
if %errorLevel% equ 0 (
    echo        Java already installed. Skipping.
) else (
    echo        Installing JDK 17 via winget...
    winget install --id Microsoft.OpenJDK.17 --accept-source-agreements --accept-package-agreements -h
    if %errorLevel% neq 0 (
        echo  [WARN] winget JDK install failed. Trying Eclipse Temurin...
        winget install --id EclipseAdoptium.Temurin.17.JDK --accept-source-agreements --accept-package-agreements -h
    )
    echo        JDK 17 installed.
)

echo.
echo  [2/6] Setting JAVA_HOME...
:: Find JDK path — try common locations
set "JDK_PATH="
for /d %%D in ("C:\Program Files\Microsoft\jdk-17*") do set "JDK_PATH=%%D"
if not defined JDK_PATH (
    for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do set "JDK_PATH=%%D"
)
if not defined JDK_PATH (
    for /d %%D in ("C:\Program Files\Java\jdk-17*") do set "JDK_PATH=%%D"
)

if defined JDK_PATH (
    setx JAVA_HOME "%JDK_PATH%" /M >nul
    setx PATH "%PATH%;%JDK_PATH%\bin" /M >nul
    set "JAVA_HOME=%JDK_PATH%"
    echo        JAVA_HOME = %JDK_PATH%
) else (
    echo  [WARN] Could not auto-detect JDK path.
    echo         Set JAVA_HOME manually after install.
)

echo.
echo  [3/6] Checking Maven...
mvn -version >nul 2>&1
if %errorLevel% equ 0 (
    echo        Maven already installed. Skipping.
) else (
    echo        Installing Maven via winget...
    winget install --id Apache.Maven --accept-source-agreements --accept-package-agreements -h
    :: Refresh PATH for this session
    for /f "tokens=2*" %%A in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v PATH 2^>nul') do set "SYS_PATH=%%B"
    set "PATH=%SYS_PATH%"
    echo        Maven installed.
)

echo.
echo  [4/6] Checking MySQL 8...
mysql --version >nul 2>&1
if %errorLevel% equ 0 (
    echo        MySQL already installed. Skipping.
) else (
    echo        Installing MySQL 8 via winget...
    winget install --id Oracle.MySQL --accept-source-agreements --accept-package-agreements -h
    echo        MySQL installed.
    echo.
    echo  [NOTE] MySQL root password in this project = P@ssword2073
    echo         If you set a different password during install,
    echo         update it in: backend\src\main\resources\application.properties
)

echo.
echo  [5/6] Checking VS Code...
code --version >nul 2>&1
if %errorLevel% equ 0 (
    echo        VS Code already installed. Skipping.
) else (
    echo        Installing VS Code via winget...
    winget install --id Microsoft.VisualStudioCode --accept-source-agreements --accept-package-agreements -h
    echo        VS Code installed.
)

echo.
echo  [6/6] Installing VS Code Extensions...
code --install-extension vscjava.vscode-java-pack >nul 2>&1
code --install-extension vscjava.vscode-spring-boot-dashboard >nul 2>&1
code --install-extension ritwickdey.LiveServer >nul 2>&1
code --install-extension rangav.vscode-thunder-client >nul 2>&1
echo        Extensions installed:
echo          - Java Extension Pack  (run Java in VS Code)
echo          - Spring Boot Dashboard (start/stop from sidebar)
echo          - Live Server           (frontend on :5500)
echo          - Thunder Client        (API testing like Postman)

echo.
echo  ─────────────────────────────────────────────────────────────────────
echo  SETUP COMPLETE!
echo  ─────────────────────────────────────────────────────────────────────
echo.
echo  HOW TO RUN (3 ways — pick any):
echo.
echo  WAY 1 — VS Code Tasks (easiest):
echo    1. Open this folder in VS Code
echo    2. Press Ctrl+Shift+B  →  Spring Boot starts in terminal
echo    3. Open index.html with Live Server (right-click → Open with Live Server)
echo.
echo  WAY 2 — Spring Boot Dashboard (visual):
echo    1. Open VS Code
echo    2. Click the Spring Boot icon in left sidebar (looks like a leaf)
echo    3. Click the Play button next to LmsApplication
echo.
echo  WAY 3 — Manual CMD:
echo    cd backend
echo    mvn spring-boot:run
echo.
echo  FRONTEND: Open index.html → Right-click → Open with Live Server
echo  BACKEND:  http://localhost:8080
echo  ADMIN:    Open admin.html in browser
echo.
echo  ─────────────────────────────────────────────────────────────────────
echo  FIRST TIME DB SETUP:
echo    mysql -u root -pYOUR_PASSWORD lms_db ^< backend\src\main\resources\full-seed.sql
echo  ─────────────────────────────────────────────────────────────────────
echo.

set /p RUNOW="Start Spring Boot now? (y/n): "
if /i "%RUNOW%"=="y" (
    echo.
    echo  Starting Spring Boot... (this takes ~30 seconds first time)
    echo  Watch for: "Started LmsApplication" in the output below
    echo.
    cd backend
    mvn spring-boot:run
) else (
    echo.
    echo  Open this folder in VS Code and press Ctrl+Shift+B to start.
    pause
)
