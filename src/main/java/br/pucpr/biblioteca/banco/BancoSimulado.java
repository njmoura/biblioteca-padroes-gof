package br.pucpr.biblioteca.banco;

/** So para a demonstracao: faz o banco simulado "receber o pagamento" e notificar o sistema. */
public interface BancoSimulado {
    void simularPagamento(String idExterno);
}
