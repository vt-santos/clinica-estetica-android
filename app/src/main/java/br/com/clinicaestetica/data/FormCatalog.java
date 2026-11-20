package br.com.clinicaestetica.data;

/** Conteúdo das fichas em papel: perguntas, grupos de avaliação e equipamentos. */
public final class FormCatalog {
    private FormCatalog() {
    }

    public static final class Question {
        public final String key;
        public final String label;
        public final String detailLabel;

        Question(String key, String label, String detailLabel) {
            this.key = key;
            this.label = label;
            this.detailLabel = detailLabel;
        }
    }

    public static final class SkinGroup {
        public final String title;
        public final boolean single;
        public final String[] options;

        SkinGroup(String title, boolean single, String... options) {
            this.title = title;
            this.single = single;
            this.options = options;
        }
    }

    public static final Question[] HISTORY = {
            new Question("aesthetic_treatment", "Fez tratamento estético anterior?", "Qual?"),
            new Question("allergies", "Antecedentes alérgicos?", "Quais?"),
            new Question("bowel", "Funcionamento intestinal regular?", "Observações"),
            new Question("sports", "Pratica esportes?", "Quais?"),
            new Question("smoker", "É fumante?", null),
            new Question("diet", "Alimentação balanceada?", "Tipo?"),
            new Question("medical_treatment", "Faz algum tratamento médico?", "Qual?"),
            new Question("medication", "Usa algum medicamento?", "Qual?"),
            new Question("acids", "Usa ou já usou ácidos na pele?", "Quais?"),
            new Question("pregnant", "É gestante?", "Filhos? Quantos?"),
            new Question("pacemaker", "Portador de marcapasso?", "Qual?"),
            new Question("metal_prosthesis", "Presença de próteses metálicas?", "Local?"),
            new Question("heart", "Tem problemas cardíacos?", "Qual?"),
            new Question("epilepsy", "Portador de epilepsia?", null),
            new Question("oncology", "Antecedentes oncológicos?", "Qual?"),
            new Question("menstrual", "Ciclo menstrual regular?", "Observações"),
            new Question("contraceptive", "Usa método anticoncepcional?", "Qual?"),
            new Question("daily_care", "Cuidados diários e produtos em uso?", "Qual?"),
            new Question("diabetes", "Tem diabetes?", null),
            new Question("dental_prosthesis", "Próteses dentárias?", null),
            new Question("sun", "Costuma tomar sol?", null),
    };

    public static final SkinGroup[] SKIN_GROUPS = {
            new SkinGroup("Manchas pigmentares relacionadas à melanina", false,
                    "Acromia", "Cloasma", "Efélides", "Hipercromia", "Hipocromia"),
            new SkinGroup("Manchas por alterações vasculares", false,
                    "Angioma", "Cianose", "Eritema", "Hematoma", "Petéquias", "Teleangectasias"),
            new SkinGroup("Formações sólidas", false,
                    "Ceratose", "Nódulos", "Pápulas", "Verrugas", "Comedão", "Millium", "Necrose"),
            new SkinGroup("Formações com conteúdo líquido", false,
                    "Bolha", "Pústula", "Vesícula"),
            new SkinGroup("Lesões de pele", false,
                    "Crosta", "Descamação", "Escara", "Escoriação", "Fissura", "Fístula", "Ulceração"),
            new SkinGroup("Sequelas", false, "Atrofia", "Cicatriz"),
            new SkinGroup("Pelos", false, "Hipertricose", "Hirsutismo"),
            new SkinGroup("Alterações da queratinização", false,
                    "Eczema", "Hiperqueratose", "Psoríase"),
            new SkinGroup("Quanto à hidratação", true, "Desidratada", "Normal"),
            new SkinGroup("Quanto ao grau de oleosidade", true,
                    "Alípica", "Lipídica", "Normal", "Seborreica"),
            new SkinGroup("Quanto à espessura", true, "Espessa", "Fina", "Muito fina"),
    };

    public static final String[] EQUIPMENT = {
            "Alta Frequência", "Desencruste", "Ionizador", "Eletrolifting", "Eletrólise",
            "Vaporizador", "Eletrosucção", "Microcorrentes", "Estim. Muscular",
    };

    public static final String[] PAYMENT_METHODS = {
            "Pix", "Dinheiro", "Cartão de débito", "Cartão de crédito", "Transferência",
    };

    public static final String[] APPOINTMENT_TYPES = {"Consulta", "Retorno"};

    public static final int ADULT_AGE = 18;

    public static final String[] GUARDIAN_RELATIONSHIPS = {
            "Mãe", "Pai", "Avó", "Avô", "Tutor(a) legal", "Outro",
    };

    public static final String[] REMINDER_LABELS = {
            "Sem lembrete", "15 minutos antes", "1 hora antes", "1 dia antes",
    };

    public static final int[] REMINDER_MINUTES = {-1, 15, 60, 1440};
}
