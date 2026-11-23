package br.pucpr.biblioteca.dominio;

public class Pessoa {
    private final String cpf;
    private String nome;
    private String sobrenome;
    private String email;

    public Pessoa(String cpf, String nome, String sobrenome, String email) {
        this.cpf = cpf;
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
    }

    public void atualizar(String nome, String sobrenome, String email) {
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
    }

    public String getCpf() { return cpf; }

    public String getNome() { return nome; }

    public String getSobrenome() { return sobrenome; }

    public String getEmail() { return email; }

    @Override
    public String toString() {
        return "Pessoa " + cpf + " " + nome + " " + sobrenome + " <" + email + ">";
    }
}
