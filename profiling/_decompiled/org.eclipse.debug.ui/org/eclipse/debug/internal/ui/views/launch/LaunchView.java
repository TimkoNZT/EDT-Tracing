package org.eclipse.debug.internal.ui.views.launch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.commands.contexts.ContextManagerEvent;
import org.eclipse.core.commands.contexts.IContextManagerListener;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.commands.IRestartHandler;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.DelegatingModelPresentation;
import org.eclipse.debug.internal.ui.actions.AddToFavoritesAction;
import org.eclipse.debug.internal.ui.actions.EditLaunchConfigurationAction;
import org.eclipse.debug.internal.ui.commands.actions.DisconnectCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.DropToFrameCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.RestartCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.ResumeCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.StepIntoCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.StepOverCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.StepReturnCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.SuspendCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.TerminateAllAction;
import org.eclipse.debug.internal.ui.commands.actions.TerminateAndRelaunchAction;
import org.eclipse.debug.internal.ui.commands.actions.TerminateAndRemoveAction;
import org.eclipse.debug.internal.ui.commands.actions.TerminateCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.ToggleStepFiltersAction;
import org.eclipse.debug.internal.ui.sourcelookup.EditSourceLookupPathAction;
import org.eclipse.debug.internal.ui.sourcelookup.LookupSourceAction;
import org.eclipse.debug.internal.ui.viewers.model.InternalTreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.VirtualFindAction;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelChangedListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDeltaVisitor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.views.DebugModelPresentationContext;
import org.eclipse.debug.internal.ui.views.ViewContextService;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.actions.DebugCommandAction;
import org.eclipse.debug.ui.contexts.AbstractDebugContextProvider;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextProvider;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener2;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBookView;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.IUpdate;

public class LaunchView extends AbstractDebugView implements ISelectionChangedListener, IPerspectiveListener2, IPageListener, IShowInTarget, IShowInSource, IShowInTargetList, IPartListener2, IViewerUpdateListener, IContextManagerListener, IModelChangedListener {
   public static final String ID_CONTEXT_ACTIVITY_BINDINGS = "contextActivityBindings";
   private static final String TERMINATE = "terminate";
   private static final String DISCONNECT = "disconnect";
   private static final String SUSPEND = "suspend";
   private static final String RESUME = "resume";
   private static final String STEP_RETURN = "step_return";
   private static final String STEP_OVER = "step_over";
   private static final String DROP_TO_FRAME = "drop_to_frame";
   private static final String STEP_INTO = "step_into";
   public static final String TERMINATE_AND_REMOVE = "terminate_and_remove";
   public static final String TERMINATE_ALL = "terminate_all";
   public static final String TERMINATE_AND_RELAUNCH = "terminate_relaunch";
   private static final String TOGGLE_STEP_FILTERS = "toggle_step_filters";
   private static final String RESTART = "restart";
   private static final int BREADCRUMB_TRIGGER_HEIGHT_DEFAULT = 30;
   private static final int BREADCRUMB_TRIGGER_RANGE = 5;
   private static final int BREADCRUMB_STICKY_RANGE = 20;
   private boolean fIsActive = true;
   private IDebugModelPresentation fPresentation;
   private IPresentationContext fPresentationContext;
   private EditLaunchConfigurationAction fEditConfigAction;
   private AddToFavoritesAction fAddToFavoritesAction;
   private EditSourceLookupPathAction fEditSourceAction;
   private LookupSourceAction fLookupAction;
   private String fCurrentViewMode = "Debug_view.mode.auto";
   private DebugViewModeAction[] fDebugViewModeActions;
   private DebugToolBarAction fDebugToolBarAction;
   private BreadcrumbDropDownAutoExpandAction fBreadcrumbDropDownAutoExpandAction;
   private IContextService fContextService;
   private String PREF_STATE_MEMENTO = "pref_state_memento.";
   private static final String BREADCRUMB_DROPDOWN_AUTO_EXPAND = DebugUIPlugin.getUniqueIdentifier() + ".BREADCRUMB_DROPDOWN_AUTO_EXPAND";
   private boolean fBreadcrumbDropDownAutoExpand;
   private final Map<String, IHandler2> fHandlers = new HashMap();
   private boolean fDebugToolbarInView = true;
   private Set<String> fDebugToolbarPerspectives = new TreeSet();
   private BreadcrumbPage fBreadcrumbPage;
   private TreeViewerContextProvider fTreeViewerDebugContextProvider;
   private PageBookView.PageRec fDefaultPageRec = null;
   private final ISelectionChangedListener fTreeViewerSelectionChangedListener = (event) -> this.fTreeViewerDebugContextProvider.activate(event.getSelection());
   private ContextProviderProxy fContextProviderProxy;

   protected String getHelpContextId() {
      return "org.eclipse.debug.ui.debug_view_context";
   }

