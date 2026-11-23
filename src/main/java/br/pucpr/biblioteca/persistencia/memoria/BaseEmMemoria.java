package br.pucpr.biblioteca.persistencia.memoria;

import br.pucpr.biblioteca.dominio.Atendente;
import br.pucpr.biblioteca.dominio.Devolucao;
import br.pucpr.biblioteca.dominio.Emprestimo;
import br.pucpr.biblioteca.dominio.Estudante;
import br.pucpr.biblioteca.dominio.Livro;
import br.pucpr.biblioteca.dominio.Pessoa;
import br.pucpr.biblioteca.dominio.Reserva;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** "Banco" da familia em memoria: as colecoes compartilhadas pelos repositorios em memoria. */
public class BaseEmMemoria {
    final Map<String, Livro> livros = new LinkedHashMap<>();
    final Map<String, Pessoa> pessoas = new LinkedHashMap<>();
    final Map<String, Estudante> estudantes = new LinkedHashMap<>();
    final Map<String, Atendente> atendentes = new LinkedHashMap<>();
    final List<Reserva> reservas = new ArrayList<>();
    final List<Emprestimo> emprestimos = new ArrayList<>();
    final List<Devolucao> devolucoes = new ArrayList<>();

    /** Fotografia das colecoes, usada pela unidade de trabalho para poder desfazer. */
    BaseEmMemoria copia() {
        BaseEmMemoria c = new BaseEmMemoria();
        c.copiarDe(this);
        return c;
    }

    void restaurar(BaseEmMemoria fotografia) {
        copiarDe(fotografia);
    }

    private void copiarDe(BaseEmMemoria o) {
        livros.clear();
        livros.putAll(o.livros);
        pessoas.clear();
        pessoas.putAll(o.pessoas);
        estudantes.clear();
        estudantes.putAll(o.estudantes);
        atendentes.clear();
        atendentes.putAll(o.atendentes);
        reservas.clear();
        reservas.addAll(o.reservas);
        emprestimos.clear();
        emprestimos.addAll(o.emprestimos);
        devolucoes.clear();
        devolucoes.addAll(o.devolucoes);
    }
}
