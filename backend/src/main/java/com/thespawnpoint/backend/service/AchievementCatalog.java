package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.entity.achievement.AchievementType;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AchievementCatalog {

    public static final String WELCOME_ABOARD = "WELCOME_ABOARD";
    public static final String PROFILE_COMPLETED = "PROFILE_COMPLETED";

    public static final String FIRST_FAVORITE_GAME = "FIRST_FAVORITE_GAME";
    public static final String FIRST_FRIEND = "FIRST_FRIEND";
    public static final String FRIENDS_25 = "FRIENDS_25";
    public static final String FRIENDS_50 = "FRIENDS_50";
    public static final String FIRST_PARTY_CREATED = "FIRST_PARTY_CREATED";
    public static final String CREATED_PARTIES_25 = "CREATED_PARTIES_25";
    public static final String CREATED_PARTIES_50 = "CREATED_PARTIES_50";
    public static final String FIRST_PARTY_JOINED = "FIRST_PARTY_JOINED";
    public static final String JOINED_PARTIES_25 = "JOINED_PARTIES_25";
    public static final String JOINED_PARTIES_50 = "JOINED_PARTIES_50";
    public static final String FIRST_CHAT_MESSAGE = "FIRST_CHAT_MESSAGE";
    public static final String CHATTERBOX_100 = "CHATTERBOX_100";
    public static final String CHATTERBOX_1000 = "CHATTERBOX_1000";
    public static final String CHATTERBOX_10000 = "CHATTERBOX_10000";
    public static final String FIRST_GAME_COMPLETED = "FIRST_GAME_COMPLETED";
    public static final String COMPLETED_PARTIES_25 = "COMPLETED_PARTIES_25";
    public static final String COMPLETED_PARTIES_50 = "COMPLETED_PARTIES_50";
    public static final String FOOTER_TRIPLE_CLICK = "FOOTER_TRIPLE_CLICK";
    public static final String ANSWER_TO_LIFE = "ANSWER_TO_LIFE";

    public static final String SECRET_DOOM_FOUND = "SECRET_DOOM_FOUND";
    public static final String ROOM_OF_REQUIREMENT = "ROOM_OF_REQUIREMENT";
    public static final String NOT_WHAT_YOU_EXPECTED = "NOT_WHAT_YOU_EXPECTED";
    public static final String LAUGH_TALE = "LAUGH_TALE";
    public static final String SECRET_DOOM_COMPLETED = "SECRET_DOOM_COMPLETED";
    public static final String ONE_RING = "ONE_RING";

    public enum ProgressMetric {
        NONE,
        FRIEND_COUNT,
        SENT_MESSAGE_COUNT,
        CREATED_PARTY_COUNT,
        JOINED_PARTY_COUNT,
        COMPLETED_PARTY_COUNT
    }

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
        private final ProgressMetric progressMetric;
        private final int targetProgress;

        public AchievementDefinition(
                String code,
                String title,
                String description,
                AchievementType type,
                String icon,
                String requirementText,
                String secretHint,
                boolean hiddenBeforeUnlock,
                int order,
                ProgressMetric progressMetric,
                int targetProgress
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
            this.progressMetric = progressMetric;
            this.targetProgress = targetProgress;
        }

        public boolean hasDynamicProgress() {
            return progressMetric != ProgressMetric.NONE && targetProgress > 0;
        }

        public boolean showsProgressBar() {
            return targetProgress > 1;
        }
    }

    private final List<AchievementDefinition> definitions = new ArrayList<>();
    private final Map<String, AchievementDefinition> byCode = new LinkedHashMap<>();

    @PostConstruct
    void init() {
        register(new AchievementDefinition(
                WELCOME_ABOARD,
                "Hey you. You're finally awake.",
                "Ласкаво просимо до TheSpawnPoint! Твоя пригода починається тут.",
                AchievementType.STANDARD,
                "welcome",
                "Зареєструйся на платформі.",
                null,
                false,
                5,
                ProgressMetric.NONE,
                0
        ));
        register(new AchievementDefinition(
                PROFILE_COMPLETED,
                "ПОВНА ГОТОВНІСТЬ",
                "Твій профіль заповнений на 100% — тепер тебе точно помітять у будь-якому загоні.",
                AchievementType.STANDARD,
                "profile-complete",
                "Заповни всі поля профілю (соціальні мережі не враховуються).",
                null,
                false,
                10,
                ProgressMetric.NONE,
                0
        ));
        register(new AchievementDefinition(
                FIRST_FAVORITE_GAME,
                "ПЕРШИЙ ПІК",
                "Додай першу улюблену гру до свого профілю.",
                AchievementType.STANDARD,
                "favorite",
                "Додай одну гру в обрані.",
                null,
                false,
                20,
                ProgressMetric.NONE,
                0
        ));
        register(new AchievementDefinition(
                FIRST_FRIEND,
                "У КОМАНДІ КРАЩЕ",
                "Отримай першого друга на платформі.",
                AchievementType.STANDARD,
                "friends-1",
                "Прийми або отримай першу дружбу.",
                null,
                false,
                30,
                ProgressMetric.FRIEND_COUNT,
                1
        ));
        register(new AchievementDefinition(
                FRIENDS_25,
                "СВОЯ ТУСОВКА",
                "Збери 25 друзів у своєму списку.",
                AchievementType.STANDARD,
                "friends-25",
                "Додай 25 друзів.",
                null,
                false,
                35,
                ProgressMetric.FRIEND_COUNT,
                25
        ));
        register(new AchievementDefinition(
                FRIENDS_50,
                "ПОВНИЙ РОСТЕР",
                "Заповни ліміт і зберіть повний список із 50 друзів.",
                AchievementType.STANDARD,
                "friends-50",
                "Додай 50 друзів.",
                null,
                false,
                36,
                ProgressMetric.FRIEND_COUNT,
                50
        ));
        register(new AchievementDefinition(
                FIRST_PARTY_CREATED,
                "ЗБІР ЗАГОНУ",
                "Створи своє перше лобі.",
                AchievementType.STANDARD,
                "party-created",
                "Створи перше лобі.",
                null,
                false,
                40,
                ProgressMetric.NONE,
                0
        ));
        register(new AchievementDefinition(
                CREATED_PARTIES_25,
                "ОРГАНІЗАТОР ЗАГОНІВ",
                "Створи 25 лобі для спільної гри.",
                AchievementType.STANDARD,
                "party-created-25",
                "Створи 25 лобі.",
                null,
                false,
                41,
                ProgressMetric.CREATED_PARTY_COUNT,
                25
        ));
        register(new AchievementDefinition(
                CREATED_PARTIES_50,
                "АРХІТЕКТОР РЕЙДІВ",
                "Створи 50 лобі для спільної гри.",
                AchievementType.STANDARD,
                "party-created-50",
                "Створи 50 лобі.",
                null,
                false,
                42,
                ProgressMetric.CREATED_PARTY_COUNT,
                50
        ));
        register(new AchievementDefinition(
                FIRST_PARTY_JOINED,
                "ПРИЄДНАНО ДО РЕЙДУ",
                "Приєднайся до чужого лобі.",
                AchievementType.STANDARD,
                "party-joined-1",
                "Увійди в чуже лобі.",
                null,
                false,
                50,
                ProgressMetric.NONE,
                0
        ));
        register(new AchievementDefinition(
                JOINED_PARTIES_25,
                "СВІЙ У БУДЬ-ЯКОМУ СКВАДІ",
                "Приєднайся до 25 різних лобі.",
                AchievementType.STANDARD,
                "party-joined-25",
                "Приєднайся до 25 чужих лобі.",
                null,
                false,
                51,
                ProgressMetric.JOINED_PARTY_COUNT,
                25
        ));
        register(new AchievementDefinition(
                JOINED_PARTIES_50,
                "МАНДРІВНИК РЕЙДІВ",
                "Приєднайся до 50 різних лобі.",
                AchievementType.STANDARD,
                "party-joined-50",
                "Приєднайся до 50 чужих лобі.",
                null,
                false,
                52,
                ProgressMetric.JOINED_PARTY_COUNT,
                50
        ));
        register(new AchievementDefinition(
                FIRST_CHAT_MESSAGE,
                "ПЕРШИЙ СИГНАЛ",
                "Надішли перше повідомлення в чаті.",
                AchievementType.STANDARD,
                "chat-1",
                "Надішли 1 повідомлення.",
                null,
                false,
                55,
                ProgressMetric.SENT_MESSAGE_COUNT,
                1
        ));
        register(new AchievementDefinition(
                CHATTERBOX_100,
                "РОЗІГРІВ ЧАТУ",
                "Надішли 100 повідомлень у чатах.",
                AchievementType.STANDARD,
                "chat-100",
                "Надішли 100 повідомлень.",
                null,
                false,
                56,
                ProgressMetric.SENT_MESSAGE_COUNT,
                100
        ));
        register(new AchievementDefinition(
                CHATTERBOX_1000,
                "ГОЛОС СПІЛЬНОТИ",
                "Надішли 1000 повідомлень у чатах.",
                AchievementType.STANDARD,
                "chat-1000",
                "Надішли 1000 повідомлень.",
                null,
                false,
                57,
                ProgressMetric.SENT_MESSAGE_COUNT,
                1000
        ));
        register(new AchievementDefinition(
                CHATTERBOX_10000,
                "ЛЕГЕНДА КАНАЛУ",
                "Надішли 10 000 повідомлень у чатах.",
                AchievementType.STANDARD,
                "chat-10000",
                "Надішли 10 000 повідомлень.",
                null,
                false,
                58,
                ProgressMetric.SENT_MESSAGE_COUNT,
                10000
        ));
        register(new AchievementDefinition(
                FIRST_GAME_COMPLETED,
                "ГРА ЗАВЕРШЕНА",
                "Доведи першу спільну гру до завершення.",
                AchievementType.STANDARD,
                "game-completed-1",
                "Заверши хоча б одну гру разом із лобі.",
                null,
                false,
                60,
                ProgressMetric.NONE,
                0
        ));
        register(new AchievementDefinition(
                COMPLETED_PARTIES_25,
                "ВЕТЕРАН ЗАБІГІВ",
                "Заверши 25 лобі разом із командою.",
                AchievementType.STANDARD,
                "game-completed-25",
                "Заверши 25 лобі.",
                null,
                false,
                61,
                ProgressMetric.COMPLETED_PARTY_COUNT,
                25
        ));
        register(new AchievementDefinition(
                COMPLETED_PARTIES_50,
                "КОМАНДИР КАМПАНІЙ",
                "Заверши 50 лобі разом із командою.",
                AchievementType.STANDARD,
                "game-completed-50",
                "Заверши 50 лобі.",
                null,
                false,
                62,
                ProgressMetric.COMPLETED_PARTY_COUNT,
                50
        ));
        register(new AchievementDefinition(
                FOOTER_TRIPLE_CLICK,
                "ПІДПІЛЬНИЙ СИГНАЛ",
                "Ти знайшов прихований тригер у підвалі сайту.",
                AchievementType.SECRET,
                "footer-secret",
                null,
                "Іноді найцікавіше ховається внизу сторінки. Не завжди достатньо одного натискання.",
                true,
                1000,
                ProgressMetric.NONE,
                0
        ));
        register(new AchievementDefinition(
                ANSWER_TO_LIFE,
                "Відповідь на питання життя",
                "У правильно поставленому запитанні міститься половина відповіді.",
                AchievementType.SECRET,
                "life-answer",
                null,
                "Підказка: це відповідь на питання життя, Всесвіту і взагалі.",
                false,
                1005,
                ProgressMetric.NONE,
                0
        ));
        register(new AchievementDefinition(
                SECRET_DOOM_FOUND,
                "RIP AND TEAR",
                "Як ти сюди потрапив?",
                AchievementType.SECRET,
                "doom-found",
                null,
                "Найтеємничніша таємниця в історії сайту; Слеш секрет.",
                true,
                1010,
                ProgressMetric.NONE,
                0
        ));
        register(new AchievementDefinition(
                ROOM_OF_REQUIREMENT,
                "Кімната на вимогу",
                "Ти знайшов двері, які з'являються лише тим, хто достатньо наполегливо шукає.",
                AchievementType.SECRET,
                "room-secret",
                null,
                "Іноді там, де розмова ще не почалась, варто постукати кілька разів у саму тишу.",
                true,
                1015,
                ProgressMetric.NONE,
                0
        ));
        register(new AchievementDefinition(
                NOT_WHAT_YOU_EXPECTED,
                "Не те на що ти очікував, еге ж?",
                "Ти все ж таки відкрив ці двері. Наслідки були... музичними.",
                AchievementType.SECRET,
                "rickroll-secret",
                null,
                "Іноді краще не перевіряти, що саме ховається по той бік магічних дверей.",
                true,
                1016,
                ProgressMetric.NONE,
                0
        ));
        register(new AchievementDefinition(
                LAUGH_TALE,
                "Лафтель",
                "Чотири червоні камені зійшлися в одну точку. Острів сміху нарешті знайдено.",
                AchievementType.SECRET,
                "laugh-tale",
                null,
                "Чотири червоні камені не назвуть острів напряму. Вони лише залишать точки на карті.",
                false,
                1018,
                ProgressMetric.NONE,
                0
        ));
        register(new AchievementDefinition(
                SECRET_DOOM_COMPLETED,
                "IDDQD",
                "Ти знаєш секретний код. God mode activated.",
                AchievementType.SECRET,
                "doom-completed",
                null,
                "Класичний чіт-код — ключ до безсмертя.",
                true,
                1020,
                ProgressMetric.NONE,
                0
        ));
        register(new AchievementDefinition(
                ONE_RING,
                "Одне кільце, щоб правити всіма",
                "Ash nazg durbatulûk, ash nazg gimbatul, ash nazg thrakatulûk, agh burzum-ishi krimpatul.",
                AchievementType.SECRET,
                "one-ring",
                null,
                "Не все золото блищить, але дещо вміло ховається біля самого краю.",
                true,
                1025,
                ProgressMetric.NONE,
                0
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
