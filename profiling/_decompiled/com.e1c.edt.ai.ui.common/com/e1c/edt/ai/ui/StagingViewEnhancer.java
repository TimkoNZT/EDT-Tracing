package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IStateListener;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponent;
import org.eclipse.egit.ui.internal.staging.StagingEntry;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Section;

public class StagingViewEnhancer implements IViewEnhancer {
   private final Optional<String> viewId = Optional.of("org.eclipse.egit.ui.StagingView");
   private final IDispatcher dispatcher;
   private final IReflection reflection;
   private final IWidgets widgets;
   private final IGitActions gitActions;
   private final ISettings settings;
   private final IStateService stateService;
   private CancellationTokenSource reviewChangesCancellationToken = new CancellationTokenSource();
   private CancellationTokenSource createCommitMessageCancellationToken = new CancellationTokenSource();

   @Inject
   public StagingViewEnhancer(IDispatcher dispatcher, IReflection reflection, IWidgets widgets, IGitActions gitActions, ISettings settings, IStateService stateService) {
      Preconditions.checkNotNull(dispatcher);
      Preconditions.checkNotNull(reflection);
      Preconditions.checkNotNull(widgets);
      Preconditions.checkNotNull(gitActions);
      Preconditions.checkNotNull(settings);
      Preconditions.checkNotNull(stateService);
      this.dispatcher = dispatcher;
      this.reflection = reflection;
      this.widgets = widgets;
      this.gitActions = gitActions;
      this.settings = settings;
      this.stateService = stateService;
   }

   public Optional<String> getViewId() {
      return this.viewId;
   }

   public void setup(IWorkbenchPart view) {
      if (view instanceof StagingView) {
         StagingView stagingView = (StagingView)view;
         final TreeViewer stagedViewer = (TreeViewer)this.reflection.getField(StagingView.class, stagingView, "stagedViewer", TreeViewer.class).orElse((Object)null);
         if (stagedViewer != null) {
            ToolBarManager stagedToolBarManager = (ToolBarManager)this.reflection.getField(StagingView.class, stagingView, "stagedToolBarManager", ToolBarManager.class).orElse((Object)null);
            if (stagedToolBarManager != null) {
               <undefinedtype> selfReviewAction = new Action(Messages.GitReview, BaseActivator.getImageDescriptor("GIT_REVIEW")) {
                  public void run() {
                     CancellationTokenSource newCancellationToken = new CancellationTokenSource();
                     StagingViewEnhancer.this.reviewChangesCancellationToken.cancel();
                     StagingViewEnhancer.this.reviewChangesCancellationToken = newCancellationToken;
                     List<StagingEntry> stagingEntries = StagingViewEnhancer.this.getStagingEntries(stagedViewer.getTree());
                     List<GitDiff> diffs = StagingViewEnhancer.this.getDiffs(stagingEntries);
                     StagingViewEnhancer.this.gitActions.reviewGitChanges(diffs, StagingViewEnhancer.this.reviewChangesCancellationToken);
                  }
               };
               selfReviewAction.setEnabled(false);
               stagedToolBarManager.add(selfReviewAction);
               stagedToolBarManager.update(true);
               this.addStageListener(stagingView, (isEnabled) -> this.dispatcher.dispatch((Runnable)(() -> selfReviewAction.setEnabled(isEnabled))));
            }

            Section commitMessageSection = (Section)this.reflection.getField(StagingView.class, stagingView, "commitMessageSection", Section.class).orElse((Object)null);
            if (commitMessageSection != null) {
               ToolBar commitMessageToolBar = (ToolBar)this.widgets.getChildren(commitMessageSection).map((control) -> ToolBar.class.isAssignableFrom(control.getClass()) ? (ToolBar)ToolBar.class.cast(control) : null).filter((i) -> i != null).findFirst().orElse((Object)null);
               if (commitMessageToolBar != null) {
                  final CommitMessageComponent commitMessageComponent = (CommitMessageComponent)this.reflection.getField(StagingView.class, stagingView, "commitMessageComponent", CommitMessageComponent.class).orElse((Object)null);
                  if (commitMessageComponent != null) {
                     final ArrayList<CommitMessageInfo> commitMessages = new ArrayList();
                     ToolItem createMessageButton = new ToolItem(commitMessageToolBar, 524288);
                     createMessageButton.setImage(BaseActivator.getImage("GIT_MESSAGE"));
                     createMessageButton.setToolTipText(Messages.CommitMessage);
                     createMessageButton.addSelectionListener(new SelectionAdapter() {
                        public void widgetSelected(SelectionEvent e) {
                           commitMessages.clear();
                           List<StagingEntry> stagingEntries = StagingViewEnhancer.this.getStagingEntries(stagedViewer.getTree());
                           List<GitDiff> diffs = StagingViewEnhancer.this.getDiffs(stagingEntries);
                           final CancellationTokenSource newCancellationToken = new CancellationTokenSource();
                           StagingViewEnhancer.this.createCommitMessageCancellationToken.cancel();
                           StagingViewEnhancer.this.createCommitMessageCancellationToken = newCancellationToken;
                           final String baseMessage = commitMessageComponent.getCommitMessage().trim();
                           IObservable<CommitMessage> commitMessageSource = StagingViewEnhancer.this.gitActions.ceateGitCommitMessageSource(baseMessage, diffs, newCancellationToken);
                           commitMessageSource.subscribe(new IObserver<CommitMessage>() {
                              public void onNext(CommitMessage commitMessage) {
                                 StagingViewEnhancer.this.dispatcher.dispatch((Runnable)(() -> {
                                    if (!newCancellationToken.isCanceled()) {
                                       String message = commitMessage.getMessage();
                                       if (!baseMessage.isBlank()) {
                                          message = baseMessage + System.lineSeparator() + System.lineSeparator() + message;
                                       }

                                       commitMessageComponent.setCommitMessage(message);
                                       commitMessageComponent.updateUI();
                                       commitMessages.clear();
                                       commitMessages.add(new CommitMessageInfo(commitMessage, newCancellationToken));
                                    }
                                 }));
                              }

                              public void onError(Throwable error) {
                              }

                              public void onCompleted() {
                              }
                           });
                        }
                     });
                     this.addStageListener(stagingView, (isEnabled) -> this.dispatcher.dispatch((Runnable)(() -> createMessageButton.setEnabled(isEnabled))));
                     this.reflection.getField(StagingView.class, stagingView, "commitButton", Button.class).ifPresent((button) -> button.addSelectionListener(new SelectionAdapter() {
                           public void widgetSelected(SelectionEvent e) {
                              StagingViewEnhancer.this.commit(commitMessages, commitMessageComponent.getCommitMessage());
                           }
                        }));
                     this.reflection.getField(StagingView.class, stagingView, "commitAndPushButton", Button.class).ifPresent((button) -> button.addSelectionListener(new SelectionAdapter() {
                           public void widgetSelected(SelectionEvent e) {
                              StagingViewEnhancer.this.commit(commitMessages, commitMessageComponent.getCommitMessage());
                           }
                        }));
                  }
               }
            }

         }
      }
   }

