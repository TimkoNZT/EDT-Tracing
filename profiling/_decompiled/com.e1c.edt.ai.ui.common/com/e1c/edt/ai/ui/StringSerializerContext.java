package com.e1c.edt.ai.ui;

import com.google.common.base.Preconditions;
import org.eclipse.xtext.nodemodel.ILeafNode;

class StringSerializerContext {
   private final StringBuilder text = new StringBuilder();
   private final int cursorNodeOffset;
   private int offset;
   private final int maxLength;
   private boolean achiveCursor;

   public StringSerializerContext(ILeafNode cursorNode, int offset, int maxLength) {
      Preconditions.checkArgument(offset >= 0);
      Preconditions.checkArgument(maxLength > 0);
      this.maxLength = maxLength;
      if (cursorNode != null) {
         this.cursorNodeOffset = cursorNode.getTotalOffset();
         this.offset = offset - this.cursorNodeOffset;
      } else {
         this.cursorNodeOffset = 0;
         this.offset = 0;
      }

   }

   public boolean serialize(ILeafNode node) {
      if (!this.achiveCursor && this.cursorNodeOffset == node.getTotalOffset()) {
         this.offset += this.text.length();
         this.achiveCursor = true;
      }

      this.text.append(node.getText());
      return this.text.length() <= this.maxLength;
   }

   public String getText() {
      return this.text.toString();
   }

   public int getOffset() {
      return this.achiveCursor ? this.offset : this.text.length();
   }
}
