package br.pucpr.biblioteca.multa;

import br.pucpr.biblioteca.banco.Cobranca;
import br.pucpr.biblioteca.banco.GatewayDeCobranca;
import br.pucpr.biblioteca.banco.RegistroDeCobrancas;
import br.pucpr.biblioteca.banco.SolicitacaoCobranca;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servico de multas do Financeiro.
 * Strategy (papel Context): calcula pela politica vigente, sem saber qual e.
 * Adapter (papel Client): cobra pelo contrato GatewayDeCobranca, sem "if" por banco.
 */
public class ServicoDeMulta implements iMulta {
    private final SeletorDePolitica seletor;
    private final GatewayDeCobranca gateway;
    private final RegistroDeCobrancas registro;
    private final Map<String, Multa> multas = new LinkedHashMap<>();

    public ServicoDeMulta(SeletorDePolitica seletor, GatewayDeCobranca gateway, RegistroDeCobrancas registro) {
        this.seletor = seletor;
        this.gateway = gateway;
        this.registro = registro;
    }

    @Override
    public Multa emitirMulta(String idDevolucao, String cpf, int diasAtraso, LocalDate dataEmissao) {
        Multa existente = multas.get(idDevolucao);          // idDevolucao e a chave de idempotencia
        if (existente != null) {
            return existente;
        }
        PoliticaDeMulta politica = seletor.vigente();
        BigDecimal valor = politica.calcular(diasAtraso).setScale(2, RoundingMode.HALF_UP);   // sempre duas casas
        if (valor.signum() <= 0) {
            return null;                                    // sem atraso cobravel: nenhuma multa
        }
        Multa multa = new Multa(idDevolucao, cpf, diasAtraso, valor, politica.id(), dataEmissao);
        multa.definirCobranca(cobrar(multa));
        multas.put(idDevolucao, multa);
        return multa;
    }

    /** Emite o boleto pelo contrato institucional; qual banco atende e decidido na montagem da aplicacao. */
    public Cobranca cobrar(Multa multa) {
        SolicitacaoCobranca solicitacao = new SolicitacaoCobranca("COB-" + multa.getIdDevolucao(), multa.getCpf(), multa.getValor());
        Cobranca cobranca = gateway.emitirBoleto(solicitacao);
        registro.registrar(cobranca);
        return cobranca;
    }

    @Override
    public Multa consultarMulta(String idDevolucao) {
        return multas.get(idDevolucao);
    }

    @Override
    public List<Multa> listarMultas() {
        return new ArrayList<>(multas.values());
    }
}
