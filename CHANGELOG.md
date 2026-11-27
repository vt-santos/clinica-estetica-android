# 📋 Histórico de versões

Todas as mudanças do app ficam registradas aqui, da mais nova para a mais antiga.

| Tipo | Como é a versão | Quando usamos |
| :-- | :-- | :-- |
| 🧪 **Beta** | `0.4.0-beta.1` | Funções novas, ainda em teste no tablet da clínica |
| 🩹 **Patch** | `0.2.2` | Correções e ajustes pequenos, sem função nova |

O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e as versões seguem o [Versionamento Semântico](https://semver.org/lang/pt-BR/). Cada versão abaixo vira automaticamente uma [Release no GitHub](https://github.com/vt-santos/clinica-estetica-android/releases) quando chega ao `main`.

<!--
Como publicar uma versão nova:
1. Adicione uma seção no topo, no formato "## [X.Y.Z] - AAAA-MM-DD" (patch) ou
   "## [X.Y.Z-beta.N] - AAAA-MM-DD" (beta).
2. Atualize versionName e versionCode em app/build.gradle.
3. Ao fazer o merge no main, o GitHub Actions cria a tag, a release e anexa o APK.
A linha "commit:" só é usada nas versões antigas, para a tag apontar para o commit certo.
-->
## [0.7.0-beta.1] - 2026-11-27

### ✨ Novidades
  - **Filtro do mês na Agenda**: botões **Dia** e **Mês inteiro**. No modo mês aparecem todas as consultas do mês escolhido, com um resumo no topo: total, realizadas e pagas, que já passaram sem pagamento e as que ainda vão acontecer.
- Cada consulta mostra se está **concluída e paga** ou **sem pagamento**, na Agenda, no Início e na ficha da paciente.

## [0.6.0-beta.1] - 2026-11-27

### ✨ Novidades
- Campo **CPF ou CNPJ** na ficha da paciente (opcional), ao lado do e-mail.
  - A **máscara é automática**: é só digitar os números, e o app põe pontos, barra e traço.
  - A **verificação é automática**: assim que o número fica completo, o app confere os dígitos verificadores e mostra embaixo do campo se ele é válido.
  - **Número repetido**: se o CPF já está na ficha de outra paciente, o app avisa e não deixa salvar. CNPJ repetido só gera aviso, porque mais de uma paciente pode usar a mesma empresa.
  - **CNPJ na Receita Federal**: com internet, o app consulta o CNPJ na BrasilAPI e mostra a razão social, o nome fantasia e a situação (ativa, baixada, inapta…). CNPJ inexistente ou que não está ativo aparece em vermelho.
  - Aceita o **CNPJ alfanumérico**, com letras, que a Receita começou a emitir em julho de 2026.
- O CPF/CNPJ aparece no prontuário, na exportação de dados e na busca de pacientes.

### 🛠️ Melhorias
- O Aviso de Privacidade (versão 0.2) e a política de Cookies e Serviços de Terceiros explicam a consulta de CNPJ.

### 🔒 Privacidade
- O app passa a ter permissão de internet, usada **só** para a consulta de CNPJ, e só por HTTPS. Sai do tablet apenas o número do CNPJ, nunca dados da paciente. O **CPF nunca é consultado fora do tablet**: não existe serviço gratuito da Receita para isso, e é dado pessoal.
- Um teste automático falha se qualquer outra parte do app abrir conexão com a internet.

### 🔧 Interno
- Banco de dados na versão 5: coluna `taxId` com índice. As fichas antigas continuam iguais, com o campo vazio.
- Testes novos para CPF, CNPJ numérico e alfanumérico, máscara, número repetido e resposta da Receita (simulada, sem internet).

## [0.5.0-beta.1] - 2026-11-27

### ✨ Novidades
- Tela **Sobre** no menu, com os dados da empresa, Aviso de Privacidade, Termos de Uso, Política de Cancelamento e Reembolso, Cookies e Declaração de Acessibilidade. Os textos são modelos e precisam de revisão jurídica.
- **Consentimento LGPD** para dados de saúde no termo, em caixa própria e obrigatória, guardado no pacote criptografado com a versão do aviso.
- **Exportar dados** da paciente no prontuário (LGPD art. 18).

### 🛠️ Melhorias
- **Acessibilidade (WCAG AA)**: contraste maior, descrições para o TalkBack, títulos e uso completo por teclado físico. Botões com nomes mais claros, como "Concluir e receber".

### 🔧 Interno
- Mapa dos 19 itens de conformidade em `docs/CONFORMIDADE.md`.

## [0.4.0-beta.1] - 2026-11-27

### ✨ Novidades
- Nova pergunta na anamnese: **"Tem hipertensão (pressão alta)?"**, logo abaixo de "Tem diabetes?".

### 🛠️ Melhorias
- As perguntas do histórico agora ficam **agrupadas por assunto**, com um título para cada grupo:
  - **Condições de saúde**: diabetes, hipertensão, problemas cardíacos, marcapasso, epilepsia, antecedentes oncológicos e alérgicos.
  - **Tratamentos, medicamentos e próteses**: tratamento médico, medicamentos, próteses metálicas e dentárias.
  - **Saúde da mulher**: gestação, ciclo menstrual e anticoncepcional.
  - **Hábitos de vida**: fumo, alimentação, intestino, esportes e sol.
  - **Pele e cuidados estéticos**: tratamentos anteriores, ácidos e cuidados diários.
- O prontuário da paciente mostra as respostas com os mesmos grupos.
- O campo **"Qual?" / "Quais?"** só fica disponível quando a resposta é **Sim**. Com **Não** (ou sem resposta) ele fica bloqueado e o texto é apagado. Campos de "Observações" e "Filhos? Quantos?" continuam liberados, porque valem para qualquer resposta.
- As fichas já gravadas abrem com as mesmas respostas de antes.

### 🔧 Interno
- O build funciona fora do Windows, o que permite rodar os testes e gerar o APK no GitHub Actions.
- Testes novos para o agrupamento e para o bloqueio do campo "Qual?".

## [0.3.0-beta.1] - 2026-11-20
<!-- commit: a9d77ce -->

### ✨ Novidades
- **Aviso de retorno**: 28 dias depois de cada consulta concluída, às 9h, o tablet lembra de chamar a cliente. Tocar no aviso abre o WhatsApp com a mensagem pronta.
- **Concluir consulta** abre o registro do pagamento: a consulta só fica concluída quando o pagamento é salvo.

### 🛠️ Melhorias
- Os cartões do Início levam direto para Pacientes, Agenda e Pagamentos.
- "A receber" virou "Recebido no mês".

## [0.2.2] - 2026-11-20
<!-- commit: 6d00fdb -->

### 🎨 Visual
- Ícone novo do app com o "M" do logo, centralizado para não ser cortado pelo launcher.
- O "M" também aparece no topo do menu lateral.

## [0.2.1] - 2026-11-20
<!-- commit: e0213e1 -->

### 🐛 Correções
- Os botões **OK** e **Cancelar** dos seletores de data e hora apareciam brancos sobre fundo branco no tablet Samsung. Agora ficam visíveis.

### 🎨 Visual
- Tema novo e mais limpo: fundo cor de papel, cartões brancos, campos e botões arredondados, títulos com serifa.
- Conteúdo centralizado em telas largas e saudação no Início conforme a hora do dia.

## [0.2.0-beta.1] - 2026-11-20
<!-- commit: c294d31 -->

### 🔒 Segurança
- O termo, o aceite e a assinatura ficam **criptografados juntos** (AES-256-GCM, chave guardada no Android Keystore).

### ✨ Novidades
- **Verificação de idade**: a data de nascimento passa a ser obrigatória. Para menores de 18 anos, o app pede o responsável legal (nome, CPF e parentesco) e usa o termo próprio para menores.
- O termo na tela se atualiza na hora, e a assinatura é apagada se o texto mudar.
- O prontuário mostra o responsável legal e o histórico de assinaturas.

## [0.1.0-beta.1] - 2026-11-18
<!-- commit: 8f2fdb9 -->

### ✨ Primeira versão
- Cadastro de pacientes com a **Ficha de Anamnese Facial** completa.
- Avaliação da pele, sessões de tratamento e equipamentos usados.
- Agenda com lembretes por notificação, que continuam valendo depois que o tablet reinicia.
- Registro de pagamentos.
- Termo de consentimento com **assinatura digital** feita na tela.

[0.4.0-beta.1]: https://github.com/vt-santos/clinica-estetica-android/compare/v0.3.0-beta.1...v0.4.0-beta.1
[0.3.0-beta.1]: https://github.com/vt-santos/clinica-estetica-android/compare/v0.2.2...v0.3.0-beta.1
[0.2.2]: https://github.com/vt-santos/clinica-estetica-android/compare/v0.2.1...v0.2.2
[0.2.1]: https://github.com/vt-santos/clinica-estetica-android/compare/v0.2.0-beta.1...v0.2.1
[0.2.0-beta.1]: https://github.com/vt-santos/clinica-estetica-android/compare/v0.1.0-beta.1...v0.2.0-beta.1
[0.1.0-beta.1]: https://github.com/vt-santos/clinica-estetica-android/releases/tag/v0.1.0-beta.1
