/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide.hierarchy.type;

//import com.intellij.ide.DataManager;
//import com.intellij.ide.hierarchy.HierarchyBrowserManager;
//import com.intellij.ide.hierarchy.HierarchyNodeRenderer;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.type.TypeHierarchyBrowser;
//import com.intellij.openapi.actionSystem.CommonDataKeys;
//import com.intellij.openapi.actionSystem.DataContext;
//import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
//import com.intellij.openapi.fileTypes.FileTypes;
//import com.intellij.openapi.fileTypes.INativeFileType;
import com.intellij.openapi.project.Project;
//import com.intellij.openapi.util.ActionCallback;
//import com.intellij.openapi.vfs.PersistentFSConstants;
//import com.intellij.openapi.vfs.VirtualFile;
//import com.intellij.openapi.wm.ToolWindow;
//import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
//import com.intellij.ui.AutoScrollToSourceHandler;
//import com.intellij.ui.TreeSpeedSearch;
//import com.intellij.ui.treeStructure.Tree;
//import com.intellij.util.OpenSourceUtil;
//import com.intellij.util.ui.UIUtil;
//import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

//import javax.swing.tree.TreeSelectionModel;
//import java.awt.*;

/**
 * Created by srikanthg on 10/23/14.
 */
public class HaxeTypeHierarchyBrowser extends TypeHierarchyBrowser {
  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.ide.hierarchy.type.HaxeTypeHierarchyBrowser");

  //private Tree  myTree;
  //private final AutoScrollToSourceHandler typeHierarchyAutoScrl2Src;

  public HaxeTypeHierarchyBrowser(final Project project, final PsiClass psiClass) {
    super(project, psiClass);
    //typeHierarchyAutoScrl2Src = new MyAutoScrollToSourceHandler();
    //typeHierarchyAutoScrl2Src.install(myTree);
  }

  protected HierarchyTreeStructure createHierarchyTreeStructure(@NotNull final String typeName, @NotNull final PsiElement psiElement) {
    HierarchyTreeStructure currentActiveTree = null;
    if (SUPERTYPES_HIERARCHY_TYPE.equals(typeName)) {
      currentActiveTree = new HaxeSupertypesHierarchyTreeStructure(myProject, (PsiClass) psiElement);
    }
    else if (SUBTYPES_HIERARCHY_TYPE.equals(typeName)) {
      currentActiveTree = new HaxeSubtypesHierarchyTreeStructure(myProject, (PsiClass) psiElement);
    }
    else if (TYPE_HIERARCHY_TYPE.equals(typeName)) {
      currentActiveTree = new HaxeTypeHierarchyTreeStructure(myProject, (PsiClass) psiElement);
    }
    else {
      LOG.error("unexpected type: " + typeName);
    }
    return currentActiveTree;
  }

  //@Override
  //protected void configureTree(@NotNull Tree tree) {
  //  tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
  //  tree.setToggleClickCount(-1);
  //  tree.setCellRenderer(new HierarchyNodeRenderer());
  //  UIUtil.setLineStyleAngled(tree);
  //  new TreeSpeedSearch(tree);
  //  TreeUtil.installActions(tree);
  //  myTree = tree;
  //}
  //
  //private final class MyAutoScrollToSourceHandler extends AutoScrollToSourceHandler {
  //  protected MyAutoScrollToSourceHandler() {
  //    super();
  //  }
  //
  //  @Override
  //  protected boolean isAutoScrollMode() {
  //    return HierarchyBrowserManager.getInstance(myProject).getState().IS_AUTOSCROLL_TO_SOURCE;
  //  }
  //
  //  @Override
  //  protected void setAutoScrollMode(boolean state) {
  //    HierarchyBrowserManager.getInstance(myProject).getState().IS_AUTOSCROLL_TO_SOURCE = state;
  //  }
  //
  //  @Override
  //  protected void scrollToSource(Component tree) {
  //    DataContext dataContext=DataManager.getInstance().getDataContext(myTree);
  //    final VirtualFile vFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
  //    if (vFile != null) {
  //      // Attempt to navigate to the virtual file with unknown file type will show a modal dialog
  //      // asking to register some file type for this file. This behaviour is undesirable when autoscrolling.
  //      if (vFile.getFileType() == FileTypes.UNKNOWN || vFile.getFileType() instanceof INativeFileType) return;
  //      //IDEA-84881 Don't autoscroll to very large files
  //      if (vFile.getLength() > PersistentFSConstants.getMaxIntellisenseFileSize()) return;
  //    }
  //    Navigatable[] navigatables = CommonDataKeys.NAVIGATABLE_ARRAY.getData(dataContext);
  //    if (navigatables != null) {
  //      if (navigatables.length > 1) {
  //        return;
  //      }
  //      for (Navigatable navigatable : navigatables) {
  //        // we are not going to open modal dialog during auto-scrolling
  //        if (!navigatable.canNavigateToSource()) return;
  //      }
  //    }
  //    OpenSourceUtil.navigate(false, true, navigatables);
  //  }
  //}
}
