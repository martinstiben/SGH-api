@echo off
echo ============================================
echo    EJEMPLOS DE PLANTILLAS HTML - SGH
echo ============================================
echo.
echo Abriendo ejemplos de plantillas en el navegador...
echo.

REM Abrir versiones PREMIUM (con animaciones)
echo [PREMIUM] Abriendo versiones con efectos visuales...
start estudiante.html
timeout /t 1 /nobreak > nul

start maestro.html
timeout /t 1 /nobreak > nul

start director.html
timeout /t 1 /nobreak > nul

start coordinador.html
timeout /t 1 /nobreak > nul

start general.html
timeout /t 1 /nobreak > nul

echo.
echo [SIMPLE] Abriendo versiones compatibles con email...
start estudiante-simple.html
timeout /t 1 /nobreak > nul

start maestro-simple.html
timeout /t 1 /nobreak > nul

echo.
echo ============================================
echo    ¡Todos los ejemplos abiertos!
echo ============================================
echo.
echo 🌟 VERSIONES PREMIUM (Animadas - Para Web):
echo • estudiante.html      - Verde (Estudiantes)
echo • maestro.html         - Azul (Docentes)
echo • director.html        - Purpura (Directores)
echo • coordinador.html     - Naranja (Coordinadores)
echo • general.html         - Gris (General)
echo.
echo 📧 VERSIONES SIMPLE (Email-Compatible):
echo • estudiante-simple.html - Verde (Estudiantes)
echo • maestro-simple.html    - Azul (Docentes)
echo.
echo 💡 RECOMENDACIONES:
echo • Usa PREMIUM para visualizar en navegador
echo • Usa SIMPLE para envío real por email
echo.
echo Presiona cualquier tecla para continuar...
pause > nul