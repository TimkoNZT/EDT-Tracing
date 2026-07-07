package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsSetter;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IStateListener;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.ui.preferences.DiagnosticDialog;
import com.google.inject.Inject;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

public class BaseStatusBarControl extends WorkbenchWindowControlContribution implements IStateListener, DisposeListener, SelectionListener {
   @Inject
   private IStateService stateService;
   @Inject
   private IDispatcher dispatcher;
   @Inject
   private IVersionProvider versionProvider;
   @Inject
   private ISettings settings;
   @Inject
   private ISettingsSetter settingsSetter;
   @Inject
   private IReflection reflection;
   @Inject
   private IThemeManager themeManager;
   @Inject
   private IWeb web;
   @Inject
   private IPreferences preferences;
   private final CodeCompletionPolicy[] policies;
   private final String[] policyNames;
   private Font font;
   private Canvas statusCanvas;
   private CCombo policyCombo;
   private DefaultToolTip policyTooltip;
   private Menu policyMenu;
   private static final RGB COLOR_ONLINE = new RGB(120, 180, 120);
   private static final RGB COLOR_SETTINGS_CHANGED = new RGB(100, 150, 100);
   private static final RGB COLOR_BUSY = new RGB(150, 210, 150);
   private static final RGB COLOR_OFF = new RGB(180, 120, 120);
   private static final RGB COLOR_DISABLED = new RGB(150, 150, 150);
   private static final int ICON_SIZE = 12;
   private static final int ICON_CORNER_RADIUS = 3;
   private static final int STATUS_TEXT_MARGIN = 5;
   private Color colorOnline;
   private Color colorSettingsChanged;
   private Color colorBusy;
   private Color colorOff;
   private Color colorDisabled;
   private AIState lastAIState;
   private String statusText;
   private int policyTextX;
   private int policyTextWidth;
   private boolean isErrorStateWithLink;
   private boolean isMissingTokenState;
   private String currentUrlPath;
   // $FF: synthetic field
   private static volatile int[] $SWITCH_TABLE$com$e1c$edt$ai$ServiceState;

   public BaseStatusBarControl() {
      this.statusText = Messages.AIName + " ";
      this.policyTextX = 0;
      this.policyTextWidth = 0;
      this.isErrorStateWithLink = false;
      this.isMissingTokenState = false;
      this.currentUrlPath = "";
      BaseActivator.injectMembers(this);
      this.policies = new CodeCompletionPolicy[CodeCompletionPolicy.values().length];
      this.policyNames = new String[CodeCompletionPolicy.values().length];

      CodeCompletionPolicy[] var4;
      for(CodeCompletionPolicy codeCompletionPolicy : var4 = CodeCompletionPolicy.values()) {
         this.policies[codeCompletionPolicy.getIndex()] = codeCompletionPolicy;
         this.policyNames[codeCompletionPolicy.getIndex()] = codeCompletionPolicy.getName().toLowerCase();
      }

   }

