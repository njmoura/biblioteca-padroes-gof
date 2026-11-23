package br.pucpr.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.pucpr.biblioteca.app.Aplicacao;
import br.pucpr.biblioteca.cadastro.DadosPapel;
import br.pucpr.biblioteca.cadastro.ResultadoCadastro;
import br.pucpr.biblioteca.cadastro.StatusCadastro;
import br.pucpr.biblioteca.cadastro.TratadorDeCadastro;
import br.pucpr.biblioteca.cadastro.TratadorDeEstudante;
import br.pucpr.biblioteca.dominio.Atendente;
import br.pucpr.biblioteca.dominio.Estudante;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Requisito 7 - Rotina comum para cadastros (Template Method). */
class R07TemplateMethodTest {

    /** Tratador de estudante que anota a ordem em que os passos do metodo-modelo sao chamados. */
    private static class TratadorQueAnota extends TratadorDeEstudante {
        final List<String> passos = new ArrayList<>();

        TratadorQueAnota(Aplicacao app) {
            super(app.relacionamento, app.tratadorDePessoa);
        }

        @Override
        protected Estudante buscarExistente(DadosPapel d) { passos.add("buscarExistente"); return super.buscarExistente(d); }

        @Override
        protected void validar(DadosPapel d) { passos.add("validar"); super.validar(d); }

        @Override
        protected void verificarPreRequisitos(DadosPapel d) { passos.add("verificarPreRequisitos"); super.verificarPreRequisitos(d); }

        @Override
        protected Estudante criar(DadosPapel d) { passos.add("criar"); return super.criar(d); }

        @Override
        protected void armazenar(Estudante e) { passos.add("armazenar"); super.armazenar(e); }

        @Override
        protected ResultadoCadastro<Estudante> apresentar(Estudante e, boolean novo) { passos.add("apresentar"); return super.apresentar(e, novo); }
    }

    @Test
    void R07_estudanteDuplicadoRetornaOCadastroExistente() {
        Aplicacao app = Cenario.novo();                                       // E1 ja cadastrado para a Ana
        TratadorQueAnota tratador = new TratadorQueAnota(app);

        ResultadoCadastro<Estudante> r = tratador.inserirEstudante("E1", Cenario.CPF_BRUNO);

        assertEquals(StatusCadastro.EXISTENTE, r.status());
        assertEquals("E1", r.entidade().getId());
        assertEquals(Cenario.CPF_ANA, r.entidade().getCpf());                 // dados do cadastro existente
        assertEquals(List.of("buscarExistente", "apresentar"), tratador.passos);
    }

    @Test
    void R07_estudanteComCpfNaoCadastradoFalhaAntesDeQualquerGravacao() {
        Aplicacao app = Cenario.novo();
        TratadorQueAnota tratador = new TratadorQueAnota(app);

        ResultadoCadastro<Estudante> r = tratador.inserirEstudante("E9", "99999999999");

        assertEquals(StatusCadastro.RECUSADO, r.status());
        assertNull(r.entidade());                                             // nenhum dado ficticio e apresentado
        assertTrue(r.motivo().contains("pessoa nao cadastrada"));
        assertEquals(List.of("buscarExistente", "validar", "verificarPreRequisitos"), tratador.passos);   // criar/armazenar nao rodaram
        assertNull(app.relacionamento.consultarEstudante("E9"));              // nada foi gravado
        assertNull(app.relacionamento.consultarPessoa("99999999999"));        // nem uma Pessoa para o CPF desconhecido
    }

    @Test
    void R07_metodoModeloNaoPodeSerSobrescrito() throws NoSuchMethodException {
        assertTrue(Modifier.isFinal(TratadorDeCadastro.class.getMethod("incluir", Object.class).getModifiers()));
    }

    @Test
    void R07_inclusaoValidaSegueAOrdemFixaDosPassos() {
        Aplicacao app = Cenario.novo();
        app.tratadorDePessoa.inserirPessoa("33333333333", "Carla", "Dias", "carla@exemplo.br");
        TratadorQueAnota tratador = new TratadorQueAnota(app);

        ResultadoCadastro<Estudante> r = tratador.inserirEstudante("E3", "33333333333");

        assertEquals(StatusCadastro.CRIADO, r.status());
        assertEquals(List.of("buscarExistente", "validar", "verificarPreRequisitos", "criar", "armazenar", "apresentar"),
                tratador.passos);
        assertEquals("E3", app.relacionamento.consultarEstudante("E3").getId());
    }

    @Test
    void R07_atendenteTambemExigePessoaELivroEPessoaNaoTemPreRequisito() {
        Aplicacao app = Cenario.novo();

        ResultadoCadastro<Atendente> semPessoa = app.tratadorDeAtendente.inserirAtendente("A9", "99999999999");
        ResultadoCadastro<Atendente> comPessoa = app.tratadorDeAtendente.inserirAtendente("A1", Cenario.CPF_BRUNO);

        assertEquals(StatusCadastro.RECUSADO, semPessoa.status());
        assertEquals(StatusCadastro.CRIADO, comPessoa.status());
        assertEquals(StatusCadastro.CRIADO, app.tratadorDeAcervo.inserirLivro("L7", "Iracema").status());
        assertEquals(StatusCadastro.EXISTENTE, app.tratadorDeAcervo.inserirLivro("L7", "Outro titulo").status());
        assertEquals(StatusCadastro.RECUSADO, app.tratadorDePessoa.inserirPessoa("123", "Curto", "", "").status());   // validacao propria
    }
}
