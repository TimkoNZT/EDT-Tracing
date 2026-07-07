package com.e1c.edt.ai.ui.preferences;

import com.e1c.edt.ai.IClientTokenValidator;
import com.e1c.edt.ai.IDefaultSettings;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsSetter;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.IValidator;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.ValidationError;
import com.e1c.edt.ai.ValidationResult;
import com.e1c.edt.ai.WellknownError;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IWeb;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class ClientAIPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
   private final Image SPLASH = createImage("icons/obj16/splash.png");
   private static final String[][] LANGUAGES;
   @Inject
   ILog log;
   @Inject
   @Named("URL")
   IValidator<String> urlValidator;
   @Inject
   @Named("Parameters")
   IValidator<String> parametersValidator;
   @Inject
   IPreferenceStore preferenceStore;
   @Inject
   IDefaultSettings defaultSettings;
   @Inject
   IStateService stateService;
   @Inject
   ISettings settings;
   @Inject
   ISettingsSetter settingsSetter;
   @Inject
   IClientTokenValidator clientTokenValidator;
   @Inject
   IWeb web;
   private String prevToken;
   private TokenFieldEditor tokenFieldEditor;
   private boolean settingsChanged = false;

   static {
      LANGUAGES = new String[][]{{Messages.ClientAIPreferencePage_Language_Default, ""}, {Messages.ClientAIPreferencePage_Language_English, "english"}, {Messages.ClientAIPreferencePage_Language_Russian, "russian"}};
   }

   public ClientAIPreferencePage() {
      super(1);
      BaseActivator.injectMembers(this);
      this.setPreferenceStore(this.preferenceStore);
   }

   public void createFieldEditors() {
      Composite parent = this.getFieldEditorParent();
      this.tokenFieldEditor = new TokenFieldEditor("stringPreferenceClientID", Messages.ClientAIPreferencePage_Client_Token, parent, new IValidator<String>() {
         public ValidationResult validate(String token) {
            if (token != null && !token.isBlank()) {
               return !ClientAIPreferencePage.this.clientTokenValidator.isValid(token) ? new ValidationResult(new ValidationError[]{new ValidationError(WellknownError.InvalidToken, token)}) : ValidationResult.SUCCESS;
            } else {
               return ValidationResult.SUCCESS;
            }
         }
      });
      BaseActivator.injectMembers(this.tokenFieldEditor);
      this.setLabelTooltip(this.tokenFieldEditor, Messages.ClientAIPreferencePage_Client_Token_Tooltip);
      Text tokenText = this.tokenFieldEditor.getTextControl(this.getFieldEditorParent());
      tokenText.setEchoChar('*');
      this.addField(this.tokenFieldEditor);
      PolicyComboFieldEditor policyCombo = new PolicyComboFieldEditor(parent);
      this.setLabelTooltip(policyCombo, Messages.ClientAIPreferencePage_CodeCompletionPolicy_Tooltip);
      this.addField(policyCombo);
      IntegerFieldEditor codeCompletionLinesCount = new IntegerFieldEditor("stringPreferenceCodeCompletionLinesCount", Messages.ClientAIPreferencePage_CodeCompletionLinesCount, parent);
      codeCompletionLinesCount.setValidRange(1, 64);
      this.setLabelTooltip(codeCompletionLinesCount, Messages.ClientAIPreferencePage_CodeCompletionLinesCount_Tooltip);
      this.addField(codeCompletionLinesCount);
      ComboFieldEditor comboField = new ComboFieldEditor("stringPreferenceLanguage", Messages.ClientAIPreferencePage_Language, LANGUAGES, parent);
      this.setLabelTooltip(comboField, Messages.ClientAIPreferencePage_Language_Tooltip);
      this.addField(comboField);
      ValidatingStringFieldEditor validatorField = new ValidatingStringFieldEditor("stringPreferenceLLMParameters", Messages.ClientAIPreferencePage_Parameters, parent, this.parametersValidator);
      this.setLabelTooltip(validatorField, Messages.ClientAIPreferencePage_Parameters_Tooltip);
      this.addField(validatorField);
   }

   private void setLabelTooltip(FieldEditor editor, String tooltip) {
      Label label = editor.getLabelControl(this.getFieldEditorParent());
      label.setToolTipText(tooltip);
   }

   public void init(IWorkbench workbench) {
      this.prevToken = this.settings.getClientToken();
   }

   public void propertyChange(PropertyChangeEvent event) {
      super.propertyChange(event);
      this.settingsChanged = true;
   }

   protected Control createContents(Composite parent) {
      Control control = super.createContents(parent);
      this.createDiagnosticSection(parent);
      Link pluginLink = new Link(parent, 0);
      pluginLink.setText("<a href=\"" + this.defaultSettings.getHomePage() + "\">" + this.defaultSettings.getHomePage() + "</a>");
      pluginLink.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            try {
               ClientAIPreferencePage.this.web.browse(ClientAIPreferencePage.this.defaultSettings.getHomePage());
            } catch (Exception ex) {
               ClientAIPreferencePage.this.log.logError("Failed to open URL: " + ClientAIPreferencePage.this.defaultSettings.getHomePage());
               ClientAIPreferencePage.this.log.logError(ex);
            }

         }
      });
      Label iconLabel = new Label(parent, 0);
      iconLabel.setImage(this.SPLASH);
      return control;
   }

   public void dispose() {
      this.SPLASH.dispose();
      super.dispose();
   }

   public boolean performOk() {
      boolean result = super.performOk();
      if (this.settingsChanged) {
         String token = this.settings.getClientToken();
         if (this.clientTokenValidator.isValid(token) && this.settings.getCodeCompletionPolicy() == CodeCompletionPolicy.OFF && !Objects.equals(token, this.prevToken)) {
            this.settingsSetter.setCodeCompletionPolicy(CodeCompletionPolicy.MODERATE);
         }

         this.stateService.setState(ServiceState.SETTINGS_CHANGED);
      }

      this.prevToken = this.settings.getClientToken();
      return result;
   }

   private void createDiagnosticSection(Composite parent) {
      Group diagnosticGroup = new Group(parent, 0);
      diagnosticGroup.setText(Messages.ClientAIPreferencePage_Diagnostic);
      diagnosticGroup.setLayoutData(new GridData(4, 128, true, false));
      diagnosticGroup.setLayout(new GridLayout(2, false));
      GridLayout gl = (GridLayout)diagnosticGroup.getLayout();
      gl.marginHeight = 8;
      gl.marginWidth = 10;
      gl.horizontalSpacing = 10;
      Label info = new Label(diagnosticGroup, 64);
      info.setText(Messages.ClientAIPreferencePage_Diagnostic_Title);
      GridData infoGD = new GridData(4, 128, true, false);
      infoGD.widthHint = 300;
      info.setLayoutData(infoGD);
      Button button = new Button(diagnosticGroup, 8);
      button.setText(Messages.ClientAIPreferencePage_Diagnostic_RunButton);
      button.setLayoutData(new GridData(16384, 16777216, false, false));
      button.addSelectionListener(new SelectionListener() {
         public void widgetSelected(SelectionEvent e) {
            DiagnosticDialog dialog = new DiagnosticDialog(ClientAIPreferencePage.this.getShell());
            dialog.open();
         }

         public void widgetDefaultSelected(SelectionEvent e) {
         }
      });
   }

   private static Image createImage(String path) {
      ImageDescriptor descriptor = ImageDescriptor.createFromURL(FileLocator.find(BaseActivator.getDefault().getBundle(), new Path(path), (Map)null));
      return descriptor.createImage();
   }

   private static class PolicyComboFieldEditor extends ComboFieldEditor {
      private static final String[][] CODE_COMPLETION_POLICIES = (String[][])Arrays.stream(CodeCompletionPolicy.values()).map((policy) -> new String[]{policy.getLongName(), policy.getId()}).toArray((var0) -> new String[var0][]);
      private Combo combo;

      public PolicyComboFieldEditor(Composite parent) {
         super("stringPreferenceCodeCompletionPolicy", Messages.ClientAIPreferencePage_CodeCompletionPolicy, CODE_COMPLETION_POLICIES, parent);
      }

      protected void doFillIntoGrid(Composite parent, int numColumns) {
         super.doFillIntoGrid(parent, numColumns);
         Control[] childernAfter = parent.getChildren();
         if (childernAfter.length > 0) {
            Control control = childernAfter[childernAfter.length - 1];
            if (control instanceof Combo) {
               this.combo = (Combo)control;
            }
         }

      }

      protected void doLoad() {
         super.doLoad();
         this.updateToolTipText();
      }

      protected void doLoadDefault() {
         super.doLoadDefault();
         this.updateToolTipText();
      }

      protected void valueChanged(String oldValue, String newValue) {
         super.valueChanged(oldValue, newValue);
         this.updateToolTipText();
      }

      private void updateToolTipText() {
         if (this.combo != null) {
            int index = this.combo.getSelectionIndex();
            CodeCompletionPolicy[] policies = CodeCompletionPolicy.values();
            if (policies != null && policies.length > 0 && index >= 0 && index < policies.length) {
               this.combo.setToolTipText(policies[index].getDescription());
            } else {
               this.combo.setToolTipText("");
            }

         }
      }
   }
}
