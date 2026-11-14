# Thindesk — Dashboard Administrativo de Chamados

Sistema de chamados técnicos com um **Dashboard Administrativo** que consome uma **API REST** própria, integrada ao **MongoDB** (local embarcado ou **MongoDB Atlas**).

| | |
|---|---|
| **Aluna** | Thaíssa Victória Bianchini de Oliveira — RGM 11231102992 |
| **Disciplina** | Aplicativos Web — M2 |
| **Professor** | Alessandro Aparecido da Silva Horas |

---

## Sumário

1. [Tecnologias](#1-tecnologias)
2. [Pré-requisitos](#2-pré-requisitos)
3. [Execução rápida (sem configurar banco)](#3-execução-rápida-sem-configurar-banco)
4. [Integração com o MongoDB Atlas](#4-integração-com-o-mongodb-atlas)
5. [Usando o sistema](#5-usando-o-sistema)
6. [Endpoints da API](#6-endpoints-da-api)
7. [Testes automatizados](#7-testes-automatizados)
8. [Estrutura do projeto](#8-estrutura-do-projeto)
9. [Problemas comuns](#9-problemas-comuns)

---

## 1. Tecnologias

- **Java 17** e **Spring Boot 3.4** (Web, Data MongoDB, Security, Validation)
- **MongoDB** (Atlas ou embarcado para desenvolvimento)
- **Thymeleaf**, **Bootstrap 5** e **Chart.js 4** no dashboard
- **JUnit 5** e **MockMvc** nos testes
- **Maven** para build

## 2. Pré-requisitos

| Ferramenta | Versão | Como conferir |
|---|---|---|
| JDK | 17 ou superior | `java -version` |
| Maven | 3.9 ou superior (opcional, o projeto inclui o Maven Wrapper) | `mvn -v` |
| Git | qualquer | `git --version` |
| Conta no MongoDB Atlas | gratuita (cluster M0) | só para a seção 4 |

> É necessário acesso à internet na primeira execução, para baixar as dependências do Maven e, no modo de desenvolvimento, o MongoDB embarcado (~100 MB).

## 3. Execução rápida (sem configurar banco)

O jeito mais simples de avaliar o projeto. O perfil **`dev`** (padrão) sobe um **MongoDB embarcado** junto com a aplicação e cadastra **320 chamados de exemplo** dos últimos 6 meses. Não é preciso instalar nem configurar banco.

```bash
git clone https://github.com/Im-Thaissa/Atividade-M2.git
cd Atividade-M2
```

**Windows (PowerShell ou CMD):**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

> Com o Maven instalado, `mvn spring-boot:run` também funciona.

Quando aparecer `Started ThindeskApplication` no terminal, acesse **<http://localhost:8080>** e entre com:

| Usuário | Senha |
|---|---|
| `admin` | `thindesk` |

Após o login o sistema abre direto no dashboard (`/dashboard`).

> No perfil `dev` os dados ficam só na memória: ao parar a aplicação, o banco embarcado é descartado e, na próxima execução, os dados de exemplo são gerados de novo.

## 4. Integração com o MongoDB Atlas

### 4.1 Como a integração funciona

A aplicação usa o **Spring Data MongoDB**. A conexão é escolhida pelo **perfil** do Spring:

| Perfil | Banco usado | Quando usar |
|---|---|---|
| `dev` (padrão) | MongoDB embarcado, criado na hora | Avaliar e desenvolver sem configurar nada |
| `prod` | O MongoDB informado em `MONGODB_URI` (Atlas ou outro) | Usar um banco real e persistente |

A string de conexão **não fica no código**. No perfil `prod`, o arquivo `src/main/resources/application-prod.properties` lê a variável de ambiente:

```properties
spring.data.mongodb.uri=${MONGODB_URI}
```

Os dados ficam no banco **`thindesk`**, na coleção **`chamados`**. A aplicação cria a coleção e os índices (`status`, `tipo`, `tecnico`, `dataAbertura`) automaticamente na primeira conexão.

### 4.2 Variáveis de ambiente

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | sim, para usar o Atlas | `dev` | Use `prod` para conectar no Atlas |
| `MONGODB_URI` | sim, no perfil `prod` | — | String de conexão do Atlas |
| `THINDESK_SEED` | não | `false` | `true` cadastra 320 chamados de exemplo **se a coleção estiver vazia** |
| `THINDESK_USER` | não | `admin` | Usuário de acesso ao sistema |
| `THINDESK_PASSWORD` | não | `thindesk` | Senha de acesso ao sistema |
| `SERVER_PORT` | não | `8080` | Porta HTTP da aplicação |

### 4.3 Preparando o Atlas (passo a passo)

1. Acesse <https://cloud.mongodb.com>, crie uma conta e um **cluster gratuito (M0)**.
2. Em **Database Access** → **Add New Database User**, crie um usuário e uma senha com permissão **Read and write to any database**.
3. Em **Network Access** → **Add IP Address**, adicione o seu IP atual (**Add Current IP Address**). Para testes em qualquer rede, é possível liberar `0.0.0.0/0`, o que **não é recomendado em produção**.
4. No cluster, clique em **Connect** → **Drivers** → **Java** e copie a string de conexão. Ela tem este formato:

   ```text
   mongodb+srv://<usuario>:<senha>@<cluster>.xxxxx.mongodb.net/?retryWrites=true&w=majority
   ```

5. Troque `<usuario>` e `<senha>` pelos dados do passo 2 e inclua o nome do banco `thindesk` depois da barra:

   ```text
   mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/thindesk?retryWrites=true&w=majority
   ```

   > Se a senha tiver caracteres especiais (`@ : / ? # %`), eles precisam ser codificados (por exemplo, `@` vira `%40`).

### 4.4 Executando com o Atlas

**Windows (PowerShell):**

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:MONGODB_URI = "mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/thindesk?retryWrites=true&w=majority"
$env:THINDESK_SEED = "true"   # opcional: popula dados de exemplo na primeira execução
.\mvnw.cmd spring-boot:run
```

**Windows (CMD):**

```bat
set SPRING_PROFILES_ACTIVE=prod
set MONGODB_URI=mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/thindesk?retryWrites=true^&w=majority
set THINDESK_SEED=true
mvnw.cmd spring-boot:run
```

> No CMD o `&` da URI precisa do `^` antes, como no exemplo.

**Linux / macOS:**

```bash
export SPRING_PROFILES_ACTIVE=prod
export MONGODB_URI="mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/thindesk?retryWrites=true&w=majority"
export THINDESK_SEED=true   # opcional
./mvnw spring-boot:run
```

**IntelliJ IDEA:** em *Run → Edit Configurations → ThindeskApplication → Environment variables*, informe `SPRING_PROFILES_ACTIVE=prod;MONGODB_URI=...`.

### 4.5 Conferindo a integração

1. No log de inicialização deve aparecer `The following 1 profile is active: "prod"` e, com `THINDESK_SEED=true`, a linha `320 chamados de exemplo inseridos.`
2. No Atlas, abra **Browse Collections**: o banco `thindesk` terá a coleção `chamados` com os documentos.
3. No dashboard (<http://localhost:8080/dashboard>) os indicadores e gráficos passam a refletir os dados do Atlas. Chamados criados pela API (`POST /api/chamados`) aparecem no Atlas e no dashboard, que se atualiza sozinho a cada 60 segundos.

> **Segurança:** nunca coloque a string de conexão em arquivos do repositório. Use sempre a variável de ambiente `MONGODB_URI`.

### 4.6 Formato dos documentos (coleção `chamados`)

```json
{
  "_id": "6af7d7ada1b58b72667115e5",
  "descricao": "Notebook não conecta no Wi-Fi",
  "tipo": "Rede",
  "usuario": "Juliana",
  "tecnico": "Carlos",
  "setor": "Comercial",
  "prioridade": "Alta",
  "status": "Resolvido",
  "consideracoesTecnicas": "Driver da placa de rede atualizado.",
  "dataAbertura": "2026-11-14T00:38:21",
  "dataFechamento": "2026-11-14T02:10:00"
}
```

Situações: `Aberto`, `Em andamento`, `Aguardando`, `Resolvido`, `Cancelado`. Prioridades e prazos de atendimento (SLA): `Crítica` 4 h, `Alta` 8 h, `Média` 24 h, `Baixa` 72 h. Registros antigos sem datas continuam aceitos.

## 5. Usando o sistema

| Tela | Endereço | O que mostra |
|---|---|---|
| Login | `/login` | Acesso com usuário e senha |
| **Dashboard** | `/dashboard` | Indicadores, gráfico de abertos × resolvidos (dia, semana ou mês), situação, tipos mais atendidos, ranking de técnicos e chamados recentes com considerações técnicas |
| Chamados | `/chamados` | Lista de chamados (tela original) |
| Clientes | `/clientes` | Lista de clientes (tela original) |

No dashboard, os **filtros** (período, situação, tipo, técnico, prioridade e setor) valem para todos os blocos ao mesmo tempo. Os dados se atualizam automaticamente a cada 60 segundos ou pelo botão **Atualizar**.

## 6. Endpoints da API

Todos exigem autenticação: a sessão do login ou **HTTP Basic** (`admin` / `thindesk`).

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/api/dashboard/indicadores` | Indicadores gerais (RF01) |
| GET | `/api/dashboard/chamados-por-periodo?agrupamento=dia\|semana\|mes` | Abertos × resolvidos por período (RF02) |
| GET | `/api/dashboard/status` | Situação dos chamados (RF03) |
| GET | `/api/dashboard/tipos?limite=5` | Tipos mais atendidos (RF04) |
| GET | `/api/dashboard/tecnicos?limite=5` | Ranking de técnicos |
| GET | `/api/dashboard/filtros` | Valores disponíveis para os filtros (RF05) |
| GET | `/api/chamados?pagina=0&tamanho=20` | Lista paginada e filtrada |
| GET | `/api/chamados/{id}` | Detalhe de um chamado |
| POST | `/api/chamados` | Cria um chamado |
| PUT | `/api/chamados/{id}` | Atualiza um chamado |
| PATCH | `/api/chamados/{id}/status` | Altera a situação (registra a data de fechamento) |

**Filtros** aceitos pelos endpoints de consulta: `dataInicio` e `dataFim` (formato `yyyy-MM-dd`), `status`, `tipo`, `tecnico`, `prioridade`, `setor`.

Exemplos com `curl` (Git Bash, Linux, macOS ou CMD; no PowerShell use `curl.exe`):

```bash
curl -u admin:thindesk "http://localhost:8080/api/dashboard/indicadores?dataInicio=2026-10-01&dataFim=2026-10-31"
```

```bash
curl -u admin:thindesk -X POST http://localhost:8080/api/chamados -H "Content-Type: application/json" -d "{\"descricao\":\"Impressora sem toner\",\"tipo\":\"Impressora\",\"usuario\":\"Ana\",\"prioridade\":\"baixa\"}"
```

Os erros seguem sempre o mesmo formato JSON (`status`, `erro`, `mensagem`, `caminho`, `detalhes`), com os códigos 400 (dados inválidos), 401 (sem autenticação), 404 (não encontrado) e 503 (banco indisponível).

## 7. Testes automatizados

```bash
./mvnw test
```

No Windows, use `.\mvnw.cmd test`. São **28 testes** (integração com MongoDB embarcado e unitários). Eles não usam o Atlas, então rodam sem nenhuma configuração. O resultado esperado é `Tests run: 28, Failures: 0` e `BUILD SUCCESS`.

## 8. Estrutura do projeto

```text
src/main/java/com/pfc/thindesk
├── controller/api   Endpoints REST (DashboardController, ChamadoController)
├── controller       Páginas Thymeleaf (HomeController)
├── service          Regras de negócio e consultas ao MongoDB
├── dto              Objetos de entrada e saída da API
├── entity           Chamado, StatusChamado, Prioridade
├── repository       Repositórios do Spring Data MongoDB
├── exception        Tratamento padronizado de erros
└── config           Carga de dados de exemplo e relógio
src/main/resources
├── application.properties        Configuração comum
├── application-dev.properties    Perfil dev (MongoDB embarcado)
├── application-prod.properties   Perfil prod (MongoDB Atlas via MONGODB_URI)
├── templates/dashboard.html      Página do dashboard
└── static/js/dashboard.js        Consumo da API e gráficos
```

**Branches (Gitflow):** `main` (versão entregue), `develop` (integração) e `feature/api-dashboard` (implementação da API e do dashboard).

## 9. Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `release version 17 not supported` | JDK antigo | Instale o JDK 17 ou superior e confira `java -version` |
| `Port 8080 was already in use` | Outra aplicação na porta | Defina `SERVER_PORT=8081` e acesse `http://localhost:8081` |
| Timeout ao conectar no Atlas | IP não liberado | Adicione seu IP em **Network Access** no Atlas |
| `Authentication failed` | Usuário ou senha incorretos | Confira o usuário do **Database Access** e codifique caracteres especiais da senha |
| `The connection string is invalid. Connection strings must start with either 'mongodb://' or 'mongodb+srv://'` | Perfil `prod` sem a variável `MONGODB_URI` (ou com valor errado) | Defina `MONGODB_URI` no mesmo terminal, antes de executar, com a string completa do Atlas |
| Dashboard vazio no Atlas | Coleção sem documentos | Execute uma vez com `THINDESK_SEED=true` ou cadastre chamados pela API |
| Demora na primeira execução | Download de dependências e do MongoDB embarcado | Aguarde; as execuções seguintes são rápidas |
