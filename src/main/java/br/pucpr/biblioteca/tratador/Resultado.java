package br.pucpr.biblioteca.tratador;

/** O que os tratadores devolvem a interface: sucesso ou recusa, com uma mensagem. Nunca colecoes internas. */
public record Resultado(boolean sucesso, String mensagem) {

    public static Resultado ok(String mensagem) {
        return new Resultado(true, mensagem);
    }

    public static Resultado recusado(String motivo) {
        return new Resultado(false, motivo);
    }

    @Override
    public String toString() {
        return (sucesso ? "OK: " : "RECUSADO: ") + mensagem;
    }
}
