package com.montech;

import java.io.IOException;
import com.montech.config.Jira;
import io.github.cdimascio.dotenv.Dotenv;

public class OpenTicket {

    public static void main(String[] args) {
        // Dados de acesso ao Jira Cloud
        // ATENÇÃO: USE VARIÁVEIS DE AMBIENTE!

        Dotenv dotenv = Dotenv.load();

        // Puxa as variáveis do arquivo
        String baseUrl = dotenv.get("JIRA_URL");
        String email = dotenv.get("JIRA_EMAIL");
        String apiToken = dotenv.get("JIRA_TOKEN");

        Jira jira = new Jira(baseUrl, email, apiToken);

        try {
            String response = jira.createIssue(
                    "LOGS",
                    "Ticket criada via Java Montech validado",
                    "Bug",
                    "Erro ao processar o lote 123.",
                    "High",
                    "2026-10-05"
            );

            // Exibe o JSON de resposta com os dados da issue criada
            System.out.println(response);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
