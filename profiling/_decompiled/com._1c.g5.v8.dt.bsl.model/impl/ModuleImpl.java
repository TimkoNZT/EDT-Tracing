Compiled from "ModuleImpl.java"
public class com._1c.g5.v8.dt.bsl.model.impl.ModuleImpl extends com._1c.g5.v8.dt.bsl.model.impl.BlockImpl implements com._1c.g5.v8.dt.bsl.model.Module {
  protected org.eclipse.emf.ecore.EObject owner;

  protected static final com._1c.g5.v8.dt.bsl.model.ModuleType MODULE_TYPE_EDEFAULT;

  protected com._1c.g5.v8.dt.bsl.model.ModuleType moduleType;

  protected org.eclipse.emf.common.util.EList<com._1c.g5.v8.dt.bsl.model.Pragma> defaultPragmas;

  protected org.eclipse.emf.common.util.EList<com._1c.g5.v8.dt.bsl.model.Method> methods;

  protected org.eclipse.emf.common.util.EList<com._1c.g5.v8.dt.bsl.model.Preprocessor> preprocessors;

  protected com._1c.g5.v8.dt.mcore.ContextDef contextDef;

  static {};
    Code:
       0: getstatic     #25                 // Field com/_1c/g5/v8/dt/bsl/model/ModuleType.COMMON_MODULE:Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;
       3: putstatic     #30                 // Field MODULE_TYPE_EDEFAULT:Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;
       6: return

  protected com._1c.g5.v8.dt.bsl.model.impl.ModuleImpl();
    Code:
       0: aload_0
       1: invokespecial #35                 // Method com/_1c/g5/v8/dt/bsl/model/impl/BlockImpl."<init>":()V
       4: aload_0
       5: getstatic     #30                 // Field MODULE_TYPE_EDEFAULT:Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;
       8: putfield      #37                 // Field moduleType:Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;
      11: return

  protected org.eclipse.emf.ecore.EClass eStaticClass();
    Code:
       0: getstatic     #43                 // Field com/_1c/g5/v8/dt/bsl/model/BslPackage$Literals.MODULE:Lorg/eclipse/emf/ecore/EClass;
       3: areturn

  public org.eclipse.emf.ecore.EObject getOwner();
    Code:
       0: aload_0
       1: getfield      #51                 // Field owner:Lorg/eclipse/emf/ecore/EObject;
       4: ifnull        71
       7: aload_0
       8: getfield      #51                 // Field owner:Lorg/eclipse/emf/ecore/EObject;
      11: invokeinterface #53,  1           // InterfaceMethod org/eclipse/emf/ecore/EObject.eIsProxy:()Z
      16: ifeq          71
      19: aload_0
      20: getfield      #51                 // Field owner:Lorg/eclipse/emf/ecore/EObject;
      23: checkcast     #59                 // class org/eclipse/emf/ecore/InternalEObject
      26: astore_1
      27: aload_0
      28: aload_0
      29: aload_1
      30: invokevirtual #61                 // Method eResolveProxy:(Lorg/eclipse/emf/ecore/InternalEObject;)Lorg/eclipse/emf/ecore/EObject;
      33: putfield      #51                 // Field owner:Lorg/eclipse/emf/ecore/EObject;
      36: aload_0
      37: getfield      #51                 // Field owner:Lorg/eclipse/emf/ecore/EObject;
      40: aload_1
      41: if_acmpeq     71
      44: aload_0
      45: invokevirtual #65                 // Method eNotificationRequired:()Z
      48: ifeq          71
      51: aload_0
      52: new           #68                 // class org/eclipse/emf/ecore/impl/ENotificationImpl
      55: dup
      56: aload_0
      57: bipush        9
      59: iconst_5
      60: aload_1
      61: aload_0
      62: getfield      #51                 // Field owner:Lorg/eclipse/emf/ecore/EObject;
      65: invokespecial #70                 // Method org/eclipse/emf/ecore/impl/ENotificationImpl."<init>":(Lorg/eclipse/emf/ecore/InternalEObject;IILjava/lang/Object;Ljava/lang/Object;)V
      68: invokevirtual #73                 // Method eNotify:(Lorg/eclipse/emf/common/notify/Notification;)V
      71: aload_0
      72: getfield      #51                 // Field owner:Lorg/eclipse/emf/ecore/EObject;
      75: areturn

