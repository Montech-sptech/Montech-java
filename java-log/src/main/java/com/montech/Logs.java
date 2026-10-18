package com.montech;

import org.springframework.jdbc.core.JdbcTemplate;

public class Logs {
    public static void main(String[] args) {
        ConexaoBanco conexaoBanco = new ConexaoBanco();
        JdbcTemplate template = conexaoBanco.getJdbcTemplate();


        MontechDao dao = new MontechDao(template);
        dao.BuscarServidor();

    }
}