   private void addStageListener(StagingView stagingView, Consumer<Boolean> enabledHandler) {
      this.reflection.getField(StagingView.class, stagingView, "unstageAllAction", IAction.class).ifPresent((unstageAllAction) -> {
         this.stateService.addListener(new IStateListener() {
            public void onServiceStateChange(ServiceState serviceState) {
               enabledHandler.accept(StagingViewEnhancer.this.settings.isEnabled() && unstageAllAction.isEnabled());
            }

            public void onActionStateChange(ActionState actionState) {
               enabledHandler.accept(StagingViewEnhancer.this.settings.isEnabled() && unstageAllAction.isEnabled());
            }
         });
         enabledHandler.accept(unstageAllAction.isEnabled());
         unstageAllAction.addPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
               if (!StagingViewEnhancer.this.settings.isEnabled()) {
                  enabledHandler.accept(false);
               } else {
                  if (event.getProperty().equals("enabled")) {
                     Object newVal = event.getNewValue();
                     if (newVal instanceof Boolean) {
                        enabledHandler.accept((Boolean)newVal);
                     }
                  }

               }
            }
         });
      });
   }

   private void commit(ArrayList<CommitMessageInfo> commitMessages, String finalText) {
      if (!commitMessages.isEmpty()) {
         CommitMessageInfo messageInfo = (CommitMessageInfo)commitMessages.get(0);
         this.gitActions.feedbackAsync(messageInfo.message, finalText, messageInfo.cancellationToken);
      }
   }

   private List<StagingEntry> getStagingEntries(Tree tree) {
      ArrayList<StagingEntry> stagingEntries = new ArrayList();
      Stack<TreeItem> stack = new Stack();

      TreeItem[] var7;
      for(TreeItem child : var7 = tree.getItems()) {
         stack.push(child);
      }

      while(!stack.isEmpty()) {
         TreeItem item = (TreeItem)stack.pop();
         Object data = item.getData();
         if (data instanceof StagingEntry) {
            stagingEntries.add((StagingEntry)data);
         }

         TreeItem[] var9;
         for(TreeItem child : var9 = item.getItems()) {
            stack.push(child);
         }
      }

      return stagingEntries;
   }

   private List<GitDiff> getDiffs(List<StagingEntry> stagingEntries) {
      return (List)((Map)stagingEntries.stream().collect(Collectors.groupingBy(StagingEntry::getRepository))).entrySet().stream().map((i) -> new GitDiff((Repository)i.getKey(), (List)((List)i.getValue()).stream().map((j) -> j.getPath()).collect(Collectors.toList()))).collect(Collectors.toList());
   }

   private static class CommitMessageInfo {
      private final CommitMessage message;
      private final ICancellationToken cancellationToken;

      public CommitMessageInfo(CommitMessage message, ICancellationToken cancellationToken) {
         Preconditions.checkNotNull(message);
         Preconditions.checkNotNull(cancellationToken);
         this.message = message;
         this.cancellationToken = cancellationToken;
      }
   }
}
