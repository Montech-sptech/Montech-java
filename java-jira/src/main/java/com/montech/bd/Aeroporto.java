package com.montech.bd;

import java.util.ArrayList;
import java.util.List;

public class Aeroporto {

    private Integer idAeroporto;
    private String nome;
    private String codigoAeroporto;
    private Boolean statusAtividade;
    private String urlJira;
    private String tokenJira;
    private String emailJira;
    private List<Servidor> servidores;

    public Aeroporto() {
        this.statusAtividade = true;
    }

    public Aeroporto(String nome, String codigoAeroporto, String urlJira,
                     String tokenJira, String emailJira) {
        this();
        this.nome = nome;
        this.codigoAeroporto = codigoAeroporto;
        this.urlJira = urlJira;
        this.tokenJira = tokenJira;
        this.emailJira = emailJira;
        this.servidores = new ArrayList<>();
    }

    public Aeroporto(Integer idAeroporto, String nome, String codigoAeroporto,
                     Boolean statusAtividade, String urlJira, String tokenJira,
                     String emailJira) {
        this.idAeroporto = idAeroporto;
        this.nome = nome;
        this.codigoAeroporto = codigoAeroporto;
        this.statusAtividade = statusAtividade;
        this.urlJira = urlJira;
        this.tokenJira = tokenJira;
        this.emailJira = emailJira;
        this.servidores = new ArrayList<>();
    }

    public Integer getIdAeroporto() { return idAeroporto; }
    public void setIdAeroporto(Integer idAeroporto) { this.idAeroporto = idAeroporto; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCodigoAeroporto() { return codigoAeroporto; }
    public void setCodigoAeroporto(String codigoAeroporto) { this.codigoAeroporto = codigoAeroporto; }

    public Boolean getStatusAtividade() { return statusAtividade; }
    public void setStatusAtividade(Boolean statusAtividade) { this.statusAtividade = statusAtividade; }

    public String getUrlJira() { return urlJira; }
    public void setUrlJira(String urlJira) { this.urlJira = urlJira; }

    public String getTokenJira() { return tokenJira; }
    public void setTokenJira(String tokenJira) { this.tokenJira = tokenJira; }

    public String getEmailJira() { return emailJira; }
    public void setEmailJira(String emailJira) { this.emailJira = emailJira; }

    public List<Servidor> getServidores() {
        return servidores;
    }

    public void AdicionarServidor(Servidor servidor) {
        servidores.add(servidor);
    }

    @Override
    public String toString() {
        return "Aeroporto{" +
                "idAeroporto=" + idAeroporto +
                ", nome='" + nome + '\'' +
                ", codigoAeroporto='" + codigoAeroporto + '\'' +
                ", statusAtividade=" + statusAtividade +
                ", urlJira='" + urlJira + '\'' +
                ", emailJira='" + emailJira + '\'' +
                ", emailJira='" + tokenJira + '\'' +
                '}';
    }
}
