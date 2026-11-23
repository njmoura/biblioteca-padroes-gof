import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.editor.BasicModelEditor;
import com.change_vision.jude.api.inf.editor.ClassDiagramEditor;
import com.change_vision.jude.api.inf.editor.SequenceDiagramEditor;
import com.change_vision.jude.api.inf.editor.StateMachineDiagramEditor;
import com.change_vision.jude.api.inf.editor.TransactionManager;
import com.change_vision.jude.api.inf.model.IAssociation;
import com.change_vision.jude.api.inf.model.IAttribute;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.IEnumeration;
import com.change_vision.jude.api.inf.model.ILifeline;
import com.change_vision.jude.api.inf.model.IMessage;
import com.change_vision.jude.api.inf.model.IModel;
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.model.IOperation;
import com.change_vision.jude.api.inf.model.IPackage;
import com.change_vision.jude.api.inf.model.IState;
import com.change_vision.jude.api.inf.model.ITransition;
import com.change_vision.jude.api.inf.presentation.ILinkPresentation;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import java.awt.geom.Point2D;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Gera o modelo Astah do Trabalho 03 A PARTIR DO CODIGO COMPILADO (reflexao), para que
 * classes, atributos, operacoes, heranca, realizacao e associacoes do UML sejam exatamente
 * os do codigo. Os diagramas (quais classes aparecem em cada um, a maquina de estados e os
 * diagramas de sequencia dos cenarios) sao descritos neste arquivo.
 *
 * Uso: java -cp target/classes;astah-api.jar;astah-uml.jar GerarModelos.java target/classes src/main/java saida.asta
 */
public class GerarModelos {
    static final String BASE = "br.pucpr.biblioteca.";
    static ProjectAccessor pa;
    static BasicModelEditor me;
    static IModel raiz;
    static IPackage pacoteDiagramas;
    static IPackage pacoteAtores;
    static final Map<String, Class<?>> classesJava = new LinkedHashMap<>();
    static final Map<String, IClass> classesUml = new LinkedHashMap<>();
    static final Map<String, IPackage> pacotes = new LinkedHashMap<>();
    static final List<Object[]> relacoes = new ArrayList<>();      // {elemento, nomeOrigem, nomeDestino}
    static Path fontes;

    public static void main(String[] a) throws Exception {
        Path classes = Paths.get(a[0]);
        fontes = Paths.get(a[1]);
        carregarClasses(classes);

        pa = AstahAPI.getAstahAPI().getProjectAccessor();
        pa.create(a[2]);
        raiz = pa.getProject();
        me = pa.getModelEditorFactory().getBasicModelEditor();

        emTransacao("modelo", GerarModelos::criarModelo);
        diagramasDeClasses();
        emTransacao("estados", GerarModelos::maquinaDeEstados);
        diagramasDeSequencia();

        pa.save();
        pa.close();
        System.out.println("Modelo gravado em " + a[2]);
        System.exit(0);
    }

    interface Passo { void executar() throws Exception; }

    static void emTransacao(String nome, Passo passo) {
        try {
            TransactionManager.beginTransaction();
            passo.executar();
            TransactionManager.endTransaction();
        } catch (Throwable e) {
            TransactionManager.abortTransaction();
            System.out.println("FALHOU [" + nome + "]: " + e);
            e.printStackTrace(System.out);
        }
    }

    // ------------------------------------------------------------------ modelo a partir do codigo

    static void carregarClasses(Path dir) throws Exception {
        List<String> nomes = new ArrayList<>();
        try (Stream<Path> s = Files.walk(dir)) {
            s.filter(p -> p.toString().endsWith(".class") && !p.getFileName().toString().contains("$"))
             .forEach(p -> nomes.add(dir.relativize(p).toString().replace('\\', '.').replace('/', '.').replaceAll("\\.class$", "")));
        }
        nomes.sort(null);
        for (String n : nomes) {
            Class<?> c = Class.forName(n, false, GerarModelos.class.getClassLoader());
            classesJava.put(c.getSimpleName(), c);
        }
    }

    static void criarModelo() throws Exception {
        pacoteDiagramas = me.createPackage(raiz, "Diagramas");
        pacoteAtores = me.createPackage(raiz, "atores");
        IPackage codigo = me.createPackage(raiz, "biblioteca");
        // 1) classes, interfaces e enumeracoes
        for (Class<?> c : classesJava.values()) {
            String nomePacote = c.getPackageName().substring(BASE.length() - 1).replaceFirst("^\\.", "");
            IPackage p = codigo;
            for (String parte : nomePacote.split("\\.")) {
                if (parte.isEmpty()) continue;
                String chave = p.getName() + "/" + parte;
                IPackage sub = pacotes.get(chave);
                if (sub == null) {
                    sub = me.createPackage(p, parte);
                    pacotes.put(chave, sub);
                }
                p = sub;
            }
            IClass u;
            if (c.isEnum()) {
                IEnumeration e = me.createEnumeration(p, c.getSimpleName());
                for (Object literal : c.getEnumConstants()) me.createEnumerationLiteral(e, literal.toString());
                u = e;
            } else if (c.isInterface()) {
                u = me.createInterface(p, c.getSimpleName());
            } else {
                u = me.createClass(p, c.getSimpleName());
                if (Modifier.isAbstract(c.getModifiers())) u.setAbstract(true);
                if (c.isRecord()) u.addStereotype("value");
            }
            classesUml.put(c.getSimpleName(), u);
        }
        // 2) atributos, associacoes e operacoes
        for (Class<?> c : classesJava.values()) {
            IClass u = classesUml.get(c.getSimpleName());
            if (c.isEnum()) continue;
            String fonte = lerFonte(c);
            for (Field f : c.getDeclaredFields()) {
                if (f.isSynthetic()) continue;
                Class<?> alvo = classeDoProjeto(f.getGenericType());
                boolean muitos = !f.getType().getName().startsWith(BASE);
                if (alvo != null && !alvo.isEnum() && alvo != c) {
                    IAssociation as = me.createAssociation(u, classesUml.get(alvo.getSimpleName()), "", "", "");
                    for (IAttribute ponta : as.getMemberEnds()) {
                        if (ponta.getType() == classesUml.get(alvo.getSimpleName())) {
                            ponta.setName(f.getName());
                            ponta.setVisibility(visibilidade(f.getModifiers()));
                            tentar(() -> ponta.setNavigability("Navigable"));
                            ponta.setMultiplicity(muitos ? new int[][] {{0, -1}} : new int[][] {{1}});
                        }
                    }
                    relacoes.add(new Object[] {as, c.getSimpleName(), alvo.getSimpleName()});
                } else {
                    IAttribute at = criarAtributo(u, f.getName(), tipo(f.getGenericType()));
                    at.setVisibility(visibilidade(f.getModifiers()));
                    if (Modifier.isStatic(f.getModifiers())) at.setStatic(true);
                }
            }
            List<Method> metodos = new ArrayList<>(Arrays.asList(c.getDeclaredMethods()));
            metodos.removeIf(m -> ignorar(c, m));
            metodos.sort((m1, m2) -> Integer.compare(posicao(fonte, m1), posicao(fonte, m2)));
            for (Method m : metodos) {
                IOperation op = criarOperacao(u, m.getName(), tipo(m.getGenericReturnType()));
                op.setVisibility(visibilidade(m.getModifiers()));
                if (Modifier.isAbstract(m.getModifiers()) && !c.isInterface()) op.setAbstract(true);
                if (Modifier.isStatic(m.getModifiers())) op.setStatic(true);
                if (Modifier.isFinal(m.getModifiers())) op.setLeaf(true);
                for (Parameter p : m.getParameters()) {
                    criarParametro(op, p.getName(), tipo(p.getParameterizedType()));
                }
            }
        }
        // 3) heranca e realizacao
        for (Class<?> c : classesJava.values()) {
            IClass u = classesUml.get(c.getSimpleName());
            Class<?> sup = c.getSuperclass();
            if (sup != null && classesUml.containsKey(sup.getSimpleName()) && sup.getName().startsWith(BASE)) {
                relacoes.add(new Object[] {me.createGeneralization(u, classesUml.get(sup.getSimpleName()), ""), c.getSimpleName(), sup.getSimpleName()});
            }
            for (Class<?> i : c.getInterfaces()) {
                if (!i.getName().startsWith(BASE)) continue;
                IClass ui = classesUml.get(i.getSimpleName());
                if (c.isInterface()) {
                    relacoes.add(new Object[] {me.createGeneralization(u, ui, ""), c.getSimpleName(), i.getSimpleName()});
                } else {
                    relacoes.add(new Object[] {me.createRealization(u, ui, ""), c.getSimpleName(), i.getSimpleName()});
                }
            }
        }
        // atores dos diagramas de sequencia
        for (String ator : new String[] {"Estudante", "Atendente", "Administrador", "Banco A", "Inicializacao", "Usuario"}) {
            IClass ac = me.createClass(pacoteAtores, ator);
            ac.addStereotype("actor");
            classesUml.put("@" + ator, ac);
        }
    }

    static void tentar(Passo p) {
        try { p.executar(); } catch (Exception ignorada) { /* propriedade opcional */ }
    }

    static IAttribute criarAtributo(IClass u, String nome, String tipo) throws Exception {
        IClass t = classesUml.get(tipo);
        if (t != null) return me.createAttribute(u, nome, t);
        try {
            return me.createAttribute(u, nome, tipo);
        } catch (Exception e) {
            IAttribute at = me.createAttribute(u, nome, "Object");
            tentar(() -> at.setTypeExpression(tipo));
            return at;
        }
    }

