package br.com.mercadinhofacil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Mercadinho Fácil: servidor web simples (somente JDK, sem dependências externas). */
public class Main {
    record Produto(int id, String nome, double preco, int estoque) {}
    record Venda(int numero, String data, String produto, int quantidade, double unitario, double total) {}

    static final int ESTOQUE_BAIXO = 5;
    static final Path PRODUTOS = Path.of("dados/produtos.txt"), VENDAS = Path.of("dados/vendas.txt");
    static final List<Produto> produtos = new ArrayList<>();
    static final List<Venda> vendas = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("dados"));
        carregar();
        if (produtos.isEmpty()) { dadosIniciais(); salvar(); }
        int porta = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer s = HttpServer.create(new InetSocketAddress(porta), 0);
        s.createContext("/", Main::pagina);
        s.createContext("/api/produtos", Main::apiProdutos);
        s.createContext("/api/vendas", Main::apiVendas);
        s.createContext("/api/resumo", Main::apiResumo);
        s.start();
        System.out.println("Mercadinho Fácil em http://localhost:" + porta);
    }

    // ---------- regras de negócio ----------
    static synchronized void cadastrar(Map<String, String> f) {
        String nome = f.getOrDefault("nome", "").trim().replace("|", " ");
        if (nome.isEmpty() || nome.length() > 60) throw new IllegalArgumentException("Informe o nome do produto (até 60 caracteres).");
        double preco; int estoque;
        try { preco = Double.parseDouble(f.getOrDefault("preco", "").replace(',', '.')); }
        catch (Exception e) { throw new IllegalArgumentException("Informe um preço válido."); }
        try { estoque = Integer.parseInt(f.getOrDefault("estoque", "").trim()); }
        catch (Exception e) { throw new IllegalArgumentException("Informe o estoque inicial (número inteiro)."); }
        if (preco <= 0) throw new IllegalArgumentException("O preço deve ser maior que zero.");
        if (estoque < 0) throw new IllegalArgumentException("O estoque não pode ser negativo.");
        int id = produtos.stream().mapToInt(Produto::id).max().orElse(0) + 1;
        produtos.add(new Produto(id, nome, Math.round(preco * 100) / 100.0, estoque));
        salvar();
    }

    static Produto buscar(int id) {
        for (Produto p : produtos) if (p.id() == id) return p;
        return null;
    }

    /** Registra uma venda com um ou mais itens. Formato de "itens": "idProduto:quantidade,idProduto:quantidade". */
    static synchronized String vender(Map<String, String> f) {
        Map<Integer, Integer> pedido = new LinkedHashMap<>();
        try {
            for (String par : f.get("itens").split(",")) {
                String[] kv = par.split(":");
                int q = Integer.parseInt(kv[1].trim());
                if (q <= 0) throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
                pedido.merge(Integer.parseInt(kv[0].trim()), q, Integer::sum);
            }
        } catch (NumberFormatException | NullPointerException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Adicione ao menos um produto, com quantidade inteira.");
        }
        if (pedido.isEmpty()) throw new IllegalArgumentException("Adicione ao menos um produto à venda.");
        for (var e : pedido.entrySet()) {  // valida tudo antes de alterar o estoque
            Produto p = buscar(e.getKey());
            if (p == null) throw new IllegalArgumentException("Produto não encontrado.");
            if (e.getValue() > p.estoque())
                throw new IllegalArgumentException("Há apenas " + p.estoque() + " unidades de " + p.nome() + " em estoque.");
        }
        int numero = vendas.stream().mapToInt(Venda::numero).max().orElse(0) + 1;
        String data = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        StringBuilder itens = new StringBuilder();
        double total = 0;
        for (var e : pedido.entrySet()) {
            Produto p = buscar(e.getKey());
            int q = e.getValue();
            produtos.set(produtos.indexOf(p), new Produto(p.id(), p.nome(), p.preco(), p.estoque() - q));
            double sub = Math.round(p.preco() * q * 100) / 100.0;
            total += sub;
            vendas.add(new Venda(numero, data, p.nome(), q, p.preco(), sub));
            itens.append(itens.length() > 0 ? "," : "").append(String.format(Locale.US,
                    "{\"produto\":\"%s\",\"quantidade\":%d,\"unitario\":%.2f,\"subtotal\":%.2f,\"estoqueAnterior\":%d,\"estoqueAtual\":%d}",
                    esc(p.nome()), q, p.preco(), sub, p.estoque(), p.estoque() - q));
        }
        salvar();
        return String.format(Locale.US, "{\"numero\":%d,\"data\":\"%s\",\"total\":%.2f,\"itens\":[%s]}", numero, data, total, itens);
    }

    static void dadosIniciais() {
        produtos.add(new Produto(1, "Arroz 5 kg", 25.00, 10));
        produtos.add(new Produto(2, "Feijão carioca 1 kg", 9.20, 20));
        produtos.add(new Produto(3, "Óleo de soja 900 ml", 7.90, 4));
        produtos.add(new Produto(4, "Café torrado 500 g", 15.90, 12));
        produtos.add(new Produto(5, "Açúcar 1 kg", 5.50, 3));
    }

    // ---------- persistência em arquivos de texto ----------
    static void salvar() {
        try {
            List<String> p = new ArrayList<>(), v = new ArrayList<>();
            for (Produto x : produtos) p.add(String.format(Locale.US, "%d|%s|%.2f|%d", x.id(), x.nome(), x.preco(), x.estoque()));
            for (Venda x : vendas) v.add(String.format(Locale.US, "%d|%s|%s|%d|%.2f|%.2f", x.numero(), x.data(), x.produto(), x.quantidade(), x.unitario(), x.total()));
            Files.write(PRODUTOS, p, StandardCharsets.UTF_8);
            Files.write(VENDAS, v, StandardCharsets.UTF_8);
        } catch (IOException e) { throw new RuntimeException("Não foi possível salvar os dados.", e); }
    }

    static void carregar() throws IOException {
        if (Files.exists(PRODUTOS)) for (String l : Files.readAllLines(PRODUTOS, StandardCharsets.UTF_8)) {
            String[] c = l.split("\\|");
            produtos.add(new Produto(Integer.parseInt(c[0]), c[1], Double.parseDouble(c[2]), Integer.parseInt(c[3])));
        }
        if (Files.exists(VENDAS)) for (String l : Files.readAllLines(VENDAS, StandardCharsets.UTF_8)) {
            String[] c = l.split("\\|");
            vendas.add(new Venda(Integer.parseInt(c[0]), c[1], c[2], Integer.parseInt(c[3]), Double.parseDouble(c[4]), Double.parseDouble(c[5])));
        }
    }

    // ---------- API HTTP ----------
    static void pagina(HttpExchange ex) throws IOException {
        if (!ex.getRequestURI().getPath().equals("/")) { enviar(ex, 404, "text/plain", "Não encontrado"); return; }
        enviar(ex, 200, "text/html", Files.readString(Path.of("public/index.html")));
    }

    static void apiProdutos(HttpExchange ex) throws IOException {
        tratar(ex, () -> {
            if (ex.getRequestMethod().equals("GET")) {
                StringBuilder b = new StringBuilder("[");
                synchronized (Main.class) {
                    for (Produto p : produtos)
                        b.append(b.length() > 1 ? "," : "").append(String.format(Locale.US,
                                "{\"id\":%d,\"nome\":\"%s\",\"preco\":%.2f,\"estoque\":%d,\"baixo\":%b}",
                                p.id(), esc(p.nome()), p.preco(), p.estoque(), p.estoque() <= ESTOQUE_BAIXO));
                }
                enviar(ex, 200, "application/json", b.append("]").toString());
            } else if (ex.getRequestMethod().equals("POST")) {
                cadastrar(formulario(ex));
                enviar(ex, 201, "application/json", "{\"ok\":true}");
            } else enviar(ex, 405, "application/json", "{\"erro\":\"Método não permitido.\"}");
        });
    }

    static void apiVendas(HttpExchange ex) throws IOException {
        tratar(ex, () -> {
            if (ex.getRequestMethod().equals("GET")) {
                StringBuilder b = new StringBuilder("[");
                synchronized (Main.class) {
                    for (int i = vendas.size() - 1; i >= 0; i--) {
                        Venda v = vendas.get(i);
                        b.append(b.length() > 1 ? "," : "").append(String.format(Locale.US,
                                "{\"numero\":%d,\"data\":\"%s\",\"produto\":\"%s\",\"quantidade\":%d,\"total\":%.2f}",
                                v.numero(), v.data(), esc(v.produto()), v.quantidade(), v.total()));
                    }
                }
                enviar(ex, 200, "application/json", b.append("]").toString());
            } else if (ex.getRequestMethod().equals("POST")) {
                enviar(ex, 201, "application/json", vender(formulario(ex)));
            } else enviar(ex, 405, "application/json", "{\"erro\":\"Método não permitido.\"}");
        });
    }

    static void apiResumo(HttpExchange ex) throws IOException {
        tratar(ex, () -> {
            synchronized (Main.class) {
                long baixo = produtos.stream().filter(p -> p.estoque() <= ESTOQUE_BAIXO).count();
                enviar(ex, 200, "application/json", "{\"produtos\":" + produtos.size() + ",\"vendas\":" + vendas.stream().mapToInt(Venda::numero).distinct().count() + ",\"baixo\":" + baixo + "}");
            }
        });
    }

    interface Acao { void executar() throws IOException; }

    static void tratar(HttpExchange ex, Acao a) throws IOException {
        try { a.executar(); }
        catch (IllegalArgumentException e) { enviar(ex, 400, "application/json", "{\"erro\":\"" + esc(e.getMessage()) + "\"}"); }
        catch (Exception e) { e.printStackTrace(); enviar(ex, 500, "application/json", "{\"erro\":\"Erro interno. Tente novamente.\"}"); }
    }

    static Map<String, String> formulario(HttpExchange ex) throws IOException {
        String corpo = new String(ex.getRequestBody().readNBytes(10_000), StandardCharsets.UTF_8);
        Map<String, String> m = new HashMap<>();
        for (String par : corpo.split("&")) {
            String[] kv = par.split("=", 2);
            if (kv.length == 2) m.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return m;
    }

    static void enviar(HttpExchange ex, int codigo, String tipo, String corpo) throws IOException {
        byte[] b = corpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", tipo + "; charset=utf-8");
        ex.sendResponseHeaders(codigo, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }

    static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " "); }
}
