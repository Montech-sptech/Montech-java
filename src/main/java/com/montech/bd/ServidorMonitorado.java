package com.montech.bd;

public class ServidorMonitorado {

    private Integer idServidor;
    private String token;
    private String nomeServidor;
    private Integer fkAeroporto;
    private Integer idEmpresa;

    public ServidorMonitorado(Integer idServidor, String token, String nomeServidor,
                              Integer fkAeroporto, Integer idEmpresa) {
        this.idServidor = idServidor;
        this.token = token;
        this.nomeServidor = nomeServidor;
        this.fkAeroporto = fkAeroporto;
        this.idEmpresa = idEmpresa;
    }

    public Integer getIdServidor() { return idServidor; }
    public String getToken() { return token; }
    public String getNomeServidor() { return nomeServidor; }
    public Integer getFkAeroporto() { return fkAeroporto; }
    public Integer getIdEmpresa() { return idEmpresa; }
}
