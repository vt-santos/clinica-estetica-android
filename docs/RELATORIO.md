# Clínica Estética: relatório do projeto

Situação em 20/11/2026. Este documento explica o que o app faz, como o código está organizado, como funcionam o banco de dados e a segurança, e o que ainda falta.

---

## 1. Visão geral

App Android (APK) para gestão de uma clínica de estética. Ele roda **sozinho no tablet**, sem servidor e sem internet, e foi testado em um Galaxy Tab S6 Lite (Android 13) com S Pen.

| Item | Escolha |
|---|---|
| Linguagem | Java 17 |
| Interface | Componentes Android + Material Design 3, montados por código |
| Banco de dados | SQLite, acessado pela biblioteca Room 2.8.5 |
| Criptografia | AES-256-GCM com chave guardada no Android Keystore |
| Versão mínima do Android | 8.0 (API 26). Compilado para API 36 |
| Testes | Robolectric 4.17, que roda as telas reais no computador |

### O que o app já faz

- **Pacientes:** cadastro com todos os campos da Ficha de Anamnese Facial, pesquisa por nome ou celular e prontuário em abas.
- **Verificação de idade:** a data de nascimento é obrigatória. Se a paciente tiver menos de 18 anos, o app exige os dados do responsável legal e troca o termo pelo termo para menores.
- **Termo e assinatura:** texto do termo, caixa de aceite, local e assinatura com caneta. Tudo é gravado junto, criptografado e codificado em Base64.
- **Nova assinatura em toda alteração:** uma ficha assinada não salva mudanças sem uma nova assinatura. O novo termo lista o que mudou, com o valor antigo e o novo.
- **Avaliação da pele:** todos os grupos da ficha em papel, com data e observações.
- **Sessões e protocolos:** produtos, equipamentos, supervisão e relatório, sem limite de sessões.
- **Agenda:** calendário, consultas e retornos, lembrete por notificação, atalho para o calendário do tablet e mensagem pronta no WhatsApp.
- **Pagamentos:** forma de pagamento, valor, situação (pago ou pendente) e total a receber.
- **Início:** número de pacientes, consultas de hoje, retornos nos próximos 7 dias e valor a receber.

---

## 2. Como gerar e instalar

- **Gerar o APK:** no terminal do VS Code, na pasta do projeto, rode `.\gerar-apk.ps1`. O arquivo sai em `dist\ClinicaEstetica-debug.apk`.
- **Instalar no tablet:** com o cabo USB e a depuração ativada, rode `adb install -r dist\ClinicaEstetica-debug.apk`. O `-r` atualiza por cima e **mantém os dados**.
- **Rodar os testes:** `.\gradlew.bat testDebugUnitTest`.
- **Pasta de build:** fica em `%LOCALAPPDATA%\ClinicaEstetica\build`, fora do OneDrive, porque a sincronização travava os arquivos.

> **Nunca desinstale o app para reinstalar.** A desinstalação apaga o banco e a chave de criptografia. Atualize sempre por cima.

---

## 3. Organização do código

```
app/src/main/java/br/com/clinicaestetica/
├── data/       banco de dados, criptografia e regras de dados
├── ui/         telas e componentes visuais
└── reminder/   alarmes e notificações dos lembretes
app/src/main/res/          textos, cores, ícones e layouts
app/src/test/java/...      teste de ponta a ponta (AppFlowTest)
```

### 3.1 Pacote `data`: dados, banco e segurança

#### Entidades (cada uma vira uma tabela)

| Arquivo | O que representa |
|---|---|
| `Patient.java` | Ficha da paciente: dados pessoais, contato de emergência, histórico de saúde e responsável legal. Tem um índice no nome, para a pesquisa ser rápida |
| `SkinAssessment.java` | Uma avaliação da pele: data, itens marcados (em JSON) e observações |
| `TreatmentSession.java` | Uma sessão de tratamento: data, protocolo, equipamentos (em JSON), outro equipamento, supervisão e relatório |
| `Appointment.java` | Consulta ou retorno: data e hora, tipo, observações, se foi concluída e antecedência do lembrete (negativo significa sem lembrete) |
| `Payment.java` | Pagamento: data, valor em centavos, forma, descrição e se está pago |
| `Consent.java` | Termo assinado. Só a data fica legível; todo o resto fica no `sealedPackage`, criptografado. O campo `content` existe apenas na memória, até a ficha ser salva |