    static IOperation criarOperacao(IClass u, String nome, String retorno) throws Exception {
        IClass t = classesUml.get(retorno);
        if (t != null) return me.createOperation(u, nome, t);
        try {
            return me.createOperation(u, nome, retorno);
        } catch (Exception e) {
            IOperation op = me.createOperation(u, nome, "void");
            tentar(() -> op.setReturnTypeExpression(retorno));
            return op;
        }
    }

    static void criarParametro(IOperation op, String nome, String tipo) throws Exception {
        IClass t = classesUml.get(tipo);
        if (t != null) { me.createParameter(op, nome, t); return; }
        try {
            me.createParameter(op, nome, tipo);
        } catch (Exception e) {
            me.createParameter(op, nome, "Object");
        }
    }

    static boolean ignorar(Class<?> c, Method m) {
        String n = m.getName();
        if (m.isSynthetic() || m.isBridge() || n.contains("$")) return true;
        if (n.equals("toString") || n.equals("equals") || n.equals("hashCode")) return true;
        if (m.getParameterCount() == 0) {
            for (Field f : c.getDeclaredFields()) {
                String campo = f.getName();
                if (c.isRecord() && n.equals(campo)) return true;                                   // acessor de record
                if (n.equals("get" + Character.toUpperCase(campo.charAt(0)) + campo.substring(1))) return true;   // getter simples
            }
        }
        return false;
    }

    static String lerFonte(Class<?> c) {
        try {
            return Files.readString(fontes.resolve(c.getName().replace('.', '/') + ".java"));
        } catch (Exception e) {
            return "";
        }
    }

    static int posicao(String fonte, Method m) {
        java.util.regex.Matcher mt = java.util.regex.Pattern
                .compile("(?m)^\\s+(?:[\\w<>\\[\\],.? ]+\\s)?" + m.getName() + "\\(").matcher(fonte);   // linha da declaracao
        return mt.find() ? mt.start() : Integer.MAX_VALUE;
    }

    static String visibilidade(int mod) {
        if (Modifier.isPublic(mod)) return "public";
        if (Modifier.isProtected(mod)) return "protected";
        if (Modifier.isPrivate(mod)) return "private";
        return "package";
    }

    static String tipo(Type t) {
        return t.getTypeName().replace('$', '.').replaceAll("\\b(?:[a-z_][a-zA-Z0-9_]*\\.)+", "");
    }

    /** Classe do projeto referenciada por um campo: o proprio tipo, ou o elemento de List/Map. */
    static Class<?> classeDoProjeto(Type t) {
        if (t instanceof Class<?> c) {
            return c.getName().startsWith(BASE) && !c.getName().contains("$") && classesJava.containsKey(c.getSimpleName()) ? c : null;
        }
        if (t instanceof ParameterizedType p) {
            Type[] args = p.getActualTypeArguments();
            return classeDoProjeto(args[args.length - 1]);
        }
        return null;
    }

    // ------------------------------------------------------------------ diagramas de classes

    static void diagramasDeClasses() {
        classes("CD 00 - Domínio e pontos internos de acesso",
                "Base do Trabalho 02 preservada: Pessoa e papéis, transações e os pontos internos de acesso\n"
                + "AcervoDeLivros (iAcervo), Movimentacao e Relacionamento, que agora guardam os dados pelos\n"
                + "repositórios da família de persistência escolhida (requisito 8).",
                "Papel Transacao", "Estudante Atendente Reserva Emprestimo Devolucao Pessoa",
                "Relacionamento Movimentacao AcervoDeLivros iAcervo");
        classes("CD 01 - R01 State - ciclo de vida do Livro",
                "STATE\nContext: Livro (guarda estado, vencimento, numRenovacoes, reservante e delega cada evento)\n"
                + "State: EstadoLivro (a implementação padrão recusa com OperacaoInvalidaException)\n"
                + "ConcreteState: Livre, Reservado, Emprestado, Renovado, Devolvido, Reparando, Incinerado\n"
                + "Guarda da renovação [numRenovacoes < 2]: em Emprestado.renovar e em Renovado.renovar, antes de prorrogar.\n"
                + "Persistência: grava Situacao; EstadoLivro.de(Situacao) reconstitui o objeto de estado.",
                "Livro Situacao OperacaoInvalidaException", "EstadoLivro",
                "Livre Reservado Emprestado Renovado Devolvido Reparando Incinerado");
        classes("CD 02 - R02 Strategy - políticas de multa",
                "STRATEGY\nStrategy: PoliticaDeMulta\nConcreteStrategy: MultaDiaria (ORIGINAL), MultaComTeto (TETO), MultaComTolerancia (TOLERANCIA)\n"
                + "Context: ServicoDeMulta (calcula pela política vigente)\n"
                + "Seleção: SeletorDePolitica + ConfiguracaoAdministrativa\n"
                + "A Multa grava valor e politicaAplicada na emissão: trocar a política não recalcula multas emitidas.",
                "ConfiguracaoAdministrativa SeletorDePolitica ServicoDeMulta Multa", "PoliticaDeMulta iMulta",
                "MultaDiaria MultaComTeto MultaComTolerancia");
        classes("CD 03 - R03 Adapter - bancos incompatíveis",
                "ADAPTER (de objeto)\nTarget: GatewayDeCobranca\n"
                + "Adapter: AdaptadorBancoA, AdaptadorBancoB (todas as conversões de cada banco)\n"
                + "Adaptee: ApiBancoA (centavos, JSON), ApiBancoB (decimal, documento formatado, XML)\n"
                + "Client: ServicoDeMulta (sem condicionais por banco)\n"
                + "Notificações (caminho inverso): o banco chama receberWebhook / receberNotificacao do adaptador,\n"
                + "que traduz e entrega a ReceptorDePagamento (ServicoDeQuitacao).\n"
                + "Quitação única: ServicoDeQuitacao + RegistroDeCobrancas.quitarSeAberta.",
                "ServicoDeMulta GatewayDeCobranca Cobranca SolicitacaoCobranca",
                "AdaptadorBancoA ReceptorDePagamento AdaptadorBancoB",
                "ApiBancoA ServicoDeQuitacao ApiBancoB",
                "RegistroDeCobrancas ConfirmacaoPagamento");
        classes("CD 04 - R04 Facade - circulação",
                "FACADE\nFacade: iCirculacao / ServicoDeCirculacao (operações completas e transacionais; devolve comprovantes)\n"
                + "Subsistema: Relacionamento, Movimentacao, AcervoDeLivros (iAcervo), Livro, UnidadeDeTrabalho\n"
                + "Clientes: TratadorDeEmprestimo (f1 a f4) e TratadorDeDevolucao (f5, f6); a interface só chama os tratadores.\n"
                + "f1 (reservar) e f4 (renovar) chegam à fachada dentro de uma solicitação (requisito 6); cada operação de escrita\n"
                + "roda em emTransacao: iniciar(), os passos, confirmar() ou, se algum passo falhar, desfazer().",
                "TratadorDeEmprestimo TratadorDeDevolucao", "ComprovanteEmprestimo iCirculacao", "ServicoDeCirculacao",
                "UnidadeDeTrabalho Relacionamento Movimentacao iAcervo");
        classes("CD 05 - R05 Observer - reações à devolução",
                "OBSERVER (modelo push)\nSubject: PublicadorDeDevolucao (isola a falha de cada ouvinte)\n"
                + "Observer: OuvinteDeDevolucao\nConcreteObserver: OuvinteInspecao, OuvinteMulta, OuvinteIndicadores, OuvinteEmail\n"
                + "Evento: EventoDevolucao (leva vencimento, data de devolução e estudante)\n"
                + "Quem dispara: TratadorDeDevolucao, depois da devolução confirmada.",
                "TratadorDeDevolucao PublicadorDeDevolucao EventoDevolucao", "OuvinteDeDevolucao",
                "OuvinteInspecao OuvinteMulta OuvinteIndicadores OuvinteEmail",
                "AgendaDeInspecao iMulta PainelDeIndicadores ServicoDeEmail");
        classes("CD 06 - R06 Command - solicitações de circulação",
                "COMMAND\nCommand: SolicitacaoDeCirculacao (id, autor, criadaEm, executar)\n"
                + "ConcreteCommand: SolicitacaoDeReserva, SolicitacaoDeRenovacao\n"
                + "Invoker: ExecutorDeSolicitacoes + RegistroDeSolicitacoes (o mesmo id não executa duas vezes)\n"
                + "Receiver: iCirculacao (as regras ficam no domínio)\n"
                + "Client: a tela cria a solicitação e a encaminha por TratadorDeEmprestimo.submeter; reservarLivro e renovarEmprestimo\n"
                + "(f1 e f4) também criam a solicitação e a submetem ao executor. Não há desfazer().",
                "TratadorDeEmprestimo ExecutorDeSolicitacoes RegistroDeSolicitacoes",
                "SolicitacaoDeCirculacao iCirculacao ResultadoSolicitacao",
                "SolicitacaoDeReserva SolicitacaoDeRenovacao");
        classes("CD 07 - R07 Template Method - cadastros",
                "TEMPLATE METHOD\nAbstractClass: TratadorDeCadastro<T, D>; incluir() é o método-modelo {leaf}\n"
                + "Primitivas: buscarExistente, validar, criar, armazenar. Gancho: verificarPreRequisitos\n"
                + "ConcreteClass: TratadorDeAcervo, TratadorDePessoa, TratadorDeEstudante, TratadorDeAtendente\n"
                + "(realizam iLivro, iPessoa, iEstudante, iAtendente). Estudante e Atendente exigem a Pessoa cadastrada.",
                "iLivro iPessoa", "TratadorDeAcervo TratadorDePessoa",
                "TratadorDeCadastro ResultadoCadastro StatusCadastro",
                "TratadorDeEstudante TratadorDeAtendente", "iEstudante iAtendente");
        classes("CD 08 - R08 Abstract Factory - famílias de persistência",
                "ABSTRACT FACTORY\nAbstractFactory: FabricaDePersistencia. ConcreteFactory: FabricaMySQL (PRODUCAO), FabricaEmMemoria (TESTE)\n"
                + "AbstractProduct: UnidadeDeTrabalho, RepositorioLivro, RepositorioMovimentacao, RepositorioRelacionamento\n"
                + "ConcreteProduct: ...MySQL e ...EmMemoria. A família é escolhida uma vez em ConfiguradorDePersistencia.fabricaPara(perfil).\n"
                + "O Oracle é réplica do MySQL: nenhuma fábrica cria repositório que escreva nele.",
                "ConfiguradorDePersistencia Perfil FabricaDePersistencia", "FabricaMySQL FabricaEmMemoria BaseEmMemoria",
                "UnidadeDeTrabalhoEmMemoria RepositorioLivroEmMemoria RepositorioMovimentacaoEmMemoria RepositorioRelacionamentoEmMemoria",
                "UnidadeDeTrabalho RepositorioLivro RepositorioMovimentacao RepositorioRelacionamento",
                "UnidadeDeTrabalhoMySQL RepositorioLivroMySQL RepositorioMovimentacaoMySQL RepositorioRelacionamentoMySQL");
        classes("CD 09 - R09 Decorator - consulta ao acervo",
                "DECORATOR\nComponent: ConsultaAcervo. ConcreteComponent: ConsultaAcervoBasica (original, inalterada)\n"
                + "Decorator: DecoradorDeConsulta. ConcreteDecorator: DecoradorDeMedicao, DecoradorDeCache\n"
                + "Montagem: MontadorDeConsulta -> Medicao(Cache(Basica)) ou Medicao(Basica); a sobrecarga com comMedicao também monta\n"
                + "Basica e Cache(Basica). O DecoradorDeCache guarda em ArmazenamentoDeCache (ArmazenamentoEmMemoria, validade de 5 min)\n"
                + "só MetadadosLivro (id, titulo); a reserva consulta o Livro no AcervoDeLivros.",
                "TratadorDeConsulta ConsultaAcervo MetadadosLivro MontadorDeConsulta",
                "ConsultaAcervoBasica DecoradorDeConsulta", "DecoradorDeMedicao DecoradorDeCache RegistroDeMetricas",
                "ArmazenamentoEmMemoria ArmazenamentoDeCache");
        classes("CD 10 - R10 Composite - hierarquia do acervo",
                "COMPOSITE\nComponent: ComponenteDoAcervo (contarLivros, listarIds, pai 0..1)\n"
                + "Leaf: ItemDoAcervo (referencia o Livro pelo id)\nComposite: GrupoDoAcervo (UNIDADE, SECAO, SUBSECAO; recusa ciclo, componente com pai,\n"
                + "livro que já está na árvore e grupo de nível acima dentro de nível abaixo)\n"
                + "Client: OrganizadorDoAcervo (mover = remover + adicionar; não toca em Livro nem em Movimentacao).",
                "OrganizadorDoAcervo ComponenteDoAcervo TipoDeGrupo", "ItemDoAcervo GrupoDoAcervo HierarquiaInvalidaException");
    }

