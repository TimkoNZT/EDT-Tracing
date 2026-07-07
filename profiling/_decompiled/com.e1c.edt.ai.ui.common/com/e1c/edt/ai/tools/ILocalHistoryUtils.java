package com.e1c.edt.ai.tools;

import java.util.List;
import org.eclipse.core.resources.IFile;

interface ILocalHistoryUtils {
   List<LocalHistoryEntry> getLocalHistory(IFile var1, int var2) throws Exception;
}
