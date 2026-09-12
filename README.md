# Checkpoint 4 — Bug Hunt StreamFIAP

## Identificação

**Grupo:** _(preencher)_

| Integrante | RM | Turma |
|---|---|---|
| | | |
| | | |
| | | |
| | | |

| Campo | |
|---|---|
| **Total de bugs corrigidos** | 12 / 12 |
| **Total de ajustes de Clean Code** | 6 / 6 |

---

## Parte 1 — Bugs encontrados

| # | Sintoma observado (o que fiz/vi) | Causa raiz (arquivo e linha aproximada) | Correção aplicada | Conceito da disciplina |
|---|---|---|---|---|
| bug01 | Cadastrei uma série com `POST /api/conteudos/serie` e a resposta veio com `titulo`, `categoria`, `duracaoMinutos` e `classificacaoEtaria` todos nulos/zerados. Só o número de temporadas foi salvo. | `Serie.java`, construtor (~linha 14). O construtor recebia os dados mas só atribuía `numeroTemporadas` e nunca chamava `super(...)`. Também não recebia `disponivel`. | Adicionei a chamada `super(titulo, categoria, duracaoMinutos, classificacaoEtaria, disponivel)` e o parâmetro `disponivel`, alinhando com o construtor de `Filme`. Atualizei o `ConteudoController` para passar `serie.isDisponivel()`. | Herança e construtores — a subclasse precisa delegar à superclasse a inicialização dos atributos herdados |
| bug02 | A série de 5 temporadas devolveu preço R$ 9,90 em vez de R$ 24,50 (4,90 × 5). | `Serie.java` (~linha 19). O método era `calcularPrecoAluguel(double desconto)` — com parâmetro. Isso é uma **sobrecarga**, não uma sobrescrita, então o método chamado continuava sendo o da classe `Conteudo`. | Removi o parâmetro `desconto`, deixando `calcularPrecoAluguel()` com a mesma assinatura da superclasse, e coloquei `@Override`. | Sobrescrita (override) vs sobrecarga (overload); uso do `@Override` |
| bug03 | Consultei `/api/conteudos/{id}/preco-promocional` de um filme de R$ 9,90 e veio R$ 11,88 — mais caro que o preço normal. | `Filme.java` (~linha 25). `aplicarPromocao` fazia `preco * 1.2`, que aumenta 20% em vez de descontar. | Troquei por `preco * 0.8`, cumprindo os 20% de desconto que a interface `Promocionavel` documenta. | Implementação de interface — a classe tem que respeitar o contrato declarado |
| bug04 | Aluguel de documentário debitou R$ 9,90 dos créditos, mas pelo contrato documentário é gratuito. | `Documentario.java`. A classe não sobrescrevia `calcularPrecoAluguel()`, então herdava o `return 9.90` da classe `Conteudo`. | Sobrescrevi `calcularPrecoAluguel()` retornando `0.0`. | Polimorfismo — cada subclasse define sua própria regra de preço |
| bug05 | Usuário com 0 créditos conseguiu alugar um filme de R$ 9,90 e ficou com saldo −9,90. | `Usuario.java` (~linha 28). `temCreditosSuficientes` fazia `return preco >= this.creditos`, comparação invertida. | Corrigi para `return this.creditos >= preco`. | Regra de negócio no model; atenção a operadores de comparação |
| bug06 | Cadastrei um usuário com nome "Ana" e o `GET /api/usuarios/1` devolveu `"nome": null`. | `Usuario.java`, construtor (~linha 22). Estava escrito `nome = nome;` — o parâmetro sendo atribuído a ele mesmo, sem o `this`. | Troquei para `this.nome = nome;`. | Escopo de variáveis e uso do `this` para desambiguar atributo e parâmetro |
| bug07 | `POST /api/usuarios` falhava ao salvar / o usuário vinha com `id` nulo. | `Usuario.java` (~linha 11). O campo tinha `@Id` mas não tinha `@GeneratedValue`, ao contrário de `Conteudo`. | Adicionei `@GeneratedValue(strategy = GenerationType.IDENTITY)`. | JPA — mapeamento de chave primária e geração automática de id |
| bug08 | Aluguei um conteúdo com `"disponivel": false` e o aluguel foi aceito normalmente. | `Usuario.java`, método `alugar`. A validação de disponibilidade simplesmente não existia — a `ConteudoIndisponivelException` estava no projeto mas nunca era lançada. | Adicionei no início do `alugar` a checagem `if (!conteudo.isDisponivel())` lançando `ConteudoIndisponivelException`. | Exceções customizadas e validação de regra de negócio no model |
| bug09 | `GET /api/conteudos/999` respondia 200 com corpo vazio, como se tivesse dado certo. | `ConteudoController.java`, `buscarPorId`. Tinha um `try/catch (Exception e)` com o corpo vazio e um `// TODO: tratar isso depois`, e no fim `return null`. A `ConteudoNaoEncontradoException` era criada e engolida. | Removi o try/catch e deixei a exceção subir até o `GlobalExceptionHandler`, que já responde 404 com a mensagem. | Tratamento de exceções — catch vazio engole erro; deixar a exceção propagar até quem sabe tratá-la |
| bug10 | `GET /api/conteudos/categoria/FICCAO` devolvia lista vazia mesmo com conteúdos dessa categoria cadastrados. | `ConteudoController.java`, `listarPorCategoria`. Comparava com `c.getCategoria() == categoria`, que compara referências de objeto e não o texto. | Substituí o laço inteiro pela chamada `conteudoRepository.findByCategoria(categoria)`, que já existia no repository e resolve a busca no banco. | Comparação de String (`==` vs `equals`); query methods do Spring Data JPA |
| bug11 | Cadastrei um filme com `"duracaoMinutos": -30` e ele foi salvo normalmente. | `Conteudo.java`, construtor. Não havia nenhuma validação da duração. | Adicionei no construtor de `Conteudo` a validação `if (duracaoMinutos <= 0)` lançando `IllegalArgumentException`, e criei o handler correspondente no `GlobalExceptionHandler` devolvendo 400 com a mensagem. | Validação no construtor (objeto nasce válido) + tratamento centralizado de exceções |
| bug12 | Usuário de 12 anos tentando alugar filme de classificação 14 recebia erro 500 genérico, sem nenhuma mensagem explicando a classificação. | `GlobalExceptionHandler.java`. Havia handler para as outras três exceções, mas nenhum para `ClassificacaoIndicativaException`. | Adicionei `@ExceptionHandler(ClassificacaoIndicativaException.class)` devolvendo 422 com a mensagem da exceção. | Exceções checked vs unchecked; `@RestControllerAdvice` e tradução de exceção em resposta HTTP |