O valor em dinheiro é guardado **em centavos** (`long`) para evitar erros de arredondamento.

#### Classes auxiliares

| Arquivo | Para que serve |
|---|---|
| `AppointmentRow.java` e `PaymentRow.java` | Resultado de consultas que juntam a consulta ou o pagamento com o nome da paciente, para não precisar de uma segunda busca |
| `ConsentContent.java` | Conteúdo do termo assinado: texto aceito, local, se aceitou, nome, papel e CPF de quem assinou, e a imagem PNG. `toJson()` transforma tudo em JSON, com a imagem em Base64. `fromJson()` faz o caminho inverso |
| `ConsentVault.java` | O cofre de criptografia, explicado na seção 5 |
| `Cpf.java` | `digits()` tira pontos e traço, `isValid()` confere os dois dígitos verificadores e rejeita CPFs com todos os números iguais, e `format()` exibe como `529.982.247-25` |
| `FormCatalog.java` | Conteúdo das fichas em papel: as 21 perguntas do histórico, os 11 grupos da avaliação da pele, os equipamentos, as formas de pagamento, os tipos de consulta, as opções de lembrete, a idade adulta (18) e os parentescos |
| `Json.java` | Ajudantes de JSON: `object()` lê um texto (vazio vira objeto vazio), `array()` grava uma lista e `stringSet()` lê uma lista |
| `Async.java` | Executa o acesso ao banco em uma fila separada e devolve o resultado na tela. `load()` serve para buscar algo e `run()` para gravar. O Android trava a tela se o banco for usado na thread principal |

#### `AppDao.java`: todas as consultas ao banco

**Pacientes**
- `insert`, `update` e `delete` gravam, atualizam e apagam.
- `patient(id)` busca uma paciente pelo id.
- `allPatients()` lista todas em ordem alfabética, sem diferenciar maiúsculas.
- `searchPatients(term)` pesquisa por parte do nome ou do celular.
- `patientCount()` conta as pacientes.

**Termos**
- `insert(Consent)` grava um termo.
- `latestConsent(patientId)` busca o termo mais recente.
- `consents(patientId)` traz o histórico, do mais novo para o mais antigo.
- `savePatient(patient, consent)` grava a ficha e o termo em uma única transação: ou os dois são salvos, ou nenhum. É aqui que o termo é criptografado, já com o id definitivo da paciente.

**Avaliações e sessões**
- `insert`, `update`, `delete`, `assessment(id)`, `assessments(patientId)`, `session(id)` e `sessions(patientId)`.

**Agenda**
- `insert`, `update`, `delete` e `appointment(id)`.
- `pendingAppointments(from)` lista as consultas futuras não concluídas; é usada para recriar os alarmes quando o tablet liga.
- `appointmentRow(id)` traz a consulta com o nome da paciente, para montar a notificação.
- `appointmentsBetween(from, to)` traz as consultas de um dia.
- `appointmentsOf(patientId)` traz as consultas de uma paciente.
- `returnCount(from, to)` conta os retornos pendentes em um período.

**Pagamentos**
- `insert`, `update`, `delete` e `payment(id)`.
- `allPayments()` lista primeiro os pendentes.
- `paymentsOf(patientId)` traz os pagamentos de uma paciente.
- `pendingCents()` soma o total a receber.

#### `AppDatabase.java`: abertura do banco e migrações

- `dao(context)` abre o banco `clinica.db` uma única vez e o reaproveita.
- As migrações (`MIGRATION_1_2`, `MIGRATION_2_3` e `MIGRATION_3_4`) atualizam o banco de quem já usa o app **sem apagar dados**. A seção 4.3 explica cada uma.

