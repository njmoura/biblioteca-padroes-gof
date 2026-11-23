package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.acesso.Relacionamento;
import br.pucpr.biblioteca.dominio.Atendente;

/** Template Method (ConcreteClass) para Atendente. Sobrescreve o gancho: exige a Pessoa cadastrada. */
public class TratadorDeAtendente extends TratadorDeCadastro<Atendente, DadosPapel> implements iAtendente {
    private final Relacionamento relacionamento;
    private final iPessoa cadastroPessoa;

    public TratadorDeAtendente(Relacionamento relacionamento, iPessoa cadastroPessoa) {
        this.relacionamento = relacionamento;
        this.cadastroPessoa = cadastroPessoa;
    }

    @Override
    public ResultadoCadastro<Atendente> inserirAtendente(String id, String cpf) {
        return incluir(new DadosPapel(id, cpf));
    }

    @Override
    protected Atendente buscarExistente(DadosPapel d) { return relacionamento.consultarAtendente(d.id()); }

    @Override
    protected void validar(DadosPapel d) {
        exigirPreenchido(d.id(), "id do atendente");
        exigirPreenchido(d.cpf(), "CPF");
        exigirTamanhoMaximo(d.id(), 20, "id do atendente");
    }

    @Override
    protected void verificarPreRequisitos(DadosPapel d) {
        if (cadastroPessoa.consultarPessoa(d.cpf()) == null) {
            throw new PessoaNaoCadastradaException(d.cpf());
        }
    }

    @Override
    protected Atendente criar(DadosPapel d) { return new Atendente(d.id(), d.cpf()); }

    @Override
    protected void armazenar(Atendente atendente) { relacionamento.inserirAtendente(atendente); }

    @Override
    public Atendente consultarAtendente(String id) { return relacionamento.consultarAtendente(id); }

    @Override
    public boolean atualizarAtendente(String id, String cpf) {
        if (relacionamento.consultarAtendente(id) == null || cadastroPessoa.consultarPessoa(cpf) == null) {
            return false;
        }
        relacionamento.inserirAtendente(new Atendente(id, cpf));
        return true;
    }

    @Override
    public boolean excluirAtendente(String id) {
        Atendente atendente = relacionamento.consultarAtendente(id);
        if (atendente == null) {
            return false;
        }
        relacionamento.excluirAtendente(atendente);
        return true;
    }
}
