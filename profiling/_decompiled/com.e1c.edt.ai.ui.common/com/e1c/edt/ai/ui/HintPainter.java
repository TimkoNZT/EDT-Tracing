package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.IHintTextBuilder;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.OS;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.ScrollBar;

class HintPainter implements IHintPainter {
   private static final char CONTINUATION_SIGN = '…';
   private static final int BORDER = 1;
   private static final int TEXT_EXTENT_FLAGS = 15;
   private final IHintTextBuilder hintTextBuilder;
   private final ISettings settings;
   private final IUserActions userActions;
   private final IGCTools gcTools;
   private final IEnvironment environment;
   private String hintText = "";
   private String nextToken = "";
   private String displayedHintText = "";
   private String suffix = "";
   private String prefix = "";
   private int acceptedTokens;
   private StyledText textWidget;
   private int pinnedOffset = -1;
   private boolean showBlank;
   private boolean isSingleWordMode;
   private int vBarMax = 0;

   @Inject
   public HintPainter(IHintTextBuilder hintTextBuilder, ISettings uiSettings, IUserActions userActions, IGCTools gcTools, IEnvironment environment) {
      Preconditions.checkNotNull(hintTextBuilder);
      Preconditions.checkNotNull(uiSettings);
      Preconditions.checkNotNull(userActions);
      Preconditions.checkNotNull(gcTools);
      Preconditions.checkNotNull(environment);
      this.hintTextBuilder = hintTextBuilder;
      this.settings = uiSettings;
      this.userActions = userActions;
      this.gcTools = gcTools;
      this.environment = environment;
   }

   public synchronized void pinOffset(StyledText textWidget, int offset, boolean showBlank, boolean isSingleWordMode) {
      this.textWidget = textWidget;
      this.pinnedOffset = offset;
      this.showBlank = showBlank;
      this.isSingleWordMode = isSingleWordMode;
      if (textWidget != null) {
         ScrollBar vBar = textWidget.getVerticalBar();
         this.vBarMax = vBar.getMaximum();
      }

   }

   public synchronized String getHintText() {
      return this.hintText;
   }

   public synchronized String getDisplayedHintText() {
      return this.displayedHintText;
   }

   public synchronized int getOffset() {
      return this.pinnedOffset;
   }

   public synchronized void reset() {
      this.pinnedOffset = -1;
      this.setHintAt((String)null, "", 0);
   }

   public synchronized void setHintAt(String hintText, String nextToken, int acceptedTokens) {
      if (hintText == null) {
         this.pinnedOffset = -1;
         this.displayedHintText = "";
         this.suffix = "";
         this.prefix = "";
         this.vBarMax = 0;
         this.hintText = "";
         this.nextToken = "";
         this.acceptedTokens = 0;
      } else {
         this.hintText = hintText;
         this.nextToken = nextToken;
         this.acceptedTokens = acceptedTokens;
         if (this.pinnedOffset >= 0 && hintText != null && this.textWidget != null) {
            String hint = hintText;
            String text = this.textWidget.getText();
            int line;
            if (this.pinnedOffset < text.length()) {
               line = this.textWidget.getLineAtOffset(this.pinnedOffset);
            } else {
               line = this.textWidget.getLineCount() - 1;
            }

            int lineOffset = this.textWidget.getOffsetAtLine(line);
            String lineText = this.textWidget.getLine(line);
            this.prefix = lineOffset < this.pinnedOffset ? this.textWidget.getText(lineOffset, this.pinnedOffset - 1) : "";
            int suffixEnd = lineOffset + lineText.length();
            int totalLength = text.length();
            if (suffixEnd >= totalLength) {
               suffixEnd = totalLength - 1;
            }

            this.suffix = lineOffset < this.pinnedOffset && suffixEnd > 0 && this.pinnedOffset < suffixEnd ? this.textWidget.getText(this.pinnedOffset, suffixEnd) : lineText;
            if (!this.isSingleWordMode || hintText.length() == 0) {
               hint = this.hintTextBuilder.build(this.prefix, hintText, this.settings.getTabWidth()) + '…';
            }

            this.displayedHintText = hint;
         }

      }
   }

   public synchronized void paintControl(PaintEvent event) {
      Preconditions.checkNotNull(event);
      if (this.pinnedOffset != -1 && !event.gc.isDisposed() && this.textWidget != null && !this.textWidget.isDisposed()) {
         if (this.showBlank || !this.hintText.isBlank()) {
            int firstLineFinish = this.displayedHintText.indexOf(10);
            String firstLine = "";
            String otherLines = "";
            if (firstLineFinish >= 0) {
               firstLine = this.displayedHintText.substring(0, firstLineFinish);
               otherLines = this.displayedHintText.substring(firstLineFinish + 1);
            } else {
               firstLine = this.displayedHintText;
            }

            String token = this.hintTextBuilder.build(this.prefix, this.nextToken, this.settings.getTabWidth());
            token = firstLine.startsWith(token) ? token : "";
            this.drawHint(event.gc, this.textWidget, token, firstLine, otherLines, this.suffix);
         }
      }
   }

