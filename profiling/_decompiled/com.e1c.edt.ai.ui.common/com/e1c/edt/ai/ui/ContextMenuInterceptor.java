package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IVisualContextProvider;
import com.e1c.edt.ai.assistent.model.VisualContext;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;

public class ContextMenuInterceptor implements IInitializable {
   private final IDispatcher dispatcher;
   private final IVisualContextProvider visualContextProviewr;
   private final ITextActions textActions;
   private final IUI ui;
   private final ISettings settings;

   @Inject
   public ContextMenuInterceptor(IDispatcher dispatcher, IVisualContextProvider visualContextProviewr, ITextActions textActions, IUI ui, ISettings settings) {
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(visualContextProviewr);
      Preconditions.checkNotNull(textActions);
      Preconditions.checkNotNull(ui);
      Preconditions.checkNotNull(settings);
      this.dispatcher = dispatcher;
      this.visualContextProviewr = visualContextProviewr;
      this.textActions = textActions;
      this.ui = ui;
      this.settings = settings;
   }

   public void initialize() {
      this.dispatcher.dispatchAsync(() -> Display.getDefault().addFilter(4, (event) -> this.handleOnFocusEvent(event)));
   }

   public void handleOnFocusEvent(Event event) {
      if (event.widget instanceof StyledText) {
         final StyledText text = (StyledText)event.widget;
         if (this.isEnabled(text) && text.getEditable()) {
            if (this.ui.getSourceViewer(text).isPresent()) {
               return;
            }

            this.initialize(new IText() {
               public Control getControl() {
                  return text;
               }

               public String getContent() {
                  return text.getText();
               }

               public void setContent(String content) {
                  text.setText(content);
               }

               public void selectAll() {
                  text.selectAll();
               }

               public void addModifyListener(ModifyListener modifyListener) {
                  text.addModifyListener(modifyListener);
               }
            });
         }

      } else if (event.widget instanceof Text) {
         final Text text = (Text)event.widget;
         if (this.isEnabled(text) && text.getEditable()) {
            this.initialize(new IText() {
               public Control getControl() {
                  return text;
               }

               public String getContent() {
                  return text.getText();
               }

               public void setContent(String content) {
                  text.setText(content);
               }

               public void selectAll() {
                  text.selectAll();
               }

               public void addModifyListener(ModifyListener modifyListener) {
                  text.addModifyListener(modifyListener);
               }
            });
         }

      }
   }

   private boolean isEnabled(Control control) {
      return !control.isDisposed() && control.isVisible() && control.isEnabled();
   }

   private void initialize(final IText text) {
      Menu menu = text.getControl().getMenu();
      if (menu == null) {
         menu = new Menu(text.getControl());
         text.getControl().setMenu(menu);
      } else if (!this.isContextMenu(menu) || this.hasMenuItems(menu)) {
         return;
      }

      VisualContext context = this.visualContextProviewr.create(text.getControl(), CancellationTokens.NONE);
      if (!context.isEmpty()) {
         text.getControl().addMouseListener(new MouseListener() {
            public void mouseDoubleClick(MouseEvent e) {
               if (e.button == 2) {
                  this.handle();
               }

            }

            public void mouseDown(MouseEvent e) {
            }

            public void mouseUp(MouseEvent e) {
            }

            private void handle() {
               TextListener styledTextListener = ContextMenuInterceptor.this.new TextListener(text, (MenuItem)null, true);
               text.getControl().addFocusListener(styledTextListener);
               text.addModifyListener(styledTextListener);
               TextAction action = text.getContent().isBlank() ? TextAction.SUGGEST_YOUR_OPTION : TextAction.CORRECT_ERRORS;
               ContextMenuInterceptor.this.executeAction(text, action, styledTextListener);
            }
         });
         this.addMenuItems(menu, text);
      }
   }

   private boolean isContextMenu(Menu menu) {
      return (menu.getStyle() & 8) != 0;
   }

