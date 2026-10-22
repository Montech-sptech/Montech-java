package com.montech;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.montech.bd.Aeroporto;
import com.montech.bd.ConexaoBanco;
import com.montech.bd.MontechDao;
import com.montech.bd.Servidor;
import com.montech.jira.config.Jira;
import com.montech.s3.S3Provider;
import com.montech.s3.S3Service;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

public class OpenTicket {

    public static void main(String[] args) {
        ConexaoBanco conexaoBanco = new ConexaoBanco();
        JdbcTemplate template = conexaoBanco.getJdbcTemplate();


//        MontechDao dao = new MontechDao(template);
//        Servidor servidor1 = dao.BuscarServidor("3b12f1df-5232-4df4-8d48-64a938c2");
//        Aeroporto aeroporto1 = dao.buscarAeroportoId(servidor1.getFkAeroporto());
//        System.out.println(aeroporto1);
//
//        Jira jira = new Jira(aeroporto1.getUrlJira(), aeroporto1.getEmailJira(), aeroporto1.getTokenJira());
//
//        String titleIssue = String.format("Problema no servidor %s do aeroporto: %s", servidor1.getNomeServidor(), aeroporto1.getNome());
////        String descriptionIssue = String.format("Passou do uso da mémoria ram");
//
//
//        String labelServidor = "srv-" + servidor1.getIdServidor(); // ajuste ao getter real
//        String labelProblema = "ram-alta";
//
//        try {
//            if (jira.existeChamadoAberto("LOGS", labelServidor, labelProblema)) {
//                System.out.println("Já existe chamado aberto para " + labelServidor + " / " + labelProblema);
//                return;
//            }
//
//            String response = jira.createIssue(
//                    "LOGS",
//                    titleIssue,
//                    "Bug",
//                    "Uso de memória RAM acima do limite",
//                    "High",
//                    LocalDate.now().plusDays(2).toString(),
//                    List.of(labelServidor, labelProblema)
//            );
//            System.out.println(response);
//
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//            System.exit(1);
//        }

        // Instanciando o cliente S3 via S3Provider

        S3Client s3Client = new S3Provider().getS3Client();
        S3Service s3 = new S3Service(s3Client, "montech-datalake-4281");

        String idEmpresa = "1";
        String idAeroporto = "1";
        String token = "3b12f1df-5232-4df4-8d48-64a938c2";
        String data = "2026-10-20";

        String prefixo = "03-gold/" + idEmpresa + "/" + idAeroporto + "/" + token + "/" + data + "/";

        try {
            for (String key : s3.listarPorExtensao(prefixo, ".json")) {
                System.out.println("JSON encontrado: " + key);

                String conteudo = s3.lerTexto(key);
                JsonNode registros = new ObjectMapper().readTree(conteudo);

                for (JsonNode r : registros) {
                    System.out.println(
                            r.get("Hora").asText() + " | "
                                    + "CPU média: " + r.get("UsoCPU_Geral_mean").asDouble() + " | "
                                    + "CPU máx: " + r.get("UsoCPU_Geral_max").asDouble() + " | "
                                    + "RAM média: " + r.get("UsoRAM_mean").asDouble() + " | "
                                    + "Coletas: " + r.get("Qtd_Coletas").asInt()
                    );
                }

                System.out.println(registros);
            }
        } catch (S3Exception e) {
            System.err.println("Erro no S3: " + e.awsErrorDetails().errorMessage());
        } catch (IOException e) {
            System.err.println("Erro de leitura: " + e.getMessage());
        }
    }

}