  public org.eclipse.emf.ecore.EObject basicGetOwner();
    Code:
       0: aload_0
       1: getfield      #51                 // Field owner:Lorg/eclipse/emf/ecore/EObject;
       4: areturn

  public void setOwner(org.eclipse.emf.ecore.EObject);
    Code:
       0: aload_0
       1: getfield      #51                 // Field owner:Lorg/eclipse/emf/ecore/EObject;
       4: astore_2
       5: aload_0
       6: aload_1
       7: putfield      #51                 // Field owner:Lorg/eclipse/emf/ecore/EObject;
      10: aload_0
      11: invokevirtual #65                 // Method eNotificationRequired:()Z
      14: ifeq          36
      17: aload_0
      18: new           #68                 // class org/eclipse/emf/ecore/impl/ENotificationImpl
      21: dup
      22: aload_0
      23: iconst_1
      24: iconst_5
      25: aload_2
      26: aload_0
      27: getfield      #51                 // Field owner:Lorg/eclipse/emf/ecore/EObject;
      30: invokespecial #70                 // Method org/eclipse/emf/ecore/impl/ENotificationImpl."<init>":(Lorg/eclipse/emf/ecore/InternalEObject;IILjava/lang/Object;Ljava/lang/Object;)V
      33: invokevirtual #73                 // Method eNotify:(Lorg/eclipse/emf/common/notify/Notification;)V
      36: return

  public com._1c.g5.v8.dt.bsl.model.ModuleType getModuleType();
    Code:
       0: aload_0
       1: getfield      #37                 // Field moduleType:Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;
       4: areturn

  public void setModuleType(com._1c.g5.v8.dt.bsl.model.ModuleType);
    Code:
       0: aload_0
       1: getfield      #37                 // Field moduleType:Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;
       4: astore_2
       5: aload_0
       6: aload_1
       7: ifnonnull     16
      10: getstatic     #30                 // Field MODULE_TYPE_EDEFAULT:Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;
      13: goto          17
      16: aload_1
      17: putfield      #37                 // Field moduleType:Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;
      20: aload_0
      21: invokevirtual #65                 // Method eNotificationRequired:()Z
      24: ifeq          47
      27: aload_0
      28: new           #68                 // class org/eclipse/emf/ecore/impl/ENotificationImpl
      31: dup
      32: aload_0
      33: iconst_1
      34: bipush        6
      36: aload_2
      37: aload_0
      38: getfield      #37                 // Field moduleType:Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;
      41: invokespecial #70                 // Method org/eclipse/emf/ecore/impl/ENotificationImpl."<init>":(Lorg/eclipse/emf/ecore/InternalEObject;IILjava/lang/Object;Ljava/lang/Object;)V
      44: invokevirtual #73                 // Method eNotify:(Lorg/eclipse/emf/common/notify/Notification;)V
      47: return

  public org.eclipse.emf.common.util.EList<com._1c.g5.v8.dt.bsl.model.Pragma> getDefaultPragmas();
    Code:
       0: aload_0
       1: getfield      #93                 // Field defaultPragmas:Lorg/eclipse/emf/common/util/EList;
       4: ifnonnull     23
       7: aload_0
       8: new           #95                 // class org/eclipse/emf/ecore/util/EObjectContainmentEList
      11: dup
      12: ldc           #97                 // class com/_1c/g5/v8/dt/bsl/model/Pragma
      14: aload_0
      15: bipush        7
      17: invokespecial #99                 // Method org/eclipse/emf/ecore/util/EObjectContainmentEList."<init>":(Ljava/lang/Class;Lorg/eclipse/emf/ecore/InternalEObject;I)V
      20: putfield      #93                 // Field defaultPragmas:Lorg/eclipse/emf/common/util/EList;
      23: aload_0
      24: getfield      #93                 // Field defaultPragmas:Lorg/eclipse/emf/common/util/EList;
      27: areturn

