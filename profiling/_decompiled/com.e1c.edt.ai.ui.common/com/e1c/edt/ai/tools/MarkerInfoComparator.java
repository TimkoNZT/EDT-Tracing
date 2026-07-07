package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.assistent.model.MarkerInfo;
import java.util.Comparator;

public class MarkerInfoComparator implements Comparator<MarkerInfo> {
   public int compare(MarkerInfo m1, MarkerInfo m2) {
      int severityCompare = this.compareSeverity(m1.severity, m2.severity);
      return severityCompare != 0 ? severityCompare : this.comparePriority(m1.priority, m2.priority);
   }

   private int compareSeverity(String severity1, String severity2) {
      int severityValue1 = this.getSeverityValue(severity1);
      int severityValue2 = this.getSeverityValue(severity2);
      return Integer.compare(severityValue2, severityValue1);
   }

   private int getSeverityValue(String severity) {
      if ("error".equals(severity)) {
         return 3;
      } else if ("warning".equals(severity)) {
         return 2;
      } else {
         return "info".equals(severity) ? 1 : 0;
      }
   }

   private int comparePriority(String priority1, String priority2) {
      int priorityValue1 = this.getPriorityValue(priority1);
      int priorityValue2 = this.getPriorityValue(priority2);
      return Integer.compare(priorityValue2, priorityValue1);
   }

   private int getPriorityValue(String priority) {
      if ("high".equals(priority)) {
         return 3;
      } else if ("normal".equals(priority)) {
         return 2;
      } else {
         return "low".equals(priority) ? 1 : 0;
      }
   }
}
