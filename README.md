# Clínica Estética: app de gestão para Android

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Room](https://img.shields.io/badge/Room-SQLite-003B57?logo=sqlite&logoColor=white)
![Status](https://img.shields.io/badge/status-em%20desenvolvimento-orange)

Aplicativo Android nativo para a rotina de uma clínica de estética. Ele roda **sozinho no tablet da clínica, sem internet e sem servidor**: todos os dados ficam no próprio aparelho.

Projeto feito para uma cliente real. O repositório tem só o código, sem nenhum dado de pacientes.

## Funcionalidades

- **Pacientes**: cadastro com ficha de anamnese e histórico.
- **Avaliação da pele**: registro da avaliação de cada paciente.
- **Sessões de tratamento**: protocolos e acompanhamento por sessão.
- **Agenda**: agendamentos com lembretes por notificação, que continuam valendo depois que o tablet reinicia.
- **Pagamentos**: registro e acompanhamento de valores.
- **Termo de consentimento**: assinatura digital feita com o dedo na tela. Se a ficha mudar depois da assinatura, o app pede uma nova assinatura e mostra o que mudou.
- **Painel inicial** com atalhos para as principais áreas.

## Tecnologias

- Java 17 e Android SDK (minSdk 26, targetSdk 36)
- Room (SQLite) para o banco de dados local
- Material Components
- JUnit e Robolectric para testes automatizados do fluxo principal

## Estrutura

```
app/src/main/java/br/com/clinicaestetica/
  data/       entidades, DAO e banco Room
  reminder/   agendamento e disparo dos lembretes
  ui/         telas (activities e fragments)
app/src/test/ testes com Robolectric
```

## Como gerar o APK

Com o JDK 21 e o Android SDK instalados:

```powershell
.\gerar-apk.ps1
```

O APK de teste fica em `dist\ClinicaEstetica-debug.apk`.

## Autor

Victor Santos · [LinkedIn](https://www.linkedin.com/in/victor-santos-572a88347) · [GitHub](https://github.com/vt-santos)
