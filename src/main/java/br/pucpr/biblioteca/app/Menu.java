package br.pucpr.biblioteca.app;

import br.pucpr.biblioteca.consulta.MetadadosLivro;
import br.pucpr.biblioteca.solicitacao.SolicitacaoDeCirculacao;
import br.pucpr.biblioteca.solicitacao.SolicitacaoDeRenovacao;
import br.pucpr.biblioteca.solicitacao.SolicitacaoDeReserva;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Interface de linha de comando. So le dados, chama os TRATADORES e mostra o resultado:
 * nao ha regra de dominio aqui, e a tela nao recebe nada alem dos tratadores.
 * Os textos nao usam acentos de proposito, para nao depender da pagina de codigo do console.
 */
public class Menu {
    private final Tratadores t;
    private final String titulo;
    private final Scanner teclado = new Scanner(System.in);
    private SolicitacaoDeCirculacao ultimaSolicitacao;      // Command: guardada para poder ser reenviada (opcao 14)

    public Menu(Tratadores tratadores, String titulo) {
        this.t = tratadores;
        this.titulo = titulo;
    }

    public void executar() {
        mostrarOpcoes();
        while (true) {
            System.out.println();
            String opcao = lerOpcional("Opcao (m = menu, 0 = sair)");
            if (opcao.equals("0")) {
                System.out.println("Ate logo.");
                return;
            }
            if (opcao.isEmpty() || opcao.equalsIgnoreCase("m")) {
                mostrarOpcoes();
                continue;
            }
            try {
                atender(opcao);
            } catch (RuntimeException erro) {
                System.out.println("ERRO TECNICO: " + erro.getMessage());
            }
        }
    }

    private void mostrarOpcoes() {
        System.out.println();
        System.out.println("=== BIBLIOTECA (" + titulo + ") ===");
        System.out.println("Administrador");
        linha(" 1 Incluir pessoa", " 2 Incluir estudante", " 3 Incluir atendente");
        linha(" 4 Incluir livro", " 5 Consultar livro", " 6 Enviar livro para reparo");
        linha(" 7 Excluir livro", " 8 Definir politica de multa", " 9 Simular politicas de multa");
        System.out.println("Estudante");
        linha("10 Reservar livro (f1)", "11 Consultar reserva (f2)", "12 Efetivar emprestimo (f3)");
        linha("13 Renovar emprestimo (f4)", "14 Reenviar ultima solicitacao", "15 Pesquisar no acervo");
        System.out.println("Atendente");
        linha("16 Registrar devolucao (f5)", "17 Boleto de multa, 2a via (f6)", "18 Inspecionar livro devolvido");
        linha("19 Registrar retorno do reparo", "20 Reacoes a devolucao", "");
        System.out.println("Financeiro e banco");
        linha("21 Listar multas", "22 Simular pagamento no banco", "");
        System.out.println("Organizacao do acervo");
        linha("23 Criar grupo", "24 Alocar livro em grupo", "25 Mover livro de grupo");
        linha("26 Contar e listar livros", "", "");
        System.out.println("Geral");
        linha("90 Carregar dados de exemplo", " m Mostrar este menu", " 0 Sair");
        System.out.println("(f1 a f6 = funcoes do Trabalho 02; datas no formato AAAA-MM-DD)");
    }

    private static void linha(String a, String b, String c) {
        System.out.println(String.format("  %-32s%-34s%s", a, b, c).stripTrailing());
    }

