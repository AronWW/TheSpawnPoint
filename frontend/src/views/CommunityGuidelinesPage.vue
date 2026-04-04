<script setup lang="ts">
import { ref } from 'vue'

const expandedRule = ref<number | null>(null)

function toggleRule(index: number) {
  expandedRule.value = expandedRule.value === index ? null : index
}

interface Rule {
  icon: string
  title: string
  short: string
  details: string[]
  funNote?: string
}

const rules: Rule[] = [
  {
    icon: 'handshake',
    title: 'ПОВАГА ДО ІНШИХ ГРАВЦІВ',
    short: 'Стався до кожного так, як хочеш, щоб ставились до тебе.',
    details: [
      'Заборонені образи, приниження, буллінг, расизм, сексизм та будь-яка форма дискримінації.',
      'Конструктивна критика — ок. Токсичність — ні.',
      'Не провокуй, не тролій, не псуй настрій іншим.',
      'Пам\'ятай: за кожним нікнеймом — реальна людина.'
    ],
    funNote: 'Золоте правило: якщо б ти не сказав це кому-небудь в обличчя — не пиши.'
  },
  {
    icon: 'ban',
    title: 'БЕЗ СПАМУ І РЕКЛАМИ',
    short: 'Платформа — не дошка оголошень. Тримай чати чистими.',
    details: [
      'Заборонена самореклама, посилання на сторонні сервіси та канали без згоди модерації.',
      'Масова розсилка однакових повідомлень = бан.',
      'Не зловживай повторюваними повідомленнями або капсом.',
      'Реферальні та партнерські посилання заборонені.'
    ],
    funNote: 'Єдиний спам, який тут допустимий — це SPAM у Terraria.'
  },
  {
    icon: 'gamepad',
    title: 'ЧЕСНА ГРА',
    short: 'Грай чесно. Читери та абьюзери отримають бан.',
    details: [
      'Використання читів, макросів, експлойтів або будь-яких інших нечесних переваг заборонено.',
      'Не саботуй свою команду навмисно.',
      'Якщо знайшов баг — повідом через «Підтримку», а не експлуатуй.',
      'Смурфінг та навмисний деранк порушують дух спільноти.'
    ],
    funNote: 'Єдиний допустимий чіт-код — IDDQD. І то лише в DOOM.'
  },
  {
    icon: 'lock',
    title: 'ЗАХИСТ ОСОБИСТИХ ДАНИХ',
    short: 'Не шерь чужу персональну інформацію. Ніколи.',
    details: [
      'Заборонено публікувати реальні імена, адреси, номери телефонів, фото інших людей без їхньої згоди.',
      'Doxxing — це серйозне порушення, яке тягне за собою негайний перманентний бан.',
      'Свої дані теж бережи: не залишай паролі та платіжну інформацію в чатах.',
      'Аватари та контент профілю не повинні містити зображення реальних людей без їхнього дозволу.'
    ]
  },
  {
    icon: 'edit',
    title: 'АДЕКВАТНИЙ КОНТЕНТ',
    short: 'Тримай контент у рамках — платформа для всіх вікових категорій.',
    details: [
      'Заборонений NSFW-контент: порнографія, надмірне насильство, шок-контент.',
      'Нікнейми та назви лобі не повинні містити образливих виразів.',
      'Аватари повинні бути пристойними та не провокативними.',
      'Контент, що пропагує насильство, наркотики або незаконну діяльність — заборонений.'
    ],
    funNote: 'Правило пальця: якщо контент не можна показати на стрімі — не постить його тут.'
  },
  {
    icon: 'scales',
    title: 'МОДЕРАЦІЯ',
    short: 'Рішення модераторів — фінальні. Не сперечайся, а подай апеляцію.',
    details: [
      'Модератори можуть виносити попередження, мʼютити, кікати та банити користувачів.',
      'Якщо ти не згоден з рішенням — скористайся системою апеляцій через «Підтримку».',
      'Спроби обійти бан (мультиакаунти, VPN) лише подовжать його.',
      'Повідомляй про порушення через кнопку «Скарга» — це анонімно.'
    ]
  },
  {
    icon: 'chat',
    title: 'КУЛЬТУРА ЧАТУ',
    short: 'Чати — для спілкування, а не для базару.',
    details: [
      'Пиши зрозуміло, поважай мову спільноти.',
      'Не флуди, не кидай випадкові стікери та гіфки пачками.',
      'Обговорення політики, релігії та інших чутливих тем — краще в приватних повідомленнях.',
      'Не вимагай від людей приєднатися до голосового чату, якщо вони цього не хочуть.'
    ],
    funNote: 'Правило «3 повідомлення»: якщо можна вмістити думку в одне — не розбивай на 10.'
  },
  {
    icon: 'shield',
    title: 'БЕЗПЕКА АКАУНТУ',
    short: 'Ти відповідаєш за все, що відбувається під твоїм акаунтом.',
    details: [
      'Не передавай свій акаунт іншим людям.',
      'Використовуй надійний пароль (букви + цифри, від 6 символів).',
      'Не повідомляй свій пароль нікому, навіть «адміністраторам» — ми ніколи не запитуємо паролі.',
      'Якщо підозрюєш, що акаунт зламано — негайно зміни пароль.'
    ]
  }
]

