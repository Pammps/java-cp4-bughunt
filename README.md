# Checkpoint 4 — Bug Hunt StreamFIAP

## Identificação

**Grupo:**

| Integrante | RM | Turma |
|---|---|---|
| Pamella Souza da Silva Ferreira| 566172 | 2CCPH|

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

O Spring cria e gerencia uma implementação dos repositories usados pelos controllers. Com o `@Autowired`, ele injeta essa instância já configurada para acessar o banco. Não seria possível usar `new ConteudoRepository()`, pois ele é uma interface e sua implementação é gerada pelo Spring. Assim, métodos como `save`, `findAll` e `findById` ficam disponíveis sem implementação manual.

### 2. JDBC vs Spring Data JPA (Aulas 12 e 13)

No JDBC, é necessário criar a conexão, escrever o SQL, executar o comando e tratar o `ResultSet`. O Spring Data JPA automatiza essas tarefas e já fornece as operações básicas de CRUD. O método `findByCategoria` funciona porque o Spring interpreta seu nome e cria a consulta usando o atributo `categoria`. O JDBC ainda pode ser útil quando é necessário ter mais controle sobre consultas específicas.

### 3. Exceções checked vs unchecked (Aula 11)

Uma exceção checked, que herda de `Exception`, precisa ser tratada ou declarada com `throws`. Já uma unchecked, que herda de `RuntimeException`, pode se propagar até o tratamento global. No projeto, alterei a `ClassificacaoIndicativaException` e adicionei seu tratamento no `GlobalExceptionHandler`. Dessa forma, a API passou a retornar uma mensagem clara quando a idade do usuário não permite o aluguel.

### 4. Sobrescrita vs sobrecarga (Aula 7)

Sobrescrita acontece quando a subclasse redefine um método herdado com a mesma assinatura. Sobrecarga ocorre quando o método possui o mesmo nome, mas parâmetros diferentes. Em `Serie`, `calcularPrecoAluguel(double desconto)` não sobrescrevia o método sem parâmetros de `Conteudo`. Retirar o parâmetro e adicionar `@Override` corrigiu o cálculo e faria o compilador identificar esse erro.

### 5. Onde blindar o objeto? (Aulas 3, 4 e 13)

A duração foi validada em `Conteudo`, pois nenhum tipo de conteúdo deve aceitar duração menor ou igual a zero. O construtor usa o setter validado para impedir valores inválidos tanto na criação quanto em alterações posteriores. As regras de disponibilidade, idade e créditos ficaram em `Usuario.alugar`, pois dependem do momento do aluguel. Validar somente no controller não protegeria o objeto caso ele fosse usado por outra parte da aplicação.

### 6. Abstração e interface (Aulas 8 e 9)

`Conteudo` é abstrata porque reúne dados e comportamentos comuns de filmes, séries e documentários. `Promocionavel` representa apenas a capacidade de aplicar uma promoção. Se o documentário passasse a ter desconto, ele precisaria implementar essa interface e o método `aplicarPromocao`. As outras classes e o cálculo promocional de `Conteudo` poderiam continuar iguais.

---

## Parte 4 — Espaço livre (opcional)

O bug mais difícil foi o da `Serie` (bug02), porque o código compilava e parecia certo —
só olhando a assinatura com calma dá para perceber que o parâmetro `desconto` sobrava e nunca
era usado. Também percebi os bugs em cascata na prática: o preço da série só apareceu errado
depois de corrigir o construtor (bug01), porque antes disso a série nem tinha os dados salvos.