## Parte 2 — Ajustes de Clean Code

| # | Onde estava | Qual princípio/boas práticas era violado | O que eu mudei |
|---|---|---|---|
| clean01 | `Conteudo.java`, campo `public int duracaoMinutos` (e os três métodos de cadastro do `ConteudoController` que acessavam o campo direto) | Encapsulamento. Todos os outros atributos eram privados com getter/setter; esse estava público, permitindo alterar a duração por fora sem passar por nenhuma validação | Tornei o campo `private` e troquei os acessos `filme.duracaoMinutos` por `filme.getDuracaoMinutos()` nos três métodos de cadastro |
| clean02 | `Usuario.java`, comentário dentro de `debitarCreditos` | Comentário mentiroso. Dizia "adiciona o valor aos créditos do usuário" num método que subtrai. Comentário errado é pior que comentário nenhum, porque induz ao erro | Removi o comentário. O nome `debitarCreditos` e a linha `creditos - valor` já dizem o que o método faz |
| clean03 | `ConteudoController.java`, método `calcularDescontoAntigo` e o bloco de código comentado sobre cupons no fim da classe | Código morto. O método privado não era chamado por ninguém e o bloco comentado ("não apagar, pode ser útil") só polui a leitura | Apaguei os dois. O histórico do Git já guarda o código antigo se algum dia precisar |
| clean04 | `Usuario.java`, método `alugar` — parâmetro `c` e variável `p` | Nomes sem significado. `c` e `p` obrigam quem lê a voltar linhas atrás para lembrar o que são | Renomeei para `conteudo` e `preco` |
| clean05 | `Usuario.java`, bloco de 8 `System.out.println` imprimindo o recibo dentro do `alugar` | Responsabilidade única. O model cuida da regra de negócio; imprimir recibo é apresentação e não é responsabilidade dele. Além disso numa API REST ninguém vê o console | Removi o bloco inteiro. O método continua devolvendo o usuário atualizado, que é o que o controller usa para montar a resposta |
| clean06 | `Conteudo.java` (`return 9.90`), `Filme.java`, `Serie.java`, `Documentario.java` — valores 9.90, 5.00, 4.90 e 0.8 soltos no meio do código | Números mágicos e abstração incompleta. Os valores não diziam o que representavam, e a classe abstrata tinha um preço concreto de filme como padrão, o que foi justamente o que escondeu o bug02 e o bug04 | Criei as constantes `PRECO_BASE`, `ADICIONAL_ESTREIA`, `PRECO_POR_TEMPORADA`, `PRECO_GRATUITO` e `MULTIPLICADOR_PROMOCAO` (essa na interface `Promocionavel`), e transformei `calcularPrecoAluguel()` em método abstrato, obrigando cada subclasse a declarar seu preço |