   protected void createActions() {
      this.setAction("Properties", new PropertyDialogAction(this.getSite(), this.getSite().getSelectionProvider()));
      this.fEditConfigAction = new EditLaunchConfigurationAction();
      this.fAddToFavoritesAction = new AddToFavoritesAction();
      this.fEditSourceAction = new EditSourceLookupPathAction(this);
      this.fLookupAction = new LookupSourceAction(this);
      this.setAction(FIND_ACTION, new VirtualFindAction((TreeModelViewer)this.getViewer()));
      this.addCapabilityAction(new TerminateCommandAction(), "terminate");
      this.addCapabilityAction(new DisconnectCommandAction(), "disconnect");
      this.addCapabilityAction(new SuspendCommandAction(), "suspend");
      this.addCapabilityAction(new ResumeCommandAction(), "resume");
      this.addCapabilityAction(new StepReturnCommandAction(), "step_return");
      this.addCapabilityAction(new StepOverCommandAction(), "step_over");
      this.addCapabilityAction(new StepIntoCommandAction(), "step_into");
      this.addCapabilityAction(new DropToFrameCommandAction(), "drop_to_frame");
      DebugCommandAction action = new TerminateAndRemoveAction();
      this.addCapabilityAction(action, "terminate_and_remove");
      this.setHandler("terminate_and_remove", new ActionHandler(action));
      DebugCommandAction var2 = new TerminateAndRelaunchAction();
      this.addCapabilityAction(var2, "terminate_relaunch");
      this.setHandler("terminate_relaunch", new ActionHandler(var2));
      this.addCapabilityAction(new RestartCommandAction(), "restart");
      DebugCommandAction var3 = new TerminateAllAction();
      this.addCapabilityAction(var3, "terminate_all");
      this.setHandler("terminate_all", new ActionHandler(var3));
      this.addCapabilityAction(new ToggleStepFiltersAction(), "toggle_step_filters");
   }

   private void setHandler(String id, IHandler2 handler) {
      this.fHandlers.put(id, handler);
   }

   public IHandler2 getHandler(String id) {
      return (IHandler2)this.fHandlers.get(id);
   }

   private void addCapabilityAction(DebugCommandAction capability, String actionID) {
      capability.init((IWorkbenchPart)this);
      this.setAction(actionID, capability);
   }

   private void disposeCommandAction(String actionID) {
      DebugCommandAction action = (DebugCommandAction)this.getAction(actionID);
      action.dispose();
   }

   public void createPartControl(final Composite parent) {
      super.createPartControl(parent);
      this.setGlobalActionBarsToPage((IPageBookViewPage)this.getDefaultPage());
      this.getSite().getSelectionProvider().addSelectionChangedListener(this);
      ((IPageBookViewPage)this.getDefaultPage()).getSite().setSelectionProvider(this.getViewer());
      this.partActivated(new BreadcrumbWorkbenchPart(this.getSite()));
      this.fContextProviderProxy = new ContextProviderProxy(new IDebugContextProvider[]{this.fTreeViewerDebugContextProvider, this.fBreadcrumbPage.getContextProvider()});
      DebugUITools.getDebugContextManager().getContextService(this.getSite().getWorkbenchWindow()).addDebugContextProvider(this.fContextProviderProxy);
      this.createViewModeActions(parent);
      IPreferenceStore prefStore = DebugUIPlugin.getDefault().getPreferenceStore();
      String mode = prefStore.getString("org.eclispe.debug.ui.Debug_view.mode");
      this.setViewMode(mode, parent);

      DebugViewModeAction[] var7;
      for(DebugViewModeAction action : var7 = this.fDebugViewModeActions) {
         action.setChecked(action.getMode().equals(mode));
      }

      this.createDebugToolBarInViewActions(parent);
      parent.addControlListener(new ControlListener() {
         public void controlMoved(ControlEvent e) {
         }

         public void controlResized(ControlEvent e) {
            if (!parent.isDisposed()) {
               if ("Debug_view.mode.auto".equals(LaunchView.this.fCurrentViewMode)) {
                  LaunchView.this.autoSelectViewPage(parent);
               }

            }
         }
      });
      this.fContextService.addContextManagerListener(this);
   }

   private void setGlobalActionBarsToPage(IPageBookViewPage page) {
      IActionBars pageActionBars = page.getSite().getActionBars();
      IActionBars bars = this.getViewSite().getActionBars();
      pageActionBars.setGlobalActionHandler(FIND_ACTION, bars.getGlobalActionHandler(FIND_ACTION));
      pageActionBars.setGlobalActionHandler(COPY_ACTION, bars.getGlobalActionHandler(COPY_ACTION));
   }

   protected PageBookView.PageRec doCreatePage(IWorkbenchPart part) {
      if (part instanceof BreadcrumbWorkbenchPart) {
         this.fBreadcrumbPage = new BreadcrumbPage();
         this.fBreadcrumbPage.createControl(this.getPageBook());
         this.initPage(this.fBreadcrumbPage);
         this.setGlobalActionBarsToPage(this.fBreadcrumbPage);
         return new PageBookView.PageRec(part, this.fBreadcrumbPage);
      } else {
         return null;
      }
   }

   protected boolean isImportant(IWorkbenchPart part) {
      return part instanceof BreadcrumbWorkbenchPart;
   }

   protected void showPageRec(PageBookView.PageRec pageRec) {
      if (pageRec.page == this.getDefaultPage()) {
         this.fDefaultPageRec = pageRec;
      }

      super.showPageRec(pageRec);
   }