interface Punishment {
  offense: string
  result: string
  icon: string
}

const punishments: Punishment[] = [
  { offense: 'Перше порушення', result: 'Попередження', icon: 'warning' },
  { offense: 'Повторне порушення', result: 'Мʼют на 24 години', icon: 'mute' },
  { offense: 'Серйозне порушення', result: 'Тимчасовий бан (7 днів)', icon: 'ban' },
  { offense: 'Критичне порушення', result: 'Перманентний бан', icon: 'skull' }
]
</script>

<template>
  <div class="guidelines-page">
    <div class="guidelines-bg"></div>

    <div class="guidelines-container">

      <section class="guidelines-hero">
        <div class="guidelines-hero-badge">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          ПРАВИЛА СПІЛЬНОТИ
        </div>
        <h1 class="guidelines-hero-title">КОДЕКС<br><span class="accent">ВОЇНА</span></h1>
        <p class="guidelines-hero-sub">
          Правила, які роблять нашу спільноту безпечною та дружньою для кожного
        </p>
        <div class="guidelines-hero-line"></div>
      </section>

      <section class="guidelines-intro">
        <p>
          TheSpawnPoint — це спільнота, побудована на повазі, чесності та любові
          до ігор. Ці правила існують для того, щоб кожен гравець почувався
          комфортно та безпечно. Порушення правил тягне за собою відповідні
          санкції — від попередження до перманентного бану.
        </p>
        <p class="intro-emphasis">
          Реєструючись на платформі, ти автоматично погоджуєшся з цими правилами.
        </p>
      </section>

      <section class="rules-section">
        <div
          v-for="(rule, i) in rules"
          :key="i"
          class="rule-card ink-panel"
          :class="{ expanded: expandedRule === i }"
        >
          <div class="rule-header" @click="toggleRule(i)">
            <div class="rule-number">{{ String(i + 1).padStart(2, '0') }}</div>
            <div class="rule-emoji">

              <svg v-if="rule.icon === 'handshake'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>

              <svg v-else-if="rule.icon === 'ban'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>

              <svg v-else-if="rule.icon === 'gamepad'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="6" width="20" height="12" rx="2"/><line x1="6" y1="12" x2="10" y2="12"/><line x1="8" y1="10" x2="8" y2="14"/><circle cx="16" cy="10" r="1" fill="currentColor" stroke="none"/><circle cx="18" cy="13" r="1" fill="currentColor" stroke="none"/></svg>

              <svg v-else-if="rule.icon === 'lock'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>

              <svg v-else-if="rule.icon === 'edit'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>

              <svg v-else-if="rule.icon === 'scales'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="12" y1="3" x2="12" y2="21"/><path d="M5 8l7-5 7 5"/><path d="M5 8l-2 8h4.5a2.5 2.5 0 0 0 2.5-2.5V8"/><path d="M19 8l2 8h-4.5a2.5 2.5 0 0 1-2.5-2.5V8"/></svg>

              <svg v-else-if="rule.icon === 'chat'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>

              <svg v-else-if="rule.icon === 'shield'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            </div>
            <div class="rule-info">
              <h3 class="rule-title">{{ rule.title }}</h3>
              <p class="rule-short">{{ rule.short }}</p>
            </div>
            <div class="rule-toggle">{{ expandedRule === i ? '▲' : '▼' }}</div>
          </div>

          <Transition name="expand">
            <div v-if="expandedRule === i" class="rule-details">
              <ul>
                <li v-for="(detail, j) in rule.details" :key="j">{{ detail }}</li>
              </ul>
              <div v-if="rule.funNote" class="rule-fun-note">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="flex-shrink: 0; margin-right: 6px; vertical-align: middle;"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                {{ rule.funNote }}
              </div>
            </div>
          </Transition>
        </div>
      </section>

      <section class="punishments-section">
        <h2 class="section-heading">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="margin-right: 8px;"><line x1="12" y1="3" x2="12" y2="21"/><path d="M5 8l7-5 7 5"/><path d="M5 8l-2 8h4.5a2.5 2.5 0 0 0 2.5-2.5V8"/><path d="M19 8l2 8h-4.5a2.5 2.5 0 0 1-2.5-2.5V8"/></svg>
          СИСТЕМА ПОКАРАНЬ
        </h2>
        <p class="section-text">
          Ми не любимо банити, але інколи доводиться. Ось як працює система:
        </p>
        <div class="punishments-grid">
          <div
            v-for="(p, i) in punishments"
            :key="i"
            class="punishment-card"
          >
            <div class="punishment-icon">
              <svg v-if="p.icon === 'warning'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>

              <svg v-else-if="p.icon === 'mute'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><line x1="23" y1="9" x2="17" y2="15"/><line x1="17" y1="9" x2="23" y2="15"/></svg>

              <svg v-else-if="p.icon === 'ban'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>

              <svg v-else-if="p.icon === 'skull'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="10" r="8"/><path d="M8 22h8"/><path d="M10 14v8"/><path d="M14 14v8"/><circle cx="9" cy="9" r="1.5" fill="currentColor" stroke="none"/><circle cx="15" cy="9" r="1.5" fill="currentColor" stroke="none"/></svg>
            </div>
            <div class="punishment-offense">{{ p.offense }}</div>
            <div class="punishment-arrow">→</div>
            <div class="punishment-result">{{ p.result }}</div>
          </div>
        </div>
        <p class="section-text punishment-note">
          * Особливо серйозні порушення (doxxing, загрози, NSFW) можуть призвести
          до негайного перманентного бану без попереджень.
        </p>
      </section>

      <section class="appeal-section">
        <div class="appeal-card ink-panel">
          <h2 class="section-heading">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="margin-right: 8px;"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
            АПЕЛЯЦІЯ
          </h2>
          <p class="section-text">
            Вважаєш, що бан несправедливий? Ти маєш право подати апеляцію через
            розділ <router-link to="/support" class="guidelines-link">Підтримка</router-link>.
            Кожна апеляція розглядається індивідуально протягом 48 годин.
          </p>
          <p class="section-text appeal-reminder">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="flex-shrink: 0; margin-right: 4px; vertical-align: middle;"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            Пам'ятай: ввічлива апеляція має більше шансів на успіх, ніж «РОЗБАНЬТЕ МЕНЕ!!1».
          </p>
        </div>
      </section>

      <div class="guidelines-updated">
        Останнє оновлення: 01.04.2026
      </div>
    </div>
  </div>