### 3.2 Pacote `ui`: telas

#### Estrutura comum

| Arquivo | Função |
|---|---|
| `MainActivity.java` | Tela principal com o menu lateral (Início, Pacientes, Agenda, Pagamentos e Sobre). Troca as seções e pede permissão de notificação no Android 13 ou mais novo |
| `ScreenFragment.java` | Base das seções do menu. Cria a área rolável e chama `refresh()` sempre que a seção volta a aparecer, para os dados estarem sempre atualizados |
| `ScreenActivity.java` | Base das telas de formulário: barra superior com voltar, área rolável e `addAction()` para botões como Salvar e Excluir |
| `Ui.java` | Peças de interface reutilizadas em todo o app (detalhes abaixo) |
| `Fmt.java` | Formatação brasileira (detalhes abaixo) |
| `Cards.java` | Cartões compartilhados (detalhes abaixo) |

**`Ui.java`**
- `row()` monta uma linha que vira coluna em telas estreitas; é isso que deixa o app responsivo.
- `field()` e `multiline()` criam campos de texto.
- `dateField()` e `timeField()` criam campos que abrem o seletor de data ou de hora. O campo de data aceita uma ação para quando a data mudar, usada na verificação de idade.
- `dropdown()` cria uma lista de opções; `picker()` cria uma lista pesquisável, usada para escolher a paciente.
- `chips()` e `checked()` criam botões marcáveis e leem quais estão marcados.
- `card()` cria um cartão com título, subtítulo e linha de ações.
- `button()` e `action()` criam botões; `confirm()` pede confirmação; `toast()` mostra um aviso rápido.
- `onChange()` executa uma ação sempre que um texto muda.
- `value()` lê o texto já sem espaços nas pontas; `dp()` converte medidas para a densidade da tela.

**`Fmt.java`**
- `date()`, `time()` e `dateTime()` formatam datas e horas.
- `startOfDay()` e `plusDays()` fazem contas com dias.
- `age()` calcula a idade em anos completos.
- `money()` mostra valores como `R$ 1.500,50`.
- `parseCents()` entende "150", "150,50" e "1.500,50" e devolve -1 se o valor for inválido.

**`Cards.java`**
- `appointment()` mostra uma consulta com os botões Pôr no calendário, Lembrar no WhatsApp e Concluir e receber. O evento do calendário leva só o primeiro nome e a inicial do sobrenome.
- `payment()` mostra um pagamento com o botão Marcar como pago.
- `consent()` abre o termo criptografado fora da thread principal e mostra o texto, quem assinou e a imagem. Se a chave não estiver no aparelho, mostra um aviso em vez de travar.
- `addToCalendar()` abre o calendário do tablet com a consulta já preenchida.
- `openWhatsApp()` abre o WhatsApp com a mensagem de lembrete pronta. Se o número não tiver o código do país, ele acrescenta 55.

#### Seções do menu

| Arquivo | Função |
|---|---|
| `DashboardFragment.java` | Início: quatro cartões com as cores da paleta e a lista de consultas do dia |
| `PatientsFragment.java` | Lista de pacientes com busca, que atualiza a cada letra digitada |
| `AgendaFragment.java` | Calendário mensal à esquerda e consultas do dia escolhido à direita |
| `PaymentsFragment.java` | Total a receber e lista de pagamentos |

#### Formulários