   private void createViewModeActions(Composite parent) {
      IActionBars actionBars = this.getViewSite().getActionBars();
      IMenuManager viewMenu = actionBars.getMenuManager();
      this.fDebugViewModeActions = new DebugViewModeAction[3];
      this.fDebugViewModeActions[0] = new DebugViewModeAction(this, "Debug_view.mode.auto", parent);
      this.fDebugViewModeActions[1] = new DebugViewModeAction(this, "Debug_view.mode.full", parent);
      this.fDebugViewModeActions[2] = new DebugViewModeAction(this, "Debug_view.mode.compact", parent);
      this.fBreadcrumbDropDownAutoExpandAction = new BreadcrumbDropDownAutoExpandAction(this);
      viewMenu.add(new Separator());
      MenuManager modeSubmenu = new MenuManager(LaunchViewMessages.LaunchView_ViewModeMenu_label);
      modeSubmenu.setRemoveAllWhenShown(true);
      modeSubmenu.add(this.fDebugViewModeActions[0]);
      modeSubmenu.add(this.fDebugViewModeActions[1]);
      modeSubmenu.add(this.fDebugViewModeActions[2]);
      modeSubmenu.add(new Separator());
      modeSubmenu.add(this.fBreadcrumbDropDownAutoExpandAction);
      viewMenu.add(modeSubmenu);
      modeSubmenu.addMenuListener((manager) -> {
         modeSubmenu.add(this.fDebugViewModeActions[0]);
         modeSubmenu.add(this.fDebugViewModeActions[1]);
         modeSubmenu.add(this.fDebugViewModeActions[2]);
         modeSubmenu.add(new Separator());
         modeSubmenu.add(this.fBreadcrumbDropDownAutoExpandAction);
      });
   }

   private void createDebugToolBarInViewActions(Composite parent) {
      IActionBars actionBars = this.getViewSite().getActionBars();
      IMenuManager viewMenu = actionBars.getMenuManager();
      this.fDebugToolBarAction = new DebugToolBarAction(this);
      viewMenu.add(this.fDebugToolBarAction);
      this.updateCheckedDebugToolBarAction();
   }

   void setViewMode(String mode, Composite parent) {
      if (!this.fCurrentViewMode.equals(mode)) {
         this.fCurrentViewMode = mode;
         if ("Debug_view.mode.compact".equals(mode)) {
            this.showBreadcrumbPage();
         } else if ("Debug_view.mode.full".equals(mode)) {
            this.showTreeViewerPage();
         } else {
            this.autoSelectViewPage(parent);
         }

         DebugUIPlugin.getDefault().getPreferenceStore().setValue("org.eclispe.debug.ui.Debug_view.mode", mode);
      }
   }

   private void autoSelectViewPage(Composite parent) {
      int breadcrumbHeight = this.fBreadcrumbPage.getHeight();
      if (breadcrumbHeight == 0) {
         breadcrumbHeight = 30;
      }

      if (parent.getClientArea().height < breadcrumbHeight + 5) {
         this.showBreadcrumbPage();
      } else if (parent.getClientArea().height > breadcrumbHeight + 20) {
         this.showTreeViewerPage();
      }

   }

   void showTreeViewerPage() {
      if (this.fDefaultPageRec != null && !this.getDefaultPage().equals(this.getCurrentPage())) {
         this.showPageRec(this.fDefaultPageRec);
         this.fContextProviderProxy.setActiveProvider(this.fTreeViewerDebugContextProvider);
         this.fBreadcrumbPage.fCrumb.clearSelection();
      }

   }

   void showBreadcrumbPage() {
      PageBookView.PageRec rec = this.getPageRec(this.fBreadcrumbPage);
      if (rec != null && !this.fBreadcrumbPage.equals(this.getCurrentPage())) {
         this.showPageRec(rec);
         if (this.getSite().getPage().getActivePart() == this) {
            this.setFocus();
         }

         ISelection activeContext = this.fTreeViewerDebugContextProvider.getActiveContext();
         if (activeContext == null) {
            activeContext = StructuredSelection.EMPTY;
         }

         this.fBreadcrumbPage.fCrumb.debugContextChanged(new DebugContextEvent(this.fTreeViewerDebugContextProvider, activeContext, 1));
         this.fContextProviderProxy.setActiveProvider(this.fBreadcrumbPage.getContextProvider());
      }

   }

   protected Viewer createViewer(Composite parent) {
      this.fPresentation = new DelegatingModelPresentation();
      this.fPresentationContext = new DebugModelPresentationContext("org.eclipse.debug.ui.DebugView", this, this.fPresentation);
      TreeModelViewer viewer = new TreeModelViewer(parent, 268436226, this.fPresentationContext);
      viewer.addSelectionChangedListener(this.fTreeViewerSelectionChangedListener);
      viewer.addViewerUpdateListener(this);
      viewer.addModelChangedListener(this);
      viewer.setInput(DebugPlugin.getDefault().getLaunchManager());
      this.fTreeViewerDebugContextProvider = new TreeViewerContextProvider(viewer);
      return viewer;
   }

   private void commonInit(IViewSite site) {
      site.getPage().addPartListener(this);
      site.getWorkbenchWindow().addPageListener(this);
      site.getWorkbenchWindow().addPerspectiveListener(this);
   }

