# ✈️ Montech Logs - Deploy com Docker (Monitor Java)

> Passo a passo para subir o monitor de chamados (Java) em um container Docker na VM. O jar é **compilado dentro do Docker** (build multi-stage), então a VM só precisa de Docker e Git.

---

## 📋 Índice

- [Como funciona](#-como-funciona)
- [Pré-requisitos](#-pré-requisitos)
- [1. Clonar o repositório na VM](#1-clonar-o-repositório-na-vm)
- [2. Configurar o `.env`](#2-configurar-o-env)
- [3. Subir o container](#3-subir-o-container)
- [4. Atualizar a aplicação](#4-atualizar-a-aplicação)
- [Arquivos do projeto](#-arquivos-do-projeto)
- [Problemas comuns](#-problemas-comuns)
- [Comandos úteis](#-comandos-úteis)

---

## 🧭 Como funciona

O `Dockerfile` tem dois estágios. No primeiro, uma imagem com Maven compila o projeto e gera os jars. No segundo, uma imagem leve só com o Java copia o `MontechLogs-jar-with-dependencies.jar` e executa. Você não instala Java nem Maven na VM e não gera jar manualmente.

O container entra na network `montech-net` pelo `docker-compose.yml`. Essa network já é criada por outro arquivo, então este projeto só a usa.

---

## ✅ Pré-requisitos

- VM Linux (Ubuntu/Debian) com acesso SSH
- **Docker** e **Docker Compose** instalados
- **Git** instalado
- Network `montech-net` já criada
- Container MySQL já em execução (nome usado neste guia: `montech-db`)
- Banco `montech` e usuário com permissão (`SELECT`, `INSERT`, `UPDATE`) criados
- Acesso ao bucket S3 do datalake (por IAM Role da EC2 ou por chaves AWS)
- Alguns GB livres no disco onde o Docker guarda as imagens (`df -h`)

> Java e Maven **não** são necessários na VM.

---

## 1. Clonar o repositório na VM

```bash
git clone <URL_DO_REPOSITORIO>
cd <PASTA_DO_PROJETO>
```

> Antes de clonar, confirme que `Dockerfile`, `docker-compose.yml`, `.dockerignore` e `pom.xml` foram commitados e enviados (`git push`). Sem eles, o build falha na VM.

---

## 2. Configurar o `.env`

O `.env` não vai para o repositório (deve estar no `.gitignore`), então **crie manualmente na VM**, na raiz do projeto:

```bash
nano .env
```

```bash
# Banco de dados
DB_HOST=montech-db
DB_PORT=3306
DB_NAME=montech
DB_USER=montech
DB_PASSWORD=sua_senha_aqui

# Bucket S3 com os JSONs do datalake
MONTECH_BUCKET=nome-do-bucket
```

Pontos importantes:

- `DB_HOST` é o **nome do container** MySQL (veja em `docker ps`), nunca `localhost`.
- `DB_PORT` é sempre a porta **interna** do MySQL (`3306`), mesmo que ele esteja publicado em outra porta na VM.
- Se o banco estiver fora do Docker (IP público, RDS), use o endereço no `DB_HOST`. O Security Group precisa liberar a porta 3306 para o IP de saída da VM.
- Evite `$` nas senhas (o Compose interpreta como variável). Se precisar, escreva `$$`.
- Não use aspas nos valores.
- Nunca coloque senhas reais no README nem no repositório.

### Credenciais da AWS

- **Rodando em EC2 com IAM Role:** não precisa de nada no `.env`. Se aparecer `Unable to load credentials`, aumente o hop limit do serviço de metadados para os containers conseguirem acessá-lo:
  ```bash
  aws ec2 modify-instance-metadata-options \
    --instance-id i-SEUID \
    --http-put-response-hop-limit 2 \
    --http-endpoint enabled
  ```
- **Rodando fora da AWS:** adicione ao `.env`:
  ```bash
  AWS_ACCESS_KEY_ID=...
  AWS_SECRET_ACCESS_KEY=...
  AWS_SESSION_TOKEN=...   # só se as credenciais forem temporárias
  ```

---

## 3. Subir o container

```bash
docker compose up -d --build
```

Isso lê o `docker-compose.yml`, compila o projeto com Maven dentro do Docker, monta a imagem final e sobe o container `montech-logs`. **O primeiro build demora mais**, porque baixa as dependências do Maven.

Confira o status e os logs:

```bash
docker compose ps
docker compose logs -f --tail 30 montech-logs
```

O log deve mostrar `Monitor iniciado.` seguido de `===== Ciclo iniciado em ... =====`. Para sair do `logs -f`, use `Ctrl+C` (o container continua rodando).

> **Atenção ao `.env` dentro do container:** o `MontechLogs.java` usa `Dotenv.load()` sem `ignoreIfMissing`. Se o container cair logo ao subir com `DotenvException`, troque no código para `Dotenv.configure().ignoreIfMissing().load()`, faça o commit e rode o build de novo.

---

## 4. Atualizar a aplicação

| Situação | Comando |
| --- | --- |
| Mudou o **código** | `git pull` e depois `docker compose up -d --build` |
| Mudou só o **`.env`** | `docker compose up -d --force-recreate` |
| Quer apenas reiniciar | `docker compose restart montech-logs` |

Fluxo típico de atualização:

```bash
git pull
docker compose up -d --build
```

> O jar é recompilado automaticamente a cada `--build`. Não precisa rodar `mvn` na VM.
>
> `restart` **não** relê o `.env`. Para aplicar mudanças no `.env`, use `--force-recreate`.

---

## 📁 Arquivos do projeto

Todos ficam na raiz do projeto, ao lado do `pom.xml`.

### `Dockerfile`

```dockerfile
# ---------- Estágio 1: compila o projeto ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---------- Estágio 2: imagem final (só o Java e o jar) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copia somente o jar com dependências (o outro jar gerado não roda sozinho)
COPY --from=build /app/target/MontechLogs-jar-with-dependencies.jar app.jar

CMD ["java", "-jar", "app.jar"]
```

### `docker-compose.yml`

```yaml
services:
  montech-logs:
    build: .
    container_name: montech-logs
    restart: unless-stopped
    env_file:
      - .env
    environment:
      AWS_REGION: us-east-1
      TZ: America/Sao_Paulo
    networks:
      - montech-net

networks:
  montech-net:
    external: true
```

### `.dockerignore`

```
target
.git
.env
.idea
*.iml
```

O `target` pode ser ignorado, porque o jar é gerado dentro do Docker. O `.env` também fica de fora para não entrar na imagem: as variáveis chegam pelo `env_file` do compose.

---

## 🛠️ Problemas comuns

| Sintoma | Causa provável | Solução |
| --- | --- | --- |
| `network montech-net declared as external, but could not be found` | A network ainda não foi criada | Suba antes o projeto que cria a `montech-net` |
| `Communications link failure` | `DB_HOST` errado ou MySQL inacessível | Conferir o nome do container em `docker ps` e se o MySQL está na `montech-net` |
| `"/app/target/...jar": not found` no build | O nome do jar gerado é diferente do esperado | Conferir `artifactId` e `finalName` no `pom.xml`, ou usar `COPY --from=build /app/target/*-jar-with-dependencies.jar app.jar` |
| Build do Maven falha | Erro de compilação ou `pom.xml` fora do commit | Ler o erro em `docker compose build` e conferir se `pom.xml` e `src` foram enviados |
| `Unable to load credentials` | Container sem acesso à IAM Role ou sem chaves AWS | Ajustar o hop limit ou colocar as chaves no `.env` |
| `DotenvException` ao iniciar | O código exige um arquivo `.env` | Usar `ignoreIfMissing()` no `Dotenv.configure()` |
| Container reiniciando em loop | Erro na inicialização | `docker compose logs montech-logs` |

---

## 🧰 Comandos úteis

```bash
docker compose ps                                  # status dos containers
docker compose logs -f --tail 50 montech-logs      # acompanha os logs em tempo real
docker compose stop                                # para o monitor
docker compose down                                # para e remove o container do monitor
docker exec -it montech-logs bash                  # abre um terminal dentro do monitor
docker exec montech-logs env | grep DB_            # confere as variáveis carregadas
docker ps -a                                       # lista todos os containers
docker system df                                   # espaço usado pelo Docker
```