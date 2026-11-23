package br.pucpr.biblioteca.persistencia;

import br.pucpr.biblioteca.persistencia.memoria.FabricaEmMemoria;
import br.pucpr.biblioteca.persistencia.mysql.FabricaMySQL;

/** Unico ponto em que a familia de persistencia e escolhida, pelo perfil de execucao. */
public class ConfiguradorDePersistencia {
    public static final String URL_PADRAO =
            "jdbc:mysql://localhost:3310/biblioteca?useSSL=false&allowPublicKeyRetrieval=true";

    public FabricaDePersistencia fabricaPara(Perfil perfil) {
        if (perfil == null) {
            throw new IllegalArgumentException("perfil de execucao ausente");
        }
        switch (perfil) {
            case TESTE:
                return new FabricaEmMemoria();
            case PRODUCAO:
                return new FabricaMySQL(
                        variavel("BIBLIOTECA_DB_URL", URL_PADRAO),
                        variavel("BIBLIOTECA_DB_USUARIO", "biblioteca"),
                        variavel("BIBLIOTECA_DB_SENHA", "biblioteca"));
            default:
                throw new IllegalArgumentException("perfil desconhecido: " + perfil);
        }
    }

    private String variavel(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}
