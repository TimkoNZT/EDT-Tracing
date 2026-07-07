package com.e1c.edt.ai.context;

class SourceSpan {
   private final String path;
   private final int start;
   private final int finish;

   public SourceSpan(String path, int start, int finish) {
      this.path = path;
      this.start = start;
      this.finish = finish;
   }

   public String getPath() {
      return this.path;
   }

   public int getStart() {
      return this.start;
   }

   public int getFinish() {
      return this.finish;
   }
}