    static void classes(String nome, String nota, String... linhas) {
        emTransacao(nome, () -> {
            ClassDiagramEditor de = pa.getDiagramEditorFactory().getClassDiagramEditor();
            de.createClassDiagram(pacoteDiagramas, nome);
            Map<String, INodePresentation> nos = new LinkedHashMap<>();
            Map<String, Integer> linhaDe = new LinkedHashMap<>();
            int numero = 0;
            double y = 40;
            double larguraMaxima = 0;
            for (String linha : linhas) {
                double x = 40;
                double altura = 0;
                final int numeroDaLinha = numero++;
                for (String n : linha.trim().split("\\s+")) {
                    IClass u = classesUml.get(n);
                    if (u == null) { System.out.println("  classe nao encontrada: " + n); continue; }
                    INodePresentation p = de.createNodePresentation(u, new Point2D.Double(x, y));
                    nos.put(n, p);
                    linhaDe.put(n, numeroDaLinha);
                    if (classesJava.get(n) != null && classesJava.get(n).isInterface()) {
                        // interface como caixa (nao como icone); o Astah so recalcula o tamanho ao desenhar, entao estimamos
                        tentar(() -> p.setProperty("notation_type", "normal"));
                        int maior = n.length() + 4;
                        for (IOperation op : u.getOperations()) {
                            int t = op.getName().length() + 8 + String.valueOf(op.getReturnTypeExpression()).length();
                            for (com.change_vision.jude.api.inf.model.IParameter pr : op.getParameters()) {
                                t += pr.getName().length() + 5 + String.valueOf(pr.getTypeExpression()).length();
                            }
                            maior = Math.max(maior, t);
                        }
                        double w = maior * 6.2 + 36;
                        double h = 54 + 16.5 * u.getOperations().length;
                        Point2D lugar = new Point2D.Double(x, y);
                        tentar(() -> { p.setWidth(w); p.setHeight(h); p.setLocation(lugar); });
                        x += w + 70;
                        altura = Math.max(altura, h);
                    } else {
                        x += p.getWidth() + 70;
                        altura = Math.max(altura, p.getHeight());
                    }
                }
                larguraMaxima = Math.max(larguraMaxima, x);
                y += altura + 110;
            }
            Map<String, Integer> paresDesenhados = new LinkedHashMap<>();
            Map<String, Integer> desviosPorDestino = new LinkedHashMap<>();
            for (Object[] r : relacoes) {
                INodePresentation o = nos.get((String) r[1]);
                INodePresentation d = nos.get((String) r[2]);
                if (o == null || d == null) continue;
                // associacoes entre classes muito distantes no desenho so poluem: ficam no modelo, fora deste diagrama
                if (r[0] instanceof IAssociation && Math.abs(linhaDe.get((String) r[1]) - linhaDe.get((String) r[2])) > 1) continue;
                ILinkPresentation ligacao = null;
                try {
                    ligacao = de.createLinkPresentation((com.change_vision.jude.api.inf.model.IElement) r[0], o, d);
                } catch (Exception e1) {
                    try {
                        ligacao = de.createLinkPresentation((com.change_vision.jude.api.inf.model.IElement) r[0], d, o);
                    } catch (Exception e2) {
                        System.out.println("  ligacao nao desenhada em " + nome + ": " + r[1] + " -> " + r[2] + " (" + e2.getMessage() + ")");
                    }
                }
                if (ligacao == null || !(r[0] instanceof IAssociation)) continue;
                // associacao que passaria por cima de outra caixa (mesma fileira, com caixa no meio) ou por cima de outra
                // associacao do mesmo par: a linha e desviada pelo corredor acima da fileira, para nada ficar sobreposto
                final ILinkPresentation l = ligacao;
                INodePresentation a = l.getSource(), b = l.getTarget();
                double ax = a.getLocation().getX() + a.getWidth() / 2, ay = a.getLocation().getY();
                double bEsq = b.getLocation().getX(), by = b.getLocation().getY();
                String par = r[1] + ">" + r[2];
                int repetida = paresDesenhados.merge(par, 1, Integer::sum) - 1;
                boolean mesmaFileira = linhaDe.get((String) r[1]).equals(linhaDe.get((String) r[2]));
                boolean caixaNoMeio = false;
                if (mesmaFileira) {
                    double bx = bEsq + b.getWidth() / 2;
                    for (Map.Entry<String, INodePresentation> outro : nos.entrySet()) {
                        if (outro.getKey().equals(r[1]) || outro.getKey().equals(r[2])) continue;
                        if (!linhaDe.get(outro.getKey()).equals(linhaDe.get((String) r[1]))) continue;
                        double cx = outro.getValue().getLocation().getX() + outro.getValue().getWidth() / 2;
                        if (cx > Math.min(ax, bx) && cx < Math.max(ax, bx)) caixaNoMeio = true;
                    }
                }
                if (caixaNoMeio) {
                    int k = desviosPorDestino.merge((String) r[2], 1, Integer::sum) - 1;
                    double corredor = Math.min(ay, by) - 28 - 22 * k;
                    double chegada = bEsq + b.getWidth() * (k == 0 ? 0.72 : 0.42);
                    Point2D[] pts = {new Point2D.Double(ax, ay), new Point2D.Double(ax, corredor),
                            new Point2D.Double(chegada, corredor), new Point2D.Double(chegada, by)};
                    tentar(() -> l.setAllPoints(pts));
                } else if (repetida > 0 && by + b.getHeight() < ay) {
                    double corredor = ay - 78;
                    double chegada = bEsq + b.getWidth() / 2;
                    Point2D[] pts = {new Point2D.Double(ax - 45, ay), new Point2D.Double(ax - 45, corredor),
                            new Point2D.Double(chegada, corredor), new Point2D.Double(chegada, by + b.getHeight())};
                    tentar(() -> l.setAllPoints(pts));
                }
            }
            de.createNote(nota, new Point2D.Double(40, y));
        });
    }

