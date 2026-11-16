# Mercadinho Fácil (MVP)

Sistema web simples para organizar as atividades do **Mercadinho Nova Opção**, substituindo planilhas e controles manuais.

Projeto acadêmico desenvolvido com dados fictícios.

## Funcionalidades

- **Produtos:** cadastro e consulta de produtos, preços e estoque.
- **Vendas:** registro de vendas com vários itens e cálculo automático do total.
- **Estoque:** baixa automática após cada venda e alerta de estoque baixo.
- **Histórico de vendas:** consulta das vendas registradas.
- **Visão geral:** indicadores e últimas vendas.

## Tecnologias

- Java 17 ou superior (testado no Java 21).
- Servidor HTTP integrado ao JDK.
- HTML, CSS e JavaScript.
- Armazenamento em arquivos de texto, sem dependências externas.

## Como executar

```bash
javac -encoding UTF-8 -d out src/main/java/br/com/mercadinhofacil/Main.java
java -cp out br.com.mercadinhofacil.Main
```

Depois, acesse http://localhost:8080.

Os dados são armazenados na pasta `dados/`, criada automaticamente.

## API

| Método | Rota | Descrição |
|---|---|---|
| GET / POST | `/api/produtos` | Lista e cadastra produtos |
| GET / POST | `/api/vendas` | Lista e registra vendas |
| GET | `/api/resumo` | Apresenta indicadores |

## Regras principais

- A quantidade deve ser inteira e maior que zero.
- A venda não pode ultrapassar o estoque disponível.
- O preço é calculado pelo servidor.
- Os textos são tratados antes de serem exibidos na interface.

## Limitações do MVP

O projeto ainda não possui login nem banco de dados. Essas funcionalidades podem ser implementadas em versões futuras.

## Autor

Diego Oliveira