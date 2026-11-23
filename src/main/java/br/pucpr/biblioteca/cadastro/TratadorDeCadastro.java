package br.pucpr.biblioteca.cadastro;

import br.pucpr.biblioteca.dominio.ExcecaoDeDominio;

/**
 * Template Method (papel AbstractClass). incluir() e o metodo-modelo: e final e fixa a
 * ordem buscar existente, validar, verificar pre-requisitos, criar, armazenar, apresentar.
 * T e a entidade cadastrada; D sao os dados recebidos da interface.
 */
public abstract class TratadorDeCadastro<T, D> {

    public final ResultadoCadastro<T> incluir(D dados) {
        T existente = buscarExistente(dados);
        if (existente != null) {
            return apresentar(existente, false);         // duplicado: mostra o cadastro existente
        }
        try {
            validar(dados);
            verificarPreRequisitos(dados);               // gancho
        } catch (ExcecaoDeDominio recusa) {
            return ResultadoCadastro.recusado(recusa.getMessage());   // falhou ANTES de criar/armazenar
        }
        T novo = criar(dados);
        armazenar(novo);
        return apresentar(novo, true);
    }

    // ---- operacoes primitivas: cada cadastro implementa as suas ----
    protected abstract T buscarExistente(D dados);

    protected abstract void validar(D dados);

    protected abstract T criar(D dados);

    protected abstract void armazenar(T entidade);

    /** Gancho (hook): por padrao nao ha pre-requisito. Estudante e Atendente exigem a Pessoa cadastrada. */
    protected void verificarPreRequisitos(D dados) { }

    /** Passo comum a todos os cadastros. */
    protected ResultadoCadastro<T> apresentar(T entidade, boolean novo) {
        return new ResultadoCadastro<>(novo ? StatusCadastro.CRIADO : StatusCadastro.EXISTENTE, entidade, null);
    }

    protected void exigirPreenchido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacaoException(campo + " e obrigatorio");
        }
    }

    /** Limite igual ao da coluna no esquema relacional (schema.sql), para recusar antes de gravar. */
    protected void exigirTamanhoMaximo(String valor, int maximo, String campo) {
        if (valor != null && valor.length() > maximo) {
            throw new ValidacaoException(campo + " deve ter no maximo " + maximo + " caracteres");
        }
    }
}
