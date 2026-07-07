package com.e1c.edt.ai.context;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.context.DTO.EntityInfoRequest;
import com.e1c.edt.ai.context.DTO.EntityInfoResponse;
import java.util.Optional;

public interface IEntityInfo {
   Optional<EntityInfoResponse> getInfo(EntityInfoRequest var1, ICancellationToken var2);
}