    // ------------------------------------------------------------------ maquina de estados

    static void maquinaDeEstados() throws Exception {
        StateMachineDiagramEditor de = pa.getDiagramEditorFactory().getStateMachineDiagramEditor();
        de.createStatemachineDiagram(pacoteDiagramas, "SM 01 - Máquina de estados de Livro");
        Map<String, INodePresentation> s = new LinkedHashMap<>();
        INodePresentation inicio = de.createInitialPseudostate(null, new Point2D.Double(60, 105));
        s.put("LIVRE", de.createState("LIVRE", null, new Point2D.Double(300, 90)));
        s.put("RESERVADO", de.createState("RESERVADO", null, new Point2D.Double(820, 90)));
        s.put("EMPRESTADO", de.createState("EMPRESTADO", null, new Point2D.Double(1450, 90)));
        s.put("RENOVADO", de.createState("RENOVADO", null, new Point2D.Double(2150, 90)));
        s.put("DEVOLVIDO", de.createState("DEVOLVIDO", null, new Point2D.Double(860, 560)));
        s.put("REPARANDO", de.createState("REPARANDO", null, new Point2D.Double(300, 900)));
        s.put("INCINERADO", de.createState("INCINERADO", null, new Point2D.Double(1100, 900)));
        INodePresentation excluido = de.createFinalState(null, new Point2D.Double(70, 330));

        transicao(de, inicio, s.get("LIVRE"), "", "", "new Livro(id, titulo)", 0, 0);
        transicao(de, s.get("LIVRE"), excluido, "excluir()", "", "", 0, 0);
        transicao(de, s.get("LIVRE"), s.get("RESERVADO"), "reservar(e)", "e != null", "reservante := e.id", 0, 0);
        transicao(de, s.get("RESERVADO"), s.get("EMPRESTADO"), "emprestar(e, hoje)", "e != null and e.id = reservante", "vencimento := hoje + 7", 0, 0);
        transicao(de, s.get("EMPRESTADO"), s.get("RENOVADO"), "renovar()", "numRenovacoes < 2", "vencimento += 30; numRenovacoes++", 0, 0);
        transicao(de, s.get("RENOVADO"), s.get("RENOVADO"), "renovar()", "numRenovacoes < 2", "vencimento += 30; numRenovacoes++", 0, 0);
        transicao(de, s.get("EMPRESTADO"), s.get("DEVOLVIDO"), "devolver()", "", "", 0, 0);
        transicao(de, s.get("RENOVADO"), s.get("DEVOLVIDO"), "devolver()", "", "", 0, 0);
        transicao(de, s.get("DEVOLVIDO"), s.get("LIVRE"), "aprovarInspecao()", "", "limparCiclo()", 0, 0);
        transicao(de, s.get("DEVOLVIDO"), s.get("REPARANDO"), "reprovarInspecao()", "", "", 0, 0);
        transicao(de, s.get("LIVRE"), s.get("REPARANDO"), "enviarParaReparo()", "", "", -190, -120);
        transicao(de, s.get("REPARANDO"), s.get("LIVRE"), "concluirReparo()", "", "limparCiclo()", 130, 150);
        transicao(de, s.get("REPARANDO"), s.get("INCINERADO"), "registrarLaudoIrrecuperavel()", "", "limparCiclo()", 0, 0);
        de.createNote("Cada transição é um método sobrescrito na classe do estado de origem: Livre.reservar / enviarParaReparo / excluir,\n"
                + "Reservado.emprestar, Emprestado.renovar / devolver, Renovado.renovar / devolver,\n"
                + "Devolvido.aprovarInspecao / reprovarInspecao, Reparando.concluirReparo / registrarLaudoIrrecuperavel.\n"
                + "As guardas e as ações estão no corpo desse método. A transição inicial é o construtor Livro(id, titulo).\n"
                + "renovar() com numRenovacoes = 2 lança RenovacaoNegadaException: o livro permanece RENOVADO e o vencimento não muda.\n"
                + "reservar com e = null e emprestar para quem não é o reservante lançam ReservaInvalidaException, sem mudar de estado.\n"
                + "INCINERADO é final: não aceita nenhum evento. Qualquer evento não listado para um estado é recusado por\n"
                + "EstadoLivro com OperacaoInvalidaException, sem alterar o Livro (ex.: reservar em DEVOLVIDO).\n"
                + "Livre.excluir() só permite a exclusão (corpo vazio); quem retira o livro do acervo é TratadorDeAcervo.excluirLivro (estado final).\n"
                + "enviarParaReparo() é chamado por TratadorDeAcervo (dano identificado no acervo); aprovarInspecao() e reprovarInspecao(),\n"
                + "por ServicoDeCirculacao.registrarInspecao; concluirReparo() e registrarLaudoIrrecuperavel(), por registrarRetornoDoReparo.",
                new Point2D.Double(300, 1050));
    }

    static void transicao(StateMachineDiagramEditor de, INodePresentation de1, INodePresentation para, String evento, String guarda, String acao,
                          double desvioX, double desvioY) throws Exception {
        ILinkPresentation l = de.createTransition(de1, para);
        if (desvioX != 0 || desvioY != 0) {                 // dobra a linha para nao sobrepor outra transicao
            Point2D[] pts = l.getPoints();
            Point2D ini = pts[0], fim = pts[pts.length - 1];
            Point2D meio = new Point2D.Double((ini.getX() + fim.getX()) / 2 + desvioX, (ini.getY() + fim.getY()) / 2 + desvioY);
            tentar(() -> l.setAllPoints(new Point2D[] {ini, meio, fim}));
        }
        ITransition t = (ITransition) l.getModel();
        if (!evento.isEmpty()) t.setEvent(evento);
        if (!guarda.isEmpty()) t.setGuard(guarda);
        if (!acao.isEmpty()) t.setAction(acao);
    }

    // ------------------------------------------------------------------ diagramas de sequencia

