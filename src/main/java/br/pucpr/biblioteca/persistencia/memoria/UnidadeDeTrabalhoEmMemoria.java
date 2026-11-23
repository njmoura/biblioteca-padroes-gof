package br.pucpr.biblioteca.persistencia.memoria;

import br.pucpr.biblioteca.persistencia.UnidadeDeTrabalho;

/** ConcreteProduct da familia em memoria: desfazer() restaura a fotografia tirada em iniciar(). */
public class UnidadeDeTrabalhoEmMemoria implements UnidadeDeTrabalho {
    private final BaseEmMemoria base;
    private BaseEmMemoria fotografia;

    UnidadeDeTrabalhoEmMemoria(BaseEmMemoria base) {
        this.base = base;
    }

    BaseEmMemoria base() { return base; }

    @Override
    public void iniciar() {
        fotografia = base.copia();
    }

    @Override
    public void confirmar() {
        fotografia = null;
    }

    @Override
    public void desfazer() {
        if (fotografia != null) {
            base.restaurar(fotografia);
            fotografia = null;
        }
    }
}
