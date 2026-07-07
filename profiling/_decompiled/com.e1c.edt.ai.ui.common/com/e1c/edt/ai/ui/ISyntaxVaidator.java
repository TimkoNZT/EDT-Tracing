package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.CodeMethod;
import com.e1c.edt.ai.ICancellationToken;

interface ISyntaxVaidator {
   String getValidHint(CodeMethod var1, String var2, int var3, String var4, ICancellationToken var5);
}