    /**
     * Mini-linguagem: "A>B|mensagem(args)" chamada; "<|texto" retorno da chamada aberta mais recente;
     * "N|texto" nota. As linhas de vida sao "apelido=nome:Classe" (Classe com @ e ator).
     */
    static void diagramasDeSequencia() {
        seq("SD 01 - R01 Terceira renovação recusada",
                "Pré-condição: livro RENOVADO com numRenovacoes = 2. A guarda [numRenovacoes < 2] é avaliada em Renovado.renovar\nANTES de prorrogar: vencimento e numRenovacoes ficam inalterados, a transação é desfeita e nada é gravado.\nA renovação chega à fachada como solicitação: TratadorDeEmprestimo.renovarEmprestimo (ou a tela, por submeter) cria a SolicitacaoDeRenovacao\ne o executor chama executar (ver SD 11 e SD 12). emTransacao(operacao) é o método privado da fachada que demarca a transação\ne relança a falha (aqui, a RenovacaoNegadaException) depois de desfazer().",
                "Z=s:SolicitacaoDeRenovacao C=:ServicoDeCirculacao U=:UnidadeDeTrabalho M=:Movimentacao A=:AcervoDeLivros L=livro:Livro S=estado:Renovado",
                "Z>C|renovar(\"L1\", \"E1\")", "C>C|emTransacao(operacao)", "C>U|iniciar()", "<|",
                "C>C|exigirEmprestimo(\"L1\", \"E1\")", "C>M|consultarEmprestimo(\"L1\", \"E1\")", "<|emprestimo", "<|emprestimo",
                "C>C|exigirLivro(\"L1\")", "C>A|consultar(\"L1\")", "<|livro", "<|livro",
                "C>L|renovar()", "L>S|renovar(livro)", "S>L|getNumRenovacoes()", "<|2", "S>L|getId()", "<|\"L1\"",
                "<|RenovacaoNegadaException", "<|RenovacaoNegadaException",
                "C>U|desfazer()", "<|", "<|falha",
                "<|RenovacaoNegadaException (renovacao negada para o livro L1: limite de 2 renovacoes)");
        seq("SD 02 - R01 Reserva de livro devolvido e não inspecionado",
                "Devolvido não sobrescreve reservar: vale a implementação herdada de EstadoLivro, que recusa sem alterar o livro.\nA fachada desfaz a transação (emTransacao); o executor de solicitações converte a exceção em resultado RECUSADA (ver SD 12).\nA SolicitacaoDeReserva é criada por TratadorDeEmprestimo.reservarLivro (ou pela tela) e submetida ao executor.\nrecusa(operacao) monta a OperacaoInvalidaException com situacao(); emTransacao relança a falha depois de desfazer().",
                "Z=s:SolicitacaoDeReserva C=:ServicoDeCirculacao U=:UnidadeDeTrabalho R=:Relacionamento A=:AcervoDeLivros L=livro:Livro S=estado:Devolvido",
                "Z>C|reservar(idLivro, idEstudante, data)", "C>C|emTransacao(operacao)", "C>U|iniciar()", "<|",
                "C>R|consultarEstudante(idEstudante)", "<|e",
                "C>C|exigirLivro(idLivro)", "C>A|consultar(idLivro)", "<|livro", "<|livro",
                "C>L|reservar(e)", "L>S|reservar(livro, e)", "S>S|recusa(\"reservar\")", "<|exceção",
                "<|OperacaoInvalidaException", "<|OperacaoInvalidaException",
                "C>U|desfazer()", "<|", "<|falha",
                "<|OperacaoInvalidaException (operacao \"reservar\" recusada: o livro esta DEVOLVIDO)");
        seq("SD 03 - R02 Multa na política ORIGINAL (5 dias de atraso = R$ 5,00)",
                "ORIGINAL: 5 x 1,00 = R$ 5,00. O ServicoDeMulta não sabe qual política está vigente: pede ao SeletorDePolitica e chama calcular.\nA Multa grava o valor e a politicaAplicada na emissão; a emissão do boleto (cobrar) é a mesma para qualquer política.\nO serviço fixa duas casas no valor (setScale(2, HALF_UP)) e, se o idDevolucao já tem multa, devolve a multa existente sem emitir outra.",
                "O=:OuvinteMulta S=:ServicoDeMulta X=:SeletorDePolitica G=:ConfiguracaoAdministrativa P=p:MultaDiaria W=:GatewayDeCobranca R=:RegistroDeCobrancas",
                "O>S|emitirMulta(idDevolucao, cpf, 5, data)", "S>X|vigente()", "X>G|getPoliticaVigente()", "<|\"ORIGINAL\"",
                "X>X|obter(\"ORIGINAL\")", "<|p", "<|p",
                "S>P|calcular(5)", "<|5.00", "S>P|id()", "<|\"ORIGINAL\"",
                "S>S|cobrar(multa)", "S>W|emitirBoleto(solicitacao)", "<|cobranca", "S>R|registrar(cobranca)", "<|", "<|cobranca",
                "<|multa (valor 5.00; politicaAplicada ORIGINAL)");
        seq("SD 04 - R02 Política com tolerância (5 dias = R$ 3,00) e devolução sem atraso",
                "TOLERANCIA: (5 - 2) x 1,00 = R$ 3,00, com o mesmo ServicoDeMulta. Sem atraso, diasAtraso() = 0: o OuvinteMulta não chama emitirMulta.\ncalcular(0) devolve 0.00 em todas as políticas (R02_semAtrasoValorZero) e emitirMulta devolve null quando o valor é zero.\nsimular(dias) do TratadorDeMulta percorre todas as políticas do seletor; no desenho aparece só a de tolerância.\nsimular(0) devolve {ORIGINAL=0.00, TETO=0.00, TOLERANCIA=0.00}.",
                "TM=:TratadorDeMulta O=:OuvinteMulta V=evento:EventoDevolucao S=:ServicoDeMulta X=:SeletorDePolitica P=p:MultaComTolerancia W=:GatewayDeCobranca R=:RegistroDeCobrancas",
                "N|política vigente: TOLERANCIA; devolução com 5 dias de atraso",
                "O>V|diasAtraso()", "<|5",
                "O>S|emitirMulta(idDevolucao, cpf, 5, data)", "S>X|vigente()", "<|p",
                "S>P|calcular(5)", "<|3.00", "S>P|id()", "<|\"TOLERANCIA\"",
                "S>S|cobrar(multa)", "S>W|emitirBoleto(solicitacao)", "<|cobranca", "S>R|registrar(cobranca)", "<|", "<|cobranca",
                "<|multa (valor 3.00; politicaAplicada TOLERANCIA)",
                "N|devolução no prazo: nenhuma multa é solicitada",
                "O>V|diasAtraso()", "<|0",
                "N|sem atraso o valor é zero em qualquer política (simulação pela tela do Financeiro)",
                "TM>TM|simular(0)", "TM>X|todas()", "<|políticas", "TM>P|id()", "<|\"TOLERANCIA\"", "TM>P|calcular(0)", "<|0.00",
                "<|valores");
        seq("SD 05 - R03 R$ 7,00 no banco A e no banco B",
                "O ServicoDeMulta só conhece GatewayDeCobranca: não há \"if banco\". Qual adaptador atende a instalação é decidido\nna montagem (Aplicacao). Banco A: 7,00 -> 700 centavos. Banco B: CPF -> documento formatado e valor decimal 7.00.",
                "F=:ServicoDeMulta A=gateway:AdaptadorBancoA XA=:ApiBancoA B=gateway:AdaptadorBancoB XB=:ApiBancoB",
                "N|instalação configurada com o banco A",
                "F>A|emitirBoleto(solicitacao{valor = 7.00})", "A>A|paraCentavos(7.00)", "<|700", "A>A|paraSoDigitos(cpf)", "<|cpf", "A>XA|gerarTitulo(cpf, 700)", "<|TituloA{nossoNumero}", "<|Cobranca{A, nossoNumero}",
                "N|instalação configurada com o banco B",
                "F>B|emitirBoleto(solicitacao{valor = 7.00})", "B>B|paraDecimal(7.00)", "<|7.00", "B>B|paraDocumento(cpf)", "<|documento", "B>XB|abrirCobranca(documento, 7.00)", "<|CobrancaB{txid}", "<|Cobranca{B, txid}");
        seq("SD 06 - R03 Notificação repetida sem segunda quitação",
                "simularPagamento representa o pagamento feito no banco: a ApiBancoA chama o webhook registrado, que é AdaptadorBancoA.receberWebhook.\nO adaptador traduz o JSON (situação LIQUIDADO -> QUITADA) em ConfirmacaoPagamento; as chamadas a campo(json, expressao), que extraem cada campo, foram omitidas.\nquitarSeAberta é uma atualização condicional atômica (synchronized): na segunda notificação a cobrança já está QUITADA,\no retorno é false e nenhuma quitação nova é contada.",
                "K=:@Estudante XA=:ApiBancoA A=:AdaptadorBancoA Q=:ServicoDeQuitacao R=:RegistroDeCobrancas",
                "K>XA|simularPagamento(nossoNumero)", "XA>XA|notificacaoDe(nossoNumero)", "<|json",
                "XA>A|receberWebhook(json)", "A>A|statusDe(\"LIQUIDADO\")", "<|LIQUIDADO", "A>A|traduzir(LIQUIDADO)", "<|QUITADA",
                "A>Q|confirmar(confirmacao)", "Q>R|buscar(banco, idExterno)", "<|cobranca (ABERTA)",
                "Q>R|quitarSeAberta(idCobranca, idPagamento)", "<|true", "<|", "<|", "<|",
                "N|o banco reenvia a mesma notificação: simularPagamento é chamado de novo para o título já pago",
                "K>XA|simularPagamento(nossoNumero)", "XA>XA|notificacaoDe(nossoNumero)", "<|json",
                "XA>A|receberWebhook(json)", "A>A|statusDe(\"LIQUIDADO\")", "<|LIQUIDADO", "A>A|traduzir(LIQUIDADO)", "<|QUITADA",
                "A>Q|confirmar(confirmacao)", "Q>R|buscar(banco, idExterno)", "<|cobranca (QUITADA)",
                "Q>R|quitarSeAberta(idCobranca, idPagamento)", "<|false", "<|", "<|", "<|");
        seq("SD 07 - R04 Efetivar empréstimo pela fachada",
                "Operação atômica (emTransacao). Se a reserva estiver ausente, for de outro estudante, a data do empréstimo for anterior à da reserva\nou algum passo falhar, a fachada chama desfazer() e nada fica gravado. A interface recebe apenas o Resultado do tratador; nenhuma coleção é exposta.\nGuarda e ações da transição RESERVADO -> EMPRESTADO estão no corpo de Reservado.emprestar.",
                "E=:@Estudante T=:TratadorDeEmprestimo C=:ServicoDeCirculacao U=:UnidadeDeTrabalho R=:Relacionamento M=:Movimentacao A=:AcervoDeLivros L=livro:Livro S=estado:Reservado",
                "E>T|emprestarLivro(idLivro, idEstudante, data)", "T>C|efetivarEmprestimo(idLivro, idEstudante, data)",
                "C>C|emTransacao(operacao)",
                "C>U|iniciar()", "<|", "C>R|consultarEstudante(idEstudante)", "<|e", "C>M|consultarReserva(idLivro, idEstudante)", "<|reserva",
                "C>C|exigirReservaDo(e, reserva, idLivro, idEstudante)", "<|",
                "C>C|exigirLivro(idLivro)", "C>A|consultar(idLivro)", "<|livro", "<|livro",
                "C>L|emprestar(e, data)", "L>S|emprestar(livro, e, data)", "S>L|getReservante()", "<|reservante (= e.id)",
                "S>L|definirVencimento(data + 7 dias)", "<|", "S>L|mudarPara(Emprestado.INSTANCIA)", "<|", "<|", "<|",
                "C>M|remover(reserva)", "<|", "C>M|inserir(emprestimo)", "<|",
                "C>A|atualizar(livro)", "<|", "C>L|getVencimento()", "<|vencimento", "C>U|confirmar()", "<|",
                "<|resultado", "<|ComprovanteEmprestimo", "<|Resultado(ok)");
        aperto = 0.82;
        seq("SD 08 - R05 Devolução atrasada publica o evento",
                "A publicação ocorre depois de a devolução estar confirmada pela fachada. O evento leva vencimento, data de devolução e estudante.\nOrdem dos ouvintes: a do registro em Aplicacao. O livro permanece DEVOLVIDO até a decisão da inspeção.\nDepois da publicação o tratador consulta a multa só para mostrá-la no Resultado. Foram omitidos os acessores do evento\n(idLivro, idDevolucao, cpfEstudante, devolvidoEm), a segunda leitura de diasAtraso no OuvinteMulta e o interior do OuvinteEmail (ver SD 10).",
                "D=:@Atendente T=:TratadorDeDevolucao V=evento:EventoDevolucao C=:iCirculacao P=:PublicadorDeDevolucao I=:OuvinteInspecao AG=:AgendaDeInspecao M=:OuvinteMulta F=:iMulta X=:OuvinteIndicadores PN=:PainelDeIndicadores O=:OuvinteEmail",
                "D>T|registrarDevolucao(idLivro, idEstudante, data)", "T>C|devolver(idLivro, idEstudante, data)", "<|comprovante",
                "T>>V|EventoDevolucao(comprovante)",
                "T>P|publicar(evento)", "P>I|aoDevolver(evento)", "I>AG|agendar(idLivro)", "<|", "<|",
                "P>M|aoDevolver(evento)", "M>V|diasAtraso()", "<|3", "M>F|emitirMulta(idDevolucao, cpf, 3, data)", "<|multa", "<|",
                "P>X|aoDevolver(evento)", "X>V|diasAtraso()", "<|3", "X>PN|registrar(3)", "<|", "<|", "P>O|aoDevolver(evento)", "<|", "<|",
                "T>F|consultarMulta(idDevolucao)", "<|multa", "T>V|diasAtraso()", "<|3",
                "<|Resultado(ok)");
        seq("SD 09 - R05 Devolução no prazo não gera multa",
                "No prazo, diasAtraso() = 0: o OuvinteMulta não chama emitirMulta, e a consulta do tratador ao Financeiro devolve null.\nInspeção, indicadores e e-mail são notificados do mesmo jeito. O livro permanece DEVOLVIDO até a decisão da inspeção\n(inspecionarLivro): nenhum ouvinte envia mensagem ao Livro.",
                "D=:@Atendente T=:TratadorDeDevolucao V=evento:EventoDevolucao C=:iCirculacao P=:PublicadorDeDevolucao I=:OuvinteInspecao AG=:AgendaDeInspecao M=:OuvinteMulta F=:iMulta X=:OuvinteIndicadores PN=:PainelDeIndicadores O=:OuvinteEmail",
                "D>T|registrarDevolucao(idLivro, idEstudante, data)", "T>C|devolver(idLivro, idEstudante, data)", "<|comprovante",
                "T>>V|EventoDevolucao(comprovante)",
                "T>P|publicar(evento)", "P>I|aoDevolver(evento)", "I>AG|agendar(idLivro)", "<|", "<|",
                "P>M|aoDevolver(evento)", "M>V|diasAtraso()", "<|0", "<|",
                "P>X|aoDevolver(evento)", "X>V|diasAtraso()", "<|0", "X>PN|registrar(0)", "<|", "<|", "P>O|aoDevolver(evento)", "<|", "<|",
                "T>F|consultarMulta(idDevolucao)", "<|null (sem multa)", "T>V|diasAtraso()", "<|0",
                "<|Resultado(ok)");
        aperto = 1.0;
        seq("SD 10 - R05 Falha do e-mail não afeta os demais ouvintes",
                "Cada ouvinte é isolado pelo try/catch de publicar(): a exceção do e-mail é registrada em falhas,\nos outros ouvintes já foram (ou continuam sendo) notificados e a devolução, confirmada antes da publicação, não é desfeita.",
                "T=:TratadorDeDevolucao P=:PublicadorDeDevolucao I=:OuvinteInspecao M=:OuvinteMulta X=:OuvinteIndicadores O=:OuvinteEmail Y=:iPessoa Z=:ServicoDeEmail",
                "T>P|publicar(evento)", "P>I|aoDevolver(evento)", "<|", "P>M|aoDevolver(evento)", "<|", "P>X|aoDevolver(evento)", "<|",
                "P>O|aoDevolver(evento)", "O>Y|consultarPessoa(cpf)", "<|pessoa", "O>Z|enviar(para, assunto, texto)", "<|FalhaDeEnvioException", "<|FalhaDeEnvioException",
                "P>P|registrarFalha(ouvinte, evento, erro)", "<|", "<|");
        seq("SD 11 - R06 Renovação submetida duas vezes com o mesmo id",
                "reservarExecucao(id) é uma inserção atômica em um conjunto concorrente. No reenvio o id já existe: o executor devolve o resultado\ngravado e NÃO chama executar de novo, então o vencimento soma apenas 30 dias. As demais leituras de getId() foram omitidas.",
                "E=:@Estudante T=:TratadorDeEmprestimo X=:ExecutorDeSolicitacoes R=:RegistroDeSolicitacoes S=s42:SolicitacaoDeRenovacao C=:iCirculacao",
                "E>T|submeter(s42)", "T>X|submeter(s42)", "X>S|getId()", "<|id", "X>R|reservarExecucao(id)", "<|true",
                "X>S|executar(circulacao)", "S>C|renovar(idLivro, idEstudante)", "<|ComprovanteRenovacao (vencimento + 30 dias)",
                "<|ResultadoSolicitacao(EXECUTADA)", "X>R|concluir(id, resultado)", "<|", "<|resultado", "<|resultado",
                "N|resposta perdida: a tela reenvia a MESMA solicitação",
                "E>T|submeter(s42)", "T>X|submeter(s42)", "X>S|getId()", "<|id", "X>R|reservarExecucao(id)", "<|false",
                "X>R|resultadoDe(id)", "<|gravado", "<|gravado.comoRepetida()", "<|resultado gravado (repetida = true)");
        seq("SD 12 - R06 Solicitações diferentes continuam sujeitas ao limite de duas renovações",
                "Pré-condição: duas solicitações de renovação, com ids diferentes, já foram executadas (numRenovacoes = 2).\nA terceira tem id novo, então é executada; quem recusa é o domínio (Renovado.renovar). O executor só converte a exceção em RECUSADA e grava.",
                "E=:@Estudante T=:TratadorDeEmprestimo X=:ExecutorDeSolicitacoes R=:RegistroDeSolicitacoes S=s3:SolicitacaoDeRenovacao C=:iCirculacao",
                "E>T|submeter(s3)", "T>X|submeter(s3)", "X>S|getId()", "<|id3", "X>R|reservarExecucao(id3)", "<|true",
                "X>S|executar(circulacao)", "S>C|renovar(idLivro, idEstudante)", "<|RenovacaoNegadaException", "<|RenovacaoNegadaException",
                "X>R|concluir(id3, resultado)", "<|", "<|ResultadoSolicitacao(RECUSADA)", "<|ResultadoSolicitacao(RECUSADA)");
        seq("SD 13 - R07 Inclusão de estudante (duplicado e CPF não cadastrado)",
                "incluir() é o método-modelo (final). 1) E1 já existe: devolve EXISTENTE logo após buscarExistente.\n2) CPF 99999999999 não cadastrado: o gancho verificarPreRequisitos lança PessoaNaoCadastradaException,\nque incluir() captura (recusa) e converte em ResultadoCadastro.recusado; criar() e armazenar() não são executados.",
                "D=:@Administrador T=:TratadorDeEstudante R=:Relacionamento P=:iPessoa",
                "D>T|inserirEstudante(\"E1\", cpf)", "T>T|incluir(dados)", "T>T|buscarExistente(dados)", "T>R|consultarEstudante(\"E1\")", "<|estudante", "<|estudante",
                "T>T|apresentar(estudante, false)", "<|resultado", "<|resultado", "<|ResultadoCadastro(EXISTENTE, estudante, null)",
                "N|novo estudante associado a CPF não cadastrado",
                "D>T|inserirEstudante(\"E9\", \"99999999999\")", "T>T|incluir(dados)", "T>T|buscarExistente(dados)", "T>R|consultarEstudante(\"E9\")", "<|null", "<|null",
                "T>T|validar(dados)", "<|", "T>T|verificarPreRequisitos(dados)", "T>P|consultarPessoa(\"99999999999\")", "<|null", "<|recusa",
                "<|resultado", "<|ResultadoCadastro(RECUSADO, null, \"pessoa nao cadastrada: CPF 99999999999\")");
        seq("SD 14 - R08 Perfil PRODUCAO fornece a família MySQL",
                "A família é escolhida uma única vez, em fabricaPara(perfil). A Aplicacao só conhece FabricaDePersistencia e passa a MESMA\nunidade de trabalho u aos três repositórios. daFamilia(u) recusa unidade de trabalho de outra família (FamiliaIncompativelException).\nAs mensagens 3 a 6 são enviadas pelo construtor de Aplicacao, que guarda cada repositório em AcervoDeLivros, Movimentacao e Relacionamento.",
                "I=:Main K=:ConfiguradorDePersistencia A=:Aplicacao F=f:FabricaMySQL",
                "I>K|fabricaPara(PRODUCAO)", "<|f : FabricaMySQL", "I>>A|Aplicacao(f, banco, email, comCache)",
                "A>F|criarUnidadeDeTrabalho()", "<|u : UnidadeDeTrabalhoMySQL",
                "A>F|criarRepositorioLivro(u)", "F>F|daFamilia(u)", "<|u", "<|rl : RepositorioLivroMySQL",
                "A>F|criarRepositorioMovimentacao(u)", "F>F|daFamilia(u)", "<|u", "<|rm : RepositorioMovimentacaoMySQL",
                "A>F|criarRepositorioRelacionamento(u)", "F>F|daFamilia(u)", "<|u", "<|rr : RepositorioRelacionamentoMySQL");
        seq("SD 15 - R08 Repositórios MySQL compartilham a mesma unidade de trabalho",
                "Parte de gravação de efetivarEmprestimo no perfil PRODUCAO. A fachada fala com os pontos internos de acesso (Movimentacao, AcervoDeLivros),\nque delegam aos repositórios. Todos escrevem pela conexão única de u, por isso confirmar() é um único COMMIT e desfazer(), um único ROLLBACK.",
                "C=:ServicoDeCirculacao MV=:Movimentacao M=rm:RepositorioMovimentacaoMySQL AC=:AcervoDeLivros L=rl:RepositorioLivroMySQL U=u:UnidadeDeTrabalhoMySQL",
                "C>C|emTransacao(operacao)", "C>U|iniciar()", "<|",
                "C>MV|remover(reserva)", "MV>M|removerReserva(reserva)", "M>U|conexao()", "<|conexão de u", "<|", "<|",
                "C>MV|inserir(emprestimo)", "MV>M|inserirEmprestimo(emprestimo)", "M>U|conexao()", "<|conexão de u", "<|", "<|",
                "C>AC|atualizar(livro)", "AC>L|salvar(livro)", "L>U|conexao()", "<|conexão de u", "<|", "<|",
                "C>U|confirmar()", "<|COMMIT único", "<|");
        seq("SD 16 - R08 Perfil TESTE fornece apenas objetos em memória",
                "A mesma sequência de montagem, com outra fábrica: nenhuma classe MySQL é criada e nenhum banco de dados é necessário.\nÉ a configuração usada pelos testes automatizados. As mensagens 3 a 6 são enviadas pelo construtor de Aplicacao.",
                "I=:Main K=:ConfiguradorDePersistencia A=:Aplicacao F=f:FabricaEmMemoria",
                "I>K|fabricaPara(TESTE)", "<|f : FabricaEmMemoria", "I>>A|Aplicacao(f, banco, email, comCache)",
                "A>F|criarUnidadeDeTrabalho()", "<|u : UnidadeDeTrabalhoEmMemoria",
                "A>F|criarRepositorioLivro(u)", "F>F|daFamilia(u)", "<|u", "<|RepositorioLivroEmMemoria",
                "A>F|criarRepositorioMovimentacao(u)", "F>F|daFamilia(u)", "<|u", "<|RepositorioMovimentacaoEmMemoria",
                "A>F|criarRepositorioRelacionamento(u)", "F>F|daFamilia(u)", "<|u", "<|RepositorioRelacionamentoEmMemoria");
        seq("SD 17 - R09 Segunda consulta atendida pelo cache e ainda medida",
                "Composição montada por MontadorDeConsulta: Medicao(Cache(Basica)). A medição fica por fora, então a consulta atendida\npelo cache também é medida. O cache guarda só MetadadosLivro (id e titulo) e a ConsultaAcervoBasica não foi alterada.\nO decorador de cache avisa o acerto ao RegistroDeMetricas pelo retorno de chamada recebido na montagem; se ler ou gravar falhar, a consulta segue sem o cache.",
                "V=:@Usuario Q=:TratadorDeConsulta D=med:DecoradorDeMedicao H=cache:DecoradorDeCache K=:ArmazenamentoDeCache B=:ConsultaAcervoBasica A=:iAcervo G=:RegistroDeMetricas",
                "V>Q|consultar(\"Dom\")", "Q>D|buscarMetadados(\"Dom\")", "D>H|buscarMetadados(\"Dom\")",
                "H>H|lerDoCache(\"Dom\")", "H>K|ler(\"Dom\")", "<|null", "<|null",
                "H>B|buscarMetadados(\"Dom\")", "B>A|listar()", "<|livros", "<|lista",
                "H>H|gravarNoCache(\"Dom\", lista)", "H>K|gravar(\"Dom\", lista)", "<|", "<|",
                "<|lista", "D>D|registrar(\"Dom\", nanos)", "D>G|registrar(\"Dom\", nanos)", "<|", "<|", "<|lista", "<|lista",
                "N|segunda consulta com o mesmo termo, dentro da validade do cache",
                "V>Q|consultar(\"Dom\")", "Q>D|buscarMetadados(\"Dom\")", "D>H|buscarMetadados(\"Dom\")",
                "H>H|lerDoCache(\"Dom\")", "H>K|ler(\"Dom\")", "<|lista", "<|lista",
                "H>H|avisarAcerto(\"Dom\")", "H>G|registrarAcertoDeCache(\"Dom\")", "<|", "<|", "<|lista (do cache)",
                "D>D|registrar(\"Dom\", nanos)", "D>G|registrar(\"Dom\", nanos)", "<|", "<|", "<|lista", "<|lista");
        seq("SD 18 - R09 Resultado antigo do cache não autoriza a reserva",
                "O livro L1 ainda aparece na lista em cache, mas já foi emprestado. A reserva não passa pelo cache: a fachada lê o Livro\nno AcervoDeLivros e o estado atual (EMPRESTADO) recusa o evento.\nrecusa(operacao) monta a OperacaoInvalidaException com situacao(); emTransacao relança a falha depois de desfazer().",
                "Z=s:SolicitacaoDeReserva C=:ServicoDeCirculacao U=:UnidadeDeTrabalho R=:Relacionamento A=:AcervoDeLivros L=L1:Livro S=estado:Emprestado",
                "Z>C|reservar(\"L1\", idEstudante, data)", "C>C|emTransacao(operacao)", "C>U|iniciar()", "<|",
                "C>R|consultarEstudante(idEstudante)", "<|e",
                "C>C|exigirLivro(\"L1\")", "C>A|consultar(\"L1\")", "<|livro (EMPRESTADO)", "<|livro",
                "C>L|reservar(e)", "L>S|reservar(livro, e)", "S>S|recusa(\"reservar\")", "<|exceção",
                "<|OperacaoInvalidaException", "<|OperacaoInvalidaException",
                "C>U|desfazer()", "<|", "<|falha",
                "<|OperacaoInvalidaException (operacao \"reservar\" recusada: o livro esta EMPRESTADO)");
        seq("SD 19 - R10 Contar cinco livros em uma unidade com duas seções",
                "A contagem é recursiva: o grupo soma o que cada filho responde (grupo ou item, a mesma operação). 3 + 2 = 5.\nA unidade é a raiz da árvore. listarIds(nome) repete componente(nome) e percorre a árvore do mesmo jeito, juntando os ids.",
                "D=:@Administrador T=:TratadorDeOrganizacao O=:OrganizadorDoAcervo U=unidade:GrupoDoAcervo A=secaoA:GrupoDoAcervo B=secaoB:GrupoDoAcervo I=item:ItemDoAcervo",
                "D>T|contarEListar(\"Unidade Centro\")", "T>O|contarLivros(\"Unidade Centro\")", "O>O|componente(\"Unidade Centro\")", "O>U|buscarGrupo(\"Unidade Centro\")", "<|unidade", "<|unidade",
                "O>U|contarLivros()", "U>A|contarLivros()", "A>I|contarLivros()", "<|1 (cada um dos 3 itens)", "<|3",
                "U>B|contarLivros()", "<|2", "<|5", "<|5",
                "T>O|listarIds(\"Unidade Centro\")", "<|ids dos 5 livros", "<|Resultado(ok)");
        seq("SD 20 - R10 Mover um livro entre seções",
                "Mover = remover da origem + adicionar no destino; adicionar verifica ciclo, se o componente já tem pai, a ordem dos níveis (só para grupos)\ne se o livro já está na árvore; se adicionar falhar, o item volta à origem.\nSó o pai do item muda: Livro (id, estado, empréstimo), AcervoDeLivros e Movimentacao não recebem nenhuma mensagem.",
                "D=:@Administrador T=:TratadorDeOrganizacao O=:OrganizadorDoAcervo U=unidade:GrupoDoAcervo A=secaoA:GrupoDoAcervo B=secaoB:GrupoDoAcervo I=item:ItemDoAcervo",
                "D>T|moverLivro(\"L1\", \"Secao B\")", "T>O|mover(\"L1\", \"Secao B\")", "O>U|buscarItem(\"L1\")", "U>A|buscarItem(\"L1\")", "<|item", "<|item",
                "O>O|grupo(\"Secao B\")", "O>U|buscarGrupo(\"Secao B\")", "U>A|buscarGrupo(\"Secao B\")", "<|null", "U>B|buscarGrupo(\"Secao B\")", "<|secaoB", "<|secaoB", "<|secaoB",
                "O>O|mover(item, secaoB)", "O>I|getPai()", "<|secaoA",
                "O>A|remover(item)", "A>I|definirPai(null)", "<|", "<|",
                "O>B|adicionar(item)", "B>B|ehAncestralDeste(item)", "<|false", "B>I|getPai()", "<|null",
                "B>B|raizDaArvore()", "<|unidade", "B>U|listarIds()", "<|ids já na árvore", "B>I|listarIds()", "<|[L1]",
                "B>I|definirPai(secaoB)", "<|", "<|",
                "<|", "<|", "<|Resultado(ok)");
    }