    private void atender(String opcao) {
        switch (opcao) {
            case "1":
                System.out.println(t.pessoa().inserirPessoa(ler("CPF (11 digitos)"), ler("Nome"), ler("Sobrenome"), ler("E-mail")));
                break;
            case "2":
                System.out.println(t.estudante().inserirEstudante(ler("Id do estudante"), ler("CPF da pessoa")));
                break;
            case "3":
                System.out.println(t.atendente().inserirAtendente(ler("Id do atendente"), ler("CPF da pessoa")));
                break;
            case "4":
                System.out.println(t.acervo().inserirLivro(ler("Id do livro"), ler("Titulo")));
                break;
            case "5":
                System.out.println(t.emprestimo().consultarSituacao(ler("Id do livro")));
                break;
            case "6":
                System.out.println(t.acervo().enviarParaReparo(ler("Id do livro")));
                break;
            case "7":
                System.out.println(t.acervo().excluirLivro(ler("Id do livro")));
                break;
            case "8":
                System.out.println("politica vigente: " + t.multa().politicaVigente());
                System.out.println(t.multa().definirPolitica(ler("Nova politica (" + String.join(", ", t.multa().politicasDisponiveis()) + ")").toUpperCase()));
                break;
            case "9":
                mostrarSimulacao(lerInteiroNaoNegativo("Dias de atraso"));
                break;
            case "10": {
                String idLivro = ler("Id do livro");
                String idEstudante = ler("Id do estudante");
                ultimaSolicitacao = new SolicitacaoDeReserva(UUID.randomUUID(), "estudante " + idEstudante,
                        idLivro, idEstudante, lerData("Data da reserva"));
                System.out.println(t.emprestimo().submeter(ultimaSolicitacao));
                mostrarSituacao(idLivro);
                break;
            }
            case "11":
                System.out.println(t.emprestimo().consultarReserva(ler("Id do livro"), ler("Id do estudante")));
                break;
            case "12": {
                String idLivro = ler("Id do livro");
                System.out.println(t.emprestimo().emprestarLivro(idLivro, ler("Id do estudante"), lerData("Data do emprestimo")));
                mostrarSituacao(idLivro);
                break;
            }
            case "13": {
                String idLivro = ler("Id do livro");
                String idEstudante = ler("Id do estudante");
                ultimaSolicitacao = new SolicitacaoDeRenovacao(UUID.randomUUID(), "estudante " + idEstudante, idLivro, idEstudante);
                System.out.println(t.emprestimo().submeter(ultimaSolicitacao));
                mostrarSituacao(idLivro);
                break;
            }
            case "14":
                if (ultimaSolicitacao == null) {
                    System.out.println("nenhuma solicitacao enviada ainda (use 10 ou 13 primeiro)");
                } else {
                    System.out.println("reenviando a MESMA solicitacao, com o mesmo id " + ultimaSolicitacao.getId());
                    System.out.println(t.emprestimo().submeter(ultimaSolicitacao));
                }
                break;
            case "15": {
                List<MetadadosLivro> achados = t.consulta().consultar(lerOpcional("Parte do titulo (vazio = todos)"));
                for (MetadadosLivro m : achados) {
                    System.out.println("  " + m.id() + " - " + m.titulo());
                }
                if (achados.isEmpty()) {
                    System.out.println("  nenhum livro encontrado");
                }
                System.out.println(t.consulta().resumoDaUltimaConsulta());
                break;
            }
            case "16": {
                String idLivro = ler("Id do livro");
                System.out.println(t.devolucao().registrarDevolucao(idLivro, ler("Id do estudante"), lerData("Data da devolucao")));
                mostrarSituacao(idLivro);
                break;
            }
            case "17":
                System.out.println(t.devolucao().emitirBoletoMulta(ler("Id do livro"), ler("Id do estudante")));
                break;
            case "18":
                System.out.println(t.devolucao().inspecionarLivro(ler("Id do livro"), lerSimNao("Livro em bom estado")));
                break;
            case "19":
                System.out.println(t.devolucao().registrarRetornoDoReparo(ler("Id do livro"), lerSimNao("Livro foi reparado")));
                break;
            case "20":
                System.out.println(t.devolucao().resumoDasReacoes());
                break;
            case "21": {
                List<String> multas = t.multa().listarMultas();
                for (String linha : multas) {
                    System.out.println("  " + linha);
                }
                if (multas.isEmpty()) {
                    System.out.println("  nenhuma multa emitida");
                }
                System.out.println("politica vigente: " + t.multa().politicaVigente());
                break;
            }
            case "22":
                System.out.println(t.multa().simularPagamentoNoBanco(ler("Numero do boleto (aparece na opcao 21)")));
                break;
            case "23":
                System.out.println(t.organizacao().criarGrupo(
                        ler("Grupo pai (raiz = " + t.organizacao().raiz() + ")"), ler("Nome do novo grupo"),
                        ler("Tipo (UNIDADE, SECAO ou SUBSECAO)")));
                break;
            case "24":
                System.out.println(t.organizacao().alocarLivro(ler("Id do livro"), ler("Nome do grupo")));
                break;
            case "25":
                System.out.println(t.organizacao().moverLivro(ler("Id do livro"), ler("Grupo de destino")));
                break;
            case "26":
                System.out.println(t.organizacao().contarEListar(ler("Nome do grupo")));
                break;
            case "90":
                carregarExemplo();
                break;
            default:
                System.out.println("opcao invalida (digite m para ver o menu)");
        }
    }

