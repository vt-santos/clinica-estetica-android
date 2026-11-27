# Conformidade legal e de acessibilidade

Situação em 27/11/2026. Este documento acompanha a checklist de 19 itens (privacidade, LGPD, CDC e acessibilidade) e diz, para cada um, o que o app já faz e o que ainda depende da clínica.

> **Importante:** os textos legais do app são **modelos**. Eles precisam ser revisados por um advogado antes do uso com pacientes. Os dados da empresa ainda estão entre colchetes em `app/src/main/res/values/company.xml`.

Contexto que muda o peso de vários itens: o app roda **só no tablet da clínica**, e a internet é usada só para conferir CNPJ na Receita (desde a 0.6.0-beta.1). Quem usa é a equipe, não a paciente. Por isso, itens pensados para sites (cookies, analytics, embeds) aqui se resolvem por não existirem, e o app garante isso no build.

## Mapa dos 19 itens

| # | Item | Situação | Onde |
|---|---|---|---|
| 1 | Política de privacidade | ✅ Modelo pronto (LGPD) | Sobre › Aviso de Privacidade; botão "Ler o Aviso de Privacidade" na ficha. `res/raw/legal_privacidade.txt` |
| 2 | Termos de uso | ✅ Modelo pronto, para a equipe que usa o app | Sobre › Termos de Uso. `res/raw/legal_termos.txt` |
| 3 | Política de cookies | ✅ Declara que não há cookies nem rastreamento | Sobre › Cookies, Rastreamento e Serviços de Terceiros. `res/raw/legal_cookies.txt` |
| 4 | Aviso (banner) de cookies | ✅ Não se aplica ao app: não há cookies. ⚠️ Se a clínica tiver site, ele precisa de banner próprio | — |
| 5 | Política de reembolso | ✅ Modelo pronto (CDC arts. 20, 49 e 51) com prazos a definir | Sobre › Política de Cancelamento e Reembolso. `res/raw/legal_reembolso.txt` |
| 6 | Consentimento do usuário | ✅ Caixa separada e obrigatória de autorização para dados de saúde (LGPD art. 11, I), registrada no termo criptografado com a versão do aviso | `PatientFormActivity`, `ConsentContent.privacyAccepted` |
| 7 | Coletar só os dados necessários | ✅ Só nome e nascimento são obrigatórios; evento do calendário leva só "Maria S."; sem internet. ⚠️ Revisar com a dona se "Etnia" e "Telefone comercial" são mesmo necessários (etnia é dado sensível) | `Cards.shortName`, ficha |
| 8 | Rastreamento de analytics | ✅ Não existe. A internet só é usada pela consulta de CNPJ (só HTTPS, só o número do CNPJ) e um teste falha se outro código abrir conexão | `AndroidManifest.xml`, `CnpjLookup`, `AppFlowTest.onlyCnpjLookupReachesTheInternet` |
| 9 | Embeds de terceiros | ✅ Não existem. Calendário e WhatsApp só abrem por toque da equipe e estão explicados na política | `legal_cookies.txt` |
| 10 | Site/app acessível | ✅ Itens 11 a 14 + títulos anunciados como títulos pelo TalkBack + declaração de acessibilidade | Sobre › Acessibilidade |
| 11 | Texto alternativo | ✅ Botão voltar, logotipo, área de assinatura (diz se está vazia ou assinada), imagem da assinatura e cartões do Início | `ScreenActivity`, `SignatureView`, `Cards`, `DashboardFragment` |
| 12 | Contraste de cor | ✅ Texto cinza escurecido de 4,4:1 para 5,4:1; bordas de campos e chips de 1,6:1 para 3,7:1 (WCAG 2.1 AA) | `res/values/themes.xml` |
| 13 | Formulários pelo teclado | ✅ Campos de data e hora antes não recebiam foco do teclado; agora recebem e abrem com Enter. Listas abrem com Enter; cartões recebem foco | `Ui.pickerField`, `Ui.dropdown`, `Ui.card` |
| 14 | Botões claros | ✅ "Calendário" → "Pôr no calendário", "WhatsApp" → "Lembrar no WhatsApp", "Concluir" → "Concluir e receber". Botões com 48 dp | `Cards.appointment` |
| 15 | Sem avaliação falsa | 📋 Não é código. Ver checklist abaixo | — |
| 16 | Dados da empresa | 🟡 Tela Sobre pronta; **faltam os dados reais** (razão social, CNPJ, endereço, contato, responsável técnico, encarregado de dados) | `res/values/company.xml` |
| 17 | Direito das imagens | 📋 O app não guarda fotos. Ver checklist abaixo | — |
| 18 | Leis locais (LGPD, CDC) | 🟡 LGPD: consentimento, aviso, direitos e exportação dos dados (art. 18) prontos. CDC: política de reembolso e dados da empresa. Falta revisão jurídica | Sobre; Prontuário › Exportar dados |
| 19 | Outros riscos legais | 🟡 Ver "Riscos que continuam abertos" | — |