    /** Fator aplicado a distancia entre linhas de vida; menor que 1 nos diagramas com muitas linhas de vida, para caberem na pagina. */
    static double aperto = 1.0;

    static void seq(String nome, String nota, String linhasDeVida, String... passosBrutos) {
        List<String> lista = new ArrayList<>();
        for (String p : passosBrutos) if (!p.isEmpty()) lista.add(p);
        final String[] passos = lista.toArray(new String[0]);
        emTransacao(nome, () -> {
            SequenceDiagramEditor de = pa.getDiagramEditorFactory().getSequenceDiagramEditor();
            de.createSequenceDiagram(pacoteDiagramas, nome);
            Map<String, INodePresentation> vidas = new LinkedHashMap<>();
            Map<String, IClass> bases = new LinkedHashMap<>();
            List<String> specs = splitVidas(linhasDeVida);
            double[] distancias = distanciasEntreVidas(specs, passos);
            double x = 40;
            int indice = 0;
            for (String spec : specs) {
                String apelido = spec.substring(0, spec.indexOf('='));
                String resto = spec.substring(spec.indexOf('=') + 1);
                String nomeVida = resto.substring(0, resto.indexOf(':'));
                String classe = resto.substring(resto.indexOf(':') + 1);
                INodePresentation p = de.createLifeline(nomeVida, x);
                IClass base = classesUml.get(classe);
                if (base != null) ((ILifeline) p.getModel()).setBase(base);
                else System.out.println("  base nao encontrada: " + classe);
                vidas.put(apelido, p);
                bases.put(apelido, base);
                x += distancias[indice++] * aperto;
            }
            Deque<Object[]> abertas = new ArrayDeque<>();        // {apelidoDestino, link, apelidoOrigem}
            double y = 150;
            for (String passo : passos) {
                String cabeca = passo.substring(0, passo.indexOf('|'));
                String texto = passo.substring(passo.indexOf('|') + 1);
                if (cabeca.equals("N")) {
                    de.createNote(texto, new Point2D.Double(40, y - 12));
                    y += 55;
                } else if (cabeca.equals("<")) {
                    Object[] aberta = abertas.pop();
                    if (!texto.isEmpty()) {
                        de.createReturnMessage(texto, (ILinkPresentation) aberta[1]);
                        y += aberta[0].equals(aberta[2]) ? 58 : 38;      // retorno de mensagem para si ocupa mais altura
                    } else {
                        y += 12;
                    }
                } else {
                    boolean criacao = cabeca.contains(">>");               // "A>>B|Classe(args)": mensagem de criacao
                    String de1 = cabeca.substring(0, cabeca.indexOf('>'));
                    String para = cabeca.substring(cabeca.lastIndexOf('>') + 1);
                    INodePresentation origem = vidas.get(de1);
                    for (Object[] aberta : abertas) {             // remetente com ativacao aberta: a chamada sai da ativacao
                        if (aberta[0].equals(de1)) {
                            origem = ((ILinkPresentation) aberta[1]).getTarget();
                            break;
                        }
                    }
                    int abre = texto.indexOf('(');
                    String operacao = abre < 0 ? texto : texto.substring(0, abre);
                    String argumentos = abre < 0 ? "" : texto.substring(abre + 1, texto.lastIndexOf(')'));
                    if (criacao) {
                        ILinkPresentation link = de.createCreateMessage(operacao, origem, vidas.get(para), y);
                        IMessage m = (IMessage) link.getModel();
                        if (!argumentos.isEmpty()) tentar(() -> m.setArgument(argumentos));
                        y += 50;
                        continue;
                    }
                    ILinkPresentation link = de.createMessage(operacao, origem, vidas.get(para), y);
                    IMessage m = (IMessage) link.getModel();
                    IOperation op = operacaoPara(bases.get(para), operacao, argumentos);
                    if (op != null) tentar(() -> m.setOperation(op));
                    if (!argumentos.isEmpty()) tentar(() -> m.setArgument(argumentos));
                    abertas.push(new Object[] {para, link, de1});
                    y += 42;
                }
            }
            if (!abertas.isEmpty()) System.out.println("  chamadas sem retorno em " + nome + ": " + abertas.size());
            de.createNote(nota, new Point2D.Double(40, y + 60));
        });
    }