   protected Control createControl(Composite parent) {
      Composite composite = new Composite(parent, 0);
      GridLayout gridLayout = new GridLayout(1, false);
      gridLayout.marginWidth = 2;
      gridLayout.marginHeight = 0;
      gridLayout.marginTop = 0;
      gridLayout.marginBottom = 0;
      gridLayout.verticalSpacing = 0;
      composite.setLayout(gridLayout);
      this.statusCanvas = new Canvas(composite, 0);
      this.statusCanvas.addPaintListener(new PaintListener() {
         public void paintControl(PaintEvent e) {
            BaseStatusBarControl.this.paintStatus(e.gc);
         }
      });
      this.statusCanvas.addListener(4, new Listener() {
         public void handleEvent(Event event) {
            BaseStatusBarControl.this.onStatusCanvasClick(event);
         }
      });
      this.statusCanvas.addListener(11, new Listener() {
         public void handleEvent(Event event) {
            BaseStatusBarControl.this.statusCanvas.redraw();
         }
      });
      GridData canvasGridData = new GridData(4, 4, true, true);
      canvasGridData.widthHint = 120;
      this.statusCanvas.setLayoutData(canvasGridData);
      Font defaultFont = this.statusCanvas.getFont();
      FontData fontData = defaultFont.getFontData()[0];
      fontData.setHeight((int)((double)fontData.getHeight() * 0.9));
      this.font = new Font(defaultFont.getDevice(), fontData);
      this.statusCanvas.setFont(this.font);
      this.colorOnline = new Color(parent.getDisplay(), COLOR_ONLINE);
      this.colorSettingsChanged = new Color(parent.getDisplay(), COLOR_SETTINGS_CHANGED);
      this.colorBusy = new Color(parent.getDisplay(), COLOR_BUSY);
      this.colorOff = new Color(parent.getDisplay(), COLOR_OFF);
      this.colorDisabled = new Color(parent.getDisplay(), COLOR_DISABLED);
      this.policyCombo = new CCombo(parent, 8);
      this.policyCombo.setFont(this.font);
      this.policyCombo.setItems(this.policyNames);
      CodeCompletionPolicy policy = this.settings.getCodeCompletionPolicy();
      this.policyCombo.select(policy.getIndex());
      this.policyCombo.addSelectionListener(this);
      this.policyTooltip = new DefaultToolTip(this.policyCombo);
      this.policyTooltip.setText(policy.getDescription());
      this.policyTooltip.setHideOnMouseDown(true);
      this.policyTooltip.setPopupDelay(500);
      this.policyTooltip.setHideDelay(5000);
      this.policyTooltip.activate();

      try {
         this.reflection.getField(CCombo.class, this.policyCombo, "list", List.class).ifPresent((list) -> {
            list.addMouseMoveListener(new MouseMoveListener() {
               public void mouseMove(MouseEvent e) {
                  int itemHeight = BaseStatusBarControl.this.policyCombo.getItemHeight();
                  if (itemHeight != 0) {
                     Integer index = (e.y - BaseStatusBarControl.this.policyCombo.getBounds().y) / itemHeight;
                     if (index >= 0 && index < BaseStatusBarControl.this.policies.length) {
                        BaseStatusBarControl.this.policyCombo.select(index);
                        list.redraw();
                        if (!index.equals(BaseStatusBarControl.this.policyTooltip.getData("index"))) {
                           CodeCompletionPolicy codeCompletionPolicy = BaseStatusBarControl.this.policies[index];
                           BaseStatusBarControl.this.policyTooltip.setText(codeCompletionPolicy.getDescription());
                           BaseStatusBarControl.this.policyTooltip.setData("index", index);
                           Rectangle comboBounds = BaseStatusBarControl.this.policyCombo.getBounds();
                           Rectangle listBounds = list.getBounds();
                           BaseStatusBarControl.this.policyTooltip.show(new Point(0, -(comboBounds.height + listBounds.height + 90)));
                        }
                     }
                  }
               }
            });
            list.getParent().addListener(23, new Listener() {
               public void handleEvent(Event event) {
                  BaseStatusBarControl.this.policyTooltip.hide();
               }
            });
         });
      } catch (Exception var13) {
      }

      this.policyMenu = new Menu(this.statusCanvas);

      CodeCompletionPolicy[] var11;
      for(final CodeCompletionPolicy codeCompletionPolicy : var11 = this.policies) {
         MenuItem menuItem = new MenuItem(this.policyMenu, 0);
         menuItem.setText(codeCompletionPolicy.getName().toLowerCase());
         menuItem.addListener(13, new Listener() {
            public void handleEvent(Event event) {
               BaseStatusBarControl.this.settingsSetter.setCodeCompletionPolicy(codeCompletionPolicy);
               BaseStatusBarControl.this.policyTooltip.setText(codeCompletionPolicy.getDescription());
               BaseStatusBarControl.this.statusCanvas.redraw();
            }
         });
      }

      this.policyCombo.setVisible(false);
      parent.getParent().setRedraw(true);
      composite.addDisposeListener(this);
      this.dispatcher.dispatch((Runnable)(() -> this.statusCanvas.redraw()));
      this.stateService.addListener(this);
      return composite;
   }

