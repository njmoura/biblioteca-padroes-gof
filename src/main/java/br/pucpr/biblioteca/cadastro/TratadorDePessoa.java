package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.acesso.Relacionamento;
import br.pucpr.biblioteca.dominio.Pessoa;

/** Template Method (ConcreteClass) para Pessoa. Realiza iPessoa e delega ao Relacionamento. */
public class TratadorDePessoa extends TratadorDeCadastro<Pessoa, DadosPessoa> implements iPessoa {
    private final Relacionamento relacionamento;

    public TratadorDePessoa(Relacionamento relacionamento) {
        this.relacionamento = relacionamento;
    }

    @Override
    public ResultadoCadastro<Pessoa> inserirPessoa(String cpf, String nome, String sobrenome, String email) {
        return incluir(new DadosPessoa(cpf, nome, sobrenome, email));
    }

    @Override
    protected Pessoa buscarExistente(DadosPessoa d) { return relacionamento.consultarPessoa(d.cpf()); }

    @Override
    protected void validar(DadosPessoa d) {
        exigirPreenchido(d.cpf(), "CPF");
        exigirPreenchido(d.nome(), "nome");
        if (!d.cpf().matches("\\d{11}")) {
            throw new ValidacaoException("CPF deve ter 11 digitos");
        }
        exigirTamanhoMaximo(d.nome(), 80, "nome");
        exigirTamanhoMaximo(d.sobrenome(), 80, "sobrenome");
        exigirTamanhoMaximo(d.email(), 120, "e-mail");
    }

    @Override
    protected Pessoa criar(DadosPessoa d) { return new Pessoa(d.cpf(), d.nome(), d.sobrenome(), d.email()); }

    @Override
    protected void armazenar(Pessoa pessoa) { relacionamento.inserirPessoa(pessoa); }

    @Override
    public Pessoa consultarPessoa(String cpf) { return relacionamento.consultarPessoa(cpf); }

    @Override
    public boolean atualizarPessoa(String cpf, String nome, String sobrenome, String email) {
        Pessoa pessoa = relacionamento.consultarPessoa(cpf);
        if (pessoa == null) {
            return false;
        }
        pessoa.atualizar(nome, sobrenome, email);
        relacionamento.atualizarPessoa(pessoa);
        return true;
    }

    @Override
    public boolean excluirPessoa(String cpf) {
        Pessoa pessoa = relacionamento.consultarPessoa(cpf);
        if (pessoa == null) {
            return false;
        }
        relacionamento.excluirPessoa(pessoa);
        return true;
    }
}
