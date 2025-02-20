package com.intellij.plugins.haxe.ide.index;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.Pair;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@CustomLog
public class HaxeStaticMemberIndex extends FileBasedIndexExtension<String, HaxeStaticMemberInfo> {
  public static final ID<String, HaxeStaticMemberInfo> HAXE_STATIC_MEMBER_INDEX = ID.create("HaxeStaticMemberIndex");
  private static final int INDEX_VERSION = HaxeIndexUtil.BASE_INDEX_VERSION + 12;
  private DataIndexer<String, HaxeStaticMemberInfo, FileContent> myDataIndexer = new MyDataIndexer();
  private final DataExternalizer<HaxeStaticMemberInfo> myExternalizer = new HaxeStaticMemberInfoExternalizer();

  @NotNull
  @Override
  public ID<String, HaxeStaticMemberInfo> getName() {
    return HAXE_STATIC_MEMBER_INDEX;
  }

  @Override
  public int getVersion() {
    return INDEX_VERSION;
  }

  @NotNull
  @Override
  public DataIndexer<String, HaxeStaticMemberInfo, FileContent> getIndexer() {
    return myDataIndexer;
  }

  @NotNull
  @Override
  public KeyDescriptor<String> getKeyDescriptor() {
    return new EnumeratorStringDescriptor();
  }

  @Override
  public @NotNull DataExternalizer<HaxeStaticMemberInfo> getValueExternalizer() {
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
    return FileBasedIndex.getInstance().getAllKeys(HAXE_STATIC_MEMBER_INDEX, project);
  }

  public static void processAll(Project project, Processor<Pair<String, HaxeStaticMemberInfo>> processor, GlobalSearchScope scope,
                                @NlsSafe String filterText) {
    HaxeIndexUtil.warnIfDumbMode(project);
    final Collection<String> keys = getNames(project);
    for (String key : keys) {
      final List<HaxeStaticMemberInfo> values = FileBasedIndex.getInstance().getValues(HAXE_STATIC_MEMBER_INDEX, key, scope);
      for (HaxeStaticMemberInfo value : values) {
        if (value.getOwnerName().startsWith(filterText)) {
          final Pair<String, HaxeStaticMemberInfo> pair = Pair.create(key, value);
          if (!processor.process(pair)) {
            return;
          }
        }
      }
    }
  }



  private static class MyDataIndexer implements DataIndexer<String, HaxeStaticMemberInfo, FileContent> {

    @Override
    @NotNull
    public Map<String, HaxeStaticMemberInfo> map(final FileContent inputData) {
      final PsiFile psiFile = inputData.getPsiFile();

      if (HaxeIndexUtil.fileBelongToPlatformSpecificStd(psiFile)) {
        return Collections.emptyMap();
      }
      if (HaxeIndexUtil.belongToPlatformNotTargeted(psiFile)) {
        return Collections.emptyMap();
      }

      final List<HaxeClass> classes = HaxeResolveUtil.findComponentDeclarations(psiFile);
      if (classes.isEmpty()) {
        return Collections.emptyMap();
      }
      final Map<String, HaxeStaticMemberInfo> result = new HashMap<>();
      for (HaxeClass haxeClass : classes) {
        if (haxeClass.getName() == null) {
          continue;
        }

        if (haxeClass.isTypeDef() || haxeClass.isAnonymousType() || haxeClass.isAbstractType() || haxeClass.isInterface()) {
          continue;
        }
        //TODO considder adding support for static methods ?
        //List<HaxeMethod> allMethods = haxeClass.getHaxeMethodsSelf(null).stream().filter(HaxeNamedComponent::isStatic).filter(HaxeNamedComponent::isPublic).toList();

        List<HaxeFieldDeclaration> allFields = haxeClass.getFieldSelf(null).stream()
          .filter(HaxeNamedComponent::isStatic)
          .filter(HaxeNamedComponent::isPublic)
          .toList();

        for (HaxeFieldDeclaration field : allFields) {
          String qualifiedName = haxeClass.getQualifiedName();
          if(qualifiedName != null) {
            final Pair<String, String> packageAndName = HaxeResolveUtil.splitQName(qualifiedName);
            String packageString = packageAndName.getFirst();
            String classString = packageAndName.getSecond();

            String memberName = field.getComponentName().getName();
            HaxeComponentType componentType = field.getComponentType();

            HaxeTypeTag tag = field.getTypeTag();
            if (tag != null) {
              HaxeTypeOrAnonymous toa = tag.getTypeOrAnonymous();
              if (toa != null) {
                HaxeType type = toa.getType();
                if (type != null) {
                  HaxeStaticMemberInfo info = new HaxeStaticMemberInfo(packageString, classString, memberName, componentType, type.getText());
                  result.put(classString + "." + memberName, info);
                  continue;
                }

              }
              HaxeFunctionType functionType = tag.getFunctionType();
              if (functionType != null) {
                //HaxeFunctionReturnType returnType = functionType.getFunctionReturnType();
                //List<HaxeFunctionArgument> argumentList = functionType.getFunctionArgumentList();

                //TODO handle this correctly
                HaxeStaticMemberInfo info = new HaxeStaticMemberInfo(packageString, classString, memberName, componentType, functionType.getText());
                result.put(classString + "." + memberName, info);
                continue;
              }
            }
          }
        }
        //final Pair<String, String> packageAndName = HaxeResolveUtil.splitQName(haxeClass.getQualifiedName());
        //final HaxeStaticMemberInfo info = new HaxeStaticMemberInfo(packageAndName.getFirst(), HaxeComponentType.typeOf(haxeClass));
        //result.put(packageAndName.getSecond(), info);
      }
      return result;
    }
  }
}