| Arquivo | Função |
|---|---|
| `PatientFormActivity.java` | Ficha de anamnese, termo e assinatura (detalhes abaixo) |
| `PatientDetailActivity.java` | Prontuário em abas: Ficha, Avaliações, Sessões, Consultas e Pagamentos. Na aba Ficha, mostra o responsável legal, o termo atual e as assinaturas anteriores |
| `PatientChanges.java` | `between()` compara duas versões da ficha campo a campo, incluindo o histórico e o responsável, e devolve frases como "Profissão: (vazio) → Dentista". `answer()` escreve uma resposta do histórico como "Sim · Dipirona" |
| `SkinAssessmentActivity.java` | Avaliação da pele, com botões marcáveis por grupo. Hidratação, oleosidade e espessura aceitam só uma opção |
| `SessionFormActivity.java` | Registro de uma sessão de tratamento |
| `AppointmentFormActivity.java` | Nova consulta ou edição. Agenda o lembrete ao salvar e cancela ao excluir |
| `PaymentFormActivity.java` | Registro de pagamento, com validação do valor |
| `SignatureView.java` | Campo de assinatura (detalhes abaixo) |

**`PatientFormActivity.java`**
- `buildForm()` monta a ficha com os mesmos campos do papel.
- `buildGuardian()` monta a seção do responsável legal, que só aparece para menores.
- `buildConsent()` monta o termo, a caixa de aceite, o local e o campo de assinatura.
- `onBirthDateChanged()` mostra a idade, exibe ou esconde o responsável e troca o título da assinatura.
- `refreshTerm()` mantém o termo da tela igual ao que será gravado. Se o texto mudar depois da assinatura, a assinatura é apagada, porque foi feita para outro texto.
- `fromForm()` monta uma cópia da paciente com o que está na tela, sem alterar a original. É assim que o app sabe o que mudou.
- `validate()` confere nome, data de nascimento (obrigatória e não futura) e, para menores, nome, CPF válido e parentesco do responsável.
- `save()` decide o que fazer: fecha a tela se nada mudou, exige nova assinatura se algo mudou ou se o termo da tela está desatualizado, e grava a ficha junto com o termo.
- `termFor()` escolhe o termo de adulto ou de menor e acrescenta o aviso de alteração quando é o caso.
- `newConsent()` monta o pacote do termo com texto, local, aceite, quem assinou e imagem.

**`SignatureView.java`**
- Desenha o traço com suavização: `quadTo()` usa todos os pontos intermediários que a caneta envia.
- **Rejeição da palma:** assim que a S Pen é detectada, mesmo apenas passando por cima da tela, o toque do dedo ou da palma passa a ser ignorado.
- `toPng()` gera a imagem final em fundo branco.

### 3.3 Pacote `reminder`: lembretes

| Arquivo | Função |
|---|---|
| `ReminderScheduler.java` | `schedule()` agenda um alarme para a antecedência escolhida; se a consulta já passou ou foi concluída, cancela. `cancel()` remove o alarme |
| `ReminderReceiver.java` | Na hora do alarme, busca a consulta no banco e, se ela ainda existe e não foi concluída, mostra a notificação |
| `BootReceiver.java` | O Android apaga os alarmes quando o tablet desliga. Este receptor recria todos ao ligar |

---

## 4. Banco de dados

### 4.1 Onde fica

O arquivo é `clinica.db`, na área privada do app (`/data/data/br.com.clinicaestetica/databases/`). Outros apps e o gerenciador de arquivos não têm acesso. O backup automático do Android está **desligado** (`allowBackup="false"`), para os dados de saúde não irem para a nuvem sem controle.

### 4.2 Tabelas

Todas as tabelas filhas apontam para `patients`, com **exclusão em cascata**: apagar uma paciente apaga as avaliações, sessões, consultas, pagamentos e termos dela.

**patients**
- `id`, `createdAt` e `name`, este com índice para a busca.
- Dados pessoais: `sex`, `birthDate`, `address`, `neighborhood`, `city`, `cep`, `cellPhone`, `homePhone`, `workPhone`, `email`, `ethnicity`, `maritalStatus`, `profession`, `referral` e `visitReason`.
- Emergência: `emergencyName`, `emergencyPhone`, `doctorName`, `doctorPhone`, `healthPlan`, `healthPlanCard` e `hospital`.
- `historyJson` guarda as respostas do histórico, no formato `{"allergies": {"a": "S", "d": "Dipirona"}}`.
- Responsável legal: `guardianName`, `guardianCpf` (só números), `guardianRelationship` e `guardianPhone`.

