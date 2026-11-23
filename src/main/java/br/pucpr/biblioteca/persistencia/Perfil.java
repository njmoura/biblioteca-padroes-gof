package br.pucpr.biblioteca.persistencia;

/** Perfil de execucao: decide, uma unica vez, a familia de persistencia. */
public enum Perfil {
    TESTE, PRODUCAO;

    public static Perfil de(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("perfil de execucao ausente: informe TESTE ou PRODUCAO");
        }
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("perfil de execucao desconhecido: " + texto + " (use TESTE ou PRODUCAO)");
        }
    }
}
