package br.pucpr.biblioteca.dominio;

/** Papel assumido por uma Pessoa (Estudante ou Atendente). Todo papel tem id e esta ligado a uma pessoa pelo CPF. */
public abstract class Papel {
    private final String id;
    private final String cpf;

    protected Papel(String id, String cpf) {
        this.id = id;
        this.cpf = cpf;
    }

    public String getId() { return id; }

    public String getCpf() { return cpf; }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + id + " (CPF " + cpf + ")";
    }
}