   private void preferenceInit(IViewSite site) {
      String var10001 = String.valueOf(this.PREF_STATE_MEMENTO);
      this.PREF_STATE_MEMENTO = var10001 + site.getId();
      IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
      String string = store.getString(this.PREF_STATE_MEMENTO);
      if (string.length() > 0) {
         try {
            Throwable var4 = null;
            Object var5 = null;

            try {
               ByteArrayInputStream bin = new ByteArrayInputStream(string.getBytes());

               try {
                  InputStreamReader reader = new InputStreamReader(bin);

                  try {
                     XMLMemento stateMemento = XMLMemento.createReadRoot(reader);
                     this.setMemento(stateMemento);
                  } finally {
                     if (reader != null) {
                        reader.close();
                     }

                  }
               } catch (Throwable var25) {
                  if (var4 == null) {
                     var4 = var25;
                  } else if (var4 != var25) {
                     var4.addSuppressed(var25);
                  }

                  if (bin != null) {
                     bin.close();
                  }

                  throw var4;
               }

               if (bin != null) {
                  bin.close();
               }
            } catch (Throwable var26) {
               if (var4 == null) {
                  var4 = var26;
               } else if (var4 != var26) {
                  var4.addSuppressed(var26);
               }

               throw var4;
            }
         } catch (WorkbenchException var27) {
         } catch (IOException var28) {
         }
      }

      IMemento mem = this.getMemento();
      if (mem != null) {
         Boolean auto = mem.getBoolean(BREADCRUMB_DROPDOWN_AUTO_EXPAND);
         if (auto != null) {
            this.setBreadcrumbDropDownAutoExpand(auto);
         }
      }

      String preference = DebugUIPlugin.getDefault().getPreferenceStore().getString("org.eclispe.debug.ui.Debug_view.debug_toolbar_hidden_perspectives");
      if (preference != null) {
         this.fDebugToolbarPerspectives = ViewContextService.parseList(preference);
      }

      IPerspectiveDescriptor perspective = this.getSite().getPage().getPerspective();
      this.fDebugToolbarInView = this.isDebugToolbarShownInPerspective(perspective);
   }

   public void init(IViewSite site) throws PartInitException {
      super.init(site);
      this.commonInit(site);
      this.preferenceInit(site);
      this.fContextService = (IContextService)site.getService(IContextService.class);
   }

   public void init(IViewSite site, IMemento memento) throws PartInitException {
      super.init(site, memento);
      this.commonInit(site);
      this.preferenceInit(site);
      this.fContextService = (IContextService)site.getService(IContextService.class);
   }

   public void partDeactivated(IWorkbenchPart part) {
      String id = part.getSite().getId();
      if (id.equals(this.getSite().getId())) {
         try {
            Throwable var3 = null;
            Object var4 = null;

            try {
               ByteArrayOutputStream bout = new ByteArrayOutputStream();

               try {
                  OutputStreamWriter writer = new OutputStreamWriter(bout);

                  try {
                     XMLMemento memento = XMLMemento.createWriteRoot("DebugViewMemento");
                     this.saveViewerState(memento);
                     memento.save(writer);
                     IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
                     String xmlString = bout.toString();
                     store.putValue(this.PREF_STATE_MEMENTO, xmlString);
                  } finally {
                     if (writer != null) {
                        writer.close();
                     }

                  }
               } catch (Throwable var23) {
                  if (var3 == null) {
                     var3 = var23;
                  } else if (var3 != var23) {
                     var3.addSuppressed(var23);
                  }

                  if (bout != null) {
                     bout.close();
                  }

                  throw var3;
               }

               if (bout != null) {
                  bout.close();
               }
            } catch (Throwable var24) {
               if (var3 == null) {
                  var3 = var24;
               } else if (var3 != var24) {
                  var3.addSuppressed(var24);
               }

               throw var3;
            }
         } catch (IOException var25) {
         }
      }

      StringBuilder buffer = new StringBuilder();

      for(String perspectiveId : this.fDebugToolbarPerspectives) {
         buffer.append(perspectiveId).append(',');
      }

      this.getPreferenceStore().setValue("org.eclispe.debug.ui.Debug_view.debug_toolbar_hidden_perspectives", buffer.toString());
      super.partDeactivated(part);
   }

   public void saveViewerState(IMemento memento) {
      memento.putBoolean(BREADCRUMB_DROPDOWN_AUTO_EXPAND, this.getBreadcrumbDropDownAutoExpand());
   }

   protected void configureToolBar(IToolBarManager tbm) {
      tbm.add(new Separator("threadGroup"));
      tbm.add(new Separator("stepGroup"));
      tbm.add(new GroupMarker("stepIntoGroup"));
      tbm.add(new GroupMarker("stepOverGroup"));
      tbm.add(new GroupMarker("stepReturnGroup"));
      tbm.add(new GroupMarker("emptyStepGroup"));
      tbm.add(new Separator("renderGroup"));
      if (this.fDebugToolbarInView) {
         this.addDebugToolbarActions(tbm);
      }

   }

