package com.montech;

import java.io.IOException;
import com.montech.config.Jira;

public class OpenTicket {

    public static void main(String[] args) {
        if (args.length < 9) {
            System.err.println("Uso: java Main <baseUrl> <email> <apiToken> <projeto> <titulo> <tipo> <descricao> <prioridade> <data>");
            System.exit(1);
        }

        String baseUrl   = args[0];
        String email     = args[1];
        String apiToken  = args[2];
        String projeto   = args[3];
        String titulo    = args[4];
        String tipo      = args[5];
        String descricao = args[6];
        String prioridade = args[7];
        String data      = args[8];

        Jira jira = new Jira(baseUrl, email, apiToken);

        try {
            String response = jira.createIssue(
                    projeto,
                    titulo,
                    tipo,
                    descricao,
                    prioridade,
                    data
            );

            // Exibe o JSON de resposta com os dados da issue criada
            System.out.println(response);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
