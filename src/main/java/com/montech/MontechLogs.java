package com.montech;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.montech.bd.*;
import com.montech.jira.config.Jira;
import com.montech.s3.S3Provider;
import com.montech.s3.S3Service;
import io.github.cdimascio.dotenv.Dotenv;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MontechLogs {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String BUCKET = dotenv.get("MONTECH_BUCKET");
    private static  String PROJETO_JIRA = "LOGS";
    private static  ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static  Integer JANELA_MINUTOS = 15;

    private static  Map<String, String> CAMPO_POR_COMPONENTE = Map.of(
            "UsoCPU_Geral", "UsoCPU_Geral_mean",
            "UsoRAM", "UsoRAM_mean",
            "UsoDisco", "UsoDisco_mean",
            "Qtd_Processos", "Qtd_Processos_mean",
            "Swap_In", "Delta_Swap_In_sum",
            "Swap_Out", "Delta_Swap_Out_sum",
            "Disco_Read_Bytes", "Delta_Read_Bytes_sum",
            "Disco_Write_Bytes", "Delta_Write_Bytes_sum",
            "Rede_Bytes_Enviados", "Delta_Rede_Bytes_Enviados_sum",
            "Rede_Bytes_Recebidos", "Delta_Rede_Bytes_Recebidos_sum");

    enum Severidade { NORMAL, ATENCAO, CRITICO }

    static class Alerta {
        private  LimiteComponente componente;
        private  Severidade severidade;
        private  Double valor;
        private  String label;

        Alerta(LimiteComponente componente, Severidade severidade, Double valor, String label) {
            this.componente = componente;
            this.severidade = severidade;
            this.valor = valor;
            this.label = label;
        }

        LimiteComponente getComponente() { return componente; }
        Severidade getSeveridade() { return severidade; }
        Double getValor() { return valor; }
        String getLabel() { return label; }
    }

    private static long INTERVALO_MS = 60000L;

    public static void main(String[] args) {
        MontechDao dao = new MontechDao(new ConexaoBanco().getJdbcTemplate());
        S3Service s3 = new S3Service(new S3Provider().getS3Client(), BUCKET);
        ObjectMapper mapper = new ObjectMapper();

        while (true) {
            long inicio = System.currentTimeMillis();

            try {
                executarCiclo(dao, s3, mapper);
            } catch (Exception e) {
                System.err.println("Falha no ciclo: " + e.getMessage());
            }

            long restante = INTERVALO_MS - (System.currentTimeMillis() - inicio);
            if (restante > 0) {
                try {
                    Thread.sleep(restante);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Monitor encerrado.");
                    return;
                }
            }
        }
    }

    private static void executarCiclo(MontechDao dao, S3Service s3, ObjectMapper mapper) {
        // Caches recriados a cada ciclo, para pegar mudanças de URL, e-mail ou token do Jira no banco
        Map<Integer, Aeroporto> aeroportos = new HashMap<>();
        Map<Integer, Jira> jiras = new HashMap<>();

        LocalDateTime agora = LocalDateTime.now(FUSO);
        String data = agora.toLocalDate().toString();

        System.out.println("\n===== Ciclo iniciado em " + agora + " =====");

        for (ServidorMonitorado srv : dao.listarServidoresAtivos()) {
            // erro em um servidor não pode parar os demais
            try {
                String chave = String.format("03-gold/%d/%d/%s/%s/%s-%s.json",
                        srv.getIdEmpresa(), srv.getFkAeroporto(), srv.getToken(),
                        data, srv.getToken(), data);

                String conteudo;
                try {
                    conteudo = s3.lerTexto(chave);
                } catch (NoSuchKeyException e) {
                    System.out.println("[" + srv.getNomeServidor() + "] sem JSON hoje: " + chave);
                    continue;
                }

                List<JsonNode> janela = registrosDaJanela(mapper.readTree(conteudo), agora);
                if (janela.isEmpty()) {
                    System.out.println("[" + srv.getNomeServidor() + "] sem registros na janela de "
                            + JANELA_MINUTOS + " min");
                    continue;
                }

                List<Alerta> alertas = avaliar(dao.listarLimites(srv.getIdServidor()), janela);
                if (alertas.isEmpty()) {
                    System.out.println("[" + srv.getNomeServidor() + "] tudo normal");
                    continue;
                }

                Aeroporto aero = aeroportos.computeIfAbsent(srv.getFkAeroporto(), dao::buscarAeroportoId);
                Jira jira = jiras.computeIfAbsent(srv.getFkAeroporto(),
                        id -> new Jira(aero.getUrlJira(), aero.getEmailJira(), aero.getTokenJira()));

                abrirChamado(jira, srv, aero, alertas, agora);

            } catch (Exception e) {
                System.err.println("[" + srv.getToken() + "] falha: " + e.getMessage());
            }
        }

        System.out.println("===== Ciclo finalizado =====");
    }

    private static List<JsonNode> registrosDaJanela(JsonNode registros, LocalDateTime agora) {
        LocalDateTime inicio = agora.minusMinutes(JANELA_MINUTOS);
        List<JsonNode> janela = new ArrayList<>();
        for (JsonNode r : registros) {
            LocalDateTime hora = LocalDateTime.parse(r.path("Hora").asText());
            if (!hora.isAfter(agora) && hora.plusHours(1).isAfter(inicio)) {
                janela.add(r);
            }
        }
        return janela;
    }

    private static List<Alerta> avaliar(List<LimiteComponente> limites, List<JsonNode> janela) {
        List<Alerta> alertas = new ArrayList<>();

        for (LimiteComponente lim : limites) {
            String campo = CAMPO_POR_COMPONENTE.get(lim.getNomeCodigo());
            if (campo == null) {
                System.err.println("Componente sem mapeamento para o JSON: " + lim.getNomeCodigo());
                continue;
            }

            Double valor = null;
            for (JsonNode r : janela) {
                if (r.hasNonNull(campo)) {
                    double v = r.get(campo).asDouble();
                    valor = (valor == null) ? v : Math.max(valor, v);
                }
            }
            if (valor == null) continue;

            Severidade sev = classificar(valor, lim);
            if (sev == Severidade.NORMAL) continue;

            String label = lim.getNomeCodigo().toLowerCase().replaceAll("[^a-z0-9_]", "-")
                    + (sev == Severidade.CRITICO ? "-critico" : "-atencao");
            alertas.add(new Alerta(lim, sev, valor, label));
        }
        return alertas;
    }

    private static Severidade classificar(double valor, LimiteComponente lim) {
        if (lim.getLimiteCritico() != null && valor >= lim.getLimiteCritico()) return Severidade.CRITICO;
        if (lim.getLimiteAtencao() != null && valor >= lim.getLimiteAtencao()) return Severidade.ATENCAO;
        return Severidade.NORMAL;
    }

    private static void abrirChamado(Jira jira, ServidorMonitorado srv, Aeroporto aero,
                                     List<Alerta> alertas, LocalDateTime agora) throws Exception {

        String labelServidor = "srv-" + srv.getIdServidor();

        List<Alerta> novos = new ArrayList<>();
        for (Alerta a : alertas) {
            if (jira.existeChamadoAberto(PROJETO_JIRA, labelServidor, a.getLabel())) {
                System.out.println("[" + srv.getNomeServidor() + "] já existe chamado aberto: " + a.getLabel());
            } else {
                novos.add(a);
            }
        }
        if (novos.isEmpty()) return;

        boolean temCritico = false;
        for (Alerta a : novos) {
            if (a.getSeveridade() == Severidade.CRITICO) temCritico = true;
        }

        DateTimeFormatter hhmm = DateTimeFormatter.ofPattern("HH:mm");

        StringBuilder desc = new StringBuilder();
        desc.append("Servidor: ").append(srv.getNomeServidor())
                .append(" (").append(srv.getToken()).append(")\n");
        desc.append("Aeroporto: ").append(aero.getNome())
                .append(" (").append(aero.getCodigoAeroporto()).append(")\n");
        desc.append("Janela analisada: ").append(agora.minusMinutes(JANELA_MINUTOS).format(hhmm))
                .append(" às ").append(agora.format(hhmm))
                .append(" de ").append(agora.toLocalDate()).append("\n");

        for (Severidade sev : Arrays.asList(Severidade.CRITICO, Severidade.ATENCAO)) {
            List<Alerta> grupo = novos.stream()
                    .filter(a -> a.getSeveridade() == sev)
                    .collect(Collectors.toList());
            if (grupo.isEmpty()) continue;

            desc.append(sev == Severidade.CRITICO
                    ? "COMPONENTES EM ESTADO CRÍTICO:\n"
                    : "COMPONENTES EM ATENÇÃO:\n");

            for (Alerta a : grupo) {
                LimiteComponente c = a.getComponente();
                desc.append(String.format("- %s: %.2f %s (atenção >= %s | crítico >= %s) [label: %s]\n",
                        c.getNomeComponente(), a.getValor(), nvl(c.getUnidadeMedida()),
                        c.getLimiteAtencao(), c.getLimiteCritico(), a.getLabel()));
            }
        }

        List<String> labels = new ArrayList<>();
        labels.add(labelServidor);
        for (Alerta a : novos) {
            labels.add(a.getLabel());
        }

        String titulo = String.format("%s no servidor %s do aeroporto: %s",
                temCritico ? "Alerta crítico" : "Atenção", srv.getNomeServidor(), aero.getNome());

        String resposta = jira.createIssue(
                PROJETO_JIRA,
                titulo,
                "Bug",
                desc.toString(),
                temCritico ? "High" : "Medium",
                agora.toLocalDate().toString(),
                labels);

        System.out.println("[" + srv.getNomeServidor() + "] chamado criado: " + resposta);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}