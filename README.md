# ms-feature-flags-management

Microserviço responsável pelo ciclo de vida de feature flags dentro da plataforma **Switchboard**. Parte da [arquitetura de microsserviços](https://github.com/CassioCintra/platform-ops/blob/main/ARCHITECTURE.md).

## Stack

| Componente   | Tecnologia                  |
|--------------|-----------------------------|
| Runtime      | Java 25                     |
| Framework    | Spring Boot 4.0.6           |
| Banco        | PostgreSQL 16               |
| Event Bus    | Kafka 4.0 (KRaft)           |

=======
| Auth         | ms-auth (Spring Authorization Server) |
| Migrations   | Flyway                      |
| Testes       | JUnit 5 + Testcontainers    |

## Rodando localmente

### Pré-requisitos

- Java 25
- Docker

### 1. Subir a infraestrutura

```bash
# No repositório platform-ops
docker compose up -d
```

Sobe PostgreSQL (5432), Kafka (9092) e ms-auth (9000).

### 2. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

A API ficará disponível em:

```
http://localhost:8081/feature-flag/v1
```

Swagger UI disponível em:
- **Direto:** `http://localhost:8081/feature-flag/v1/swagger-ui.html`
- **Via gateway:** `http://localhost:8090/swagger-ui.html` (seletor com todos os serviços)

## API

### Base URL

```
http://localhost:8081/feature-flag/v1
```

Todas as rotas exigem JWT válido no header `Authorization: Bearer <token>`.

### Endpoints

| Método   | Rota                        | Descrição                                      |
|----------|-----------------------------|------------------------------------------------|
| `POST`   | `/flags`                    | Cria uma flag                                  |
| `GET`    | `/flags`                    | Lista flags com filtros                        |
| `PATCH`  | `/flags/{key}`              | Atualiza campos ou ativa/desativa por ambiente |
| `DELETE` | `/flags/{key}`              | Remove uma flag                                |
| `GET`    | `/flags/{key}/evaluate`     | Avalia uma flag para um contexto               |
| `POST`   | `/flags/evaluate-batch`     | Avalia múltiplas flags (usado pelo SDK)        |
| `GET`    | `/services`                 | Lista serviços registrados e suas flags        |

### Exemplos

**Criar flag**

```http
POST /feature-flag/v1/flags
Content-Type: application/json

{
  "flagName": "checkout_v2",
  "serviceName": "checkout-api",
  "type": "ROLLOUT",
  "rollout": 30,
  "environments": {
    "dev": true,
    "staging": false,
    "prod": false
  },
  "tags": ["payments", "checkout"],
  "owner": "payments-team",
  "expiresAt": "2026-09-01"
}
```

> Todos os ambientes nascem desativados (`false`) independente do payload enviado. A ativação por ambiente é feita via `PATCH`.

**Ativar em um ambiente**

```http
PATCH /feature-flag/v1/flags/checkout_v2
Content-Type: application/json

{
  "environments": {
    "dev": true,
    "staging": true,
    "prod": false
  }
}
```

**Avaliar flag**

```http
GET /feature-flag/v1/flags/checkout_v2/evaluate?userId=u_123&env=staging
```

```json
{
  "flagName": "checkout_v2",
  "enabled": true,
  "type": "ROLLOUT",
  "rollout": 30
}
```

**Listar com filtros**

```
GET /flags?service=checkout-api&env=prod&type=BOOLEAN&q=checkout
```

### Tipos de flag (`FlagType`)

| Tipo          | Comportamento                                                              |
|---------------|----------------------------------------------------------------------------|
| `BOOLEAN`     | Ligado ou desligado globalmente                                             |
| `ROLLOUT`     | Ativo para uma % dos usuários, determinada por hash de `userId + flagName` |
| `MULTIVARIATE`| Reservado para variantes futuras                                           |

### Lógica de avaliação

1. `enabled = false` → retorna `false` (master switch)
2. `env` informado e presente no mapa `environments` → usa o valor do ambiente
3. `env` informado e ausente no mapa → retorna `false` (não configurado para esse ambiente)
4. Sem `env` → usa o `enabled` global

## Eventos Kafka

Todas as mutações publicam no tópico `flag.events`. O campo `action` distingue o tipo do evento.

| `action`  | Quando                              |
|-----------|-------------------------------------|
| `CREATED` | Flag criada                         |
| `UPDATED` | Metadados atualizados               |
| `TOGGLED` | Campo `enabled` alterado            |
| `DELETED` | Flag removida                       |

A partition key é `{serviceName}.{flagName}`, garantindo ordem dos eventos por flag.

## Arquitetura

Arquitetura hexagonal (ports & adapters):

```
src/main/java/cassio/featureflags/
├── domain/               # FeatureFlag, FlagType, EvaluationContext/Result
├── application/
│   ├── port/in/          # FeatureFlagUseCase (commands, queries)
│   ├── port/out/         # FeatureFlagRepository, FlagEventPublisher
│   └── FeatureFlagService.java
└── adapter/
    ├── in/web/           # REST controller, requests, responses, security
    └── out/
        ├── persistence/  # JPA entity, Flyway migrations
        └── messaging/    # Kafka producer
```

## Testes

```bash
# Unitários (sem Docker)
./mvnw test -Dtest="FeatureFlagControllerTest,FeatureFlagServiceTest,FlagEventKafkaAdapterTest"

# Integração (requer Docker para Testcontainers)
./mvnw test -Dtest="FeatureFlagPersistenceAdapterTest,FeatureFlagsApplicationTests"

# Gerar spec OpenAPI (requer Docker — usado também pela pipeline de CI)
./mvnw test -Dtest=OpenApiSpecGeneratorTest

# Todos
./mvnw verify
```

| Tipo | Ferramentas | Exemplo |
|------|-------------|---------|
| Web slice | `@WebMvcTest` + MockMvc | `FeatureFlagControllerTest` |
| Serviço (unit) | JUnit 5 + Mockito | `FeatureFlagServiceTest` |
| Messaging (unit) | Mockito | `FlagEventKafkaAdapterTest` |
| Persistência (integr.) | Testcontainers (PostgreSQL + Kafka) | `FeatureFlagPersistenceAdapterTest` |
| Smoke test | Testcontainers (PostgreSQL + Kafka) | `FeatureFlagsApplicationTests` |
| OpenAPI spec | `@SpringBootTest` + MockMvc + Testcontainers | `OpenApiSpecGeneratorTest` |

## Contexto no ecossistema Switchboard

```
Fase 1 — ms-feature-flags          ✅ este serviço
Fase 2 — ms-workspace-management   ✅ concluída
Fase 3 — ms-auth                   ✅ concluída
Fase 3.1 — Adaptar ms-workspace para ms-auth  ✅ concluída
Fase 3.2 — Adaptar ms-feature-flags para ms-auth  ✅ concluída
Fase 4   — Spring Cloud Gateway    ✅ concluída
Fase 4.1 — OpenAPI specs + pipelines  ✅ este serviço incluído
Fase 4.2 — Dockerfiles             ⏳
Fase 5   — ms-audit                ⏳
Fase 6   — ms-dashboard (BFF)      ⏳
```

Documentação completa da plataforma: [`platform-ops/docs/architecture`](https://github.com/CassioCintra/platform-ops/blob/main/docs/architecture/README.md)
