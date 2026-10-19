package com.montech;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import com.montech.bd.Aeroporto;
import com.montech.bd.ConexaoBanco;
import com.montech.bd.MontechDao;
import com.montech.bd.Servidor;
import com.montech.jira.config.Jira;
import org.springframework.jdbc.core.JdbcTemplate;

public class OpenTicket {

    public static void main(String[] args) {
        ConexaoBanco conexaoBanco = new ConexaoBanco();
        JdbcTemplate template = conexaoBanco.getJdbcTemplate();


        MontechDao dao = new MontechDao(template);
        Servidor servidor1 = dao.BuscarServidor("3b12f1df-5232-4df4-8d48-64a938c2");
        Aeroporto aeroporto1 = dao.buscarAeroportoId(servidor1.getFkAeroporto());
        System.out.println(aeroporto1);

        Jira jira = new Jira(aeroporto1.getUrlJira(), aeroporto1.getEmailJira(), aeroporto1.getTokenJira());

        String titleIssue = String.format("Problema no servidor %s do aeroporto: %s", servidor1.getNomeServidor(), aeroporto1.getNome());
//        String descriptionIssue = String.format("Passou do uso da mémoria ram");


        String labelServidor = "srv-" + servidor1.getIdServidor(); // ajuste ao getter real
        String labelProblema = "ram-alta";

        try {
            if (jira.existeChamadoAberto("LOGS", labelServidor, labelProblema)) {
                System.out.println("Já existe chamado aberto para " + labelServidor + " / " + labelProblema);
                return;
            }

            String response = jira.createIssue(
                    "LOGS",
                    titleIssue,
                    "Bug",
                    "Uso de memória RAM acima do limite",
                    "High",
                    LocalDate.now().plusDays(2).toString(),
                    List.of(labelServidor, labelProblema)
            );
            System.out.println(response);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.exit(1); // sinaliza falha para o cron/systemd
        }

//        try {
//            // Cria uma issue no backlog do projeto informado
//            String response = jira.createIssue(
//                    "LOGS",
//                    titleIssue,
//                    "Bug",
//                    descriptionIssue,
//                    "High",
//                    "2026-10-05"
//            );
//
//            // Exibe o JSON de resposta com os dados da issue criada
//            System.out.println(response);
//
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
    }
}
