package br.pucpr.biblioteca.persistencia;

/** Tentativa de criar um repositorio com a unidade de trabalho de outra familia. */
public class FamiliaIncompativelException extends RuntimeException {
    public FamiliaIncompativelException(String familia, UnidadeDeTrabalho recebida) {
        super("a fabrica " + familia + " nao aceita a unidade de trabalho "
                + (recebida == null ? "nula" : recebida.getClass().getSimpleName()));
    }
}