    /** Depois de cada operacao de circulacao, mostra em que situacao o livro ficou. */
    private void mostrarSituacao(String idLivro) {
        System.out.println("  agora: " + t.emprestimo().consultarSituacao(idLivro).mensagem());
    }

    private void mostrarSimulacao(int dias) {
        StringJoiner texto = new StringJoiner(" | ", "simulacao para " + dias + (dias == 1 ? " dia" : " dias") + " de atraso: ", "");
        for (Map.Entry<String, BigDecimal> valor : t.multa().simular(dias).entrySet()) {
            texto.add(valor.getKey() + " R$ " + valor.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ','));
        }
        System.out.println(texto + "  (nenhuma multa foi criada)");
    }

    private void carregarExemplo() {
        System.out.println(t.pessoa().inserirPessoa("11111111111", "Ana", "Silva", "ana@exemplo.br"));
        System.out.println(t.pessoa().inserirPessoa("22222222222", "Bruno", "Souza", "bruno@exemplo.br"));
        System.out.println(t.estudante().inserirEstudante("E1", "11111111111"));
        System.out.println(t.atendente().inserirAtendente("A1", "22222222222"));
        System.out.println(t.acervo().inserirLivro("L1", "Dom Casmurro"));
        System.out.println(t.acervo().inserirLivro("L2", "Memorias Postumas de Bras Cubas"));
        System.out.println(t.acervo().inserirLivro("L3", "O Cortico"));
        String raiz = t.organizacao().raiz();
        System.out.println(t.organizacao().criarGrupo(raiz, "Literatura", "SECAO"));
        System.out.println(t.organizacao().criarGrupo(raiz, "Reserva Tecnica", "SECAO"));
        System.out.println(t.organizacao().alocarLivro("L1", "Literatura"));
        System.out.println(t.organizacao().alocarLivro("L2", "Literatura"));
        System.out.println(t.organizacao().alocarLivro("L3", "Reserva Tecnica"));
    }

    // ---- leitura do teclado ----

    /** Le uma linha; pode vir vazia. */
    private String lerOpcional(String rotulo) {
        System.out.print(rotulo + ": ");
        if (!teclado.hasNextLine()) {          // entrada encerrada: sai
            System.out.println();
            System.exit(0);
        }
        // entradas redirecionadas podem comecar com a marca de ordem de bytes (lida como U+FEFF ou como tres letras): ignora
        return teclado.nextLine().replace("﻿", "").replace("ï»¿", "").trim();
    }

    /** Le uma linha e insiste ate vir preenchida. */
    private String ler(String rotulo) {
        while (true) {
            String texto = lerOpcional(rotulo);
            if (!texto.isEmpty()) {
                return texto;
            }
            System.out.println("informe um valor");
        }
    }

    private int lerInteiroNaoNegativo(String rotulo) {
        while (true) {
            try {
                int numero = Integer.parseInt(ler(rotulo));
                if (numero >= 0) {
                    return numero;
                }
            } catch (NumberFormatException e) {
                // cai na mensagem abaixo
            }
            System.out.println("digite um numero inteiro maior ou igual a zero");
        }
    }

    private LocalDate lerData(String rotulo) {
        while (true) {
            String texto = lerOpcional(rotulo + " (AAAA-MM-DD, vazio = hoje)");
            if (texto.isEmpty()) {
                LocalDate hoje = LocalDate.now();
                System.out.println("  usando a data de hoje: " + hoje);
                return hoje;
            }
            try {
                LocalDate data = LocalDate.parse(texto);
                if (data.getYear() >= 1900 && data.getYear() <= 9999) {     // so formato: ano de quatro digitos
                    return data;
                }
            } catch (DateTimeParseException e) {
                // cai na mensagem abaixo
            }
            System.out.println("data invalida: use AAAA-MM-DD, por exemplo 2026-11-01");
        }
    }

    /** So aceita s/sim ou n/nao: uma tecla errada nao pode mandar um livro para o reparo ou para a incineracao. */
    private boolean lerSimNao(String rotulo) {
        while (true) {
            String texto = ler(rotulo + "? (s/n)").toLowerCase();
            if (texto.equals("s") || texto.equals("sim")) {
                return true;
            }
            if (texto.equals("n") || texto.equals("nao")) {
                return false;
            }
            System.out.println("responda s ou n");
        }
    }
}
