package api.requests.skelethon;

import api.configs.BackendVersion;
import api.models.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;

/**
 * Контракт одного эндпоинта в рамках одной версии бэкенда:
 * путь, модель запроса и модель ответа.
 * <p>
 * {@link Endpoint} хранит по такому контракту на каждую поддерживаемую версию
 * и отдаёт нужный по активной версии теста.
 */
@Getter
@AllArgsConstructor
public class Contract {

    private final String url;
    private final Class<? extends BaseModel> requestModel;
    private final Class<? extends BaseModel> responseModel;

    /**
     * Контракт, одинаковый для всех версий бэкенда.
     * <p>
     * Используется, пока для эндпоинта не появились отдельные модели новой версии:
     * тогда все версии работают на одних и тех же DTO. Как только модели разъедутся,
     * строка в {@link Endpoint} заменяется явной мапой версия -> контракт.
     * <p>
     * Перебирает {@link BackendVersion#values()}, поэтому новая версия в enum'е
     * автоматически подхватывает уже описанные общие контракты.
     */
    public static Map<BackendVersion, Contract> sameForAllVersions(
            String url,
            Class<? extends BaseModel> requestModel,
            Class<? extends BaseModel> responseModel) {

        Contract shared = new Contract(url, requestModel, responseModel);
        Map<BackendVersion, Contract> contracts = new EnumMap<>(BackendVersion.class);
        for (BackendVersion version : BackendVersion.values()) {
            contracts.put(version, shared);
        }
        return contracts;
    }
}