  public org.eclipse.emf.common.util.EList<com._1c.g5.v8.dt.bsl.model.Method> getMethods();
    Code:
       0: aload_0
       1: getfield      #104                // Field methods:Lorg/eclipse/emf/common/util/EList;
       4: ifnonnull     23
       7: aload_0
       8: new           #95                 // class org/eclipse/emf/ecore/util/EObjectContainmentEList
      11: dup
      12: ldc           #106                // class com/_1c/g5/v8/dt/bsl/model/Method
      14: aload_0
      15: bipush        8
      17: invokespecial #99                 // Method org/eclipse/emf/ecore/util/EObjectContainmentEList."<init>":(Ljava/lang/Class;Lorg/eclipse/emf/ecore/InternalEObject;I)V
      20: putfield      #104                // Field methods:Lorg/eclipse/emf/common/util/EList;
      23: aload_0
      24: getfield      #104                // Field methods:Lorg/eclipse/emf/common/util/EList;
      27: areturn

  public org.eclipse.emf.common.util.EList<com._1c.g5.v8.dt.bsl.model.Preprocessor> getPreprocessors();
    Code:
       0: aload_0
       1: getfield      #110                // Field preprocessors:Lorg/eclipse/emf/common/util/EList;
       4: ifnonnull     23
       7: aload_0
       8: new           #95                 // class org/eclipse/emf/ecore/util/EObjectContainmentEList
      11: dup
      12: ldc           #112                // class com/_1c/g5/v8/dt/bsl/model/Preprocessor
      14: aload_0
      15: bipush        9
      17: invokespecial #99                 // Method org/eclipse/emf/ecore/util/EObjectContainmentEList."<init>":(Ljava/lang/Class;Lorg/eclipse/emf/ecore/InternalEObject;I)V
      20: putfield      #110                // Field preprocessors:Lorg/eclipse/emf/common/util/EList;
      23: aload_0
      24: getfield      #110                // Field preprocessors:Lorg/eclipse/emf/common/util/EList;
      27: areturn

  public com._1c.g5.v8.dt.mcore.ContextDef getContextDef();
    Code:
       0: aload_0
       1: getfield      #116                // Field contextDef:Lcom/_1c/g5/v8/dt/mcore/ContextDef;
       4: ifnull        75
       7: aload_0
       8: getfield      #116                // Field contextDef:Lcom/_1c/g5/v8/dt/mcore/ContextDef;
      11: invokeinterface #118,  1          // InterfaceMethod com/_1c/g5/v8/dt/mcore/ContextDef.eIsProxy:()Z
      16: ifeq          75
      19: aload_0
      20: getfield      #116                // Field contextDef:Lcom/_1c/g5/v8/dt/mcore/ContextDef;
      23: checkcast     #59                 // class org/eclipse/emf/ecore/InternalEObject
      26: astore_1
      27: aload_0
      28: aload_0
      29: aload_1
      30: invokevirtual #61                 // Method eResolveProxy:(Lorg/eclipse/emf/ecore/InternalEObject;)Lorg/eclipse/emf/ecore/EObject;
      33: checkcast     #119                // class com/_1c/g5/v8/dt/mcore/ContextDef
      36: putfield      #116                // Field contextDef:Lcom/_1c/g5/v8/dt/mcore/ContextDef;
      39: aload_0
      40: getfield      #116                // Field contextDef:Lcom/_1c/g5/v8/dt/mcore/ContextDef;
      43: aload_1
      44: if_acmpeq     75
      47: aload_0
      48: invokevirtual #65                 // Method eNotificationRequired:()Z
      51: ifeq          75
      54: aload_0
      55: new           #68                 // class org/eclipse/emf/ecore/impl/ENotificationImpl
      58: dup
      59: aload_0
      60: bipush        9
      62: bipush        10
      64: aload_1
      65: aload_0
      66: getfield      #116                // Field contextDef:Lcom/_1c/g5/v8/dt/mcore/ContextDef;
      69: invokespecial #70                 // Method org/eclipse/emf/ecore/impl/ENotificationImpl."<init>":(Lorg/eclipse/emf/ecore/InternalEObject;IILjava/lang/Object;Ljava/lang/Object;)V
      72: invokevirtual #73                 // Method eNotify:(Lorg/eclipse/emf/common/notify/Notification;)V
      75: aload_0
      76: getfield      #116                // Field contextDef:Lcom/_1c/g5/v8/dt/mcore/ContextDef;
      79: areturn

