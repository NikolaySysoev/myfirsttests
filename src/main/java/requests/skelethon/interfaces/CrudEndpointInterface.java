package requests.skelethon.interfaces;

import models.BaseModel;

public interface CrudEndpointInterface {
    Object post(BaseModel model);
    Object get(long id);
    Object get();
    Object put(long id, BaseModel model);
    Object put(BaseModel model);
    Object delete(long id);
}