   private void paintStatus(GC gc) {
      Rectangle bounds = this.statusCanvas.getBounds();
      if (bounds.width > 0 && bounds.height > 0) {
         Display display = this.statusCanvas.getDisplay();
         gc.setAntialias(1);
         gc.setTextAntialias(1);
         Color statusColor = this.getCurrentStatusColor();
         int centerY = bounds.height / 2;
         int iconY = centerY - 6;
         gc.setBackground(statusColor);
         gc.fillRoundRectangle(0, iconY, 12, 12, 3, 3);
         Color brightForeground = null;
         Point textExtent = gc.textExtent(this.statusText);
         int textX = 17;

         try {
            if (this.themeManager.isDarkTheme()) {
               brightForeground = new Color(display, 220, 220, 220);
            } else {
               brightForeground = display.getSystemColor(21);
            }

            gc.setForeground(brightForeground);
            gc.setFont(this.font);
            if (this.font != null && !this.font.isDisposed()) {
               gc.setFont(this.font);
            }

            int textY = centerY - textExtent.y / 2;
            gc.drawText(this.statusText, textX, textY, 1);
         } finally {
            if (brightForeground != null && !brightForeground.isDisposed() && this.themeManager.isDarkTheme()) {
               brightForeground.dispose();
            }

         }

         String policyText = "";
         int policyTextX = textX + textExtent.x;
         gc.textExtent(policyText);
         if (this.lastAIState != null) {
            if (this.isErrorStateWithLink) {
               if (this.isMissingTokenState) {
                  policyText = Messages.Activate;
               } else {
                  policyText = Messages.Diagnostics;
               }

               Point policyTextExtent = gc.textExtent(policyText);
               int policyTextY = centerY - policyTextExtent.y / 2;
               this.policyTextX = policyTextX;
               this.policyTextWidth = policyTextExtent.x;
               Color brightLink = new Color(display, 100, 200, 255);
               gc.setForeground(brightLink);
               gc.drawText(policyText, policyTextX, policyTextY, 1);
               int underlineY = policyTextY + policyTextExtent.y - 2;
               gc.drawLine(policyTextX, underlineY, policyTextX + policyTextExtent.x, underlineY);
               brightLink.dispose();
            } else {
               CodeCompletionPolicy policy = this.settings.getCodeCompletionPolicy();
               String policyName = policy.getName();
               if (policyName != null) {
                  policyName = policyName.toLowerCase();
                  if (policyName.contains(":")) {
                     policyText = policyName.substring(policyName.lastIndexOf(":") + 1).trim();
                  } else {
                     policyText = policyName;
                  }
               } else {
                  policyText = "";
               }

               Point var23 = gc.textExtent(policyText);
               int policyTextY = centerY - var23.y / 2;
               this.policyTextX = policyTextX;
               this.policyTextWidth = var23.x;
               Color brightLink = new Color(display, 100, 200, 255);
               gc.setForeground(brightLink);
               gc.drawText(policyText, policyTextX, policyTextY, 1);
               int underlineY = policyTextY + var23.y - 2;
               gc.drawLine(policyTextX, underlineY, policyTextX + var23.x, underlineY);
               brightLink.dispose();
            }
         }

         gc.setForeground(display.getSystemColor(21));
      }
   }

   private void onStatusCanvasClick(Event event) {
      Rectangle bounds = this.statusCanvas.getBounds();
      if (event.x >= this.policyTextX && event.x <= this.policyTextX + this.policyTextWidth && event.y >= 0 && event.y <= bounds.height) {
         if (this.isErrorStateWithLink) {
            if (this.isMissingTokenState) {
               String url = this.settings.getHomePage() + this.currentUrlPath;
               this.web.browse(url);
               this.preferences.show("com.e1c.edt.ai.ui.clientPrefs");
            } else {
               DiagnosticDialog dialog = new DiagnosticDialog(this.statusCanvas.getShell());
               dialog.open();
            }
         } else {
            Point location = this.statusCanvas.toDisplay(event.x, event.y);
            this.policyMenu.setLocation(location.x, location.y);
            this.policyMenu.setVisible(true);
         }
      }

   }

   private Color getCurrentStatusColor() {
      if (this.colorOnline != null && this.colorBusy != null && this.colorOff != null && this.colorDisabled != null && this.colorSettingsChanged != null) {
         if (!this.settings.isEnabled()) {
            return this.colorDisabled;
         } else if (this.lastAIState == null) {
            return this.colorDisabled;
         } else {
            switch (this.lastAIState.getServiceState()) {
               case NONE:
               case MISSING_TOKEN:
                  return this.colorDisabled;
               case TOKEN_ERROR:
               case SERVER_ERROR:
               case SSL_ERROR:
               case SESSION_EXPIRED:
               case OFFLINE:
               default:
                  return this.colorOff;
               case SETTINGS_CHANGED:
                  return this.colorSettingsChanged;
               case ONLINE:
                  return this.lastAIState.getActionState() == ActionState.BUSY ? this.colorBusy : this.colorOnline;
            }
         }
      } else {
         return this.statusCanvas.getDisplay().getSystemColor(15);
      }
   }