    /**
     * Operacao da classe (ou de uma superclasse) que corresponde a mensagem. Entre sobrecargas, prefere a que tem
     * o mesmo numero de parametros e, depois, aquela cujos nomes de parametro coincidem com os argumentos.
     */
    static IOperation operacaoPara(IClass base, String operacao, String argumentos) {
        if (base == null) return null;
        List<String> args = separarArgumentos(argumentos);
        IOperation melhor = null;
        int melhorNota = Integer.MIN_VALUE;
        for (IOperation op : base.getOperations()) {
            if (!op.getName().equals(operacao)) continue;
            com.change_vision.jude.api.inf.model.IParameter[] ps = op.getParameters();
            int nota = ps.length == args.size() ? 10 : 0;
            for (int i = 0; i < Math.min(ps.length, args.size()); i++) {
                String a = args.get(i);
                if (a.equals(ps[i].getName())) nota += 2;
                if (a.startsWith("\"")) nota += "String".equals(String.valueOf(ps[i].getTypeExpression())) ? 1 : -1;
            }
            if (nota > melhorNota) {
                melhor = op;
                melhorNota = nota;
            }
        }
        if (melhor != null) return melhor;
        for (com.change_vision.jude.api.inf.model.IGeneralization g : base.getGeneralizations()) {
            IOperation herdada = operacaoPara(g.getSuperType(), operacao, argumentos);
            if (herdada != null) return herdada;
        }
        return null;
    }

