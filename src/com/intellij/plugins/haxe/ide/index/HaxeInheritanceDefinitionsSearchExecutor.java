package com.intellij.plugins.haxe.ide.index;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.DefinitionsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;
import com.intellij.util.indexing.FileBasedIndex;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeInheritanceDefinitionsSearchExecutor implements QueryExecutor<PsiElement, PsiElement> {
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
        else if (HaxeComponentType.typeOf(haxeNamedComponent) == HaxeComponentType.METHOD ||
                 HaxeComponentType.typeOf(haxeNamedComponent) == HaxeComponentType.FIELD) {
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
    final Project project = context.getProject();
    final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
    while (!namesQueue.isEmpty()) {
      final String name = namesQueue.pollFirst();
      if (!namesSet.add(name)) {
        continue;
      }
      List<List<HaxeClassInfo>> files = FileBasedIndex.getInstance().getValues(HaxeInheritanceIndex.HAXE_INHERITANCE_INDEX, name, scope);
      files.addAll(FileBasedIndex.getInstance().getValues(HaxeTypeDefInheritanceIndex.HAXE_TYPEDEF_INHERITANCE_INDEX, name, scope));
      for (List<HaxeClassInfo> subClassInfoList : files) {
        for (HaxeClassInfo subClassInfo : subClassInfoList) {
          final HaxeClass subClass = HaxeResolveUtil.findClassByQName(subClassInfo.getValue(), context.getManager(), scope);
          if (subClass != null) {
            if (!consumer.process(subClass)) {
              return true;
            }
            namesQueue.add(subClass.getQualifiedName());
          }
        }
      }
    }
    return true;
  }
}