   protected void addDebugToolbarActions(IToolBarManager tbm) {
      tbm.appendToGroup("threadGroup", this.getAction("resume"));
      tbm.appendToGroup("threadGroup", this.getAction("suspend"));
      tbm.appendToGroup("threadGroup", this.getAction("terminate"));
      tbm.appendToGroup("threadGroup", this.getAction("disconnect"));
      tbm.appendToGroup("stepIntoGroup", this.getAction("step_into"));
      tbm.appendToGroup("stepOverGroup", this.getAction("step_over"));
      tbm.appendToGroup("stepReturnGroup", this.getAction("step_return"));
      tbm.appendToGroup("emptyStepGroup", this.getAction("drop_to_frame"));
      tbm.appendToGroup("renderGroup", this.getAction("toggle_step_filters"));
   }

   protected void removeDebugToolbarActions(IToolBarManager tbm) {
      tbm.remove(new ActionContributionItem(this.getAction("resume")));
      tbm.remove(new ActionContributionItem(this.getAction("suspend")));
      tbm.remove(new ActionContributionItem(this.getAction("terminate")));
      tbm.remove(new ActionContributionItem(this.getAction("disconnect")));
      tbm.remove(new ActionContributionItem(this.getAction("step_into")));
      tbm.remove(new ActionContributionItem(this.getAction("step_over")));
      tbm.remove(new ActionContributionItem(this.getAction("step_return")));
      tbm.remove(new ActionContributionItem(this.getAction("drop_to_frame")));
      tbm.remove(new ActionContributionItem(this.getAction("toggle_step_filters")));
   }

   public boolean isDebugToolbarInView() {
      return this.fDebugToolbarInView;
   }

   public boolean isDebugToolbarShownInPerspective(IPerspectiveDescriptor perspective) {
      return perspective == null || this.fDebugToolbarPerspectives.contains(perspective.getId());
   }

   public void setDebugToolbarInView(boolean show) {
      if (show != this.isDebugToolbarInView()) {
         this.fDebugToolbarInView = show;
         IPerspectiveDescriptor perspective = this.getSite().getPage().getPerspective();
         if (perspective != null) {
            if (show) {
               this.fDebugToolbarPerspectives.add(perspective.getId());
            } else {
               this.fDebugToolbarPerspectives.remove(perspective.getId());
            }
         }

         IToolBarManager tbm = this.getViewSite().getActionBars().getToolBarManager();
         if (show) {
            this.addDebugToolbarActions(tbm);
         } else {
            this.removeDebugToolbarActions(tbm);
         }

         this.getViewSite().getActionBars().updateActionBars();
         if (!Boolean.toString(show).equals(System.getProperty("org.eclipse.debug.ui.debugViewToolbarVisible"))) {
            try {
               System.setProperty("org.eclipse.debug.ui.debugViewToolbarVisible", Boolean.toString(show));
            } catch (SecurityException var4) {
            }
         }

      }
   }

   public void dispose() {
      this.fContextService.removeContextManagerListener(this);
      this.getSite().getSelectionProvider().removeSelectionChangedListener(this);
      DebugUITools.getDebugContextManager().getContextService(this.getSite().getWorkbenchWindow()).removeDebugContextProvider(this.fContextProviderProxy);
      this.fContextProviderProxy.dispose();
      this.fTreeViewerDebugContextProvider.dispose();
      this.disposeActions();
      Viewer viewer = this.getViewer();
      if (viewer != null) {
         viewer.removeSelectionChangedListener(this.fTreeViewerSelectionChangedListener);
         ((TreeModelViewer)viewer).removeViewerUpdateListener(this);
         ((TreeModelViewer)viewer).removeModelChangedListener(this);
      }

      if (this.fPresentationContext != null) {
         this.fPresentationContext.dispose();
      }

      IWorkbenchPage page = this.getSite().getPage();
      page.removePartListener(this);
      IWorkbenchWindow window = this.getSite().getWorkbenchWindow();
      window.removePerspectiveListener(this);
      window.removePageListener(this);

      for(IHandler2 handler : this.fHandlers.values()) {
         handler.dispose();
      }

      this.fHandlers.clear();
      if (this.fBreadcrumbPage != null) {
         this.fBreadcrumbPage.dispose();
         this.fBreadcrumbPage = null;
      }

      super.dispose();
   }

   private void disposeActions() {
      PropertyDialogAction properties = (PropertyDialogAction)this.getAction("Properties");
      properties.dispose();
      this.disposeCommandAction("terminate");
      this.disposeCommandAction("disconnect");
      this.disposeCommandAction("suspend");
      this.disposeCommandAction("resume");
      this.disposeCommandAction("step_return");
      this.disposeCommandAction("step_over");
      this.disposeCommandAction("step_into");
      this.disposeCommandAction("drop_to_frame");
      this.disposeCommandAction("terminate_and_remove");
      this.disposeCommandAction("terminate_relaunch");
      this.disposeCommandAction("restart");
      this.disposeCommandAction("terminate_all");
      this.disposeCommandAction("toggle_step_filters");
   }

   public void selectionChanged(SelectionChangedEvent event) {
      this.updateObjects();
   }

   public void doubleClick(DoubleClickEvent event) {
      ISelection selection = event.getSelection();
      if (selection instanceof IStructuredSelection ss) {
         Object o = ss.getFirstElement();
         if (o != null && !(o instanceof IStackFrame)) {
            StructuredViewer viewer = (StructuredViewer)this.getViewer();
            viewer.refresh(o);
         }
      }
   }