---

## Parte 3 — Perguntas de reflexão

### 1. Injeção de dependência (Aula 13)

`ConteudoRepository` é uma **interface** — não existe nenhuma classe nossa que a implemente,
então `new ConteudoRepository()` nem compilaria. Quem cria a implementação é o Spring Data JPA,
que gera uma classe em tempo de execução a partir da assinatura dos métodos.

Quando o Spring sobe, ele monta um container com esses objetos (os beans). Ao encontrar o
`@Autowired` no `ConteudoController`, ele pega o bean já pronto e coloca no atributo.
Esse bean não é só o repositório "cru": vem embrulhado com transação, conexão com o Oracle
configurada pelo `application.properties` e tratamento de erro. Com um `new` comum eu teria
um objeto solto, sem conexão nenhuma e sem transação, e ainda teria que passar a conexão na mão
para cada controller. A injeção também facilita trocar a implementação depois sem mexer no controller.

### 2. JDBC vs Spring Data JPA (Aulas 12 e 13)

No `ProdutoDAO` da Aula 12 a gente escrevia o SQL na mão, abria `Connection`, montava
`PreparedStatement`, setava cada parâmetro pela posição, percorria o `ResultSet` campo a campo
montando o objeto e ainda tinha que fechar tudo no `finally`. Eram umas 20 linhas por operação.

O `ConteudoRepository` herda de `JpaRepository<Conteudo, Long>` e já ganha `save`, `findAll`,
`findById` e `deleteById` prontos. O JPA usa o mapeamento das anotações (`@Entity`, `@Id`,
`@Table`) para saber qual tabela e quais colunas usar, e gera o SQL sozinho.

O `findByCategoria` funciona sem implementação por causa dos *query methods*: o Spring lê o nome
do método, quebra em "findBy" + "Categoria", encontra o atributo `categoria` na entidade `Conteudo`
e monta o `select ... where categoria = ?`. Foi exatamente o que usei para corrigir o bug10.

O JDBC ainda ganha quando a consulta é muito específica ou precisa de otimização fina, porque
lá eu controlo o SQL exato que vai ao banco — com JPA eu dependo do que o Hibernate gera.

### 3. Exceções checked vs unchecked (Aula 11)

`ClassificacaoIndicativaException extends Exception`, ou seja, é **checked**: o compilador obriga
quem chama a tratar ou declarar. Por isso o `Usuario.alugar` tem `throws ClassificacaoIndicativaException`
e o `AluguelController` repete esse `throws`. As outras três (`ConteudoNaoEncontradoException`,
`CreditosInsuficientesException`, `ConteudoIndisponivelException`) estendem `RuntimeException`,
são **unchecked** e sobem sozinhas sem precisar declarar nada.

O bug não era o tipo dela, e sim que o `GlobalExceptionHandler` tinha `@ExceptionHandler` para
as outras três e nenhum para essa. Sem handler, o Spring caía no erro padrão e devolvia 500,
e a mensagem que a exceção carregava nunca chegava ao cliente.

