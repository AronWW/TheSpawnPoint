package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.entity.achievement.AchievementType;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AchievementCatalog {

    public static final String FIRST_FAVORITE_GAME = "FIRST_FAVORITE_GAME";
    public static final String FIRST_FRIEND = "FIRST_FRIEND";
    public static final String FIRST_PARTY_CREATED = "FIRST_PARTY_CREATED";
    public static final String FIRST_PARTY_JOINED = "FIRST_PARTY_JOINED";
    public static final String FIRST_GAME_COMPLETED = "FIRST_GAME_COMPLETED";
    public static final String FOOTER_TRIPLE_CLICK = "FOOTER_TRIPLE_CLICK";

    @Getter
    public static class AchievementDefinition {
        private final String code;
        private final String title;
        private final String description;
        private final AchievementType type;
        private final String icon;
        private final String requirementText;
        private final String secretHint;
        private final boolean hiddenBeforeUnlock;
        private final int order;

        public AchievementDefinition(
                String code,
                String title,
                String description,
                AchievementType type,
                String icon,
                String requirementText,
                String secretHint,
                boolean hiddenBeforeUnlock,
                int order
        ) {
            this.code = code;
            this.title = title;
            this.description = description;
            this.type = type;
            this.icon = icon;
            this.requirementText = requirementText;
            this.secretHint = secretHint;
            this.hiddenBeforeUnlock = hiddenBeforeUnlock;
            this.order = order;
        }
    }

    private final List<AchievementDefinition> definitions = new ArrayList<>();
    private final Map<String, AchievementDefinition> byCode = new LinkedHashMap<>();

    @PostConstruct
    void init() {
        register(new AchievementDefinition(
                FIRST_FAVORITE_GAME,
                "ПЕРШИЙ ПІК",
                "Додай першу улюблену гру до свого профілю.",
                AchievementType.STANDARD,
                "❤",
                "Додай одну гру в обрані.",
                null,
                false,
                20
        ));
        register(new AchievementDefinition(
                FIRST_FRIEND,
                "У КОМАНДІ КРАЩЕ",
                "Отримай першого друга на платформі.",
                AchievementType.STANDARD,
                "🤝",
                "Прийми або отримай першу дружбу.",
                null,
                false,
                30
        ));
        register(new AchievementDefinition(
                FIRST_PARTY_CREATED,
                "ЗБІР ЗАГОНУ",
                "Створи своє перше лобі.",
                AchievementType.STANDARD,
                "⚑",
                "Створи перше лобі.",
                null,
                false,
                40
        ));
        register(new AchievementDefinition(
                FIRST_PARTY_JOINED,
                "ПРИЄДНАНО ДО РЕЙДУ",
                "Приєднайся до чужого лобі.",
                AchievementType.STANDARD,
                "➕",
                "Увійди в чуже лобі.",
                null,
                false,
                50
        ));
        register(new AchievementDefinition(
                FIRST_GAME_COMPLETED,
                "ГРА ЗАВЕРШЕНА",
                "Доведи першу спільну гру до завершення.",
                AchievementType.STANDARD,
                "🏁",
                "Заверши хоча б одну гру разом із лобі.",
                null,
                false,
                60
        ));
        register(new AchievementDefinition(
                FOOTER_TRIPLE_CLICK,
                "ПІДПІЛЬНИЙ СИГНАЛ",
                "Ти знайшов прихований тригер у підвалі сайту.",
                AchievementType.SECRET,
                "◈",
                null,
                "Іноді найцікавіше ховається внизу сторінки. Не завжди достатньо одного натискання.",
                true,
                1000
        ));
    }

    private void register(AchievementDefinition definition) {
        definitions.add(definition);
        byCode.put(definition.getCode(), definition);
    }

    public List<AchievementDefinition> getAll() {
        return List.copyOf(definitions);
    }

    public Optional<AchievementDefinition> findByCode(String code) {
        return Optional.ofNullable(byCode.get(code));
    }
}
