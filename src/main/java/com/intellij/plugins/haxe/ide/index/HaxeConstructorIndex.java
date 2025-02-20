package com.intellij.plugins.haxe.ide.index;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeTypeDefImpl;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
@CustomLog
public class HaxeConstructorIndex extends FileBasedIndexExtension<String, HaxeConstructorInfo> {
  public static final ID<String, HaxeConstructorInfo> HAXE_CONSTRUCTOR_INDEX = ID.create("HaxeConstructorIndex");
  private static final int INDEX_VERSION = HaxeIndexUtil.BASE_INDEX_VERSION + 10;
  private DataIndexer<String, HaxeConstructorInfo, FileContent> myDataIndexer = new MyDataIndexer();
  private final DataExternalizer<HaxeConstructorInfo> myExternalizer = new HaxeConstructorInfoExternalizer();

  @NotNull
  @Override
  public ID<String, HaxeConstructorInfo> getName() {
    return HAXE_CONSTRUCTOR_INDEX;
  }

  @Override
  public int getVersion() {
    return INDEX_VERSION;
  }

  @NotNull
  @Override
  public DataIndexer<String, HaxeConstructorInfo, FileContent> getIndexer() {
    return myDataIndexer;
  }

  @NotNull
  @Override
  public KeyDescriptor<String> getKeyDescriptor() {
    return new EnumeratorStringDescriptor();
  }

  @Override
  public @NotNull DataExternalizer<HaxeConstructorInfo> getValueExternalizer() {
    return myExternalizer;
  }

  @NotNull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return HaxeSdkInputFilter.INSTANCE;
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  public static Collection<String> getNames(Project project) {
    HaxeIndexUtil.warnIfDumbMode(project);
    return FileBasedIndex.getInstance().getAllKeys(HAXE_CONSTRUCTOR_INDEX, project);
  }

  public static void processAll(Project project, Processor<Pair<String, HaxeConstructorInfo>> processor, GlobalSearchScope scope) {
    HaxeIndexUtil.warnIfDumbMode(project);
    final Collection<String> keys = getNames(project);
    for (String key : keys) {
      final List<HaxeConstructorInfo> values = FileBasedIndex.getInstance().getValues(HAXE_CONSTRUCTOR_INDEX, key, scope);
      for (HaxeConstructorInfo value : values) {
        final Pair<String, HaxeConstructorInfo> pair = Pair.create(key, value);
        if (!processor.process(pair)) {
          return;
        }
      }
    }
  }



  private static class MyDataIndexer implements DataIndexer<String, HaxeConstructorInfo, FileContent> {
    @Override
    @NotNull
    public Map<String, HaxeConstructorInfo> map(@NotNull final FileContent inputData) {
      final PsiFile psiFile = inputData.getPsiFile();
      final List<HaxeClass> classes = HaxeResolveUtil.findComponentDeclarations(psiFile);
      if (classes.isEmpty()) {
        return Collections.emptyMap();
      }
      final Map<String, HaxeConstructorInfo> result = new HashMap<>();
      for (HaxeClass haxeClass : classes) {
        final String className = haxeClass.getName();
        if (className == null) {
          continue;
        }


        String qualifiedName = haxeClass.getQualifiedName();
        if (qualifiedName != null) {
          final Pair<String, String> packageAndName = HaxeResolveUtil.splitQName(qualifiedName);
          String packageString = packageAndName.getFirst();
          String classString = packageAndName.getSecond();
          HaxeComponentType componentType = haxeClass.getComponentType();

          if (haxeClass.isTypeDef()) {
            componentType = HaxeComponentType.TYPEDEF;
          }

          for (HaxeMethod method : getConstructorMethods(haxeClass)) {
            boolean gotParameters = method.getParameterList().getParametersCount() > 0;
            HaxeConstructorInfo info = new HaxeConstructorInfo(classString, packageString, gotParameters, componentType);
            result.put(qualifiedName, info);
          }
        }
      }
      return result;
    }
  }


  private static final Class[] BODY_TYPES =
    new Class[]{HaxeClassBody.class, HaxeAbstractBody.class, HaxeEnumBody.class, HaxeExternClassDeclarationBody.class, HaxeAnonymousTypeBody.class};
  private static final Class[] MEMBER_TYPES =
    new Class[]{HaxeEnumValueDeclaration.class, HaxeMethod.class,
      HaxeFieldDeclaration.class};

  @NotNull
  private static List<HaxeMethod> getConstructorMethods(HaxeClass cls) {
    if (cls.isTypeDef()) {
      HaxeClass haxeClass = tryToFindClassFromTypeDef(cls);
      if (haxeClass != null) {
        cls = haxeClass;
      }
    }

    final PsiElement body = PsiTreeUtil.getChildOfAnyType(cls, BODY_TYPES);
    final List<HaxeMethod> components = new ArrayList<>();
    if (body != null) {
      final Collection<HaxeNamedComponent> members = PsiTreeUtil.findChildrenOfAnyType(body, MEMBER_TYPES);
      for (HaxeNamedComponent member : members) {
        if (member instanceof HaxeMethod method && method.isConstructor()) {
          components.add(method);
        }
      }
    }
    return components;
  }

  @Nullable
  private static HaxeClass tryToFindClassFromTypeDef(HaxeClass cls) {
    if (cls instanceof AbstractHaxeTypeDefImpl haxeTypeDef) {
      final HaxeTypeOrAnonymous haxeTypeOrAnonymous = haxeTypeDef.getTypeOrAnonymous();
      final HaxeType type = haxeTypeOrAnonymous == null ? null : haxeTypeOrAnonymous.getType();
      if (type != null) {
        HaxeReferenceExpression expression = type.getReferenceExpression();
        final String classNameCandidate = expression.getText();
        String name = HaxeResolveUtil.getQName(cls.getContainingFile(), classNameCandidate, true, true, type);
        if (name == null) name = classNameCandidate;// fallback so key wont be null
        return HaxeResolveUtil.findClassByQName(name, cls);
      }
    }
    return null;
  }
}
