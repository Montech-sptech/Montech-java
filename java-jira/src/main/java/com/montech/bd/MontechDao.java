package com.montech.bd;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class MontechDao {
    private final JdbcTemplate jdbcTemplate;

    public MontechDao(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    public void BuscarServidores() {
        List<Servidor> servidores = jdbcTemplate.query("SELECT * FROM servidor;", new BeanPropertyRowMapper<>(Servidor.class));
        for (Servidor servidor : servidores) {
            System.out.println(servidor.getNomeServidor());
        }
    }

    public Servidor BuscarServidor(String token) {
            String sql = "SELECT idServidor, token, nomeServidor, sistemaOperacional, intervaloColeta, statusAtividade, fkAeroporto FROM servidor WHERE token = ?";

            return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(Servidor.class), token);
    }

    public Aeroporto buscarAeroportoId(Integer id) {
        String sql = "SELECT idAeroporto, nome, codigoAeroporto, statusAtividade, urlJira, tokenJira, emailJira FROM aeroporto WHERE idAeroporto = ?;";

        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(Aeroporto.class), id);
    }
}