   private void drawHint(GC gc, StyledText textWidget, String nextToken, String firstLine, String otherLines, String suffix) {
      if (this.vBarMax > 0) {
         int currentLine = textWidget.getLineAtOffset(textWidget.getCaretOffset());
         int totalLines = textWidget.getLineCount();
         long difLines = (long)currentLine + this.displayedHintText.lines().count() + 1L - (long)totalLines;
         if (difLines > 0L) {
            ScrollBar vBar = textWidget.getVerticalBar();
            vBar.setMaximum((int)((long)this.vBarMax + difLines * (long)textWidget.getLineHeight()));
         }
      }

      Point zeroLocation = textWidget.getLocationAtOffset(0);
      Point caretLocation = textWidget.getLocationAtOffset(this.pinnedOffset);
      int x = caretLocation.x;
      int y = caretLocation.y;
      gc.setAdvanced(true);
      gc.setBackground(textWidget.getBackground());
      gc.setForeground(textWidget.getForeground());
      Font font = textWidget.getFont();
      FontData fontData = font.getFontData()[0];
      fontData.setStyle(2);
      Font hintFont = new Font(font.getDevice(), fontData);
      fontData.setStyle(3);
      Font firstTokenFont = new Font(font.getDevice(), fontData);
      fontData.setStyle(0);
      fontData.setHeight((int)((double)fontData.getHeight() * (double)0.75F));
      Font labelFont = new Font(font.getDevice(), fontData);

      try {
         Rectangle bounds = gc.getClipping();
         if (bounds != null) {
            int boundsWidth = bounds.width - 2 - 1;
            gc.setFont(hintFont);
            Point firstLineSize = gc.textExtent(firstLine, 15);
            int firstLineX = x - 1 + 1;
            int firstLineW = firstLineSize.x + 4;
            int firstLineH = firstLineSize.y + 1;
            Point otherLinesSize = gc.textExtent(otherLines, 15);
            int otherLinesX = 2;
            int otherLinesY = y + firstLineH;
            int otherLinesW = boundsWidth;
            int otherLinesH = otherLinesSize.y;
            if (otherLines.isEmpty()) {
               otherLinesX = firstLineX;
               otherLinesY = y;
               otherLinesW = 0;
               otherLinesH = 0;
            }

            gc.setFont(labelFont);
            String codeCompletionLabels = Integer.toString(this.acceptedTokens) + '┆' + this.userActions.getCodeCompletionLabels('┆');
            Point labelSize = gc.textExtent(codeCompletionLabels, 15);
            int labelX = 2;
            int labelY = y + firstLineH + otherLinesH;
            int labelW = labelSize.x + 4;
            int labelH = labelSize.y;
            int l = Integer.max(Integer.max(otherLinesX + otherLinesW, labelX + labelW), boundsWidth);
            otherLinesW = l - otherLinesX;
            labelW = l - labelX;
            labelX = labelW - labelSize.x - 1;
            if (otherLinesY >= 0 && this.environment.getOS() == OS.WINDOWS) {
               if (firstLine.length() > 0 && !suffix.isBlank() && bounds.width > firstLineX) {
                  this.gcTools.copyArea(gc, firstLineX, y, bounds.width - firstLineX, firstLineH, firstLineX + firstLineW, y);
               }

               if (otherLines.length() > 0) {
                  this.gcTools.copyArea(gc, bounds.x, otherLinesY, bounds.width, bounds.height, bounds.x, otherLinesY + otherLinesH);
               }
            }

            gc.fillRectangle(firstLineX, y, firstLineW, firstLineH);
            if (otherLines.length() > 0) {
               gc.fillRectangle(otherLinesX, otherLinesY, otherLinesW, otherLinesH);
               gc.fillRectangle(labelX, labelY, labelW, labelH);
            }

            if (!nextToken.isEmpty()) {
               gc.setAlpha(180);
               fontData.setStyle(1);
               gc.setFont(firstTokenFont);
               gc.drawText(nextToken, firstLineX + 2, y, true);
               gc.setFont(hintFont);
               gc.setAlpha(150);
               Point nextTokenSize = gc.stringExtent(nextToken);
               gc.drawText(firstLine.substring(nextToken.length()), firstLineX + nextTokenSize.x, y, true);
               if (nextToken.length() > 0 && nextToken.charAt(0) != 8230) {
                  gc.setAlpha(150);
                  gc.drawLine(firstLineX + 3, y + firstLineSize.y - 1, firstLineX + nextTokenSize.x, y + firstLineSize.y - 1);
               }
            } else {
               gc.setAlpha(150);
               gc.setFont(firstTokenFont);
               gc.drawText(firstLine, firstLineX + 2, y, true);
            }

            gc.setAlpha(120);
            gc.setFont(hintFont);
            gc.drawText(otherLines, otherLinesX + zeroLocation.x, otherLinesY, true);
            gc.setLineStyle(3);
            if (otherLines.isEmpty()) {
               gc.drawPolyline(new int[]{firstLineX, y, firstLineX + firstLineW, y, firstLineX + firstLineW, y + firstLineH, firstLineX, y + firstLineH, firstLineX, y});
            } else {
               gc.setFont(labelFont);
               gc.drawText(codeCompletionLabels, labelX + 2, labelY + 1, true);
               gc.drawPolyline(new int[]{otherLinesX, otherLinesY, firstLineX, otherLinesY, firstLineX, y, firstLineX + firstLineW, y, firstLineX + firstLineW, otherLinesY, otherLinesX + otherLinesW, otherLinesY});
               gc.drawPolyline(new int[]{labelX, otherLinesY + otherLinesH, otherLinesX + otherLinesW, otherLinesY + otherLinesH, otherLinesX + otherLinesW, otherLinesY + otherLinesH + labelH, labelX, otherLinesY + otherLinesH + labelH, labelX, labelY, otherLinesX, labelY});
            }

            return;
         }
      } finally {
         hintFont.dispose();
         firstTokenFont.dispose();
         labelFont.dispose();
         gc.setFont(font);
      }

   }
}