Corrigi adicionando `@ExceptionHandler(ClassificacaoIndicativaException.class)`, que devolve
status 422 com `{"erro": "Usuário de 12 anos não pode assistir a ..."}`. O `@RestControllerAdvice`
funciona para os dois tipos: o que decide se a mensagem chega é existir o handler, não ser checked
ou unchecked.

### 4. Sobrescrita vs sobrecarga (Aula 7)

A `Serie` tinha `public double calcularPrecoAluguel(double desconto)`. Como a assinatura inclui os
parâmetros, esse método com um `double` a mais é um método **novo** (sobrecarga), e não a
sobrescrita do `calcularPrecoAluguel()` sem parâmetro da classe `Conteudo`. Compilava sem nenhum
aviso, e o parâmetro `desconto` nem era usado dentro do método.

Na hora do aluguel, `conteudo.calcularPrecoAluguel()` chamava o método sem parâmetro — que só
existia em `Conteudo` — e a série saía por R$ 9,90 fixo, ignorando as temporadas.

Se o método tivesse `@Override`, o compilador procuraria na superclasse um método com aquela
assinatura exata, não acharia e daria erro de compilação na hora. Foi por isso que, além de
corrigir a assinatura, coloquei `@Override` nos três `calcularPrecoAluguel`: a anotação não muda
o comportamento, ela só faz o compilador conferir a intenção.

### 5. Onde blindar o objeto? (Aulas 3, 4 e 13)

Aprendi na prática que depende do tipo de regra:

- **No construtor** ficam as regras que o objeto precisa cumprir para existir. A duração menor
ou igual a zero (bug11) entrou ali: um conteúdo com duração negativa não deveria nem ser criado.
- **Nos setters** ficaria a mesma validação, para o objeto não ficar inválido depois de criado.
Esse é o buraco que sobrou no `setDuracaoMinutos`.
- **No método de negócio** ficam as regras que dependem do estado de outros objetos e do momento
da operação. Créditos, classificação e disponibilidade são validados dentro do `alugar` porque
dependem do usuário *e* do conteúdo juntos — não dá para validar isso no construtor de nenhum dos dois.

Validar em um lugar só não bastou justamente por causa do `clean01`: como `duracaoMinutos` era
`public`, qualquer código podia fazer `conteudo.duracaoMinutos = -5` e furar a validação do
construtor. Encapsular o campo foi o que fez a validação valer de verdade.

### 6. Abstração e interface (Aulas 8 e 9)

`Conteudo` é abstrata e define **o que todo conteúdo é**: tem título, categoria, duração,
classificação e um preço de aluguel. É herança, relação de "é um" — `Filme` é um `Conteudo`.
Como em Java só se herda de uma classe, isso define a identidade do objeto.

`Promocionavel` é uma interface e define **algo que o conteúdo pode fazer**: participar de
promoção. É opcional e transversal — `Filme` e `Serie` implementam, `Documentario` não. Uma classe
pode implementar várias interfaces, então dá para ir somando comportamentos sem mexer na hierarquia.

Se o documentário passasse a ter promoção, eu mudaria **uma linha** na declaração
(`class Documentario extends Conteudo implements Promocionavel`) e adicionaria o método
`aplicarPromocao`. Ficariam **intactos** `Conteudo`, `Filme`, `Serie`, os controllers, os
repositories e o `calcularPrecoPromocional` — que testa `this instanceof Promocionavel` e
passaria a funcionar para o documentário automaticamente, sem nenhum `if` novo.

Isso mostra que o design está bom: o comportamento novo entra por uma classe só, sem efeito
cascata. O que não estava bom era a classe abstrata ter um `return 9.90` concreto — isso não é
abstração, é um preço de filme escondido na classe pai, e foi o que mascarou o bug02 e o bug04.
Tornar o método abstrato obriga toda subclasse nova a declarar seu preço.

---

## Parte 4 — Espaço livre (opcional)

O bug mais difícil foi o da `Serie` (bug02), porque o código compilava e parecia certo —
só olhando a assinatura com calma dá para perceber que o parâmetro `desconto` sobrava e nunca
era usado. Também percebi os bugs em cascata na prática: o preço da série só apareceu errado
depois de corrigir o construtor (bug01), porque antes disso a série nem tinha os dados salvos.
