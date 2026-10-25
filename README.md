# MontTech - Abertura de Chamados e Logs

Aplicação Java que lê os dados, consulta o banco e abre chamados no Jira.

## Pré-requisitos

- Java 21
- Maven
- Banco de dados MySQL rodando na porta definida no `.env`
- Credenciais AWS configuradas (veja abaixo)

## Credenciais AWS

| Ambiente | Como configurar |
|---|---|
| Local (Windows/Linux) | Coloque suas credenciais em `~/.aws/credentials` |
| VM na AWS | Não precisa de credenciais. Configure a AMI com o `LabInstanceProfile` |

## Banco de dados

| Ambiente | Usuário | Porta |
|---|---|---|
| Teste local | seu usuário do MySQL | `3306` |
| Produção (nuvem) | `montech` | `3307` |

## Arquivo .env

Crie um arquivo chamado `.env` na mesma pasta onde o `.jar` será executado, com os atributos abaixo:

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=montech
DB_USER=montech
DB_PASSWORD=sua_senha_aqui
MONTECH_BUCKET=nome-do-seu-bucket
```

| Atributo | Descrição | Exemplo |
|---|---|---|
| `DB_HOST` | Endereço do servidor MySQL | `localhost` |
| `DB_PORT` | Porta do MySQL (`3306` em teste local, `3307` em produção) | `3306` |
| `DB_NAME` | Nome do banco de dados | `montech` |
| `DB_USER` | Usuário do banco | `montech` |
| `DB_PASSWORD` | Senha do usuário do banco | `sua_senha_aqui` |
| `MONTECH_BUCKET` | Nome do bucket S3 de onde os dados são lidos | `montech-datalake-XXXX` |

> **Atenção:** o `.env` contém senhas. Nunca suba esse arquivo para o GitHub. Adicione `.env` ao `.gitignore`.

## Gerar o .jar

No IntelliJ:

1. Abra a aba **Maven** no menu lateral direito.
2. Expanda o projeto **logs** e depois **Lifecycle**.
3. Dê dois cliques em **clean**.
4. Dê dois cliques em **package**.

O `.jar` será gerado na pasta `target/`.

## Instalar o Java na VM

```bash
sudo apt update && sudo apt install openjdk-21-jre-headless -y
```

## Enviar o .jar para a VM

```bash
scp -i minha-chave.pem meuapp.jar ubuntu@IP_PUBLICO:/home/ubuntu/
```

## Enviar o .env para a VM

```bash
scp -i minha-chave.pem .env ubuntu@IP_PUBLICO:/home/ubuntu/
```

## Executar

### Logs (MontechLogs)

Antes de executar, confirme que o `.env` está preenchido e na mesma pasta do `.jar`:

```bash
ls -a   # deve listar .env e MontechLogs-jar-with-dependencies.jar
java -jar MontechLogs-jar-with-dependencies.jar
```

Execute na mesma pasta do arquivo `.env`:


```bash
java -jar MontechLogs-jar-with-dependencies.jar
```