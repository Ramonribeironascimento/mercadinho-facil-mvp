# Testes da API. Rode com o sistema ligado (localhost:8080).
$base = "http://localhost:8080"
function Post($url, $corpo) {
  try { Invoke-RestMethod -Method Post -Uri "$base$url" -Body $corpo -ContentType "application/x-www-form-urlencoded" }
  catch { ($_.ErrorDetails.Message | ConvertFrom-Json).erro }
}
"1) Venda valida (esperado: venda registrada):"; Post "/api/vendas" "itens=1:1"
"2) Estoque insuficiente (esperado: erro):"; Post "/api/vendas" "itens=1:9999"
"3) Quantidade invalida (esperado: erro):"; Post "/api/vendas" "itens=1:0"
"4) Resumo:"; Invoke-RestMethod "$base/api/resumo"