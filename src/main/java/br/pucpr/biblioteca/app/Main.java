package br.pucpr.biblioteca.app;

import br.pucpr.biblioteca.banco.CodigoBanco;
import br.pucpr.biblioteca.devolucao.ServicoDeEmailConsole;
import br.pucpr.biblioteca.persistencia.ConfiguradorDePersistencia;
import br.pucpr.biblioteca.persistencia.FabricaDePersistencia;
import br.pucpr.biblioteca.persistencia.Perfil;

/**
 * Ponto de entrada da interface de linha de comando.
 * Uso: java -jar target/biblioteca.jar <TESTE|PRODUCAO> [A|B] [CACHE|SEMCACHE]
 */
public class Main {
    static final String USO = "Uso: java -jar target/biblioteca.jar <TESTE|PRODUCAO> [A|B] [CACHE|SEMCACHE]";

    public static void main(String[] args) {
        Perfil perfil;
        CodigoBanco banco;
        boolean comCache;
        try {
            if (args.length > 3) {
                throw new IllegalArgumentException("argumentos demais: " + String.join(" ", args));
            }
            perfil = Perfil.de(args.length > 0 ? args[0] : null);          // sem perfil a aplicacao nao sobe
            banco = bancoDaInstalacao(args.length > 1 ? args[1] : "A");
            comCache = consultaComCache(args.length > 2 ? args[2] : "CACHE");
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
            System.out.println(USO);
            System.exit(2);
            return;
        }
        FabricaDePersistencia fabrica = new ConfiguradorDePersistencia().fabricaPara(perfil);   // familia escolhida uma unica vez
        Aplicacao app = new Aplicacao(fabrica, banco, new ServicoDeEmailConsole(), comCache);
        String titulo = "perfil " + perfil + ", banco " + banco + ", consulta " + (comCache ? "com cache" : "sem cache");
        new Menu(app.tratadores(), titulo).executar();                     // a tela recebe so os tratadores
    }

    private static CodigoBanco bancoDaInstalacao(String texto) {
        switch (texto.toUpperCase()) {
            case "A": return CodigoBanco.A;
            case "B": return CodigoBanco.B;
            default: throw new IllegalArgumentException("banco desconhecido: " + texto + " (use A ou B)");
        }
    }

    private static boolean consultaComCache(String texto) {
        switch (texto.toUpperCase()) {
            case "CACHE": return true;
            case "SEMCACHE": return false;
            default: throw new IllegalArgumentException("opcao de consulta desconhecida: " + texto + " (use CACHE ou SEMCACHE)");
        }
    }
}
