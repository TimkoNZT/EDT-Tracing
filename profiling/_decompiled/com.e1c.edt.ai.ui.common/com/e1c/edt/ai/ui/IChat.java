package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.IFileDocument;
import com.e1c.edt.ai.McpCallToolsResult;
import java.util.List;

public interface IChat {
   void reviewCode(AIContext var1, String var2);

   void explainCode(AIContext var1, String var2);

   void fixCode(AIContext var1, String var2, String var3);

   void generateDocComments(AIContext var1, String var2);

   void askQuestion(AIContext var1, String var2);

   void addCode(AIContext var1, String var2);

   void addFiles(List<IFileDocument> var1);

   void addToolsResult(String var1, String var2, McpCallToolsResult var3);

   void continueChat(String var1, String var2);
}