</template>

<style scoped>
.guidelines-page {
  min-height: 100vh;
  padding-top: 64px;
  position: relative;
  overflow: hidden;
}

.guidelines-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    linear-gradient(135deg, rgba(245,197,24,0.03) 0%, transparent 50%),
    linear-gradient(to bottom, transparent 30%, rgba(10,10,11,0.95) 100%);
  pointer-events: none;
}

.guidelines-container {
  position: relative;
  z-index: 1;
  max-width: 900px;
  margin: 0 auto;
  padding: 60px 32px 80px;
}

.guidelines-hero {
  text-align: center;
  margin-bottom: 48px;
}

.guidelines-hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--yellow);
  border: 1px solid var(--yellow-dim);
  padding: 6px 18px;
  margin-bottom: 24px;
  text-transform: uppercase;
}

.guidelines-hero-title {
  font-family: var(--font-display);
  font-size: 56px;
  letter-spacing: 4px;
  color: var(--white);
  margin-bottom: 12px;
  line-height: 1.1;
}

.guidelines-hero-title .accent {
  color: var(--yellow);
}

.guidelines-hero-sub {
  font-size: 18px;
  color: var(--gray-light);
  letter-spacing: 1px;
}

.guidelines-hero-line {
  width: 80px;
  height: 3px;
  background: var(--yellow);
  margin: 24px auto 0;
  opacity: 0.6;
}

