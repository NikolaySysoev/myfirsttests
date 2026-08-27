package api.models.v2.responses;

import api.models.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ на смену имени в актуальной версии.
 * <p>
 * Контракт изменился сильнее, чем у остальных эндпоинтов: легаси отдавал обёртку
 * {@code {"customer": {...}, "message": "..."}}, а здесь приходит сам клиент,
 * плоско и без сообщения об успехе.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeNameResponse extends BaseModel {
    private long id;
    private String username;
    private String name;
    private String role;
}