  public com._1c.g5.v8.dt.mcore.ContextDef basicGetContextDef();
    Code:
       0: aload_0
       1: getfield      #116                // Field contextDef:Lcom/_1c/g5/v8/dt/mcore/ContextDef;
       4: areturn

  public void setContextDef(com._1c.g5.v8.dt.mcore.ContextDef);
    Code:
       0: aload_0
       1: getfield      #116                // Field contextDef:Lcom/_1c/g5/v8/dt/mcore/ContextDef;
       4: astore_2
       5: aload_0
       6: aload_1
       7: putfield      #116                // Field contextDef:Lcom/_1c/g5/v8/dt/mcore/ContextDef;
      10: aload_0
      11: invokevirtual #65                 // Method eNotificationRequired:()Z
      14: ifeq          37
      17: aload_0
      18: new           #68                 // class org/eclipse/emf/ecore/impl/ENotificationImpl
      21: dup
      22: aload_0
      23: iconst_1
      24: bipush        10
      26: aload_2
      27: aload_0
      28: getfield      #116                // Field contextDef:Lcom/_1c/g5/v8/dt/mcore/ContextDef;
      31: invokespecial #70                 // Method org/eclipse/emf/ecore/impl/ENotificationImpl."<init>":(Lorg/eclipse/emf/ecore/InternalEObject;IILjava/lang/Object;Ljava/lang/Object;)V
      34: invokevirtual #73                 // Method eNotify:(Lorg/eclipse/emf/common/notify/Notification;)V
      37: return

  public org.eclipse.emf.common.util.EList<com._1c.g5.v8.dt.bsl.model.Method> allMethods();
    Code:
       0: aload_0
       1: invokestatic  #127                // Method com/_1c/g5/v8/dt/bsl/model/util/BslUtil.allMethods:(Lorg/eclipse/emf/ecore/EObject;)Ljava/util/List;
       4: astore_1
       5: new           #132                // class org/eclipse/emf/common/util/DelegatingEList$UnmodifiableEList
       8: dup
       9: aload_1
      10: invokespecial #134                // Method org/eclipse/emf/common/util/DelegatingEList$UnmodifiableEList."<init>":(Ljava/util/List;)V
      13: areturn

  public org.eclipse.emf.common.notify.NotificationChain eInverseRemove(org.eclipse.emf.ecore.InternalEObject, int, org.eclipse.emf.common.notify.NotificationChain);
    Code:
       0: iload_2
       1: tableswitch   { // 7 to 9

                     7: 28

                     8: 43

                     9: 58
               default: 73
          }
      28: aload_0
      29: invokevirtual #143                // Method getDefaultPragmas:()Lorg/eclipse/emf/common/util/EList;
      32: checkcast     #145                // class org/eclipse/emf/ecore/util/InternalEList
      35: aload_1
      36: aload_3
      37: invokeinterface #147,  3          // InterfaceMethod org/eclipse/emf/ecore/util/InternalEList.basicRemove:(Ljava/lang/Object;Lorg/eclipse/emf/common/notify/NotificationChain;)Lorg/eclipse/emf/common/notify/NotificationChain;
      42: areturn
      43: aload_0
      44: invokevirtual #151                // Method getMethods:()Lorg/eclipse/emf/common/util/EList;
      47: checkcast     #145                // class org/eclipse/emf/ecore/util/InternalEList
      50: aload_1
      51: aload_3
      52: invokeinterface #147,  3          // InterfaceMethod org/eclipse/emf/ecore/util/InternalEList.basicRemove:(Ljava/lang/Object;Lorg/eclipse/emf/common/notify/NotificationChain;)Lorg/eclipse/emf/common/notify/NotificationChain;
      57: areturn
      58: aload_0
      59: invokevirtual #153                // Method getPreprocessors:()Lorg/eclipse/emf/common/util/EList;
      62: checkcast     #145                // class org/eclipse/emf/ecore/util/InternalEList
      65: aload_1
      66: aload_3
      67: invokeinterface #147,  3          // InterfaceMethod org/eclipse/emf/ecore/util/InternalEList.basicRemove:(Ljava/lang/Object;Lorg/eclipse/emf/common/notify/NotificationChain;)Lorg/eclipse/emf/common/notify/NotificationChain;
      72: areturn
      73: aload_0
      74: aload_1
      75: iload_2
      76: aload_3
      77: invokespecial #155                // Method com/_1c/g5/v8/dt/bsl/model/impl/BlockImpl.eInverseRemove:(Lorg/eclipse/emf/ecore/InternalEObject;ILorg/eclipse/emf/common/notify/NotificationChain;)Lorg/eclipse/emf/common/notify/NotificationChain;
      80: areturn

