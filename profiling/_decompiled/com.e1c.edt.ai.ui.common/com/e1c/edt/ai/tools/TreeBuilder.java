package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.IMarkdownUtils;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class TreeBuilder implements ITreeBuilder {
   private final IMarkdownUtils markdownUtils;
   private final StringBuilder tree = new StringBuilder();
   private final List<Integer> stack = new ArrayList();

   @Inject
   public TreeBuilder(IMarkdownUtils markdownUtils) {
      this.markdownUtils = markdownUtils;
   }

   public void addDirectory(String name, int depth) {
      if (depth > 0) {
         this.tree.append("\n");
      }

      for(int i = 0; i < depth; ++i) {
         if (i < this.stack.size() && (Integer)this.stack.get(i) > 0) {
            this.tree.append(" │  ");
         } else if (i < depth - 1) {
            this.tree.append("    ");
         }
      }

      if (depth > 0 && this.stack.size() > depth - 1) {
         this.stack.set(depth - 1, (Integer)this.stack.get(depth - 1) - 1);
      }

      if (depth > 0) {
         boolean hasMore = depth < this.stack.size() && (Integer)this.stack.get(depth) > 0;
         this.tree.append(hasMore ? " ├── " : " └── ");
      }

      this.tree.append(this.markdownUtils.escapeForMarkdown(name)).append("/");

      while(this.stack.size() <= depth) {
         this.stack.add(0);
      }

      this.stack.set(depth, (Integer)this.stack.get(depth) + 1);
   }

   public void addFile(String name, int depth) {
      this.tree.append("\n");

      for(int i = 0; i < depth; ++i) {
         if (i < this.stack.size() && (Integer)this.stack.get(i) > 0) {
            this.tree.append(" │  ");
         } else {
            this.tree.append("    ");
         }
      }

      if (this.stack.size() > depth) {
         this.stack.set(depth, (Integer)this.stack.get(depth) - 1);
         boolean hasMore = (Integer)this.stack.get(depth) > 0;
         this.tree.append(hasMore ? " ├── " : " └── ");
      } else {
         this.tree.append("  ├── ");
      }

      this.tree.append(this.markdownUtils.escapeForMarkdown(name));
   }

   public void endDirectory() {
   }

   public String build() {
      return this.tree.toString();
   }
}
