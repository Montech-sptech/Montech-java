package com.montech;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class MontechDao {
    private final JdbcTemplate jdbcTemplate;

    public MontechDao(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    public void BuscarServidor() {
        List<Servidor> servidores = jdbcTemplate.query("SELECT * FROM servidor;", new BeanPropertyRowMapper<>(Servidor.class));
        for (Servidor servidor : servidores) {
            System.out.println(servidor.getNomeServidor());
        }
    }
}
