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

    public List<ServidorMonitorado> listarServidoresAtivos() {
        String sql = "SELECT s.idServidor, s.token, s.nomeServidor, s.fkAeroporto, a.fkEmpresa "
                + "FROM servidor s "
                + "JOIN aeroporto a ON a.idAeroporto = s.fkAeroporto "
                + "WHERE s.statusAtividade = 1 AND a.statusAtividade = 1";

        return jdbcTemplate.query(sql, (rs, i) -> new ServidorMonitorado(
                rs.getInt("idServidor"),
                rs.getString("token"),
                rs.getString("nomeServidor"),
                rs.getInt("fkAeroporto"),
                rs.getInt("fkEmpresa")));
    }

    public List<LimiteComponente> listarLimites(Integer idServidor) {
        String sql = "SELECT c.nomeComponente, c.nomeCodigo, c.unidadeMedida, "
                + "sc.limiteAtencao, sc.limiteCritico "
                + "FROM servidorcomponente sc "
                + "JOIN componente c ON c.idComponente = sc.fkComponente "
                + "WHERE sc.fkServidor = ?";

        return jdbcTemplate.query(sql, (rs, i) -> {
            java.math.BigDecimal atencao = rs.getBigDecimal("limiteAtencao");
            java.math.BigDecimal critico = rs.getBigDecimal("limiteCritico");
            return new LimiteComponente(
                    rs.getString("nomeComponente"),
                    rs.getString("nomeCodigo"),
                    rs.getString("unidadeMedida"),
                    atencao == null ? null : atencao.doubleValue(),
                    critico == null ? null : critico.doubleValue());
        }, idServidor);
    }


}
