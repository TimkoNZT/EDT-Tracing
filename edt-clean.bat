@echo off
del /F /Q "D:\EDT\Workspace\.metadata\.log" 2>nul
start "" "C:\Program Files\1C\1CE\components\1c-edt-2026.1.1+1-x86_64\1cedt.exe" -clean