  public java.lang.Object eGet(int, boolean, boolean);
    Code:
       0: iload_1
       1: tableswitch   { // 5 to 10

                     5: 40

                     6: 54

                     7: 59

                     8: 64

                     9: 69

                    10: 74
               default: 88
          }
      40: iload_2
      41: ifeq          49
      44: aload_0
      45: invokevirtual #164                // Method getOwner:()Lorg/eclipse/emf/ecore/EObject;
      48: areturn
      49: aload_0
      50: invokevirtual #166                // Method basicGetOwner:()Lorg/eclipse/emf/ecore/EObject;
      53: areturn
      54: aload_0
      55: invokevirtual #168                // Method getModuleType:()Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;
      58: areturn
      59: aload_0
      60: invokevirtual #143                // Method getDefaultPragmas:()Lorg/eclipse/emf/common/util/EList;
      63: areturn
      64: aload_0
      65: invokevirtual #151                // Method getMethods:()Lorg/eclipse/emf/common/util/EList;
      68: areturn
      69: aload_0
      70: invokevirtual #153                // Method getPreprocessors:()Lorg/eclipse/emf/common/util/EList;
      73: areturn
      74: iload_2
      75: ifeq          83
      78: aload_0
      79: invokevirtual #170                // Method getContextDef:()Lcom/_1c/g5/v8/dt/mcore/ContextDef;
      82: areturn
      83: aload_0
      84: invokevirtual #172                // Method basicGetContextDef:()Lcom/_1c/g5/v8/dt/mcore/ContextDef;
      87: areturn
      88: aload_0
      89: iload_1
      90: iload_2
      91: iload_3
      92: invokespecial #174                // Method com/_1c/g5/v8/dt/bsl/model/impl/BlockImpl.eGet:(IZZ)Ljava/lang/Object;
      95: areturn

