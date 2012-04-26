package com.intellij.plugins.haxe.ide.index;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeTypeDefImpl;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.DefinitionsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeInheritanceIndex extends FileBasedIndexExtension<String, HaxeClassInfo> {
  public static final ID<String, HaxeClassInfo> HAXE_COMPONENT_INDEX = ID.create("HaxeInheritanceIndex");
  private static final int INDEX_VERSION = 1;
  private final DataIndexer<String, HaxeClassInfo, FileContent> myIndexer = new MyDataIndexer();
  private final DataExternalizer<HaxeClassInfo> myExternalizer = new HaxeClassInfoExternalizer();

  @NotNull
  @Override
  public ID<String, HaxeClassInfo> getName() {
    return HAXE_COMPONENT_INDEX;
  }

  @Override
  public int getVersion() {
    return INDEX_VERSION;
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public KeyDescriptor<String> getKeyDescriptor() {
    return new EnumeratorStringDescriptor();
  }

  @Override
  public DataExternalizer<HaxeClassInfo> getValueExternalizer() {
    return myExternalizer;
  }

  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return new FileBasedIndex.InputFilter() {
      @Override
      public boolean acceptInput(VirtualFile file) {
        return file.getFileType() == HaxeFileType.HAXE_FILE_TYPE;
      }
    };
  }

  @NotNull
  @Override
  public DataIndexer<String, HaxeClassInfo, FileContent> getIndexer() {
    return myIndexer;
  }

  private static class MyDataIndexer implements DataIndexer<String, HaxeClassInfo, FileContent> {
    @Override
    @NotNull
    public Map<String, HaxeClassInfo> map(final FileContent inputData) {
      final PsiFile psiFile = inputData.getPsiFile();
      final List<HaxeClass> classes = HaxeResolveUtil.findComponentDeclarations(psiFile);
      if (classes.isEmpty()) {
        return Collections.emptyMap();
      }
      final Map<String, HaxeClassInfo> result = new THashMap<String, HaxeClassInfo>(classes.size());
      for (HaxeClass haxeClass : classes) {
        final HaxeClassInfo value = new HaxeClassInfo(haxeClass.getQualifiedName(), HaxeComponentType.typeOf(haxeClass));
        if (haxeClass instanceof AbstractHaxeTypeDefImpl) {
          final HaxeTypeOrAnonymous haxeTypeOrAnonymous = ((AbstractHaxeTypeDefImpl)haxeClass).getTypeOrAnonymous();
          assert haxeTypeOrAnonymous != null;
          final HaxeType type = haxeTypeOrAnonymous.getType();
          final HaxeAnonymousType anonymousType = haxeTypeOrAnonymous.getAnonymousType();
          if (anonymousType != null) {
            final HaxeTypeExtends typeExtends = anonymousType.getAnonymousTypeBody().getTypeExtends();
            if (typeExtends != null) {
              final String key = HaxeResolveUtil.getQName(typeExtends.getType(), true);
              result.put(key, value);
            }
          }
          else if (type != null) {
            result.put(HaxeResolveUtil.getQName(type, true), value);
          }
        }
        else {
          for (HaxeType haxeType : haxeClass.getExtendsList()) {
            if(haxeType == null) continue;
            final String key = HaxeResolveUtil.getQName(haxeType, true);
            result.put(key, value);
          }
          for (HaxeType haxeType : haxeClass.getImplementsList()) {
            if(haxeType == null) continue;
            final String key = HaxeResolveUtil.getQName(haxeType, true);
            result.put(key, value);
          }
        }
      }
      return result;
    }
  }

  public static List<HaxeClass> getItemsByQName(final HaxeClass haxeClass) {
    final List<HaxeClass> result = new ArrayList<HaxeClass>();
    DefinitionsSearch.search(haxeClass).forEach(new Processor<PsiElement>() {
      @Override
      public boolean process(PsiElement element) {
        if (element instanceof HaxeClass) {
          result.add((HaxeClass)element);
        }
        return true;
      }
    });
    return result;
  }

  public static class DefinitionsSearchExecutor implements QueryExecutor<PsiElement, PsiElement> {
    @Override
    public boolean execute(@NotNull final PsiElement queryParameters, @NotNull final Processor<PsiElement> consumer) {
      return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
        public Boolean compute() {
          final PsiElement queryParametersParent = queryParameters.getParent();
          HaxeNamedComponent haxeNamedComponent;
          if (queryParameters instanceof HaxeClass) {
            haxeNamedComponent = (HaxeClass)queryParameters;
          }
          else if (queryParametersParent instanceof HaxeNamedComponent && queryParameters instanceof HaxeComponentName) {
            haxeNamedComponent = (HaxeNamedComponent)queryParametersParent;
          }
          else {
            return true;
          }
          if (haxeNamedComponent instanceof HaxeClass) {
            processInheritors(((HaxeClass)haxeNamedComponent).getQualifiedName(), queryParameters, consumer);
          }
          else if (HaxeComponentType.typeOf(haxeNamedComponent) == HaxeComponentType.METHOD) {
            final String nameToFind = haxeNamedComponent.getName();
            if (nameToFind == null) return true;

            HaxeClass haxeClass = PsiTreeUtil.getParentOfType(haxeNamedComponent, HaxeClass.class);
            assert haxeClass != null;

            processInheritors(haxeClass.getQualifiedName(), queryParameters, new Processor<PsiElement>() {
              @Override
              public boolean process(PsiElement element) {
                for (HaxeNamedComponent subHaxeNamedComponent : HaxeResolveUtil.getNamedSubComponents((HaxeClass)element)) {
                  if (nameToFind.equals(subHaxeNamedComponent.getName())) {
                    consumer.process(subHaxeNamedComponent);
                  }
                }
                return true;
              }
            });
          }
          return true;
        }
      });
    }

    private static boolean processInheritors(final String qName, final PsiElement context, final Processor<PsiElement> consumer) {
      final Set<String> namesSet = new THashSet<String>();
      final LinkedList<String> namesQueue = new LinkedList<String>();
      namesQueue.add(qName);
      while (!namesQueue.isEmpty()) {
        final String name = namesQueue.pollFirst();
        if (!namesSet.add(name)) {
          continue;
        }
        List<HaxeClassInfo> files =
          FileBasedIndex.getInstance().getValues(HAXE_COMPONENT_INDEX, name, GlobalSearchScope.allScope(context.getProject()));
        for (HaxeClassInfo subClassInfo : files) {
          final HaxeClass subClass = HaxeResolveUtil.findClassByQName(subClassInfo.getValue(), context);
          if (subClass != null) {
            if (!consumer.process(subClass)) {
              return true;
            }
            namesQueue.add(subClass.getQualifiedName());
          }
        }
      }
      return true;
    }
  }
}
