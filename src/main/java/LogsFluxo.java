public class LogsFluxo {
    public static void main (String[] args){
        // lógica princial: login -> dashboard -> monitoramento dos servidores (SPA, SDV, AIS) ->
        // * detecção de anomalia -> geração de alerta -> geração de incidente ->
        // * integração com Jira/Slack -> consulta de relatório.

        System.out.println("==================================================");
        System.out.println("   MONTECH - SIMULAÇÃO DE LOGS DO FLUXO DO USUÁRIO");
        System.out.println("==================================================\n");

        System.out.println("[01:57:56] [SISTEMA] Aplicação Web iniciada. Aguardando autenticação do usuário...");
        //AUTH marca mensagens relacionadas a processo de login (envio de credenciais, validações no bd e resultado da autenticação)
        System.out.println("[01:57:57] [AUTH] Usuário 'fernando.brandao@empresa.com' enviou credenciais de acesso.");
        System.out.println("[01:57:57] [AUTH] Validando credenciais no banco de dados...");
        System.out.println("[01:57:58] [AUTH] Autenticação bem-sucedida. Cargo do usuário: ANALISTA. Empresa vinculada: ID 12.");
        System.out.println("[01:57:58] [DASHBOARD] Carregando servidores vinculados à empresa (ID 12)...");
        System.out.println("[01:57:59] [DASHBOARD] 3 servidores encontrados: SPA (Planos de Voo), SDV (Dados de Vigilância), AIS (Informação Aeronáutica).");
        System.out.println("[01:57:59] [DASHBOARD] Renderizando cards de status e gráficos de CPU, memória RAM e disco.");
        System.out.println("[01:58:00] [COLETA] Servidor SPA -> CPU: 43% (NORMAL) | RAM: 58% (NORMAL) | Disco: 21% (NORMAL)");
        System.out.println("[01:58:00] [COLETA] Servidor SDV -> CPU: 40% (NORMAL) | RAM: 47% (NORMAL) | Disco: 30% (NORMAL)");
        System.out.println("[01:58:01] [COLETA] Servidor AIS -> CPU: 35% (NORMAL) | RAM: 64% (NORMAL) | Disco: 25% (NORMAL)");
        System.out.println("[01:58:02] [SISTEMA] Novo ciclo de coleta iniciado (intervalo de monitoramento atingido)...");
        System.out.println("[01:58:02] [COLETA] Servidor SDV -> CPU: 91% (CRÍTICO) | RAM: 88% (ATENÇÃO) | Disco: 74% (ATENÇÃO).");
        System.out.println("[01:58:03] [ANALISE] Limite de CPU (85%) ultrapassado no servidor SDV por 3 coletas consecutivas.");
        System.out.println("[01:58:03] [ALERTA] Gerando alerta crítico para o servidor SDV (Track Management)...");
        System.out.println("[01:58:04] [ALERTA] Alerta #A-1042 criado: 'Uso elevado de CPU pode impactar o Flight Plan / Track Correlation'.");
        System.out.println("[01:58:04] [DASHBOARD] Alerta #A-1042 exibido na tela de Alertas com severidade CRÍTICA.");
        System.out.println("[01:58:05] [INCIDENTE] Alerta #A-1042 escalado automaticamente para incidente #INC-0231.");
        System.out.println("[01:58:05] [JIRA] Criando ticket vinculado ao incidente #INC-0231...");
        System.out.println("[01:58:06] [JIRA] Ticket 'MONTECH-231' criado com sucesso. Prioridade: Alta. Responsável: Equipe de Infraestrutura.");
        System.out.println("[01:58:06] [SLACK] Enviando notificação para o canal #infra-montech...");
        System.out.println("[01:58:07] [SLACK] Mensagem entregue: '⚠️ Incidente #INC-0231 aberto — CPU crítica no servidor SDV. Ticket: MONTECH-231'.");
        System.out.println("[01:58:08] [USUARIO] Operador visualiza o incidente #INC-0231 na tela de Alertas e abre os detalhes.");
        System.out.println("[01:58:08] [DASHBOARD] Exibindo histórico de métricas do servidor SDV nas últimas 2 horas.");
        System.out.println("[01:58:09] [RELATORIO] Usuário acessa a tela de Relatórios da empresa (ID 12).");
        System.out.println("[01:58:09] [RELATORIO] Carregando relatórios anteriores relacionados à infraestrutura monitorada...");
        System.out.println("[01:58:10] [RELATORIO] 5 relatórios encontrados. Último status de análise: 'Em andamento'.");
        System.out.println("[01:58:10] [INFRAESTRUTURA] Equipe de infraestrutura identifica processo consumindo recursos em excesso no SDV.");
        System.out.println("[01:58:11] [INFRAESTRUTURA] Processo finalizado. CPU do servidor SDV normalizada para 41%.");
        System.out.println("[01:58:11] [INCIDENTE] Incidente #INC-0231 marcado como RESOLVIDO.");
        System.out.println("[01:58:12] [JIRA] Ticket 'MONTECH-231' atualizado automaticamente para status 'Concluído'.");
        System.out.println("[01:58:12] [SLACK] Notificação enviada ao canal #infra-montech: 'Incidente #INC-0231 resolvido.'");
        System.out.println("[01:58:13] [SISTEMA] Fim da simulação do fluxo de monitoramento MonTech.");
    }
}
