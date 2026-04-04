<script setup lang="ts">
defineProps<{ visible: boolean }>()
const emit = defineEmits<{ (e: 'accept'): void; (e: 'close'): void }>()
</script>

<template>
  <Transition name="fade">
    <div v-if="visible" class="modal-overlay" @click.self="emit('close')">
      <div class="modal privacy-modal">
        <div class="modal-header">
          <div class="modal-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            ПОЛІТИКА КОНФІДЕНЦІЙНОСТІ
          </div>
          <button class="modal-close" @click="emit('close')">✕</button>
        </div>

        <div class="modal-body privacy-body">
          <p class="privacy-updated">Останнє оновлення: 01.04.2026</p>

          <section class="privacy-section">
            <h3>1. Загальні положення</h3>
            <p>
              Ця Політика конфіденційності описує, яким чином платформа TheSpawnPoint
              («ми», «наш», «платформа») збирає, використовує, зберігає та захищає
              персональні дані користувачів («ви», «користувач»). Використовуючи
              платформу, ви погоджуєтесь з умовами цієї Політики.
            </p>
          </section>

          <section class="privacy-section">
            <h3>2. Які дані ми збираємо</h3>
            <ul>
              <li><strong>Дані реєстрації:</strong> ім'я гравця (нікнейм), адреса електронної пошти, хешований пароль.</li>
              <li><strong>Дані профілю:</strong> аватар, опис, країна, обрані ігри, соціальні посилання — ви надаєте їх добровільно.</li>
              <li><strong>Дані взаємодії:</strong> повідомлення в чатах, створені та приєднані лобі, список друзів, досягнення.</li>
              <li><strong>Технічні дані:</strong> IP-адреса, тип браузера, ОС, дата й час запитів, cookies сесії.</li>
            </ul>
          </section>

          <section class="privacy-section">
            <h3>3. Мета обробки даних</h3>
            <ul>
              <li>Забезпечення функціонування платформи та вашого облікового запису.</li>
              <li>Надсилання підтверджувальних листів (верифікація email, відновлення пароля).</li>
              <li>Модерація контенту та дотримання правил спільноти.</li>
              <li>Покращення якості послуг та аналітика використання.</li>
              <li>Забезпечення безпеки: виявлення шахрайства, зловживань, несанкціонованого доступу.</li>
            </ul>
          </section>

          <section class="privacy-section">
            <h3>4. Зберігання даних</h3>
            <p>
              Ваші персональні дані зберігаються на захищених серверах протягом усього
              часу існування вашого облікового запису. Після видалення акаунту дані
              анонімізуються або видаляються протягом 30 календарних днів, за
              винятком випадків, передбачених законодавством.
            </p>
          </section>

          <section class="privacy-section">
            <h3>5. Передача даних третім особам</h3>
            <p>
              Ми <strong>не продаємо</strong> та <strong>не передаємо</strong> ваші персональні
              дані третім особам у комерційних цілях. Дані можуть бути розкриті лише:
            </p>
            <ul>
              <li>На вимогу правоохоронних органів відповідно до чинного законодавства.</li>
              <li>Технічним провайдерам, що забезпечують роботу платформи (хостинг, email-сервіс), за умови дотримання ними конфіденційності.</li>
            </ul>
          </section>

          <section class="privacy-section">
            <h3>6. Файли cookies</h3>
            <p>
              Платформа використовує cookies виключно для автентифікації та підтримки
              сесії користувача. Ми не використовуємо рекламні або трекінгові cookies
              третіх сторін.
            </p>
          </section>

          <section class="privacy-section">
            <h3>7. Ваші права</h3>
            <ul>
              <li><strong>Доступ:</strong> ви маєте право запросити копію своїх персональних даних.</li>
              <li><strong>Виправлення:</strong> ви можете оновити або виправити свої дані у налаштуваннях профілю.</li>
              <li><strong>Видалення:</strong> ви маєте право вимагати видалення свого акаунту та пов'язаних даних.</li>
              <li><strong>Обмеження обробки:</strong> ви можете вимагати обмеження обробки ваших даних.</li>
              <li><strong>Відкликання згоди:</strong> ви маєте право відкликати згоду на обробку в будь-який час.</li>
            </ul>
          </section>

          <section class="privacy-section">
            <h3>8. Захист даних</h3>
            <p>
              Ми застосовуємо сучасні технічні та організаційні заходи для захисту ваших
              даних: шифрування паролів (bcrypt), HTTPS-з'єднання, обмеження доступу
              до баз даних, регулярне оновлення програмного забезпечення.
            </p>
          </section>

          <section class="privacy-section">
            <h3>9. Зміни до Політики</h3>
            <p>
              Ми залишаємо за собою право оновлювати цю Політику конфіденційності.
              Про суттєві зміни ви будете повідомлені через платформу. Продовження
              використання сервісу після змін означає вашу згоду з оновленими умовами.
            </p>
          </section>

          <section class="privacy-section">
            <h3>10. Контакти</h3>
            <p>
              З питань, пов'язаних із конфіденційністю та захистом персональних даних,
              звертайтесь через розділ «Підтримка» на платформі або на електронну пошту:
              <strong>privacy@thespawnpoint.gg</strong>
            </p>
          </section>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="emit('close')">ВІДХИЛИТИ</button>
          <button class="btn-submit" @click="emit('accept')">✔ ПРИЙМАЮ УМОВИ</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.privacy-modal {
  max-width: 680px;
}

.privacy-body {
  max-height: 55vh;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--border) transparent;
}

.privacy-updated {
  font-size: 12px;
  color: var(--gray);
  letter-spacing: 1px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}

.privacy-section {
  margin-bottom: 22px;
}

.privacy-section h3 {
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 2px;
  color: var(--yellow);
  margin-bottom: 8px;
}

.privacy-section p {
  font-size: 14px;
  color: var(--gray-light);
  line-height: 1.7;
}

.privacy-section ul {
  list-style: none;
  padding: 0;
  margin: 8px 0 0;
}

.privacy-section ul li {
  position: relative;
  padding-left: 18px;
  font-size: 14px;
  color: var(--gray-light);
  line-height: 1.7;
  margin-bottom: 4px;
}

.privacy-section ul li::before {
  content: '▸';
  position: absolute;
  left: 0;
  color: var(--yellow-dim);
}

.privacy-section strong {
  color: var(--white);
}
</style>