.guidelines-intro {
  margin-bottom: 48px;
  padding: 24px 28px;
  border-left: 3px solid var(--yellow-dim);
  background: rgba(245,197,24,0.03);
}

.guidelines-intro p {
  font-size: 15px;
  color: var(--gray-light);
  line-height: 1.8;
  margin-bottom: 8px;
}

.guidelines-intro p:last-child {
  margin-bottom: 0;
}

.intro-emphasis {
  color: var(--yellow) !important;
  font-weight: 600;
}

.rules-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 56px;
}

.rule-card {
  transition: border-color 0.2s, box-shadow 0.2s;
}

.rule-card.expanded {
  border-color: var(--yellow-dim);
}

.rule-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  cursor: pointer;
}

.rule-number {
  font-family: var(--font-display);
  font-size: 28px;
  color: var(--yellow);
  opacity: 0.3;
  min-width: 36px;
}

.rule-emoji {
  flex-shrink: 0;
  color: var(--yellow);
  display: flex;
  align-items: center;
}

.rule-info {
  flex: 1;
}

.rule-title {
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 2px;
  color: var(--yellow);
  margin-bottom: 2px;
}

.rule-short {
  font-size: 13px;
  color: var(--gray);
  letter-spacing: 0.5px;
}

.rule-toggle {
  color: var(--gray);
  font-size: 12px;
  transition: color 0.15s;
}

.rule-card:hover .rule-toggle {
  color: var(--yellow);
}

.rule-details {
  padding: 0 24px 20px 76px;
  overflow: hidden;
}

.rule-details ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.rule-details ul li {
  position: relative;
  padding-left: 18px;
  font-size: 14px;
  color: var(--gray-light);
  line-height: 1.7;
  margin-bottom: 6px;
}

.rule-details ul li::before {
  content: '▸';
  position: absolute;
  left: 0;
  color: var(--yellow-dim);
}

.rule-fun-note {
  margin-top: 12px;
  padding: 10px 14px;
  background: rgba(245,197,24,0.05);
  border-left: 2px solid var(--yellow-dim);
  font-size: 13px;
  color: var(--gray-light);
  font-style: italic;
  display: flex;
  align-items: center;
}

.punishments-section {
  margin-bottom: 48px;
}

.section-heading {
  font-family: var(--font-display);
  font-size: 28px;
  letter-spacing: 3px;
  color: var(--yellow);
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.section-text {
  font-size: 15px;
  color: var(--gray-light);
  line-height: 1.7;
  margin-bottom: 12px;
}

.punishments-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 20px 0;
}

.punishment-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--panel);
  border: 1px solid var(--border);
  transition: border-color 0.2s;
}

.punishment-card:hover {
  border-color: var(--yellow-dim);
}

.punishment-icon {
  flex-shrink: 0;
  color: var(--yellow);
  display: flex;
  align-items: center;
}

.punishment-offense {
  font-size: 14px;
  color: var(--gray-light);
  flex: 1;
  letter-spacing: 0.5px;
}

.punishment-arrow {
  color: var(--yellow-dim);
  font-size: 14px;
  flex-shrink: 0;
}

.punishment-result {
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 1px;
  color: var(--yellow);
  min-width: 200px;
}

.punishment-note {
  font-size: 13px !important;
  color: var(--gray) !important;
  font-style: italic;
}

.appeal-section {
  margin-bottom: 48px;
}

.appeal-card {
  padding: 32px;
}

.appeal-reminder {
  font-style: italic;
  color: var(--gray) !important;
}

.guidelines-link {
  color: var(--yellow);
  border-bottom: 1px solid var(--yellow-dim);
  transition: color 0.15s;
}

.guidelines-link:hover {
  color: var(--white);
}

.guidelines-updated {
  text-align: center;
  font-size: 12px;
  color: var(--gray);
  letter-spacing: 1px;
  padding-top: 20px;
  border-top: 1px solid var(--border);
}

@media (max-width: 768px) {
  .guidelines-container { padding: 40px 20px 60px; }
  .guidelines-hero-title { font-size: 36px; }
  .rule-header { padding: 16px; gap: 10px; }
  .rule-number { font-size: 20px; min-width: 28px; }
  .rule-details { padding-left: 48px; }
  .punishment-card { flex-wrap: wrap; }
  .punishment-result { min-width: auto; }
}
</style>

