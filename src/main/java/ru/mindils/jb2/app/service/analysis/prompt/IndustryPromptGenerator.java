package ru.mindils.jb2.app.service.analysis.prompt;

import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
import ru.mindils.jb2.app.util.HtmlToMarkdownConverter;

@Component
public class IndustryPromptGenerator implements PromptGenerator {

  private final HtmlToMarkdownConverter htmlConverter;

  public IndustryPromptGenerator(HtmlToMarkdownConverter htmlConverter) {
    this.htmlConverter = htmlConverter;
  }

  @Override
  public VacancyLlmAnalysisType getSupportedType() {
    return VacancyLlmAnalysisType.INDUSTRY;
  }

  @Override
  public String generatePrompt(Vacancy vacancy) {
    return """
        Проанализируй описание IT-вакансии на РУССКОМ языке и ОПРЕДЕЛИ ОТРАСЛЬ КОМПАНИИ и ПРОЕКТА.
        Основной язык входных текстов — РУССКИЙ. Приоритизируй русскоязычные сигналы и синонимы (учитывай склонения, род/число/падеж, дефисы и альтернативные написания: «маркетплейс/маркет-плейс», «эдтех/edtech», «финтех/fintech»). Английские термины учитывай как вторичные, только если они явно присутствуют.
        
        Верни результат ТОЛЬКО в формате JSON по схеме (строгий JSON, без лишнего текста):
        {
          "company_category": "positive|neutral|problematic|toxic",
          "project_category": "positive|neutral|problematic|toxic",
          "company_direction": "healthcare|education|ecology|science|space|social_impact|accessibility|agriculture|b2b_saas|govtech|logistics|manufacturing|ai_infrastructure|deeptech|fintech|insurtech|proptech|legaltech|martech|ecommerce|gaming|entertainment|adtech|dating|microcredit|tobacco_alcohol|weapons",
          "project_direction": "healthcare|education|ecology|science|space|social_impact|accessibility|agriculture|b2b_saas|govtech|logistics|manufacturing|ai_infrastructure|deeptech|fintech|insurtech|proptech|legaltech|martech|ecommerce|gaming|entertainment|adtech|dating|microcredit|tobacco_alcohol|weapons|"
        }
        ТРЕБОВАНИЯ:
        - Все значения — в нижнем регистре.
        - company_direction и project_direction — строки с направлениями через "|" БЕЗ пробелов и дубликатов (пример: "fintech|b2b_saas"). Если уверенных сигналов нет — пустая строка "".
        - Если проект не отличается от компании — project_direction="", project_category = company_category.
        
        ПОРЯДОК ДОВЕРИЯ ИСТОЧНИКОВ (русский приоритет):
        1) employerIndustries (русские наименования отраслей/ОКВЭД, если есть).
        2) employerBranded и employer (описания компании на русском).
        3) name, descriptionBranded, description (текст вакансии/проекта на русском).
        4) skills — только отраслевые индикаторы (регуляции/термины). Технологические стеки (Java, React и т.п.) НЕ влияют.
        
        МАППИНГ НАПРАВЛЕНИЙ В КАТЕГОРИИ:
        - positive: healthcare, education, ecology, science, space, social_impact, accessibility, agriculture
        - neutral: b2b_saas, govtech, logistics, manufacturing, ai_infrastructure, deeptech
        - problematic: fintech, insurtech, proptech, legaltech, martech
        - toxic: ecommerce, gaming, entertainment, adtech, dating, microcredit, tobacco_alcohol, weapons
        
        ЕСЛИ НЕСКОЛЬКО НАПРАВЛЕНИЙ:
        - Категория определяется по приоритету риска: toxic > problematic > neutral > positive (для компании и отдельно для проекта).
        
        КАК ОТДЕЛИТЬ ПРОЕКТ ОТ КОМПАНИИ:
        - Признаки проекта: «проект/продукт/направление/команда/сервис/приложение», отдельное имя продукта, отдельная аудитория/рынок.
        - Если по тексту проект относится к другой отрасли, чем компания, укажи его направления в project_direction; иначе project_direction="".
        
        СЛОВАРЬ РУССКИХ СИГНАЛОВ (англ. варианты учитывай вторично):
        - healthcare: медицина, медтех, здравоохранение, клиник*, госпитал*, телемедицина, МИС, ЕГИСЗ, ЕМИАС, НМИЦ
        - education: образование, школа, вуз, колледж, курсы, платформа обучения, ДПО, электронное обучение, лмс, эдтех
        - ecology: экология, экологич*, устойчивое развитие, ESG, «углеродный след», «зелёная энергетика»
        - science: наука, НИОКР, исследовател*, лаборатори*, публикации, рецензируемые журналы
        - space: космическ*, спутник*, орбита, аэрокосмос
        - social_impact: социальный эффект, НКО, благотворительн*, инклюзия, волонтёрство
        - accessibility: доступность, a11y, WCAG, «экранный диктор», ассистивные технологии
        - agriculture: агро*, сельское хозяйство, агротех, фермер*, агроном*, агроскаутинг
        - b2b_saas: SaaS для бизнеса, CRM, ERP, BPM, helpdesk, HRM/HRIS, биллинг B2B, мультиарендность, подписка
        - govtech: госуслуги, МФЦ, ведомств*, госсектор, госинформсистем*, ЕПГУ, ФНС, казначейств*
        - logistics: логистик*, WMS, TMS, склад, «последняя миля», доставка, фулфилмент, флот/транспорт, трекинг
        - manufacturing: производство, завод, цех, MES, PLM, SCADA, ОТК, промышленн*, IIoT
        - ai_infrastructure: mlops, платформа LLM, инференс, векторная БД, фиче-хранилище, дата-платформа, разметка данных
        - deeptech: робототех*, компьютерное зрение (core R&D), AR/VR core, квант*, фотоник*, новые материалы
        - fintech: банк, финтех, платежи, эквайринг, денежные переводы, брокер, трейдинг, форекс, крипто, KYC/KYB, AML, PCI DSS, открытое банкинг/«open banking»
        - insurtech: страхован*, полис*, андеррайтинг, актуар*, урегулирование убытков/claims
        - proptech: недвижимост*, аренда, листинг, оценка объектов, MLS
        - legaltech: юр* услуги, договор*, e-sign, электронная подпись, суд, судебн*, e-discovery
        - martech: маркетинг-платформа, CDP, атрибуция, сегментация, кампании, коммуникации: email/push/SMS, ретеншн
        - ecommerce: e-commerce, маркетплейс/маркет-плейс, корзина, checkout, мерчант, фулфилмент, интернет-магазин
        - gaming: игра, геймдев, игровая студия, внутриигров*, free-to-play
        - entertainment: медиа, кино, музыка, контент, стриминг, OTT
        - adtech: реклама, рекламная платформа, DSP, SSP, ad exchange, программатик, торги/bid*
        - dating: знакомства, дейтинг, сервис знакомств, матч*, свайп*
        - microcredit: микрофинанс*, микрозайм*, payday, МФО, ПДЛ
        - tobacco_alcohol: табак, сигарет*, алкоголь, вино, пиво, спиртные напитки
        - weapons: оружи*, оборонн* контрактор, ВПК, вооружен*
        
        ПРАВИЛА РАЗРЕШЕНИЯ КОНФЛИКТОВ:
        - Конкретика сильнее общих фраз (напр., «страховые полисы» > «финансовые решения»).
        - Если employerIndustries противоречит описанию проекта: для company_direction используй employerIndustries, а project_direction определи по тексту проекта.
        - Обобщённые термины мапь по смыслу: «маркетплейс/интернет-торговля» → ecommerce; «доставка/флот/курьеры» → logistics.
        - Технологические термины (React, Kubernetes) игнорируй при классификации.
        
        ЕСЛИ СИГНАЛОВ НЕТ:
        - company_direction = ""
        - project_direction = ""
        - company_category = "neutral"
        - project_category = "neutral" (или = company_category, если project_direction="")
        
        ДАННЫЕ ДЛЯ АНАЛИЗА:
        Название: {name}
        Описание: {description}
        Описание(Бренд): {descriptionBranded}
        Ключевые навыки: {skills}
        Компания: {employer}
        Компания (Бренд): {employerBranded}
        Компания индустрия: {employerIndustries}
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{skills}", valueOrEmpty(vacancy.getKeySkillsStr()))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()))
        .replace("{employerIndustries}", valueOrEmpty(vacancy.getEmployer().getIndustriesStr()));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }
}