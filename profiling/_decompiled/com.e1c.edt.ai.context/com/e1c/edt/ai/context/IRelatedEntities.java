package com.e1c.edt.ai.context;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.context.DTO.RelatedEntitiesRequest;
import com.e1c.edt.ai.context.DTO.RelatedEntitiesResponse;
import java.util.Optional;

public interface IRelatedEntities {
   Optional<RelatedEntitiesResponse> getRelatedEntities(RelatedEntitiesRequest var1, ICancellationToken var2);
}
