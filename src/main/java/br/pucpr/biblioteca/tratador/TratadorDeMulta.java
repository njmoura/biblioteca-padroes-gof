package br.pucpr.biblioteca.tratador;

import br.pucpr.biblioteca.banco.BancoSimulado;
import br.pucpr.biblioteca.banco.SituacaoCobranca;
import br.pucpr.biblioteca.dominio.ExcecaoDeDominio;
import br.pucpr.biblioteca.multa.Multa;
import br.pucpr.biblioteca.multa.PoliticaDeMulta;
import br.pucpr.biblioteca.multa.SeletorDePolitica;
import br.pucpr.biblioteca.multa.iMulta;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tratador do Financeiro para a interface: configuracao da politica, simulacao e consulta de multas. */
public class TratadorDeMulta {
    private final SeletorDePolitica seletor;
    private final iMulta financeiro;
    private final BancoSimulado banco;

    public TratadorDeMulta(SeletorDePolitica seletor, iMulta financeiro, BancoSimulado banco) {
        this.seletor = seletor;
        this.financeiro = financeiro;
        this.banco = banco;
    }

    public Resultado definirPolitica(String id) {
        try {
            seletor.definirVigente(id);
            return Resultado.ok("politica vigente: " + id);
        } catch (ExcecaoDeDominio e) {
            return Resultado.recusado(e.getMessage());
        }
    }

    public String politicaVigente() {
        return seletor.vigente().id();
    }

    /** Ids das politicas registradas no seletor; uma politica nova aparece aqui sem alterar a tela. */
    public List<String> politicasDisponiveis() {
        List<String> ids = new ArrayList<>();
        for (PoliticaDeMulta p : seletor.todas()) {
            ids.add(p.id());
        }
        return ids;
    }

    /** Simulacao: calcula o valor em todas as politicas; nao cria Multa nem emite boleto. */
    public Map<String, BigDecimal> simular(int dias) {
        Map<String, BigDecimal> valores = new LinkedHashMap<>();
        for (PoliticaDeMulta p : seletor.todas()) {
            valores.put(p.id(), p.calcular(dias));
        }
        return valores;
    }

    public List<String> listarMultas() {
        List<String> linhas = new ArrayList<>();
        for (Multa m : financeiro.listarMultas()) {
            linhas.add(m.toString());
        }
        return linhas;
    }

    /** Demonstracao: o estudante paga no banco, e o banco notifica o sistema pelo adaptador. */
    public Resultado simularPagamentoNoBanco(String idExterno) {
        for (Multa m : financeiro.listarMultas()) {
            if (m.getCobranca() != null && m.getCobranca().getIdExterno().equals(idExterno)) {
                boolean jaQuitada = m.getCobranca().getSituacao() == SituacaoCobranca.QUITADA;
                banco.simularPagamento(idExterno);                       // o banco notifica (de novo, se for repeticao)
                return jaQuitada
                        ? Resultado.ok("notificacao repetida do banco ignorada: a cobranca ja estava quitada, sem segunda quitacao - " + m)
                        : Resultado.ok("banco notificou o pagamento - " + m);
            }
        }
        return Resultado.recusado("nao ha boleto com o numero " + idExterno);
    }
}
