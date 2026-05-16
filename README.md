[README.md](https://github.com/user-attachments/files/27156782/README.md)
# AI Resume Forge

AI-powered resume building platform that helps users craft professional resumes using artificial intelligence.

## Architecture

```
/frontend         - Vite + Vue3 (Port 5173)
/java-backend     - Spring Boot Java API (Port 8080)
/python-backend   - FastAPI Python API (Port 8000)
```

## Quick Start

### Frontend

```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

### Java Backend

```bash
cd java-backend
# Requires Maven or Gradle
./mvnw spring-boot:run
# Runs on http://localhost:8080
```

### Python Backend

```bash
cd python-backend
pip install -r requirements.txt
uvicorn main:app --reload
# Runs on http://localhost:8000
```

## Ports

| Service       | Port |
|---------------|------|
| Frontend      | 5173 |
| Java Backend  | 8080 |
| Python Backend| 8000 |

## Development

See [ARCHITECTURE.md](./ARCHITECTURE.md) for detailed architecture information.
