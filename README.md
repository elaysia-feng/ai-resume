# AI Resume Forge

AI-powered resume building platform that helps users craft professional resumes using artificial intelligence. Users input a job description (JD), and the system analyzes gaps between their resume and the job requirements, then generates targeted modifications through a multi-agent AI system.

## Architecture

```
┌─────────────┐     ┌─────────────────┐     ┌──────────────────┐
│             │     │                 │     │                  │
│  Frontend   │────>│   Java Backend  │────>│  Python Backend  │
│   (Vue3)    │<────│  (Spring Boot)  │<────│    (FastAPI)     │
│   :5173     │     │     :8080       │     │      :8000       │
│             │     │                 │     │                  │
└─────────────┘     └─────────────────┘     └──────────────────┘
      │                     │                        │
      │    REST API         │   Internal API         │
      └─────────────────────┴────────────────────────┘
```

| Service         | Port | Role                                                    |
|-----------------|------|---------------------------------------------------------|
| Frontend        | 5173 | User interface, Copilot UI, real-time event subscription |
| Java Backend    | 8080 | Auth, session management, resume CRUD, SSE proxy        |
| Python Backend  | 8000 | LangGraph multi-agent, RAG, LLM integration             |

## Tech Stack

### Frontend
- **Vite + Vue 3** - Build tool and UI framework
- **Pinia** - State management
- **GSAP + @vuea/motion** - Animations
- **html2canvas + jspdf** - PDF export
- **Axios** - HTTP client

### Java Backend
- **Spring Boot 3.2.5** - Web framework
- **MyBatis Plus** - ORM
- **JWT (jjwt 0.12.5)** - Authentication
- **Aliyun OSS** - File storage (avatars, resumes)
- **MySQL** - Business data
- **Redis** - LangGraph checkpoint storage

### Python Backend
- **FastAPI** - Web framework
- **LangGraph** - Multi-agent orchestration
- **LangChain** - LLM integration
- **Qdrant** - Vector store for RAG
- **Redis** - LangGraph checkpointer
- **MiniMax / OpenAI** - LLM provider

## Features

### AI Agent Flow

The Python backend implements a multi-node LangGraph workflow:

```
bootstrap → summarize_conversation → supervisor
  ├─> jd_analyst → gap_analyzer → retriever → rewriter → reviewer
  │     ├─> rewriter (retry loop)
  │     └─> approval_packager → END
  └─> clarifier → END (waiting for user input)
```

**Agent Nodes:**
- `bootstrap` - Loads resume context from Java backend
- `supervisor` - Routes to JD analyst or clarifier
- `jd_analyst` - Extracts JD requirements, keywords, priorities
- `gap_analyzer` - Identifies resume-JD gaps
- `retriever` - Agentic RAG from Qdrant knowledge base
- `rewriter` - Generates section patch proposals
- `reviewer` - Validates schema, prevents fabrication
- `clarifier` - Asks user for missing information
- `approval_packager` - Packages patches for user confirmation

### Resume Management
- CRUD operations for resumes and sections
- Version history tracking
- Section schema validation
- Patch preview and apply with AI suggestions

### Interview Assistant
- AI-powered interview question generation
- Real-time evaluation and feedback

### Authentication
- JWT-based authentication
- Login/Register with email verification codes

## Project Structure

```
AI-Resume-Forge/
├── frontend/                      # Vue 3 SPA
│   ├── src/
│   │   ├── views/                # Page components
│   │   ├── components/
│   │   │   ├── resume/            # Resume editor components
│   │   │   │   ├── editors/       # Experience, Education editors
│   │   │   │   └── templates/     # Classic, Modern, Creative templates
│   │   │   └── common/
│   │   ├── store/                # Pinia stores
│   │   ├── router/               # Vue Router config
│   │   └── api/                  # Java backend API calls
│   └── package.json
│
├── java-backend/                  # Spring Boot multi-module
│   ├── src/main/java/
│   │   └── com/airesumeforge/
│   │       ├── controller/       # REST endpoints
│   │       ├── service/          # Business logic
│   │       ├── entity/           # MyBatis entities
│   │       ├── mapper/           # MyBatis mappers
│   │       ├── security/         # JWT utilities
│   │       └── config/            # Security, OSS config
│   ├── gateway/                   # API gateway module
│   ├── auth-service/              # Authentication module
│   ├── resume-service/            # Resume CRUD module
│   ├── agent-service/             # Agent orchestration module
│   ├── interview-service/         # Interview feature module
│   └── order/                     # Order/payment module
│   └── pom.xml
│
├── python-backend/                 # FastAPI + LangGraph
│   ├── src/app/
│   │   ├── agent/
│   │   │   ├── graph.py          # LangGraph workflow
│   │   │   ├── state.py          # ResumeAgentState (30+ fields)
│   │   │   ├── nodes/            # 9 agent nodes
│   │   │   ├── prompts/          # System prompts
│   │   │   ├── tools/            # Tool implementations
│   │   │   └── mcp/              # MCP server for resume domain
│   │   ├── service/
│   │   │   ├── java_gateway_service.py   # Java internal API calls
│   │   │   ├── vector_store_service.py   # Qdrant RAG
│   │   │   └── embedding_service.py      # Text embedding
│   │   ├── internal_controller/  # Java-only endpoints
│   │   └── main.py               # FastAPI entry point
│   ├── knowledge_base/            # RAG resume reference materials
│   └── pyproject.toml
│
├── docs/                          # Design documents
├── .gitignore
└── README.md
```

## Run States

```
PENDING → RUNNING → WAITING_USER (clarification needed)
                    → WAITING_CONFIRM (approval needed)
                    → FAILED
WAITING_USER → RUNNING (user answered) / CANCELLED
WAITING_CONFIRM → SUCCESS / CANCELLED / FAILED
```

## Quick Start

### Prerequisites

- **MySQL** 8.0+
- **Redis** 6.0+
- **Qdrant** (for RAG vector search)
- **Node.js** 18+
- **JDK** 17+
- **Python** 3.11+

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
./mvnw spring-boot:run
# Runs on http://localhost:8080
```

### Python Backend

```bash
cd python-backend
pip install -r requirements.txt
uvicorn src.app.main:app --reload --port 8000
# Runs on http://localhost:8000
```

### Environment Variables

Create `.env` files as needed (see `.env.example` for template):

| Variable | Description |
|----------|-------------|
| `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `JWT_SECRET` | JWT signing secret |
| `OSS_ACCESS_KEY_ID`, `OSS_ACCESS_KEY_SECRET` | Aliyun OSS credentials |
| `OPENAI_API_KEY` or `MINIMAX_API_KEY` | LLM provider key |
| `QDRANT_HOST`, `QDRANT_PORT` | Qdrant vector store |

## Design Aesthetic

Neo-editorial luxury — a premium resume tool feel with warm off-white backgrounds, green accents, and sophisticated typography (Playfair Display headlines, Inter body text).

---

See [ARCHITECTURE.md](./ARCHITECTURE.md) for detailed architecture documentation.