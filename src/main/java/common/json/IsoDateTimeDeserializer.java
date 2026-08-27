package common.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Разбирает ISO-8601 дату-время в {@link LocalDateTime}.
 * <p>
 * Нужен потому, что в проекте подключён только {@code jackson-databind}, без
 * {@code jackson-datatype-jsr310}: у Jackson «из коробки» вообще нет
 * десериализатора для типов {@code java.time}, и {@code @JsonFormat} тут не
 * помогает — форматировать нечем. Этот класс закрывает пробел без правки pom.xml.
 * <p>
 * Поддерживает оба варианта строки:
 * <ul>
 *     <li>со смещением — {@code 2026-08-27T06:30:38.363Z}, {@code ...+03:00};</li>
 *     <li>без смещения — {@code 2026-08-27T06:30:38.363}.</li>
 * </ul>
 * <p>
 * ВАЖНО про часовой пояс: строка со смещением задаёт момент времени, а
 * {@link LocalDateTime} пояса не хранит. Момент переводится в системный пояс
 * машины, где идёт прогон — чтобы сравнение с {@code LocalDateTime.now()} в
 * тестах давало ожидаемый результат, а не расхождение на величину смещения.
 * Если нужно сохранять именно UTC-время как оно пришло, заменить
 * {@code ZoneId.systemDefault()} на {@code ZoneOffset.UTC}.
 */
public class IsoDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String raw = parser.getValueAsString();
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            // вариант со смещением: ...Z или ...+03:00
            return OffsetDateTime.parse(raw)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (DateTimeParseException withOffsetFailed) {
            try {
                // вариант без смещения
                return LocalDateTime.parse(raw);
            } catch (DateTimeParseException withoutOffsetFailed) {
                throw new IOException(
                        "Не удалось разобрать дату \"" + raw + "\". Ожидается ISO-8601, "
                                + "например 2026-08-27T06:30:38.363Z или 2026-08-27T06:30:38.363",
                        withoutOffsetFailed);
            }
        }
    }
}
