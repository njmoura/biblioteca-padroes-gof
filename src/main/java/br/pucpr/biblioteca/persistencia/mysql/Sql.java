package br.pucpr.biblioteca.persistencia.mysql;

import br.pucpr.biblioteca.persistencia.ErroDePersistencia;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Auxiliar de JDBC: evita repetir PreparedStatement/ResultSet em cada repositorio. */
final class Sql {
    interface Leitor<T> {
        T ler(ResultSet rs) throws SQLException;
    }

    private Sql() { }

    static int executar(Connection c, String sql, Object... parametros) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            preencher(ps, parametros);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new ErroDePersistencia("falha ao executar: " + sql, e);
        }
    }

    static <T> List<T> consultar(Connection c, String sql, Leitor<T> leitor, Object... parametros) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            preencher(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leitor.ler(rs));
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new ErroDePersistencia("falha ao consultar: " + sql, e);
        }
    }

    static <T> T um(Connection c, String sql, Leitor<T> leitor, Object... parametros) {
        List<T> lista = consultar(c, sql, leitor, parametros);
        return lista.isEmpty() ? null : lista.get(0);
    }

    private static void preencher(PreparedStatement ps, Object... parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            ps.setObject(i + 1, parametros[i]);
        }
    }
}