  public void eSet(int, java.lang.Object);
    Code:
       0: iload_1
       1: tableswitch   { // 5 to 10

                     5: 40

                     6: 49

                     7: 58

                     8: 82

                     9: 106

                    10: 130
               default: 139
          }
      40: aload_0
      41: aload_2
      42: checkcast     #54                 // class org/eclipse/emf/ecore/EObject
      45: invokevirtual #181                // Method setOwner:(Lorg/eclipse/emf/ecore/EObject;)V
      48: return
      49: aload_0
      50: aload_2
      51: checkcast     #26                 // class com/_1c/g5/v8/dt/bsl/model/ModuleType
      54: invokevirtual #183                // Method setModuleType:(Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;)V
      57: return
      58: aload_0
      59: invokevirtual #143                // Method getDefaultPragmas:()Lorg/eclipse/emf/common/util/EList;
      62: invokeinterface #185,  1          // InterfaceMethod org/eclipse/emf/common/util/EList.clear:()V
      67: aload_0
      68: invokevirtual #143                // Method getDefaultPragmas:()Lorg/eclipse/emf/common/util/EList;
      71: aload_2
      72: checkcast     #190                // class java/util/Collection
      75: invokeinterface #192,  2          // InterfaceMethod org/eclipse/emf/common/util/EList.addAll:(Ljava/util/Collection;)Z
      80: pop
      81: return
      82: aload_0
      83: invokevirtual #151                // Method getMethods:()Lorg/eclipse/emf/common/util/EList;
      86: invokeinterface #185,  1          // InterfaceMethod org/eclipse/emf/common/util/EList.clear:()V
      91: aload_0
      92: invokevirtual #151                // Method getMethods:()Lorg/eclipse/emf/common/util/EList;
      95: aload_2
      96: checkcast     #190                // class java/util/Collection
      99: invokeinterface #192,  2          // InterfaceMethod org/eclipse/emf/common/util/EList.addAll:(Ljava/util/Collection;)Z
     104: pop
     105: return
     106: aload_0
     107: invokevirtual #153                // Method getPreprocessors:()Lorg/eclipse/emf/common/util/EList;
     110: invokeinterface #185,  1          // InterfaceMethod org/eclipse/emf/common/util/EList.clear:()V
     115: aload_0
     116: invokevirtual #153                // Method getPreprocessors:()Lorg/eclipse/emf/common/util/EList;
     119: aload_2
     120: checkcast     #190                // class java/util/Collection
     123: invokeinterface #192,  2          // InterfaceMethod org/eclipse/emf/common/util/EList.addAll:(Ljava/util/Collection;)Z
     128: pop
     129: return
     130: aload_0
     131: aload_2
     132: checkcast     #119                // class com/_1c/g5/v8/dt/mcore/ContextDef
     135: invokevirtual #196                // Method setContextDef:(Lcom/_1c/g5/v8/dt/mcore/ContextDef;)V
     138: return
     139: aload_0
     140: iload_1
     141: aload_2
     142: invokespecial #198                // Method com/_1c/g5/v8/dt/bsl/model/impl/BlockImpl.eSet:(ILjava/lang/Object;)V
     145: return

  public void eUnset(int);
    Code:
       0: iload_1
       1: tableswitch   { // 5 to 10

                     5: 40

                     6: 46

                     7: 54

                     8: 64

                     9: 74

                    10: 84
               default: 90
          }
      40: aload_0
      41: aconst_null
      42: invokevirtual #181                // Method setOwner:(Lorg/eclipse/emf/ecore/EObject;)V
      45: return
      46: aload_0
      47: getstatic     #30                 // Field MODULE_TYPE_EDEFAULT:Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;
      50: invokevirtual #183                // Method setModuleType:(Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;)V
      53: return
      54: aload_0
      55: invokevirtual #143                // Method getDefaultPragmas:()Lorg/eclipse/emf/common/util/EList;
      58: invokeinterface #185,  1          // InterfaceMethod org/eclipse/emf/common/util/EList.clear:()V
      63: return
      64: aload_0
      65: invokevirtual #151                // Method getMethods:()Lorg/eclipse/emf/common/util/EList;
      68: invokeinterface #185,  1          // InterfaceMethod org/eclipse/emf/common/util/EList.clear:()V
      73: return
      74: aload_0
      75: invokevirtual #153                // Method getPreprocessors:()Lorg/eclipse/emf/common/util/EList;
      78: invokeinterface #185,  1          // InterfaceMethod org/eclipse/emf/common/util/EList.clear:()V
      83: return
      84: aload_0
      85: aconst_null
      86: invokevirtual #196                // Method setContextDef:(Lcom/_1c/g5/v8/dt/mcore/ContextDef;)V
      89: return
      90: aload_0
      91: iload_1
      92: invokespecial #204                // Method com/_1c/g5/v8/dt/bsl/model/impl/BlockImpl.eUnset:(I)V
      95: return

