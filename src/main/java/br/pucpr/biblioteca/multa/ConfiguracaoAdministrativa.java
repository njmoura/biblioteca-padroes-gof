package br.pucpr.biblioteca.multa;

/** Configuracao administrativa: guarda o identificador da politica de multa vigente. */
public class ConfiguracaoAdministrativa {
    private String politicaVigente = "ORIGINAL";

    public String getPoliticaVigente() { return politicaVigente; }

    public void definirPolitica(String id) { this.politicaVigente = id; }
}
