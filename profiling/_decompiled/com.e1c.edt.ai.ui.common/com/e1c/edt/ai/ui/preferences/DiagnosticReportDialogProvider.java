package com.e1c.edt.ai.ui.preferences;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.DiagnosticResult;
import com.e1c.edt.ai.assistent.DiagnosticSeverity;
import com.e1c.edt.ai.assistent.IDiagnosticContext;
import com.e1c.edt.ai.assistent.IDiagnosticTest;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;

public class DiagnosticReportDialogProvider implements IDiagnosticReportDialogProvider {
   private static final String PLUGIN_ID = "com.e1c.edt.ai";
   private static final String SEP = System.lineSeparator() + "------------------------" + System.lineSeparator();
   private final ISettings settings;
   // $FF: synthetic field
   private static volatile int[] $SWITCH_TABLE$com$e1c$edt$ai$assistent$DiagnosticSeverity;

   @Inject
   public DiagnosticReportDialogProvider(ISettings settings) {
      Preconditions.checkNotNull(settings);
      this.settings = settings;
   }

   public void openErrorDialog(Shell shell, IDiagnosticTest test, DiagnosticResult r, IDiagnosticContext ctx) {
      String title = Messages.DiagnosticReportDialog_Report;
      String msg = this.safe(r.getMessage());
      if (msg.isBlank()) {
         msg = (test != null ? test.title() : Messages.DiagnosticReportDialog_Test + " ") + Messages.DiagnosticReportDialog_EmptyMessage;
      }

      int severity = this.toIStatusSeverity(r.getSeverity());
      IStatus status = new Status(severity, "com.e1c.edt.ai", msg);
      msg = (test != null ? test.title() : Messages.DiagnosticReportDialog_Test + " ") + " " + Messages.TestReportDialog_DiagnosticNotPassed;
      int mask = 7;
      String tail = r.getRemidiation().getUrlPath();
      if (tail == null || tail.isEmpty()) {
         tail = "troubleshooting/";
      }

      String url = this.settings.getHomePage() + tail;
      String supportLogText = this.buildSupportLog(test, r, ctx);
      (new TestReportDialog(shell, title, msg, status, mask, url, supportLogText)).open();
   }

   private int toIStatusSeverity(DiagnosticSeverity s) {
      if (s == null) {
         return 4;
      } else {
         switch (s) {
            case OK:
               return 0;
            case DEFAULT:
               return 8;
            case ERROR:
            default:
               return 4;
         }
      }
   }

   private String buildSupportLog(IDiagnosticTest test, DiagnosticResult r, IDiagnosticContext ctx) {
      if (ctx != null && ctx.getCAReport() == null) {
         ctx.setCaReportIfAbsent(ctx.getCaCertificateReporter().buildPlainLog());
      }

      StringBuilder sb = new StringBuilder(48000);
      sb.append("=== AI DIAGNOSTICS LOG ===\n");
      sb.append("time=").append(ZonedDateTime.now()).append("\n");
      sb.append("test.id=").append(test != null ? this.safe(test.id()) : "").append("\n");
      sb.append("test.title=").append(test != null ? this.safe(test.title()) : "").append("\n");
      sb.append("severity=").append(String.valueOf(r.getSeverity())).append("\n\n");
      sb.append("[USER MESSAGE]\n");
      sb.append(this.safe(r.getMessage())).append("\n\n");
      sb.append("[FACTS]\n");
      Map<String, String> facts = r.getFacts();
      if (facts != null && !facts.isEmpty()) {
         for(Map.Entry<String, String> e : facts.entrySet()) {
            sb.append(this.safe((String)e.getKey())).append(" = ").append(this.safe((String)e.getValue())).append("\n");
         }
      } else {
         sb.append("(no facts)\n");
      }

      sb.append("java.version = ").append(System.getProperty("java.version")).append("\n");
      sb.append("java.vendor  = ").append(System.getProperty("java.vendor")).append("\n");
      sb.append("java.home    = ").append(System.getProperty("java.home")).append("\n\n");
      sb.append("[TECHNICAL - FOR SUPPORT ONLY]\n");
      if (r.getTechnical() != null) {
         sb.append("exception=").append(r.getTechnical().getClass().getName()).append("\n");
         sb.append("message=").append(String.valueOf(r.getTechnical().getMessage())).append("\n");
      }

      sb.append(SEP);
      String log = this.safe(r.getLog());
      if (!log.isBlank()) {
         sb.append(log).append("\n");
      } else {
         sb.append("(no stacktrace captured)\n");
      }

      sb.append("\n");
      sb.append("[CA CERTIFICATES - TRUSTSTORE]\n");
      if (ctx != null && ctx.getCAReport() != null) {
         sb.append(ctx.getCAReport());
      } else {
         sb.append("(CA report not collected)\n");
      }

      sb.append("\n=== END AI DIAGNOSTICS LOG ===\n");
      return sb.toString();
   }

   private String safe(String s) {
      return s == null ? "" : s;
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$com$e1c$edt$ai$assistent$DiagnosticSeverity() {
      int[] var10000 = $SWITCH_TABLE$com$e1c$edt$ai$assistent$DiagnosticSeverity;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[DiagnosticSeverity.values().length];

         try {
            var0[DiagnosticSeverity.DEFAULT.ordinal()] = 2;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[DiagnosticSeverity.ERROR.ordinal()] = 3;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[DiagnosticSeverity.OK.ordinal()] = 1;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$com$e1c$edt$ai$assistent$DiagnosticSeverity = var0;
         return var0;
      }
   }
}
