package com.montech.bd;

public class ChamadoAberto {

    private final Integer idChamado;
    private final String titulo;
    private final String linkJira;
    private final Integer fkServidorComponente;
    private final Integer fkAeroporto; // vem do JOIN com servidor, não existe mais em chamado

    public ChamadoAberto(Integer idChamado, String titulo, String linkJira,
                         Integer fkServidorComponente, Integer fkAeroporto) {
        this.idChamado = idChamado;
        this.titulo = titulo;
        this.linkJira = linkJira;
        this.fkServidorComponente = fkServidorComponente;
        this.fkAeroporto = fkAeroporto;
    }

    public Integer getIdChamado() { return idChamado; }
    public String getTitulo() { return titulo; }
    public String getLinkJira() { return linkJira; }
    public Integer getFkServidorComponente() { return fkServidorComponente; }
    public Integer getFkAeroporto() { return fkAeroporto; }

    /** Extrai a chave do Jira (ex: LOGS-12) do link .../browse/LOGS-12 */
    public String getChaveJira() {
        if (linkJira == null) return null;
        return linkJira.substring(linkJira.lastIndexOf('/') + 1);
    }
}