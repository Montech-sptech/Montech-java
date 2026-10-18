package com.montech;

public class Servidor {
    private Integer idServidor;
    private String token;
    private String nomeServidor;
    private String sistemaOperacional;
    private Integer intervaloColeta;
    private Boolean statusAtividade;
    private Integer fkAeroporto;

    public Servidor() {
    }

    public Servidor(String token, String nomeServidor, String sistemaOperacional,
                    Integer intervaloColeta, Boolean statusAtividade, Integer fkAeroporto) {
        this.token = token;
        this.nomeServidor = nomeServidor;
        this.sistemaOperacional = sistemaOperacional;
        this.intervaloColeta = intervaloColeta;
        this.statusAtividade = statusAtividade;
        this.fkAeroporto = fkAeroporto;
    }

    public Servidor(Integer idServidor, String token, String nomeServidor,
                    String sistemaOperacional, Integer intervaloColeta,
                    Boolean statusAtividade, Integer fkAeroporto) {
        this.idServidor = idServidor;
        this.token = token;
        this.nomeServidor = nomeServidor;
        this.sistemaOperacional = sistemaOperacional;
        this.intervaloColeta = intervaloColeta;
        this.statusAtividade = statusAtividade;
        this.fkAeroporto = fkAeroporto;
    }

    public Integer getIdServidor() { return idServidor; }
    public void setIdServidor(Integer idServidor) { this.idServidor = idServidor; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNomeServidor() { return nomeServidor; }
    public void setNomeServidor(String nomeServidor) { this.nomeServidor = nomeServidor; }

    public String getSistemaOperacional() { return sistemaOperacional; }
    public void setSistemaOperacional(String sistemaOperacional) { this.sistemaOperacional = sistemaOperacional; }

    public Integer getIntervaloColeta() { return intervaloColeta; }
    public void setIntervaloColeta(Integer intervaloColeta) { this.intervaloColeta = intervaloColeta; }

    public Boolean getStatusAtividade() { return statusAtividade; }
    public void setStatusAtividade(Boolean statusAtividade) { this.statusAtividade = statusAtividade; }

    public Integer getFkAeroporto() { return fkAeroporto; }
    public void setFkAeroporto(Integer fkAeroporto) { this.fkAeroporto = fkAeroporto; }

    @Override
    public String toString() {
        return "Servidor{" +
                "idServidor=" + idServidor +
                ", token='" + token + '\'' +
                ", nomeServidor='" + nomeServidor + '\'' +
                ", sistemaOperacional='" + sistemaOperacional + '\'' +
                ", intervaloColeta=" + intervaloColeta +
                ", statusAtividade=" + statusAtividade +
                ", fkAeroporto=" + fkAeroporto +
                '}';
    }
}
