package com.intellij.plugins.haxe.ide.index;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponent;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeComponentIndex extends FileBasedIndexExtension<String, HaxeClassInfo> {
  public static final ID<String, HaxeClassInfo> HAXE_COMPONENT_INDEX = ID.create("HaxeComponentIndex");
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

  public static List<HaxeComponent> getItemsByName(final String name, Project project) {
    final GlobalSearchScope searchScope = GlobalSearchScope.allScope(project);
    return getItemsByName(name, project, searchScope);
  }

  public static List<HaxeComponent> getItemsByName(String name, Project project, GlobalSearchScope searchScope) {
    Collection<VirtualFile> files =
      FileBasedIndex.getInstance().getContainingFiles(HAXE_COMPONENT_INDEX, name, searchScope);
    final List<HaxeComponent> result = new ArrayList<HaxeComponent>();
    for (VirtualFile vFile : files) {
      PsiFile file = PsiManager.getInstance(project).findFile(vFile);
      if (file == null || file.getFileType() != HaxeFileType.HAXE_FILE_TYPE) {
        continue;
      }
      final HaxeComponent component = HaxeResolveUtil.findComponentDeclaration(file, name);
      if (component != null) {
        result.add(component);
      }
    }
    return result;
  }

  public static void processAll(Project project, @NotNull Processor<Pair<String, HaxeClassInfo>> processor) {
    processAll(project, processor, GlobalSearchScope.allScope(project));
  }

  public static void processAll(Project project, Processor<Pair<String, HaxeClassInfo>> processor, GlobalSearchScope scope) {
    final Collection<String> keys = getNames(project);
    for (String key : keys) {
      final List<HaxeClassInfo> values = FileBasedIndex.getInstance().getValues(HAXE_COMPONENT_INDEX, key, scope);
      for (HaxeClassInfo value : values) {
        final Pair<String, HaxeClassInfo> pair = Pair.create(key, value);
        if (!processor.process(pair)) {
          return;
        }
      }
    }
  }

  public static Collection<String> getNames(Project project) {
    return FileBasedIndex.getInstance().getAllKeys(HAXE_COMPONENT_INDEX, project);
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
      final String packageName = UsefulPsiTreeUtil.findPackageName(psiFile);
      final Map<String, HaxeClassInfo> result = new THashMap<String, HaxeClassInfo>(classes.size());
      for (HaxeClass haxeClass : classes) {
        final String name = haxeClass.getName();
        final HaxeClassInfo info = new HaxeClassInfo(packageName, HaxeComponentType.typeOf(haxeClass));
        if (name != null) {
          result.put(name, info);
        }
      }
      return result;
    }
  }
}
