<template>
  <div class="how-section">
    <div class="section-head how-head">
      <div class="section-title">ЯК ЦЕ ПРАЦЮЄ</div>
    </div>
    <div class="how-grid">
      <div class="how-item">
        <div class="how-num">01</div>
        <div class="how-icon">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/></svg>
        </div>
        <div class="how-title">ЗАРЕЄСТРУЙСЯ</div>
        <div class="how-desc">Створи профіль гравця. Вкажи платформи, стиль гри, рівень та улюблені ігри. Це допоможе знайти ідеальну команду.</div>
      </div>
      <div class="how-item">
        <div class="how-num">02</div>
        <div class="how-icon">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        </div>
        <div class="how-title">ЗНАЙДИ ЛОБІ</div>
        <div class="how-desc">Перегляд відкритих лобі, фільтруй за грою, рівнем та платформою. Приєднуйся до готового лобі або створюй своє.</div>
      </div>
      <div class="how-item">
        <div class="how-num">03</div>
        <div class="how-icon">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        </div>
        <div class="how-title">ГРАЙ РАЗОМ</div>
        <div class="how-desc">Спілкуйся в чаті прямо в платформі. Збирай друзів, організовуй регулярні сесії та перемагай разом.</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.how-icon {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  color: var(--yellow);
  margin-bottom: 20px;
}

.how-head {
  border: none;
  padding-bottom: 0;
  margin-bottom: 0;
}

.how-section {
  padding-bottom: 80px;
}

@media (max-width: 768px) {
  .how-section {
    padding-bottom: 56px;
  }
}

@media (max-width: 480px) {
  .how-section {
    padding-bottom: 44px;
  }
}
</style>
