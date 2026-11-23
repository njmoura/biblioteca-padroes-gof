package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.acesso.Relacionamento;
import br.pucpr.biblioteca.dominio.Estudante;

/** Template Method (ConcreteClass) para Estudante. Sobrescreve o gancho: exige a Pessoa cadastrada. */
public class TratadorDeEstudante extends TratadorDeCadastro<Estudante, DadosPapel> implements iEstudante {
    private final Relacionamento relacionamento;
    private final iPessoa cadastroPessoa;

    public TratadorDeEstudante(Relacionamento relacionamento, iPessoa cadastroPessoa) {
        this.relacionamento = relacionamento;
        this.cadastroPessoa = cadastroPessoa;
    }

    @Override
    public ResultadoCadastro<Estudante> inserirEstudante(String id, String cpf) {
        return incluir(new DadosPapel(id, cpf));
    }

    @Override
    protected Estudante buscarExistente(DadosPapel d) { return relacionamento.consultarEstudante(d.id()); }

    @Override
    protected void validar(DadosPapel d) {
        exigirPreenchido(d.id(), "id do estudante");
        exigirPreenchido(d.cpf(), "CPF");
        exigirTamanhoMaximo(d.id(), 20, "id do estudante");
    }

    @Override
    protected void verificarPreRequisitos(DadosPapel d) {
        if (cadastroPessoa.consultarPessoa(d.cpf()) == null) {
            throw new PessoaNaoCadastradaException(d.cpf());
        }
    }

    @Override
    protected Estudante criar(DadosPapel d) { return new Estudante(d.id(), d.cpf()); }

    @Override
    protected void armazenar(Estudante estudante) { relacionamento.inserirEstudante(estudante); }

    @Override
    public Estudante consultarEstudante(String id) { return relacionamento.consultarEstudante(id); }

    @Override
    public boolean atualizarEstudante(String id, String cpf) {
        if (relacionamento.consultarEstudante(id) == null || cadastroPessoa.consultarPessoa(cpf) == null) {
            return false;
        }
        relacionamento.inserirEstudante(new Estudante(id, cpf));
        return true;
    }

    @Override
    public boolean excluirEstudante(String id) {
        Estudante estudante = relacionamento.consultarEstudante(id);
        if (estudante == null) {
            return false;
        }
        relacionamento.excluirEstudante(estudante);
        return true;
    }
}
