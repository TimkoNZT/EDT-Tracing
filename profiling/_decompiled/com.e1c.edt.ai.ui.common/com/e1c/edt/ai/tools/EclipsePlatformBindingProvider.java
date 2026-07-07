package com.e1c.edt.ai.tools;

import com.google.inject.Singleton;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.commands.common.EventManager;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

@Singleton
public class EclipsePlatformBindingProvider implements IJShellBindingProvider {
   public Map<String, JShellBindingDescription> getBindings() {
      HashMap<String, JShellBindingDescription> bindings = new HashMap();
      IWorkbench workbench = PlatformUI.getWorkbench();
      if (workbench != null) {
         bindings.put("workbench", new JShellBindingDescription("Eclipse workbench instance", "var activeWindow = workbench.getActiveWorkbenchWindow();\nSystem.out.println(\"Active window: \" + activeWindow);\nvar activePage = activeWindow != null ? activeWindow.getActivePage() : null;\nSystem.out.println(\"Active page: \" + activePage);", workbench, IWorkbench.class));
      }

      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      if (root != null) {
         bindings.put("workspaceRoot", new JShellBindingDescription("Eclipse workspace root for accessing all projects", this.buildWorkspaceRootDescription(), root, IWorkspaceRoot.class));
      }

      return bindings;
   }

   public String getDescription() {
      return "Eclipse platform services (workbench, UI, resources)";
   }

   public String getUseCases() {
      return "- Access Eclipse workbench and UI components\n- Get active editor, windows, pages\n- Execute Eclipse commands programmatically\n- Access Eclipse resources and preferences";
   }

   public Collection<Class<?>> getSignificantClasses() {
      return List.of(IAdaptable.class, IWorkbench.class, Display.class, Shell.class, IWorkbenchWindow.class, IWorkbenchPage.class, IEditorPart.class, IRunnableContext.class, IAdaptable.class, IProgressMonitor.class, EventManager.class);
   }

   public Collection<String> getImports() {
      return List.of("import org.eclipse.swt.widgets.*;", "import org.eclipse.swt.*;", "import org.eclipse.ui.*;", "import org.eclipse.jface.operation.*;", "import org.eclipse.core.runtime.*;", "import org.eclipse.core.commands.common.*;");
   }

   private String buildWorkspaceRootDescription() {
      StringBuilder desc = new StringBuilder();
      desc.append("## IWorkspaceRoot - Workspace Root\n\n");
      desc.append("Eclipse workspace root for accessing all projects.\n\n");
      desc.append("### Get Project by Name\n");
      desc.append("```java\n");
      desc.append("// Get project by name\n");
      desc.append("IProject project = workspaceRoot.getProject(\"MyProject\");\n");
      desc.append("if (project.exists()) {\n");
      desc.append("    System.out.println(\"Project exists: \" + project.getName());\n");
      desc.append("}\n");
      desc.append("```\n\n");
      desc.append("### Get All Projects\n");
      desc.append("```java\n");
      desc.append("// Get all projects in workspace\n");
      desc.append("IProject[] projects = workspaceRoot.getProjects();\n");
      desc.append("for (IProject project : projects) {\n");
      desc.append("    System.out.println(\"Project: \" + project.getName());\n");
      desc.append("}\n");
      desc.append("```\n\n");
      return desc.toString();
   }
}
