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

Execute na mesma pasta do arquivo `.env`:

```bash
java -jar MontechLogs-jar-with-dependencies.jar
```