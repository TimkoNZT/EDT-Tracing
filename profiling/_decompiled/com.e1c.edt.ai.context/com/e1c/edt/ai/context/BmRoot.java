package com.e1c.edt.ai.context;

import com._1c.g5.v8.bm.core.IBmEngine;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com.google.common.base.Preconditions;
import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;

public class BmRoot {
   private final String path;
   private final URI uri;
   private final IBmModel model;
   private final IProject project;
   private final IDtProject dtProject;
   private final IBmEngine engine;
   private final IBmObject bmObject;
   private final IProjectFileSystemSupportProvider projectFileSystemSupportProvider;

   public BmRoot(String path, URI uri, IProject project, IBmModel model, IDtProject dtProject, IBmEngine engine, IBmObject bmObject, IProjectFileSystemSupportProvider projectFileSystemSupportProvider) {
      Preconditions.checkNotNull(path);
      Preconditions.checkNotNull(uri);
      Preconditions.checkNotNull(project);
      Preconditions.checkNotNull(model);
      Preconditions.checkNotNull(dtProject);
      Preconditions.checkNotNull(engine);
      Preconditions.checkNotNull(bmObject);
      Preconditions.checkNotNull(projectFileSystemSupportProvider);
      this.path = path;
      this.uri = uri;
      this.project = project;
      this.model = model;
      this.dtProject = dtProject;
      this.engine = engine;
      this.bmObject = bmObject;
      this.projectFileSystemSupportProvider = projectFileSystemSupportProvider;
   }

   public String getPath() {
      return this.path;
   }

   public URI getUri() {
      return this.uri;
   }

   public IProject getProject() {
      return this.project;
   }

   public IBmModel getModel() {
      return this.model;
   }

   public IDtProject getDtProject() {
      return this.dtProject;
   }

   public IBmEngine getEngine() {
      return this.engine;
   }

   public IBmObject getBmObject() {
      return this.bmObject;
   }

   public Optional<IFile> getFile(EObject obj) {
      return Optional.ofNullable(this.projectFileSystemSupportProvider.getProjectFileSystemSupport(this.getDtProject()).getFile(obj));
   }
}