   public boolean isDynamic() {
      return true;
   }

   public void widgetDisposed(DisposeEvent e) {
      this.stateService.removeListener(this);
      if (this.font != null && !this.font.isDisposed()) {
         this.font.dispose();
      }

      this.disposeColorIfNotDisposed(this.colorOnline);
      this.disposeColorIfNotDisposed(this.colorSettingsChanged);
      this.disposeColorIfNotDisposed(this.colorBusy);
      this.disposeColorIfNotDisposed(this.colorOff);
      this.disposeColorIfNotDisposed(this.colorDisabled);
   }

   private void disposeColorIfNotDisposed(Color color) {
      if (color != null && !color.isDisposed()) {
         color.dispose();
      }

   }

   public void onServiceStateChange(ServiceState serviceState) {
      this.dispatcher.dispatch((Runnable)(() -> this.changeServiceState(serviceState)));
   }

   public void onActionStateChange(ActionState actionState) {
      this.dispatcher.dispatch((Runnable)(() -> this.changeActionState(actionState)));
   }

   private void changeState(AIState state) {
      String info = this.versionProvider.getPluginVersion().toString();
      ServiceState serviceState = state.getServiceState();
      this.isMissingTokenState = serviceState == ServiceState.MISSING_TOKEN;
      String urlPath = serviceState.getUrlPath();
      this.isErrorStateWithLink = !urlPath.isEmpty() || this.isMissingTokenState;
      this.currentUrlPath = urlPath;
      if (this.settings.isEnabled()) {
         String message = serviceState.getMessage();
         info = info + ' ' + message;
      }

      this.lastAIState = state;
      CodeCompletionPolicy policy = this.settings.getCodeCompletionPolicy();
      this.statusCanvas.setToolTipText(info);
      this.policyCombo.select(policy.getIndex());
      this.policyTooltip.setText(policy.getDescription());
      this.statusCanvas.redraw();
   }

   private void changeServiceState(ServiceState serviceState) {
      ActionState actionState = this.lastAIState != null ? this.lastAIState.getActionState() : ActionState.INACTIVE;
      this.updateState(serviceState, actionState);
   }

   private void changeActionState(ActionState actionState) {
      ServiceState serviceState = this.lastAIState != null ? this.lastAIState.getServiceState() : ServiceState.OFFLINE;
      this.updateState(serviceState, actionState);
   }

   private void updateState(ServiceState serviceState, ActionState actionState) {
      this.lastAIState = new AIState(serviceState, actionState);
      this.changeState(this.lastAIState);
   }

   public void widgetSelected(SelectionEvent e) {
      this.policyTooltip.hide();
      int index = this.policyCombo.getSelectionIndex();
      if (index >= 0 && index < this.policies.length) {
         CodeCompletionPolicy codeCompletionPolicy = this.policies[index];
         this.settingsSetter.setCodeCompletionPolicy(codeCompletionPolicy);
         this.policyTooltip.setText(codeCompletionPolicy.getDescription());
      }
   }

   public void widgetDefaultSelected(SelectionEvent e) {
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$com$e1c$edt$ai$ServiceState() {
      int[] var10000 = $SWITCH_TABLE$com$e1c$edt$ai$ServiceState;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[ServiceState.values().length];

         try {
            var0[ServiceState.MISSING_TOKEN.ordinal()] = 2;
         } catch (NoSuchFieldError var9) {
         }

         try {
            var0[ServiceState.NONE.ordinal()] = 1;
         } catch (NoSuchFieldError var8) {
         }

         try {
            var0[ServiceState.OFFLINE.ordinal()] = 8;
         } catch (NoSuchFieldError var7) {
         }

         try {
            var0[ServiceState.ONLINE.ordinal()] = 9;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[ServiceState.SERVER_ERROR.ordinal()] = 4;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[ServiceState.SESSION_EXPIRED.ordinal()] = 7;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[ServiceState.SETTINGS_CHANGED.ordinal()] = 5;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[ServiceState.SSL_ERROR.ordinal()] = 6;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[ServiceState.TOKEN_ERROR.ordinal()] = 3;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$com$e1c$edt$ai$ServiceState = var0;
         return var0;
      }
   }
}
