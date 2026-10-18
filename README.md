# Criar arquivo .env e colocar as respectivas credencais
JIRA_URL=
JIRA_EMAIL=
JIRA_TOKEN=

# Lembre-se de colocar o .env na pasta target, para o .jarr funcionar corretamente

# Criando .jar:
Clique no mavem no menu lateral direito
Clique no 'logs' e logo em seguida no 'LifeCycle'
Então de dois clique em clean e depois dois cliques no package

# Rodar arquivo jar
java -jar openTickets-jar-with-dependencies.jar "https://montechsd.atlassian.net" "botmontech@protonmail.com" "ATATT3xFfGF0MWo_zxNbNrvWLE8wpvRB55h4rCyyNi6sECykHLR_yjItk9siiVcZ8iKrBWsWx21-UlRgeKEDexxvERCGRC7nKLfky_i-rvl9kL0DBDS1EIQ5bBoVTHMvksNY_uoZ9_YRkBBe_y_cyPe_HRPlsGTpK8VmSBM49qJCK3zbi3zwxSY=0F6A4BB3" "LOGS" "Ticket criada via Java Montech validado" "Bug" "Erro ao processar o lote 123." "High" "2026-10-05"