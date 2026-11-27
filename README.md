<div align="center">

<img src="app/src/main/res/drawable-nodpi/logo_m.png" alt="Logo do app" width="96">

# Clínica Estética

**App de gestão para clínicas de estética, feito para rodar no tablet da clínica**

Sem servidor, sem mensalidade: todos os dados ficam no próprio aparelho. A internet só é usada para conferir CNPJ na Receita.

[![Última versão estável](https://img.shields.io/github/v/release/vt-santos/clinica-estetica-android?label=patch%20est%C3%A1vel&logo=github&color=2E7D32)](https://github.com/vt-santos/clinica-estetica-android/releases/latest)
[![Última beta](https://img.shields.io/github/v/release/vt-santos/clinica-estetica-android?include_prereleases&label=beta&logo=github&color=8E44AD)](https://github.com/vt-santos/clinica-estetica-android/releases)
[![Testes](https://img.shields.io/github/actions/workflow/status/vt-santos/clinica-estetica-android/testes.yml?branch=main&label=testes&logo=githubactions&logoColor=white)](https://github.com/vt-santos/clinica-estetica-android/actions/workflows/testes.yml)

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Room](https://img.shields.io/badge/Room-SQLite-003B57?logo=sqlite&logoColor=white)
![Dados no tablet](https://img.shields.io/badge/dados-s%C3%B3%20no%20tablet-333333)

[Funcionalidades](#-funcionalidades) · [Versões](#-versões) · [Tecnologias](#-tecnologias) · [Como gerar o APK](#-como-gerar-o-apk)

</div>

---

## ✨ Funcionalidades

| | |
| :-- | :-- |
| 👩 **Pacientes** | Cadastro com a Ficha de Anamnese Facial completa e histórico de cada paciente. |
| 🪪 **CPF ou CNPJ** | Campo com máscara automática. Confere os dígitos na hora, avisa se o número já está em outra ficha e consulta o CNPJ na Receita Federal (razão social e situação). Aceita o CNPJ alfanumérico novo. |
| 🩺 **Anamnese organizada** | Perguntas agrupadas por assunto (condições de saúde, medicamentos, saúde da mulher, hábitos, pele). O campo "Qual?" só abre quando a resposta é **Sim**. |
| 🔍 **Avaliação da pele** | Registro de manchas, lesões, hidratação, oleosidade e espessura. |
| 💆 **Sessões de tratamento** | Protocolos, produtos e equipamentos usados em cada sessão. |
| 📅 **Agenda** | Agendamentos com lembretes por notificação, que continuam valendo depois que o tablet reinicia. |
| 🔁 **Aviso de retorno** | 28 dias depois da consulta, o app lembra de chamar a cliente pelo WhatsApp. |
| 💰 **Pagamentos** | A consulta só é concluída quando o pagamento é registrado. |
| ✍️ **Termo de consentimento** | Assinatura digital na tela, criptografada junto com o termo. Se a ficha mudar, o app pede uma nova assinatura e mostra o que mudou. |
| 👨‍👧 **Menores de idade** | Verificação de idade e termo assinado pelo responsável legal. |
| 🛡️ **LGPD e políticas** | Consentimento específico para dados de saúde, aviso de privacidade, termos de uso, política de reembolso, exportação dos dados da paciente e tela Sobre. Veja [docs/CONFORMIDADE.md](docs/CONFORMIDADE.md). |
| ♿ **Acessibilidade** | Contraste WCAG AA, descrições para o TalkBack e uso completo por teclado físico. |

> 🔒 Projeto feito para uma cliente real. O repositório tem só o código, sem nenhum dado de pacientes.

## 📦 Versões

As versões ficam na página de [**Releases**](https://github.com/vt-santos/clinica-estetica-android/releases), separadas em dois tipos:

| Tipo | Exemplo | O que significa |
| :-- | :-- | :-- |
| 🧪 **Beta** | `v0.7.0-beta.1` | Funções novas, ainda em teste no tablet da clínica. Aparece como *Pre-release*. |
| 🩹 **Patch** | `v0.2.2` | Correções e ajustes pequenos, sem função nova. Aparece como versão estável. |

Cada release traz a data de lançamento no título, as notas da versão e, nas versões novas, o **APK de teste** para instalar no tablet. O histórico completo está no [**CHANGELOG.md**](CHANGELOG.md).

| Versão | Tipo | Data | Destaque |
| :-- | :-- | :-- | :-- |
| [v0.7.0-beta.1](https://github.com/vt-santos/clinica-estetica-android/releases/tag/v0.7.0-beta.1) | 🧪 Beta | 27/11/2026 | Filtro do mês na Agenda com resumo de pagamentos |
| [v0.6.0-beta.1](https://github.com/vt-santos/clinica-estetica-android/releases/tag/v0.6.0-beta.1) | 🧪 Beta | 27/11/2026 | Campo CPF/CNPJ com verificação automática |
| [v0.5.0-beta.1](https://github.com/vt-santos/clinica-estetica-android/releases/tag/v0.5.0-beta.1) | 🧪 Beta | 27/11/2026 | LGPD, políticas e acessibilidade |
| [v0.4.0-beta.1](https://github.com/vt-santos/clinica-estetica-android/releases/tag/v0.4.0-beta.1) | 🧪 Beta | 27/11/2026 | Pergunta de hipertensão e anamnese agrupada |
| [v0.3.0-beta.1](https://github.com/vt-santos/clinica-estetica-android/releases/tag/v0.3.0-beta.1) | 🧪 Beta | 20/11/2026 | Aviso de retorno e pagamento ao concluir |
| [v0.2.2](https://github.com/vt-santos/clinica-estetica-android/releases/tag/v0.2.2) | 🩹 Patch | 20/11/2026 | Ícone novo com o "M" do logo |
| [v0.2.1](https://github.com/vt-santos/clinica-estetica-android/releases/tag/v0.2.1) | 🩹 Patch | 20/11/2026 | Botões OK/Cancelar visíveis e tema novo |
| [v0.2.0-beta.1](https://github.com/vt-santos/clinica-estetica-android/releases/tag/v0.2.0-beta.1) | 🧪 Beta | 20/11/2026 | Termo criptografado e verificação de idade |
| [v0.1.0-beta.1](https://github.com/vt-santos/clinica-estetica-android/releases/tag/v0.1.0-beta.1) | 🧪 Beta | 18/11/2026 | Primeira versão |

## 🧰 Tecnologias

- **Java 17** e Android SDK (minSdk 26, targetSdk 36)
- **Room** (SQLite) para o banco de dados local
- **Material Components** para a interface
- **AES-256-GCM** com chave no Android Keystore para o termo e a assinatura
- **JUnit** e **Robolectric** para os testes do fluxo principal, rodando no GitHub Actions

## 🗂️ Estrutura

```
app/src/main/java/br/com/clinicaestetica/
├── data/       entidades, DAO, banco Room e catálogo das fichas
├── reminder/   agendamento e disparo dos lembretes
└── ui/         telas (activities e fragments)
app/src/test/   testes com Robolectric
.github/        testes automáticos e publicação das releases
```

## 📱 Como gerar o APK

Com o JDK 21 e o Android SDK instalados, no Windows:

```powershell
.\gerar-apk.ps1
```

O APK de teste fica em `dist\ClinicaEstetica-debug.apk`. Para rodar os testes:

```powershell
.\gradlew.bat testDebugUnitTest
```

## 🚀 Como publicar uma versão

1. Adicione a versão nova no topo do [CHANGELOG.md](CHANGELOG.md) (`-beta.N` para beta, sem sufixo para patch).
2. Atualize `versionName` e `versionCode` em `app/build.gradle`.
3. Faça o merge no `main`. O GitHub Actions cria a tag, a release e anexa o APK.

## 👤 Autor

**Victor Santos** · [LinkedIn](https://www.linkedin.com/in/victor-santos-572a88347) · [GitHub](https://github.com/vt-santos)