## O que foi feito no código

- **Tela Sobre** no menu lateral: dados da empresa, aviso de privacidade, termos de uso, política de reembolso, política de cookies e declaração de acessibilidade. Mostra um alerta enquanto houver dados entre colchetes.
- **Textos legais em arquivos simples** (`res/raw/legal_*.txt`), com marcadores como `{{CNPJ}}` trocados pelos dados de `company.xml`. O advogado revisa o texto; a dona preenche os dados em um só lugar.
- **Consentimento LGPD no termo**: parágrafo específico e destacado no texto assinado (versões para adulta e para responsável legal) e caixa de autorização separada e obrigatória. O pacote criptografado guarda se a caixa foi marcada e a versão do aviso.
- **Exportar dados** no prontuário: gera uma cópia em texto de tudo o que o app guarda da paciente, inclusive os termos, para entregar quando ela pedir acesso ou portabilidade (prazo legal de 15 dias).
- **Internet só para CNPJ**: desde a 0.6.0-beta.1, a permissão `INTERNET` existe apenas para a consulta de CNPJ na BrasilAPI (`CnpjLookup`), só por HTTPS. Sai do tablet apenas o número do CNPJ. CPF é validado só offline. Aviso de privacidade e política de cookies foram atualizados.
- **Acessibilidade**: contraste, descrições, títulos, teclado e rótulos de botões (itens 11 a 14).
- **Testes novos** em `AppFlowTest`: autorização LGPD obrigatória, termo com o parágrafo LGPD, exportação, tela Sobre, abertura de cada documento sem marcadores soltos, ausência de internet, teclado e descrições.

## Checklist para a clínica (fora do código)

**Dados e textos**
- [ ] Preencher `company.xml`: nome fantasia, razão social, CNPJ, endereço, telefone, e-mail, horário, responsável técnico, encarregado de dados e e-mail dele, cidade do foro.
- [ ] Definir as regras: prazo de guarda dos prontuários, antecedência para cancelar, regra para faltas e prazo de reembolso.
- [ ] Revisão jurídica dos 5 textos e dos dois termos de consentimento. Depois, mudar `legal_version` para "1.0".
- [ ] Deixar o aviso de privacidade impresso na recepção.

**Avaliações (item 15)**
- [ ] Não publicar avaliações inventadas, escritas pela equipe ou compradas, nem apagar só as negativas (propaganda enganosa: CDC arts. 37 e 67; Código de Autorregulamentação Publicitária do CONAR).
- [ ] Depoimentos de pacientes só com autorização por escrito, identificando que são reais.
- [ ] Não oferecer desconto em troca de avaliação positiva sem deixar isso claro.

**Imagens (item 17)**
- [ ] Fotos de antes e depois só com termo de autorização de uso de imagem separado, assinado pela paciente (ou responsável), dizendo onde a foto será usada e por quanto tempo. Imagem de rosto é dado pessoal e, ligada a tratamento, dado de saúde.
- [ ] Respeitar as regras de publicidade do conselho profissional de quem faz o procedimento (por exemplo, CFM, CFF, COFFITO, CFBM ou CRBM), que costumam limitar fotos de antes e depois.
- [ ] Usar no site e nas redes apenas fotos próprias ou de bancos com licença comprada; guardar o comprovante da licença.

**Site e redes (itens 3, 4, 8 e 9, se houver site)**
- [ ] Banner de cookies com opções "Aceitar" e "Recusar" antes de ativar Google Analytics, Meta Pixel ou anúncios.
- [ ] Política de privacidade e de cookies próprias do site, com CNPJ e contato no rodapé.
- [ ] Formulário de agendamento on-line acessível e só com os campos necessários.

## Riscos que continuam abertos (item 19)

Estes pontos já estavam no relatório (seção 5.4) e pesam na LGPD (art. 46, segurança):

1. **Sem senha no app.** Quem pegar o tablet desbloqueado vê tudo. Recomendado: bloqueio de tela no tablet já, e senha ou biometria no app depois.
2. **Só o termo está criptografado.** O resto da ficha fica legível no arquivo do banco. Próximo passo: SQLCipher.
3. **Sem backup.** Perda do tablet = perda dos prontuários. Também conta como incidente de segurança se o tablet for roubado (comunicar à ANPD e às pacientes, art. 48).
4. **APK de teste.** Gerar APK de produção assinado antes da entrega.
5. **Excluir não deixa registro** e pode apagar prontuário antes do prazo de guarda. Recomendado: confirmar com senha da dona e guardar um registro de exclusões.
6. **Responsabilidade técnica.** Procedimentos devem ser feitos dentro da competência legal da profissional e do seu conselho; isso não é algo que o app resolve.