**skin_assessments**
- `id`, `patientId`, `date`, `selectionsJson` (itens marcados por grupo) e `notes`.

**treatment_sessions**
- `id`, `patientId`, `date`, `protocol`, `equipmentJson`, `otherEquipment`, `supervision` e `report`.

**appointments**
- `id`, `patientId`, `startsAt`, `type`, `notes`, `done` e `reminderMinutes`, com índices em `patientId` e `startsAt`.

**payments**
- `id`, `patientId`, `date`, `amountCents`, `method`, `description` e `paid`.

**consents**
- `id`, `patientId` e `signedAt`, que ficam legíveis.
- `sealedPackage`, o termo inteiro criptografado e em Base64.

As datas são guardadas em milissegundos desde 1970, o formato padrão do Java.

### 4.3 Versões e migrações

Cada mudança na estrutura do banco aumenta a versão. A migração converte o banco de quem já usa o app sem perder nada.

| Versão | O que mudou |
|---|---|
| 1 | Primeira versão: pacientes, avaliações, sessões, consultas e pagamentos |
| 2 | Criou a tabela `consents`, ainda com o texto e a imagem sem criptografia |
| 3 | Criptografou a imagem da assinatura |
| 4 | **Atual.** Juntou texto, local, aceite, assinante e imagem em um único pacote criptografado em Base64. Também criou as colunas do responsável legal |

A migração 3 → 4 foi testada no tablet com a assinatura real feita com a S Pen. Antes, o texto do termo aparecia legível no arquivo do banco. Depois, nem o texto nem a imagem aparecem mais, e o prontuário continua abrindo tudo normalmente. Se alguma assinatura antiga não puder ser aberta, a migração guarda o termo sem a imagem, em vez de travar o app.

---

## 5. Segurança

### 5.1 Como o termo é protegido

Quando a ficha é salva, `AppDao.savePatient()` faz este caminho:

1. `ConsentContent.toJson()` junta em um JSON o texto do termo, o local, o aceite, o nome, o papel e o CPF de quem assinou, e a imagem da assinatura em Base64.
2. `ConsentVault.seal()` criptografa esse JSON inteiro com **AES-256-GCM**.
3. O resultado vira **Base64** e é gravado como texto na coluna `sealedPackage`.

Formato gravado: `Base64( IV de 12 bytes + texto cifrado + etiqueta de 16 bytes )`.

**Termos usados acima:**
- **AES-256:** algoritmo de criptografia padrão de mercado, com chave de 256 bits.
- **GCM:** modo que, além de esconder o conteúdo, detecta qualquer alteração. Se um único bit do pacote for mudado, ele se recusa a abrir.
- **IV:** número aleatório novo a cada termo. Dois termos iguais geram pacotes completamente diferentes.
- **Base64:** só uma forma de escrever bytes como texto, para caberem numa coluna de texto. **Não é criptografia.** Quem protege é o AES; o Base64 é só a embalagem.

### 5.2 Onde fica a chave

- **Android Keystore:** a chave (`clinica_signature_key`) é gerada dentro do cofre de chaves do Android. **Nem o próprio app consegue ler a chave**; ele só pede ao cofre para cifrar e decifrar. A chave não pode ser copiada para fora do aparelho.
- **Vínculo com a paciente:** o id da paciente entra na autenticação do GCM. Um termo copiado para o registro de outra paciente não abre.

### 5.3 Regras de assinatura

