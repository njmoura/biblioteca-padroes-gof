package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.acesso.iAcervo;
import br.pucpr.biblioteca.dominio.ExcecaoDeDominio;
import br.pucpr.biblioteca.dominio.Livro;
import br.pucpr.biblioteca.tratador.Resultado;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Template Method (ConcreteClass) para Livro. Realiza iLivro e delega ao Acervo. */
public class TratadorDeAcervo extends TratadorDeCadastro<Livro, DadosLivro> implements iLivro {
    private final iAcervo acervo;
    private final List<Consumer<String>> aoExcluir = new ArrayList<>();
    private final List<Runnable> aoAlterar = new ArrayList<>();

    public TratadorDeAcervo(iAcervo acervo) {
        this.acervo = acervo;
    }

    /** Registra uma reacao a exclusao de um livro (recebe o id); a montagem liga aqui a hierarquia do acervo. */
    public void aoExcluirLivro(Consumer<String> reacao) {
        aoExcluir.add(reacao);
    }

    /** Registra uma reacao a qualquer alteracao do acervo (inclusao, atualizacao ou exclusao de livro). */
    public void aoAlterarAcervo(Runnable reacao) {
        aoAlterar.add(reacao);
    }

    private void avisarAlteracao() {
        for (Runnable reacao : List.copyOf(aoAlterar)) {
            reacao.run();
        }
    }

    @Override
    public ResultadoCadastro<Livro> inserirLivro(String id, String titulo) {
        return incluir(new DadosLivro(id, titulo));
    }

    @Override
    protected Livro buscarExistente(DadosLivro d) { return acervo.consultar(d.id()); }

    @Override
    protected void validar(DadosLivro d) {
        exigirPreenchido(d.id(), "id do livro");
        exigirPreenchido(d.titulo(), "titulo");
        exigirTamanhoMaximo(d.id(), 20, "id do livro");
        exigirTamanhoMaximo(d.titulo(), 160, "titulo");
    }

    @Override
    protected Livro criar(DadosLivro d) { return new Livro(d.id(), d.titulo()); }

    @Override
    protected void armazenar(Livro livro) {
        acervo.inserir(livro);
        avisarAlteracao();                       // so roda quando um livro novo foi de fato gravado
    }

    @Override
    public Livro consultarLivro(String id) { return acervo.consultar(id); }

    @Override
    public boolean atualizarLivro(String id, String titulo) {
        Livro livro = acervo.consultar(id);
        if (livro == null) {
            return false;
        }
        livro.atualizar(titulo);
        acervo.atualizar(livro);
        avisarAlteracao();
        return true;
    }

    @Override
    public Resultado excluirLivro(String id) {
        Livro livro = acervo.consultar(id);
        if (livro == null) {
            return Resultado.recusado("livro " + id + " nao cadastrado");
        }
        try {
            livro.excluir();                         // so o estado LIVRE aceita
            acervo.excluir(livro);
            for (Consumer<String> reacao : List.copyOf(aoExcluir)) {
                reacao.accept(id);                   // so depois de a exclusao ter dado certo
            }
            avisarAlteracao();
            return Resultado.ok("livro " + id + " excluido");
        } catch (ExcecaoDeDominio e) {
            return Resultado.recusado(e.getMessage());
        }
    }

    @Override
    public Resultado enviarParaReparo(String idLivro) {
        Livro livro = acervo.consultar(idLivro);
        if (livro == null) {
            return Resultado.recusado("livro " + idLivro + " nao cadastrado");
        }
        try {
            livro.enviarParaReparo();                // dano identificado no acervo: LIVRE -> REPARANDO
            acervo.atualizar(livro);
            return Resultado.ok("livro " + livro.getId() + " \"" + livro.getTitulo() + "\" esta " + livro.getSituacao());
        } catch (ExcecaoDeDominio e) {
            return Resultado.recusado(e.getMessage());
        }
    }
}
