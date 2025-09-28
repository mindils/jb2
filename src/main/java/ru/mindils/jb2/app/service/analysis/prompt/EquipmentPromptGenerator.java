package ru.mindils.jb2.app.service.analysis.prompt;

import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
import ru.mindils.jb2.app.util.HtmlToMarkdownConverter;

@Component
public class EquipmentPromptGenerator implements PromptGenerator {

  private final HtmlToMarkdownConverter htmlConverter;

  public EquipmentPromptGenerator(HtmlToMarkdownConverter htmlConverter) {
    this.htmlConverter = htmlConverter;
  }

  @Override
  public VacancyLlmAnalysisType getSupportedType() {
    return VacancyLlmAnalysisType.EQUIPMENT;
  }

  @Override
  public String generatePrompt(Vacancy vacancy) {
    return """
        Проанализируй описание IT-вакансии и ОПРЕДЕЛИ ПОЛИТИКУ ПО НОУТБУКУ/ОБОРУДОВАНИЮ.
        
        ПРИОРИТЕТЫ РЕШЕНИЯ (по убыванию важности):
        1) Выдаёт ли компания ноутбук/ПК кандидату (equipmentProvided=true|false).
        2) Если прямого ответа нет, разрешён ли BYOD (работа на собственном ноутбуке) и есть ли компенсация расходов (byodCompensation).
        3) Доп. оснащение (мониторы/периферия).
        
        Верни результат ТОЛЬКО в формате JSON без дополнительного текста по схеме:
        {
          "equipmentType": "macbook_pro|windows_laptop|byod|not_specified",
          "equipmentProvided": boolean,
          "byodCompensation": "full|partial|none|not_applicable",
          "additionalEquipment": "monitors|peripherals|monitors_peripherals|none",
          "equipmentMentioned": boolean
        }
        
        ПРАВИЛА ИНТЕРПРЕТАЦИИ:
        - equipmentProvided:
          • true — если явно сказано «выдаём/предоставляем ноутбук/технику/ПК», «company laptop», «предоставим MacBook/Windows-ноутбук», «закупим технику», «обеспечим оборудованием», «доставка техники».
          • false — если явно сказано «не выдаём/не предоставляем», «работа со своим ноутбуком обязательна», «BYOD only», «bring your own device», «требуется собственное оборудование».
          • Если упомянуты оба варианта (можно своим ИЛИ выдадим по запросу) — считаем, что компания может выдать → equipmentProvided=true.
          • Если в тексте нет прямого сигнала ни «да», ни «нет» — ставь equipmentProvided=false И одновременно equipmentMentioned=false (это значит: не найдено упоминаний).
        
        - equipmentType:
          • "macbook_pro" — если упомянуты MacBook/Mac/macOS/Apple-ноутбук.
          • "windows_laptop" — если упомянуты Windows-ноутбук/ПК/рабочая станция/ThinkPad/Dell/HP и т.п. без Apple.
          • "byod" — если основной посыл «работа на своём ноутбуке» без конкретного бренда/ОС.
          • "not_specified" — если одновременно встречаются и Mac, и Windows, или нет конкретики.
          • Если BYOD и одновременно «можем выдать технику» без бренда — выбирай "byod" только если акцент на «своём ноутбуке»; иначе "not_specified".
        
        - byodCompensation:
          • "full" — явные формулировки о 100% компенсации/полное возмещение/«компания оплачивает стоимость личного оборудования».
          • "partial" — «доплата», «стипендия/лимит/аллоуанс/stipend/allowance», «компенсация до N», «аренда/лизинг с частичной оплатой».
          • "none" — «без компенсации», «компенсации нет».
          • "not_applicable" — когда BYOD не упомянут или запрещён, либо техника гарантированно выдается и BYOD не рассматривается.
        
        - additionalEquipment:
          • monitors — если есть «монитор/дополнительный монитор/второй экран/два экрана/ultrawide».
          • peripherals — если есть «клавиатура, мышь, док-станция/док, веб-камера, гарнитура/наушники, HDMI/USB-C хаб, адаптеры» или «оборудование по запросу (периферия)».
          • monitors_peripherals — если упомянуты и мониторы, и периферия.
          • none — если ничего из этого нет.
        
        - equipmentMentioned:
          • true — если в тексте есть ЛЮБЫЕ упоминания об оборудовании/выдаче/ BYOD/мониторах/периферии.
          • false — если нет ни одного релевантного сигнала (в этом случае equipmentProvided=false и byodCompensation="not_applicable").
        
        ИЩИ СИГНАЛЬНЫЕ ФРАЗЫ (с учетом синонимов и английских вариантов):
        - Выдача техники: «выдаём/предоставляем технику», «компания предоставляет ноутбук/ПК», «company laptop», «we provide equipment», «обеспечим MacBook/Windows laptop», «закупим/оформим технику», «доставка техники».
        - Отсутствие выдачи/требование BYOD: «не предоставляем оборудование», «ноутбук не выдаётся», «работа со своим ноутбуком», «BYOD», «bring your own device», «own laptop required/allowed», «использование личного оборудования».
        - Компенсация BYOD: «компенсация/возмещение/стипендия/allowance/stipend», «до 100%/полная компенсация», «частичная компенсация/до N ₽/$/€», «без компенсации».
        - Доп. оснащение: «монитор/мониторы/второй экран/ultrawide», «клавиатура/мышь/гарнитура/наушники/веб-камера/док-станция/док/хаб/адаптеры», «оборудование по запросу».
        
        НЕ УЧИТЫВАЙ:
        - Программное обеспечение (IDE, софт, лицензии), интернет/связь, мебель.
        - Расплывчатые фразы «современное оборудование» без конкретики — не считать за сигнал.
        
        Разреши конфликты так:
        - Явное утверждение («выдаём ноутбук») сильнее общих фраз.
        - Если встречаются противоречия в разных местах, выбирай самый конкретный и недвусмысленный фрагмент (ближе к разделу «что предоставляем»/«условия работы»).
        
        ДАННЫЕ ДЛЯ АНАЛИЗА:
        Описание вакансии:
        Название: {name}
        Описание: {description}
        Описание(Бренд): {descriptionBranded}
        Компания: {employer}
        Компания (Бренд): {employerBranded}
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }
}
