package com.e1c.edt.ai.context.tools;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
   private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages";
   public static String Find1CObjectsTitle;
   public static String Found1CObjectsTemplate;
   public static String Get1CObjectByIdTitle;
   public static String ObjectRetrievedTemplate;

   static {
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {
   }
}
