package br.pucpr.biblioteca.persistencia;

/** Falha tecnica de armazenamento (nao e regra de dominio). */
public class ErroDePersistencia extends RuntimeException {
    public ErroDePersistencia(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
