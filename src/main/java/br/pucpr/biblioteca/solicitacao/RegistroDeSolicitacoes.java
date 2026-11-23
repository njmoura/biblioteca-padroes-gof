package br.pucpr.biblioteca.solicitacao;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Registro idempotente do invocador: quais identificadores ja foram executados e com que resultado. */
public class RegistroDeSolicitacoes {
    private final Set<UUID> reservados = ConcurrentHashMap.newKeySet();
    private final Map<UUID, ResultadoSolicitacao> resultados = new ConcurrentHashMap<>();

    /** Insercao atomica com chave unica: entre duas submissoes simultaneas do mesmo id, so uma recebe true. */
    public boolean reservarExecucao(UUID id) {
        return reservados.add(id);
    }

    public void concluir(UUID id, ResultadoSolicitacao resultado) {
        resultados.put(id, resultado);
    }

    public ResultadoSolicitacao resultadoDe(UUID id) {
        return resultados.get(id);
    }

    /** Falha tecnica: libera o id para uma nova tentativa. */
    public void liberar(UUID id) {
        reservados.remove(id);
    }
}
