package br.pucpr.biblioteca.multa;

import br.pucpr.biblioteca.dominio.ExcecaoDeDominio;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Selecao da estrategia por configuracao: traduz o identificador gravado na
 * ConfiguracaoAdministrativa no objeto de politica. Uma politica nova e uma classe
 * nova mais um registrar() aqui; o ServicoDeMulta nao muda.
 */
public class SeletorDePolitica {
    private final ConfiguracaoAdministrativa config;
    private final Map<String, PoliticaDeMulta> politicas = new LinkedHashMap<>();

    public SeletorDePolitica(ConfiguracaoAdministrativa config) {
        this.config = config;
        registrar(new MultaDiaria());
        registrar(new MultaComTeto());
        registrar(new MultaComTolerancia());
    }

    public void registrar(PoliticaDeMulta politica) {
        politicas.put(politica.id(), politica);
    }

    public PoliticaDeMulta vigente() {
        return obter(config.getPoliticaVigente());
    }

    public PoliticaDeMulta obter(String id) {
        PoliticaDeMulta p = politicas.get(id);
        if (p == null) {
            throw new ExcecaoDeDominio("politica de multa desconhecida: " + id);
        }
        return p;
    }

    public List<PoliticaDeMulta> todas() {
        return new ArrayList<>(politicas.values());
    }

    /** Identificador inexistente e recusado aqui, ao salvar a configuracao, e nunca chega ao calculo. */
    public void definirVigente(String id) {
        obter(id);
        config.definirPolitica(id);
    }
}