   public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
      this.setActive(page.findView(this.getSite().getId()) != null);
      this.updateObjects();
      this.setDebugToolbarInView(this.isDebugToolbarShownInPerspective(this.getSite().getPage().getPerspective()));
      this.updateCheckedDebugToolBarAction();
   }

   public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
      this.setActive(page.findView(this.getSite().getId()) != null);
   }

   public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, IWorkbenchPartReference partRef, String changeId) {
   }

   public void pageActivated(IWorkbenchPage page) {
      if (this.getSite().getPage().equals(page)) {
         this.setActive(true);
         this.updateObjects();
      }

   }

   public void pageClosed(IWorkbenchPage page) {
   }

   public void pageOpened(IWorkbenchPage page) {
   }

   public IDebugModelPresentation getPresentation(String id) {
      return ((DelegatingModelPresentation)this.fPresentation).getPresentation(id);
   }

   protected void fillContextMenu(IMenuManager menu) {
      TreeSelection sel = (TreeSelection)this.fTreeViewerDebugContextProvider.getActiveContext();
      Object element = sel != null && sel.size() > 0 ? sel.getFirstElement() : null;
      menu.add(new Separator("emptyEditGroup"));
      menu.add(new Separator("editGroup"));
      menu.add(this.getAction(FIND_ACTION));
      menu.add(new Separator("emptyStepGroup"));
      menu.add(new Separator("stepGroup"));
      menu.add(new GroupMarker("stepIntoGroup"));
      menu.add(new GroupMarker("stepOverGroup"));
      menu.add(new GroupMarker("stepReturnGroup"));
      menu.add(new Separator("renderGroup"));
      menu.add(new Separator("emptyThreadGroup"));
      menu.add(new Separator("threadGroup"));
      menu.add(new Separator("emptyLaunchGroup"));
      menu.add(new Separator("launchGroup"));
      IStructuredSelection selection = (IStructuredSelection)this.getSite().getSelectionProvider().getSelection();
      this.updateAndAdd(menu, this.fEditConfigAction, selection);
      this.updateAndAdd(menu, this.fAddToFavoritesAction, selection);
      this.updateAndAdd(menu, this.fEditSourceAction, selection);
      this.updateAndAdd(menu, this.fLookupAction, selection);
      menu.add(new Separator("emptyRenderGroup"));
      menu.add(new Separator("renderGroup"));
      menu.add(new Separator("propertyGroup"));
      PropertyDialogAction action = (PropertyDialogAction)this.getAction("Properties");
      action.setEnabled(action.isApplicableForSelection() && !(element instanceof ILaunch));
      menu.add(action);
      menu.add(new Separator("additions"));
      menu.appendToGroup("launchGroup", this.getAction("terminate_and_remove"));
      menu.appendToGroup("launchGroup", this.getAction("terminate_all"));
      menu.appendToGroup("threadGroup", this.getAction("resume"));
      menu.appendToGroup("threadGroup", this.getAction("suspend"));
      menu.appendToGroup("threadGroup", this.getAction("terminate"));
      menu.appendToGroup("threadGroup", this.getAction("terminate_relaunch"));
      if (element instanceof IAdaptable && ((IAdaptable)element).getAdapter(IRestartHandler.class) != null) {
         menu.appendToGroup("threadGroup", this.getAction("restart"));
      }

      menu.appendToGroup("threadGroup", this.getAction("disconnect"));
      menu.appendToGroup("stepIntoGroup", this.getAction("step_into"));
      menu.appendToGroup("stepOverGroup", this.getAction("step_over"));
      menu.appendToGroup("stepReturnGroup", this.getAction("step_return"));
      menu.appendToGroup("emptyStepGroup", this.getAction("drop_to_frame"));
      menu.appendToGroup("renderGroup", this.getAction("toggle_step_filters"));
   }

   public void contextManagerChanged(ContextManagerEvent event) {
      if (event.isActiveContextsChanged()) {
         Set<?> oldContexts = event.getPreviouslyActiveContextIds();
         Set<?> newContexts = event.getContextManager().getActiveContextIds();
         if (oldContexts.contains("org.eclipse.debug.ui.debugToolbarActionSet") != newContexts.contains("org.eclipse.debug.ui.debugToolbarActionSet")) {
            this.updateCheckedDebugToolBarAction();
         }
      }

   }

   private void updateCheckedDebugToolBarAction() {
      this.fDebugToolBarAction.setChecked(this.isDebugToolbarInView());
   }

   private void updateAndAdd(IMenuManager menu, SelectionListenerAction action, IStructuredSelection selection) {
      action.selectionChanged(selection);
      if (action.isEnabled()) {
         menu.add(action);
      }

   }

   protected void setActive(boolean active) {
      this.fIsActive = active;
   }

   protected boolean isActive() {
      return this.fIsActive && this.getViewer() != null;
   }

   public boolean show(ShowInContext context) {
      ISelection selection = context.getSelection();
      if (selection != null && selection instanceof IStructuredSelection ss) {
         if (ss.size() == 1) {
            Object obj = ss.getFirstElement();
            if (obj instanceof IDebugTarget || obj instanceof IProcess) {
               Viewer viewer = this.getViewer();
               if (viewer instanceof InternalTreeModelViewer) {
                  InternalTreeModelViewer tv = (InternalTreeModelViewer)viewer;
                  tv.setSelection(selection, true, true);
               } else {
                  viewer.setSelection(selection, true);
               }

               return true;
            }
         }
      }

      return false;
   }

   public ShowInContext getShowInContext() {
      if (this.isActive()) {
         IStructuredSelection selection = (IStructuredSelection)this.getViewer().getSelection();
         if (selection.size() == 1) {
            Object object = selection.getFirstElement();
            if (object instanceof IAdaptable) {
               IAdaptable adaptable = (IAdaptable)object;
               IShowInSource show = (IShowInSource)adaptable.getAdapter(IShowInSource.class);
               if (show != null) {
                  return show.getShowInContext();
               }
            }
         }
      }

      return null;
   }

   public String[] getShowInTargetIds() {
      if (this.isActive()) {
         IStructuredSelection selection = (IStructuredSelection)this.getViewer().getSelection();
         if (selection.size() == 1) {
            Object object = selection.getFirstElement();
            if (object instanceof IAdaptable) {
               IAdaptable adaptable = (IAdaptable)object;
               IShowInTargetList show = (IShowInTargetList)adaptable.getAdapter(IShowInTargetList.class);
               if (show != null) {
                  return show.getShowInTargetIds();
               }
            }
         }
      }

      return new String[0];
   }

   public void partClosed(IWorkbenchPartReference partRef) {
   }

   public void partVisible(IWorkbenchPartReference partRef) {
      IWorkbenchPart part = partRef.getPart(false);
      if (part == this) {
         this.setActive(true);
         this.getSite().getPage().showActionSet("org.eclipse.debug.ui.debugActionSet");
      }

   }

   public void partOpened(IWorkbenchPartReference partRef) {
   }

   public void partActivated(IWorkbenchPartReference partRef) {
      String debugToolBarShown = Boolean.toString(this.isDebugToolbarShownInPerspective(this.getSite().getPage().getPerspective()));
      if (!debugToolBarShown.equals(System.getProperty("org.eclipse.debug.ui.debugViewToolbarVisible"))) {
         try {
            System.setProperty("org.eclipse.debug.ui.debugViewToolbarVisible", debugToolBarShown);
         } catch (SecurityException var3) {
         }
      }

   }

   public void partBroughtToTop(IWorkbenchPartReference partRef) {
   }

   public void partDeactivated(IWorkbenchPartReference partRef) {
   }

   public void partHidden(IWorkbenchPartReference partRef) {
   }

   public void partInputChanged(IWorkbenchPartReference partRef) {
   }

   protected void becomesVisible() {
      super.becomesVisible();
      this.getViewer().refresh();
   }

   public void updateComplete(IViewerUpdate update) {
      if (!update.isCanceled() && TreePath.EMPTY.equals(update.getElementPath())) {
         this.updateFindAction();
      }

   }

   public void updateStarted(IViewerUpdate update) {
   }

   public synchronized void viewerUpdatesBegin() {
      IWorkbenchSiteProgressService progressService = (IWorkbenchSiteProgressService)this.getSite().getAdapter(IWorkbenchSiteProgressService.class);
      if (progressService != null) {
         progressService.incrementBusy();
      }

   }

   public synchronized void viewerUpdatesComplete() {
      IWorkbenchSiteProgressService progressService = (IWorkbenchSiteProgressService)this.getSite().getAdapter(IWorkbenchSiteProgressService.class);
      if (progressService != null) {
         progressService.decrementBusy();
      }

   }

   public void modelChanged(IModelDelta delta, IModelProxy proxy) {
      this.updateFindAction();
   }

   private void updateFindAction() {
      IAction action = this.getAction(FIND_ACTION);
      if (action instanceof IUpdate) {
         ((IUpdate)action).update();
      }

   }

   boolean isBreadcrumbVisible() {
      return this.fBreadcrumbPage.equals(this.getCurrentPage());
   }

   boolean getBreadcrumbDropDownAutoExpand() {
      return this.fBreadcrumbDropDownAutoExpand;
   }

   void setBreadcrumbDropDownAutoExpand(boolean expand) {
      this.fBreadcrumbDropDownAutoExpand = expand;
   }

   private class BreadcrumbPage extends Page {
      LaunchViewBreadcrumb fCrumb;
      Control fControl;

      public void createControl(Composite parent) {
         this.fCrumb = new LaunchViewBreadcrumb(LaunchView.this, (TreeModelViewer)LaunchView.this.getViewer(), LaunchView.this.fTreeViewerDebugContextProvider);
         this.fControl = this.fCrumb.createContent(parent);
      }

      public void init(IPageSite pageSite) {
         super.init(pageSite);
         pageSite.setSelectionProvider(this.fCrumb.getSelectionProvider());
      }

      public Control getControl() {
         return this.fControl;
      }

      public void setFocus() {
         this.fCrumb.activate();
      }

      IDebugContextProvider getContextProvider() {
         return this.fCrumb.getContextProvider();
      }

      int getHeight() {
         return this.fCrumb.getHeight();
      }

      public void dispose() {
         this.fCrumb.dispose();
      }
   }

   class TreeViewerContextProvider extends AbstractDebugContextProvider implements IModelChangedListener {
      private ISelection fContext = null;
      private TreeModelViewer fViewer = null;
      private final Visitor fVisitor = new Visitor();

      private TreePath getViewerTreePath(IModelDelta node) {
         ArrayList<Object> list = new ArrayList();

         for(IModelDelta parentDelta = node.getParentDelta(); parentDelta != null; parentDelta = parentDelta.getParentDelta()) {
            list.add(0, node.getElement());
            node = parentDelta;
         }

         return new TreePath(list.toArray());
      }

      public TreeViewerContextProvider(TreeModelViewer viewer) {
         super(LaunchView.this);
         this.fViewer = viewer;
         this.fViewer.addModelChangedListener(this);
      }

      protected void dispose() {
         this.fContext = null;
         this.fViewer.removeModelChangedListener(this);
      }

      public synchronized ISelection getActiveContext() {
         return this.fContext;
      }

      protected void activate(ISelection selection) {
         synchronized(this) {
            this.fContext = selection;
         }

         this.fire(new DebugContextEvent(this, selection, 1));
      }

      protected void possibleChange(TreePath element, int type) {
         final DebugContextEvent event = null;
         synchronized(this) {
            if (this.fContext instanceof ITreeSelection) {
               ITreeSelection ss = (ITreeSelection)this.fContext;

               TreePath[] var9;
               for(TreePath path : var9 = ss.getPaths()) {
                  if (path.startsWith(element, (IElementComparer)null)) {
                     if (path.getSegmentCount() == element.getSegmentCount()) {
                        event = new DebugContextEvent(this, this.fContext, type);
                     } else {
                        event = new DebugContextEvent(this, this.fContext, 16);
                     }
                  }
               }
            }
         }

         if (event != null) {
            if (LaunchView.this.getControl().getDisplay().getThread() == Thread.currentThread()) {
               this.fire(event);
            } else {
               Job job = new UIJob("context change") {
                  public IStatus runInUIThread(IProgressMonitor monitor) {
                     synchronized(TreeViewerContextProvider.this) {
                        if (TreeViewerContextProvider.this.fContext instanceof IStructuredSelection) {
                           IStructuredSelection ss = (IStructuredSelection)TreeViewerContextProvider.this.fContext;
                           Object changed = ((IStructuredSelection)event.getContext()).getFirstElement();
                           if (ss.size() != 1 || !ss.getFirstElement().equals(changed)) {
                              return Status.OK_STATUS;
                           }
                        }
                     }

                     TreeViewerContextProvider.this.fire(event);
                     return Status.OK_STATUS;
                  }
               };
               job.setSystem(true);
               job.schedule();
            }

         }
      }

      public void modelChanged(IModelDelta delta, IModelProxy proxy) {
         delta.accept(this.fVisitor);
      }

      class Visitor implements IModelDeltaVisitor {
         public boolean visit(IModelDelta delta, int depth) {
            if ((delta.getFlags() & 3072) > 0 && (delta.getFlags() & 2097152) == 0) {
               if ((delta.getFlags() & 1024) > 0) {
                  TreeViewerContextProvider.this.possibleChange(TreeViewerContextProvider.this.getViewerTreePath(delta), 1);
               } else if ((delta.getFlags() & 2048) > 0) {
                  TreeViewerContextProvider.this.possibleChange(TreeViewerContextProvider.this.getViewerTreePath(delta), 16);
               }
            }

            return true;
         }
      }
   }

   private class ContextProviderProxy extends AbstractDebugContextProvider implements IDebugContextListener {
      private IDebugContextProvider fActiveProvider;
      private IDebugContextProvider[] fProviders;

      ContextProviderProxy(IDebugContextProvider[] providers) {
         super(LaunchView.this);
         this.fProviders = providers;
         this.fActiveProvider = providers[0];

         IDebugContextProvider[] var6;
         for(IDebugContextProvider provider : var6 = this.fProviders) {
            provider.addDebugContextListener(this);
         }

      }

      void setActiveProvider(IDebugContextProvider provider) {
         if (!provider.equals(this.fActiveProvider)) {
            ISelection activeContext = this.getActiveContext();
            this.fActiveProvider = provider;
            ISelection newActiveContext = this.getActiveContext();
            if (!activeContext.equals(newActiveContext)) {
               this.fire(new DebugContextEvent(this, this.getActiveContext(), 1));
            }
         }

      }

      public ISelection getActiveContext() {
         ISelection activeContext = this.fActiveProvider.getActiveContext();
         return (ISelection)(activeContext != null ? activeContext : TreeSelection.EMPTY);
      }

      public void debugContextChanged(DebugContextEvent event) {
         if (event.getSource().equals(this.fActiveProvider)) {
            this.fire(new DebugContextEvent(this, event.getContext(), event.getFlags()));
         }

      }

      void dispose() {
         IDebugContextProvider[] var4;
         for(IDebugContextProvider provider : var4 = this.fProviders) {
            provider.removeDebugContextListener(this);
         }

         this.fProviders = null;
         this.fActiveProvider = null;
      }
   }
}
