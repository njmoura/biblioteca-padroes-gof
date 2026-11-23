package br.pucpr.biblioteca.cadastro;

/** Resultado apresentado por uma inclusao: entidade criada, entidade ja existente ou recusa com motivo. */
public record ResultadoCadastro<T>(StatusCadastro status, T entidade, String motivo) {

    public static <T> ResultadoCadastro<T> recusado(String motivo) {
        return new ResultadoCadastro<>(StatusCadastro.RECUSADO, null, motivo);
    }

    @Override
    public String toString() {
        return status + ": " + (entidade != null ? entidade : motivo);
    }
}