  public boolean eIsSet(int);
    Code:
       0: iload_1
       1: tableswitch   { // 5 to 10

                     5: 40

                     6: 51

                     7: 65

                     8: 88

                     9: 111

                    10: 134
               default: 145
          }
      40: aload_0
      41: getfield      #51                 // Field owner:Lorg/eclipse/emf/ecore/EObject;
      44: ifnull        49
      47: iconst_1
      48: ireturn
      49: iconst_0
      50: ireturn
      51: aload_0
      52: getfield      #37                 // Field moduleType:Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;
      55: getstatic     #30                 // Field MODULE_TYPE_EDEFAULT:Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;
      58: if_acmpeq     63
      61: iconst_1
      62: ireturn
      63: iconst_0
      64: ireturn
      65: aload_0
      66: getfield      #93                 // Field defaultPragmas:Lorg/eclipse/emf/common/util/EList;
      69: ifnull        86
      72: aload_0
      73: getfield      #93                 // Field defaultPragmas:Lorg/eclipse/emf/common/util/EList;
      76: invokeinterface #208,  1          // InterfaceMethod org/eclipse/emf/common/util/EList.isEmpty:()Z
      81: ifne          86
      84: iconst_1
      85: ireturn
      86: iconst_0
      87: ireturn
      88: aload_0
      89: getfield      #104                // Field methods:Lorg/eclipse/emf/common/util/EList;
      92: ifnull        109
      95: aload_0
      96: getfield      #104                // Field methods:Lorg/eclipse/emf/common/util/EList;
      99: invokeinterface #208,  1          // InterfaceMethod org/eclipse/emf/common/util/EList.isEmpty:()Z
     104: ifne          109
     107: iconst_1
     108: ireturn
     109: iconst_0
     110: ireturn
     111: aload_0
     112: getfield      #110                // Field preprocessors:Lorg/eclipse/emf/common/util/EList;
     115: ifnull        132
     118: aload_0
     119: getfield      #110                // Field preprocessors:Lorg/eclipse/emf/common/util/EList;
     122: invokeinterface #208,  1          // InterfaceMethod org/eclipse/emf/common/util/EList.isEmpty:()Z
     127: ifne          132
     130: iconst_1
     131: ireturn
     132: iconst_0
     133: ireturn
     134: aload_0
     135: getfield      #116                // Field contextDef:Lcom/_1c/g5/v8/dt/mcore/ContextDef;
     138: ifnull        143
     141: iconst_1
     142: ireturn
     143: iconst_0
     144: ireturn
     145: aload_0
     146: iload_1
     147: invokespecial #211                // Method com/_1c/g5/v8/dt/bsl/model/impl/BlockImpl.eIsSet:(I)Z
     150: ireturn

  public java.lang.Object eInvoke(int, org.eclipse.emf.common.util.EList<?>) throws java.lang.reflect.InvocationTargetException;
    Code:
       0: iload_1
       1: tableswitch   { // 3 to 3

                     3: 20
               default: 25
          }
      20: aload_0
      21: invokevirtual #219                // Method allMethods:()Lorg/eclipse/emf/common/util/EList;
      24: areturn
      25: aload_0
      26: iload_1
      27: aload_2
      28: invokespecial #221                // Method com/_1c/g5/v8/dt/bsl/model/impl/BlockImpl.eInvoke:(ILorg/eclipse/emf/common/util/EList;)Ljava/lang/Object;
      31: areturn

  public java.lang.String toString();
    Code:
       0: aload_0
       1: invokevirtual #228                // Method eIsProxy:()Z
       4: ifeq          12
       7: aload_0
       8: invokespecial #229                // Method com/_1c/g5/v8/dt/bsl/model/impl/BlockImpl.toString:()Ljava/lang/String;
      11: areturn
      12: new           #231                // class java/lang/StringBuilder
      15: dup
      16: aload_0
      17: invokespecial #229                // Method com/_1c/g5/v8/dt/bsl/model/impl/BlockImpl.toString:()Ljava/lang/String;
      20: invokespecial #233                // Method java/lang/StringBuilder."<init>":(Ljava/lang/String;)V
      23: astore_1
      24: aload_1
      25: ldc           #236                // String  (moduleType:
      27: invokevirtual #238                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
      30: pop
      31: aload_1
      32: aload_0
      33: getfield      #37                 // Field moduleType:Lcom/_1c/g5/v8/dt/bsl/model/ModuleType;
      36: invokevirtual #242                // Method java/lang/StringBuilder.append:(Ljava/lang/Object;)Ljava/lang/StringBuilder;
      39: pop
      40: aload_1
      41: bipush        41
      43: invokevirtual #245                // Method java/lang/StringBuilder.append:(C)Ljava/lang/StringBuilder;
      46: pop
      47: aload_1
      48: invokevirtual #248                // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
      51: areturn
}
