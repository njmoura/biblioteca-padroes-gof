# Registro de uso de LLM

## Ferramenta e modelo

- **Ferramenta:** Claude Code (aplicativo desktop).
- **Modelo:** Claude Fable 5.2 (Anthropic).
- **Material fornecido ao modelo:** enunciado do Trabalho 03; enunciado, modelo Astah e apresentação do Trabalho 02 da equipe; lista "Problemas de aplicação dos padrões GoF"; soluções do professor para os dez problemas; slides da disciplina.

## Principais pedidos feitos ao modelo

1. Ler e analisar o material acima antes de escrever qualquer código.
2. Auxiliar com a escrita de códigos do trabalho, de maneira simples.
3. Auxiliar a rodar o perfil de produção contra um MySQL real em Docker, resolvendo bugs dos aplicativos como o Docker, que apresentava erros.
4. Conferir o repositório contra cada item do enunciado e dos critérios de avaliação.

## Aceito / Corrigido / Descartado

### Aceito

- A estrutura das soluções do professor para os dez padrões: classes, papéis e nomes dos métodos principais.
- Os testes dos cenários de verificação, com um nome que começa pelo número do requisito.
- Auxílio no desenvolvimento dos diagramas de classes a partir do código compilado.

### Corrigido ou adaptado

Em relação às soluções do professor e ao Trabalho 02:

- A numeração f1–f6 da solução do professor foi trocada pela do Trabalho 02 (f2 é consultar reserva, f6 é emitir boleto de multa).
- Os nomes dos tratadores e das interfaces do Trabalho 02 foram mantidos (`TratadorDeAcervo`, `iLivro` etc.), em vez de `TratadorDeLivro` e semelhantes.
- `EstadoLivro`, que no Trabalho 02 era uma enumeração, virou a classe abstrata de estado; a enumeração passou a se chamar `Situacao`.
- As guardas `= 0` (em EMPRESTADO) e `< 2` (em RENOVADO) do Trabalho 02 foram unificadas em `numRenovacoes < 2`.
- `SolicitacaoDeCirculacao.executar` recebe o receptor `iCirculacao` como parâmetro. Na solução do professor ele é atributo; como parâmetro, a tela cria a solicitação sem conhecer a fachada.

### Descartado

- Interface web: ficou a linha de comando.
- Atendente dentro das transações.
- `ServicoDeSimulacao` separado: a simulação ficou em `TratadorDeMulta.simular`.
- Chaves estrangeiras no esquema relacional.
- Persistência relacional de multas, cobranças, registro de solicitações e hierarquia do acervo: o enunciado pede na fábrica os repositórios de Livro, Movimentacao e Relacionamento.
- Acentos nos textos do console: a tela não usa acentos, para não depender da página de código do terminal do Windows.
