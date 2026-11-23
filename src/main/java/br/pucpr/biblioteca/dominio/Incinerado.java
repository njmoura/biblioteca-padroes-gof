package br.pucpr.biblioteca.dominio;

/** ConcreteState INCINERADO: estado final; nao aceita nenhum evento. */
class Incinerado extends EstadoLivro {
    static final Incinerado INSTANCIA = new Incinerado();

    @Override
    Situacao situacao() { return Situacao.INCINERADO; }
}
