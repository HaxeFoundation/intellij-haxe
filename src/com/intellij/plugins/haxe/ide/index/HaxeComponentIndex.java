package com.intellij.plugins.haxe.ide.index;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.HaxeComponent;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeComponentIndex extends ScalarIndexExtension<String> {
  public static final ID<String, Void> HAXE_COMPONENT_INDEX = ID.create("HaxeComponentIndex");
  private static final int INDEX_VERSION = 0;
  private final DataIndexer<String, Void, FileContent> myIndexer = new MyDataIndexer();

  @NotNull
  @Override
  public ID<String, Void> getName() {
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
  public DataIndexer<String, Void, FileContent> getIndexer() {
    return myIndexer;
  }

  public static Collection<NavigationItem> getItemsByName(final String name, Project project) {
    Collection<VirtualFile> files =
      FileBasedIndex.getInstance().getContainingFiles(HAXE_COMPONENT_INDEX, name, GlobalSearchScope.projectScope(project));
    final Collection<NavigationItem> result = new ArrayList<NavigationItem>();
    for (VirtualFile vFile : files) {
      PsiFile file = PsiManager.getInstance(project).findFile(vFile);
      if (file == null || file.getFileType() != HaxeFileType.HAXE_FILE_TYPE) {
        continue;
      }
      for (HaxeComponent component : HaxeResolveUtil.findComponentDeclarations(file)) {
        result.add(component);
      }
    }
    return result;
  }

  public static Collection<String> getNames(Project project) {
    return FileBasedIndex.getInstance().getAllKeys(HAXE_COMPONENT_INDEX, project);
  }

  private static class MyDataIndexer implements DataIndexer<String, Void, FileContent> {
    @Override
    @NotNull
    public Map<String, Void> map(final FileContent inputData) {
      final PsiFile psiFile = inputData.getPsiFile();
      final List<HaxeComponent> components = HaxeResolveUtil.findComponentDeclarations(psiFile);
      if (components.isEmpty()) {
        return Collections.emptyMap();
      }
      final Map<String, Void> result = new THashMap<String, Void>(components.size());
      for (HaxeComponent component : components) {
        result.put(component.getName(), null);
      }
      return result;
    }
  }
}