- **Ficha nova:** só é salva com o aceite marcado e a assinatura feita.
- **Ficha alterada:** só é salva com nova assinatura. O termo novo lista cada alteração com o valor antigo e o novo.
- **Termo muda depois da assinatura:** se o texto mudar, por exemplo ao corrigir o nome ou o CPF do responsável, a assinatura e o aceite são apagados. Ninguém consegue assinar um texto e gravar outro.
- **Transação única:** ficha e termo são gravados juntos. Se um falhar, nenhum é salvo.
- **Histórico:** as assinaturas anteriores nunca são apagadas pelo app, só substituídas pela mais recente na tela.
- **Menor de idade:** a data de nascimento é obrigatória. Para menores de 18 anos, o responsável precisa ter nome, CPF válido e parentesco, e quem assina é ele. O pacote registra nome, papel e CPF do responsável.

### 5.4 O que ainda NÃO está protegido

Estes pontos precisam ser resolvidos antes de usar dados reais:

1. **Sem backup.** Se o tablet quebrar ou for perdido, tudo se perde. Se o app for desinstalado ou o tablet for restaurado de fábrica, a chave some e os termos nunca mais abrem.
2. **Só o termo está criptografado.** Nome, telefones, endereço e histórico de saúde ficam legíveis no arquivo do banco. Para proteger tudo, o próximo passo seria criptografar o banco inteiro com SQLCipher.
3. **Sem senha no app.** Quem pegar o tablet desbloqueado abre tudo.
4. **APK de teste.** Esta versão permite que um computador conectado por USB leia o banco; foi assim que conferi a migração. Para a cliente, é preciso gerar um **APK de produção**, assinado, que bloqueia esse acesso.
5. **Excluir apaga tudo.** O botão Excluir remove a paciente e todos os termos, sem registro. Seria bom exigir senha da dona ou guardar um registro de exclusões.
6. **Só a anamnese exige assinatura.** Avaliações, sessões, consultas e pagamentos podem ser alterados livremente.
7. **Valor jurídico.** A assinatura na tela é um registro de aceite com data, texto e imagem protegidos, mas não é uma assinatura eletrônica certificada (ICP-Brasil). O texto dos termos foi escrito por mim como rascunho e precisa de revisão jurídica.

A situação de cada exigência legal e de acessibilidade (LGPD, CDC, WCAG) está em [CONFORMIDADE.md](CONFORMIDADE.md).

---

## 6. Testes

`AppFlowTest.java` percorre o app inteiro com as telas reais, no computador:

- **Cadastro:** campos obrigatórios (nome e data de nascimento), aceite e assinatura.
- **Caneta:** o traço da caneta conta e o toque da palma é ignorado depois que a caneta aparece.
- **Pacote criptografado:** o termo não fica legível no banco, o Base64 é válido, o conteúdo abre com a chave e o pacote de uma paciente não abre com o id de outra.
- **Alteração da ficha:** o app exige nova assinatura e mudar algo depois de assinar apaga a assinatura. O histórico fica com 2 termos.
- **Menor de idade:** o responsável é obrigatório, o CPF inválido é recusado, o termo de menor traz nome, CPF e parentesco, e corrigir o CPF apaga a assinatura antiga.
- **Demais telas:** avaliação, sessão, consulta, alarme e notificação, pagamento, prontuário, busca, agenda e "marcar como pago".
- **Valores em reais:** conversão de textos como "1.500,50".

No tablet de verdade foram testados: cadastro, agenda, botão Calendário, notificação real, assinatura com a S Pen, nova assinatura ao alterar a ficha e a migração do banco com a chave real do aparelho. O WhatsApp não foi testado porque não está instalado no tablet.

---

## 7. Próximos passos sugeridos

1. **Backup criptografado com senha:** cópia semanal automática para uma pasta escolhida (Google Drive ou cartão SD), botão "fazer agora" e restauração em outro aparelho. Isso também resolve a chave presa ao tablet.
2. **Senha ou biometria para abrir o app**, e senha da dona para excluir pacientes.
3. **Banco inteiro criptografado** (SQLCipher).
4. **Novo visual**, mais limpo e intuitivo, no estilo do app Claude.
5. **APK de produção assinado** para entregar à cliente.
6. **Revisão jurídica** dos termos.
7. **Programa de consulta no notebook**, depois que o backup existir.
