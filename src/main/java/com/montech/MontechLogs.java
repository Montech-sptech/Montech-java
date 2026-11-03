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

public class MontechLogs {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String BUCKET = dotenv.get("MONTECH_BUCKET");
    private static String PROJETO_JIRA = "LOGS";
    private static ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static Integer JANELA_MINUTOS = 15;

    private static Map<String, String> CAMPO_POR_COMPONENTE = Map.of(
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

    // Componentes sem mapeamento: avisa uma única vez para não repetir a cada ciclo
    private static final Set<String> SEM_MAPEAMENTO_AVISADOS = new HashSet<>();

    enum Severidade { NORMAL, ATENCAO, CRITICO }

    static class Alerta {
        private LimiteComponente componente;
        private Severidade severidade;
        private Double valor;
        private String label;

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

    private static long INTERVALO_MS = 10000L;

    public static void main(String[] args) {
        MontechDao dao = new MontechDao(new ConexaoBanco().getJdbcTemplate());
        S3Service s3 = new S3Service(new S3Provider().getS3Client(), BUCKET);
        ObjectMapper mapper = new ObjectMapper();

        System.out.println("Monitor iniciado.");

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

        int totalAbertos = 0;
        int totalFechados = 0;

        System.out.println("\n===== Ciclo iniciado em " + agora + " =====");

        // 1) Primeiro registra os chamados que foram fechados no Jira
        try {
            totalFechados = verificarChamadosFechados(dao, aeroportos, jiras, agora);
        } catch (Exception e) {
            System.err.println("Falha ao verificar chamados fechados: " + e.getMessage());
        }

        // 2) Depois avalia os servidores e abre novos chamados
        System.out.println("Buscando servidores ativos no banco de dados...");
        List<ServidorMonitorado> servidores = dao.listarServidoresAtivos();
        System.out.println(servidores.size() + " servidor(es) ativo(s) para analisar.");

        for (ServidorMonitorado srv : servidores) {
            String nome = "[" + srv.getNomeServidor() + "] ";

            // erro em um servidor não pode parar os demais
            try {
                String chave = String.format("03-gold/%d/%d/%s/%s/%s-%s.json",
                        srv.getIdEmpresa(), srv.getFkAeroporto(), srv.getToken(),
                        data, srv.getToken(), data);

                System.out.println(nome + "Lendo JSON do dia no S3: " + chave);

                String conteudo;
                try {
                    conteudo = s3.lerTexto(chave);
                } catch (NoSuchKeyException e) {
                    System.out.println(nome + "Sem JSON hoje, nada para analisar.");
                    continue;
                }

                List<JsonNode> janela = registrosDaJanela(mapper.readTree(conteudo), agora);
                if (janela.isEmpty()) {
                    System.out.println(nome + "Sem registros na janela de " + JANELA_MINUTOS
                            + " min, nada para analisar.");
                    continue;
                }
                System.out.println(nome + janela.size() + " registro(s) na janela de "
                        + JANELA_MINUTOS + " min.");

                System.out.println(nome + "Buscando limites dos componentes no banco de dados...");
                List<LimiteComponente> limites = dao.listarLimites(srv.getIdServidor());

                System.out.println(nome + "Comparando " + limites.size() + " componente(s) com os limites...");
                List<Alerta> alertas = avaliar(srv, limites, janela);
                if (alertas.isEmpty()) {
                    System.out.println(nome + "Todos os componentes dentro dos limites, nada para cadastrar.");
                    continue;
                }
                System.out.println(nome + alertas.size() + " componente(s) acima do limite.");

                Aeroporto aero = obterAeroporto(dao, aeroportos, srv.getFkAeroporto());
                Jira jira = obterJira(dao, aeroportos, jiras, srv.getFkAeroporto());

                totalAbertos += abrirChamados(dao, mapper, jira, srv, aero, alertas, agora);

            } catch (Exception e) {
                System.err.println(nome + "falha: " + e.getMessage());
            }
        }

        if (totalAbertos == 0 && totalFechados == 0) {
            System.out.println("Nada para cadastrar ou atualizar neste ciclo.");
        } else {
            System.out.println("Resumo do ciclo: " + totalAbertos + " chamado(s) aberto(s), "
                    + totalFechados + " chamado(s) fechado(s).");
        }
        System.out.println("===== Ciclo finalizado =====");
    }

    // ---------------------------------------------------------------
    // Caches
    // ---------------------------------------------------------------

    private static Aeroporto obterAeroporto(MontechDao dao, Map<Integer, Aeroporto> aeroportos, Integer idAeroporto) {
        return aeroportos.computeIfAbsent(idAeroporto, dao::buscarAeroportoId);
    }

    private static Jira obterJira(MontechDao dao, Map<Integer, Aeroporto> aeroportos,
                                  Map<Integer, Jira> jiras, Integer idAeroporto) {
        Aeroporto aero = obterAeroporto(dao, aeroportos, idAeroporto);
        return jiras.computeIfAbsent(idAeroporto,
                id -> new Jira(aero.getUrlJira(), aero.getEmailJira(), aero.getTokenJira()));
    }

    // ---------------------------------------------------------------
    // Chamados fechados
    // ---------------------------------------------------------------

    /** Retorna quantos chamados foram fechados (dt_fim registrada) neste ciclo. */
    private static int verificarChamadosFechados(MontechDao dao, Map<Integer, Aeroporto> aeroportos,
                                                 Map<Integer, Jira> jiras, LocalDateTime agora) {
        System.out.println("Verificando no banco de dados se há chamados em aberto...");
        List<ChamadoAberto> abertos = dao.listarChamadosAbertos();

        if (abertos.isEmpty()) {
            System.out.println("Nenhum chamado em aberto no banco, nada para verificar no Jira.");
            return 0;
        }
        System.out.println(abertos.size() + " chamado(s) em aberto no banco.");

        // agrupa por aeroporto (cada aeroporto tem seu Jira) e consulta uma vez por aeroporto
        Map<Integer, List<ChamadoAberto>> porAeroporto = new HashMap<>();
        for (ChamadoAberto c : abertos) {
            porAeroporto.computeIfAbsent(c.getFkAeroporto(), k -> new ArrayList<>()).add(c);
        }

        int fechados = 0;

        for (Map.Entry<Integer, List<ChamadoAberto>> entry : porAeroporto.entrySet()) {
            try {
                Jira jira = obterJira(dao, aeroportos, jiras, entry.getKey());

                Map<String, ChamadoAberto> porChave = new HashMap<>();
                for (ChamadoAberto c : entry.getValue()) {
                    if (c.getChaveJira() != null) {
                        porChave.put(c.getChaveJira(), c);
                    }
                }

                System.out.println("Verificando no Jira (aeroporto " + entry.getKey() + ") se algum dos "
                        + porChave.size() + " chamado(s) foi fechado...");

                Set<String> fechadas = jira.buscarChavesFechadas(new ArrayList<>(porChave.keySet()));

                if (fechadas.isEmpty()) {
                    System.out.println("Nenhum chamado fechado no Jira (aeroporto " + entry.getKey() + ").");
                    continue;
                }

                for (String chave : fechadas) {
                    ChamadoAberto c = porChave.get(chave);
                    if (c == null) continue;

                    System.out.println("Chamado " + chave + " está fechado no Jira. "
                            + "Atualizando dt_fim no banco de dados...");
                    dao.fecharChamado(c.getIdChamado(), agora);
                    System.out.println("Chamado " + chave + " atualizado como FECHADO (" + c.getTitulo() + ")");
                    fechados++;
                }
            } catch (Exception e) {
                System.err.println("Falha ao verificar chamados fechados do aeroporto "
                        + entry.getKey() + ": " + e.getMessage());
            }
        }
        return fechados;
    }

    // ---------------------------------------------------------------
    // Avaliação dos registros
    // ---------------------------------------------------------------

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

    private static List<Alerta> avaliar(ServidorMonitorado srv, List<LimiteComponente> limites, List<JsonNode> janela) {
        List<Alerta> alertas = new ArrayList<>();

        for (LimiteComponente lim : limites) {
            String campo = CAMPO_POR_COMPONENTE.get(lim.getNomeCodigo());
            if (campo == null) {
                if (SEM_MAPEAMENTO_AVISADOS.add(lim.getNomeCodigo())) {
                    System.err.println("Componente sem mapeamento para o JSON: " + lim.getNomeCodigo());
                }
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

            // UMA label por chamado: identifica servidor + componente + severidade
            // ex: srv-1-usocpu_geral-critico
            String label = "srv-" + srv.getIdServidor() + "-"
                    + lim.getNomeCodigo().toLowerCase().replaceAll("[^a-z0-9_]", "-")
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

    // ---------------------------------------------------------------
    // Abertura de chamados (1 por componente)
    // ---------------------------------------------------------------

    /** Retorna quantos chamados foram realmente abertos. */
    private static int abrirChamados(MontechDao dao, ObjectMapper mapper, Jira jira,
                                     ServidorMonitorado srv, Aeroporto aero,
                                     List<Alerta> alertas, LocalDateTime agora) {
        int abertos = 0;
        for (Alerta a : alertas) {
            // erro em um componente não impede os outros chamados do mesmo servidor
            try {
                if (abrirChamado(dao, mapper, jira, srv, aero, a, agora)) {
                    abertos++;
                }
            } catch (Exception e) {
                System.err.println("[" + srv.getNomeServidor() + "] falha ao abrir chamado de "
                        + a.getComponente().getNomeComponente() + ": " + e.getMessage());
            }
        }

        if (abertos == 0) {
            System.out.println("[" + srv.getNomeServidor() + "] Todos os chamados já existiam, "
                    + "nada para cadastrar.");
        }
        return abertos;
    }

    /** Retorna true se abriu um chamado novo, false se já existia. */
    private static boolean abrirChamado(MontechDao dao, ObjectMapper mapper, Jira jira,
                                        ServidorMonitorado srv, Aeroporto aero,
                                        Alerta a, LocalDateTime agora) throws Exception {

        LimiteComponente c = a.getComponente();
        boolean critico = a.getSeveridade() == Severidade.CRITICO;
        String status = a.getSeveridade().name(); // ATENCAO ou CRITICO (valores do ENUM da tabela chamado)
        String nome = "[" + srv.getNomeServidor() + "] ";
        String componente = c.getNomeComponente() + " (" + status + ")";

        // já existe chamado aberto para esse componente? (banco primeiro, depois Jira)
        System.out.println(nome + "Verificando no banco de dados se já existe chamado de " + componente + "...");
        if (dao.existeChamadoAberto(c.getIdServidorComponente(), status)) {
            System.out.println(nome + "Já existe chamado em aberto no banco para " + componente + ".");
            return false;
        }

        System.out.println(nome + "Verificando no Jira se já existe chamado de " + componente + "...");
        if (jira.existeChamadoAberto(PROJETO_JIRA, a.getLabel())) {
            System.out.println(nome + "Já existe chamado em aberto no Jira para " + componente + ".");
            return false;
        }

        DateTimeFormatter hhmm = DateTimeFormatter.ofPattern("HH:mm");

        String limites = String.format("atenção >= %s | crítico >= %s", c.getLimiteAtencao(), c.getLimiteCritico());

        StringBuilder desc = new StringBuilder();
        desc.append("Servidor: ").append(srv.getNomeServidor())
                .append(" (").append(srv.getToken()).append(")\n");
        desc.append("Aeroporto: ").append(aero.getNome())
                .append(" (").append(aero.getCodigoAeroporto()).append(")\n");
        desc.append("Janela analisada: ").append(agora.minusMinutes(JANELA_MINUTOS).format(hhmm))
                .append(" às ").append(agora.format(hhmm))
                .append(" de ").append(agora.toLocalDate()).append("\n");
        desc.append(critico ? "COMPONENTE EM ESTADO CRÍTICO:\n" : "COMPONENTE EM ATENÇÃO:\n");
        desc.append(String.format("- %s: %.2f %s (%s)\n",
                c.getNomeComponente(), a.getValor(), nvl(c.getUnidadeMedida()), limites));

        String titulo = String.format("%s - %s no servidor %s do aeroporto: %s",
                critico ? "Alerta crítico" : "Atenção",
                c.getNomeComponente(), srv.getNomeServidor(), aero.getNome());

        System.out.println(nome + "Abrindo chamado no Jira para " + componente + "...");
        String resposta = jira.createIssue(
                PROJETO_JIRA,
                titulo,
                "Bug",
                desc.toString(),
                critico ? "High" : "Medium",
                agora.toLocalDate().toString(),
                List.of(a.getLabel()));

        String chaveJira = mapper.readTree(resposta).path("key").asText();
        String linkJira = semBarraFinal(aero.getUrlJira()) + "/browse/" + chaveJira;
        System.out.println(nome + "Chamado " + chaveJira + " criado no Jira.");

        // descricao do banco é VARCHAR(255) e titulo VARCHAR(200)
        String descricaoBanco = String.format("%s: %.2f %s (%s) - servidor %s",
                c.getNomeComponente(), a.getValor(), nvl(c.getUnidadeMedida()), limites, srv.getNomeServidor());

        System.out.println(nome + "Cadastrando chamado " + chaveJira + " no banco de dados...");
        dao.inserirChamado(
                limitar(titulo, 200),
                limitar(descricaoBanco, 255),
                status,
                linkJira,
                agora,
                c.getIdServidorComponente());

        System.out.println(nome + "Chamado " + chaveJira + " cadastrado: "
                + c.getNomeComponente() + " em " + (critico ? "CRÍTICO" : "ATENÇÃO")
                + " (" + String.format("%.2f", a.getValor()) + " " + nvl(c.getUnidadeMedida()) + ")");
        return true;
    }

    private static String nvl(String s) { return s == null ? "" : s; }

    private static String limitar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String semBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}