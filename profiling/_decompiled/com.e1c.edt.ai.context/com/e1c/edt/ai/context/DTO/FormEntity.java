package com.e1c.edt.ai.context.DTO;

import com.e1c.edt.ai.assistent.model.IContextEntity;
import java.util.List;

public class FormEntity extends FormGroupEntity implements IContextEntity {
   public List<AttributeEntity> attributes;
   public List<FormParameterEntity> parameters;
}