   private boolean hasMenuItems(Menu menu) {
      MenuItem[] var5;
      for(MenuItem item : var5 = menu.getItems()) {
         if (item.getData() instanceof MenuData) {
            return true;
         }
      }

      return false;
   }

   private void addMenuItems(Menu menu, IText text) {
      if (menu.getItemCount() > 0) {
         new MenuItem(menu, 2);
      }

      this.createMenuItem(menu, text, TextAction.SUGGEST_YOUR_OPTION, true);
      this.createMenuItem(menu, text, TextAction.CORRECT_ERRORS, false);
      this.createMenuItem(menu, text, TextAction.IN_OTHER_WORDS, false);
      this.createMenuItem(menu, text, TextAction.IMPROVE_STYLE, false);
   }

   private MenuItem createMenuItem(Menu menu, IText text, TextAction textAction, boolean allowForEmptyText) {
      MenuItem menuItem = new MenuItem(menu, 8);
      menuItem.setData(new MenuData(text));
      menuItem.setText(textAction.title);
      menuItem.setImage(BaseActivator.getImage(textAction.imageName));
      TextListener textListener = new TextListener(text, menuItem, allowForEmptyText);
      text.getControl().addFocusListener(textListener);
      text.addModifyListener(textListener);
      menuItem.addSelectionListener(SelectionListener.widgetSelectedAdapter((e) -> this.executeAction(text, textAction, textListener)));
      return menuItem;
   }

   private void executeAction(final IText text, TextAction textAction, final TextListener textListener) {
      if (this.settings.isEnabled()) {
         CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
         textListener.cancellationTokenSource = cancellationTokenSource;
         VisualContext context = this.visualContextProviewr.create(text.getControl(), cancellationTokenSource);
         IObservable<TextImprovements> improvementsSource = this.textActions.ceateTextImprovementsSource(context, textAction, cancellationTokenSource);
         improvementsSource.subscribe(new IObserver<TextImprovements>() {
            public void onNext(TextImprovements textImprovements) {
               ContextMenuInterceptor.this.dispatcher.dispatch((Runnable)(() -> {
                  textListener.isSuppresed = true;

                  try {
                     text.setContent(textImprovements.getText());
                  } finally {
                     textListener.isSuppresed = false;
                  }

               }));
            }

            public void onError(Throwable error) {
            }

            public void onCompleted() {
               ContextMenuInterceptor.this.dispatcher.dispatch((Runnable)(() -> text.selectAll()));
            }
         });
      }
   }

   private final class TextListener implements FocusListener, ModifyListener {
      private final IText text;
      private final MenuItem menuItem;
      private final boolean allowForEmptyText;
      public CancellationTokenSource cancellationTokenSource;
      public boolean isSuppresed;

      public TextListener(IText text, MenuItem menuItem, boolean allowForEmptyText) {
         this.text = text;
         this.menuItem = menuItem;
         this.allowForEmptyText = allowForEmptyText;
         this.setIsEnabled();
      }

      public void focusGained(FocusEvent e) {
      }

      public void focusLost(FocusEvent e) {
         this.cancel();
      }

      public void modifyText(ModifyEvent e) {
         this.cancel();
         this.setIsEnabled();
      }

      private void setIsEnabled() {
         if (this.menuItem != null) {
            this.menuItem.setEnabled(ContextMenuInterceptor.this.settings.isEnabled() && (this.allowForEmptyText || !this.text.getContent().trim().isBlank()));
         }

      }

      private void cancel() {
         CancellationTokenSource current = this.cancellationTokenSource;
         if (current != null && !this.isSuppresed) {
            this.cancellationTokenSource.cancel();
         }

      }
   }

   private class MenuData {
      public MenuData(IText text) {
      }
   }

   private interface IText {
      Control getControl();

      String getContent();

      void setContent(String var1);

      void selectAll();

      void addModifyListener(ModifyListener var1);
   }
}
