package com.montech.bd;

public class LimiteComponente {

    private String nomeComponente;
    private String nomeCodigo;
    private String unidadeMedida;
    private Double limiteAtencao;
    private Double limiteCritico;

    public LimiteComponente(String nomeComponente, String nomeCodigo, String unidadeMedida,
                            Double limiteAtencao, Double limiteCritico) {
        this.nomeComponente = nomeComponente;
        this.nomeCodigo = nomeCodigo;
        this.unidadeMedida = unidadeMedida;
        this.limiteAtencao = limiteAtencao;
        this.limiteCritico = limiteCritico;
    }

    public String getNomeComponente() { return nomeComponente; }
    public String getNomeCodigo() { return nomeCodigo; }
    public String getUnidadeMedida() { return unidadeMedida; }
    public Double getLimiteAtencao() { return limiteAtencao; }
    public Double getLimiteCritico() { return limiteCritico; }
}