    /** Separa os argumentos por virgula, ignorando virgulas dentro de aspas, chaves ou parenteses. */
    static List<String> separarArgumentos(String argumentos) {
        List<String> args = new ArrayList<>();
        if (argumentos == null || argumentos.isBlank()) return args;
        int nivel = 0;
        boolean aspas = false;
        StringBuilder atual = new StringBuilder();
        for (char c : argumentos.toCharArray()) {
            if (c == '"') aspas = !aspas;
            if (!aspas && (c == '(' || c == '{' || c == '[')) nivel++;
            if (!aspas && (c == ')' || c == '}' || c == ']')) nivel--;
            if (c == ',' && nivel == 0 && !aspas) {
                args.add(atual.toString().trim());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        args.add(atual.toString().trim());
        return args;
    }

    /**
     * Distancia entre cada linha de vida e a seguinte, larga o bastante para que o rotulo de cada
     * mensagem (numero, argumentos e tipo de retorno) caiba entre as duas linhas que ela liga.
     */
    static double[] distanciasEntreVidas(List<String> specs, String[] passos) {
        final double porLetra = 5.6;
        List<String> apelidos = new ArrayList<>();
        List<IClass> tipos = new ArrayList<>();
        double[] d = new double[specs.size()];
        for (int i = 0; i < specs.size(); i++) {
            String spec = specs.get(i);
            String resto = spec.substring(spec.indexOf('=') + 1);
            apelidos.add(spec.substring(0, spec.indexOf('=')));
            tipos.add(classesUml.get(resto.substring(resto.indexOf(':') + 1)));
            d[i] = Math.max(190, (resto.length() + 1) * 7.5 + 60);
        }
        Deque<int[]> abertas = new ArrayDeque<>();
        for (String passo : passos) {
            String cabeca = passo.substring(0, passo.indexOf('|'));
            String texto = passo.substring(passo.indexOf('|') + 1);
            if (cabeca.equals("N")) continue;
            int i, j, letras;
            if (cabeca.equals("<")) {
                int[] aberta = abertas.pop();
                i = aberta[0];
                j = aberta[1];
                if (texto.isEmpty()) continue;
                letras = texto.length();
            } else {
                i = apelidos.indexOf(cabeca.substring(0, cabeca.indexOf('>')));
                j = apelidos.indexOf(cabeca.substring(cabeca.lastIndexOf('>') + 1));
                if (i < 0 || j < 0) throw new IllegalArgumentException("linha de vida desconhecida em " + passo);
                letras = texto.length() + 4 + 3 * (abertas.size() + 1);        // numero da mensagem
                if (cabeca.contains(">>")) letras += 18;                       // a caixa do objeto criado fica na ponta
                if (!cabeca.contains(">>")) {
                    abertas.push(new int[] {i, j});
                    int abre = texto.indexOf('(');
                    String operacao = abre < 0 ? texto : texto.substring(0, abre);
                    String argumentos = abre < 0 ? "" : texto.substring(abre + 1, texto.lastIndexOf(')'));
                    IOperation op = operacaoPara(tipos.get(j), operacao, argumentos);
                    if (op != null) letras += 3 + String.valueOf(op.getReturnTypeExpression()).length();
                }
            }
            if (i == j) {                                                    // mensagem para si: o rotulo fica a direita
                if (i < d.length - 1) d[i] = Math.max(d[i], letras * porLetra * 0.6 + 90);
                continue;
            }
            int menor = Math.min(i, j), maior = Math.max(i, j);
            double atual = 0;
            for (int k = menor; k < maior; k++) atual += d[k];
            double falta = letras * porLetra + 50 - atual;
            if (falta > 0) {
                for (int k = menor; k < maior; k++) d[k] += falta / (maior - menor);
            }
        }
        return d;
    }

    /** Separa as linhas de vida por espaco, respeitando nomes de ator com espaco ("@Banco A"). */
    static List<String> splitVidas(String s) {
        List<String> partes = new ArrayList<>();
        for (String p : s.trim().split("\\s+(?=[A-Za-z]+=)")) partes.add(p.trim());
        return partes;
    }
}
