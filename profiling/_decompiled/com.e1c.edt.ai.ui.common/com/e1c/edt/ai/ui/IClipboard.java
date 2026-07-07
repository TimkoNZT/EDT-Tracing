package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.assistent.model.ClipboardInfo;
import java.util.Optional;

public interface IClipboard {
   Optional<ClipboardInfo> getClipboardInfo();

   boolean isPasting();
}
