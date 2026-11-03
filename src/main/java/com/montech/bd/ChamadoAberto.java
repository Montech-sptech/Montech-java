package com.montech.bd;

public class ChamadoAberto {

    private Integer idChamado;
    private String titulo;
    private String linkJira;
    private Integer fkAeroporto;

    public ChamadoAberto(Integer idChamado, String titulo, String linkJira, Integer fkAeroporto) {
        this.idChamado = idChamado;
        this.titulo = titulo;
        this.linkJira = linkJira;
        this.fkAeroporto = fkAeroporto;
    }

    public Integer getIdChamado() { return idChamado; }
    public String getTitulo() { return titulo; }
    public String getLinkJira() { return linkJira; }
    public Integer getFkAeroporto() { return fkAeroporto; }

    /** Extrai a chave do Jira (ex: LOGS-12) do link .../browse/LOGS-12 */
    public String getChaveJira() {
        if (linkJira == null) return null;
        return linkJira.substring(linkJira.lastIndexOf('/') + 1);
    }
}