# Aplikacja BizWise 
Aplikacja mobilna na system *Android* pomagająca w nauce zarządzania finansami osobistymi.

## Struktura
Aplikacja składa się z części frontendowej napisanej w `Kotlin` przy użyciu `Android Studio` oraz części backendowej napisanej w `Python` z wykorzystaniem frameworka `Django`.

## Backend
Backend aplikacji obsługuje logikę biznesową, zarządzanie bazą danych oraz komunikację z frontendem poprzez API RESTful.
Składa się z następujących aplikacji Django:
- `config` - konfiguracja projektu
- `users` - zarządzanie użytkownikami, rejestracja, logowanie
- `courses` - zarządzanie kursami
- `quizzes` - zarządzanie quizami i ocenami
- `forum` - zarządzanie forami dyskusyjnymi

### Development
Aby uruchomić backend lokalnie, należy wykonać następujące kroki:
1. Sklonować repozytorium
2. Utworzyć i aktywować w katalogu `backend/` wirtualne środowisko pythonowe
```bash
python -m venv django
source django/bin/activate
```
3. Zainstalować wymagane pakiety
```bash
pip install -r req.txt
```
4. Uruchomić serwer deweloperski
```bash
python manage.py runserver
```
Zarządzanie bazą danych/strukturą odbywa się poprzez standardowe `manage.py`. Więcej informacji można znaleźć w [dokumentacji Django](https://docs.djangoproject.com/en/6.0/).
### Deployment 
Projekt kożysta z `CI/CD` do automatycznego wdrażania backendu na serwerze przy użyciu *Github Actions*.

## Frontend

Reszta wasza Chłopaki

Kontrybutorzy:
- Szymon Hładyszewski
- Wiktor Koczkodaj
- Jakub Kogut
- Wojciech Typer
