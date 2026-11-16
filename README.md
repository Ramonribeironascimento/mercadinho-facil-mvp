# Mercadinho Fácil (MVP)

Sistema web simples para organizar as atividades do **Mercadinho Nova Opção**, substituindo planilhas e controles manuais.
Projeto acadêmico com dados fictícios.

## Funcionalidades
- **Produtos:** cadastro (nome, preço, estoque inicial) e consulta.
- **Vendas:** busca de produto, quantidade, subtotal automático, resumo com vários itens e confirmação da venda (funcionalidade central).
- **Estoque:** baixa automática a cada venda e alerta de estoque baixo (5 unidades ou menos).
- **Histórico de vendas:** consulta das vendas registradas.
- **Visão geral:** indicadores, acesso rápido e últimas vendas.

## Tecnologias
Java 17 ou superior (testado no Java 21), servidor HTTP do próprio JDK (`com.sun.net.httpserver`), HTML, CSS e JavaScript puros. Sem dependências externas.

## Como executar
```bash
javac -d out src/main/java/br/com/mercadinhofacil/Main.java
java -cp out br.com.mercadinhofacil.Main
```
Depois abra http://localhost:8080. Para trocar a porta: `PORT=9000 java -cp out br.com.mercadinhofacil.Main`.
Os dados ficam na pasta `dados/` (criada automaticamente, com alguns produtos de exemplo na primeira execução).

## Estrutura
```
src/main/java/br/com/mercadinhofacil/Main.java   servidor, regras de negócio e API
public/index.html                                interface (telas do sistema)
dados/                                           arquivos de dados (gerados ao executar)
```

## API
| Método | Rota | Descrição |
|---|---|---|
| GET / POST | `/api/produtos` | lista / cadastra produtos |
| GET / POST | `/api/vendas` | lista / registra venda |
| GET | `/api/resumo` | indicadores da visão geral |

## Regras principais
- Quantidade deve ser inteira e maior que zero, e não pode ultrapassar o estoque.
- Preço calculado no servidor (o cliente não envia valores).
- Textos digitados são escapados na tela (proteção contra XSS).

## Limitações do MVP
Sem login e sem banco de dados (arquivos de texto). Próximos passos: autenticação e banco MySQL.

## Autor
Diego Oliveira
