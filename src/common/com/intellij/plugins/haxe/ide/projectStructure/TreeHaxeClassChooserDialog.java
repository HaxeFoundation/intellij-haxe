/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.plugins.haxe.ide.projectStructure;

import com.intellij.ide.projectView.impl.nodes.ClassTreeNode;
import com.intellij.ide.util.AbstractTreeClassChooserDialog;
import com.intellij.ide.util.ClassFilter;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.plugins.haxe.ide.index.HaxeComponentIndex;
import com.intellij.plugins.haxe.ide.index.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeClassResolveCache;
import com.intellij.plugins.haxe.lang.psi.HaxeComponent;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.Query;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;

/**
 * taken from/heavily based on
 * https://github.com/JetBrains/intellij-community/blob/master/java/java-impl/src/com/intellij/ide/util/TreeJavaClassChooserDialog.java
 * by as3boyan
 * @author traff
 */
public class TreeHaxeClassChooserDialog extends AbstractTreeClassChooserDialog<PsiClass> implements TreeClassChooser {
  public TreeHaxeClassChooserDialog(String title, Project project) {
    super(title, project, PsiClass.class);
  }

  public TreeHaxeClassChooserDialog(String title, Project project, @Nullable PsiClass initialClass) {
    super(title, project, PsiClass.class, initialClass);
  }

  public TreeHaxeClassChooserDialog(String title,
                                    @NotNull Project project,
                                    GlobalSearchScope scope,
                                    final ClassFilter classFilter, @Nullable PsiClass initialClass) {
    super(title, project, scope, PsiClass.class, createFilter(classFilter), initialClass);
  }


  public TreeHaxeClassChooserDialog(String title,
                                    @NotNull Project project,
                                    GlobalSearchScope scope,
                                    @Nullable ClassFilter classFilter,
                                    PsiClass baseClass,
                                    @Nullable PsiClass initialClass, boolean isShowMembers) {
    super(title, project, scope, PsiClass.class, createFilter(classFilter), baseClass, initialClass, isShowMembers, true);
  }

  public static TreeHaxeClassChooserDialog withInnerClasses(String title,
                                                            @NotNull Project project,
                                                            GlobalSearchScope scope,
                                                            final ClassFilter classFilter,
                                                            @Nullable PsiClass initialClass) {
    return new TreeHaxeClassChooserDialog(title, project, scope, classFilter, null, initialClass, true);
  }

  @Nullable
  private static Filter<PsiClass> createFilter(@Nullable final ClassFilter classFilter) {
    if (classFilter == null) {
      return null;
    }
    else {
      return new Filter<PsiClass>() {
        @Override
        public boolean isAccepted(final PsiClass element) {
          return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
              return classFilter.isAccepted(element);
            }
          });
        }
      };
    }
  }

  @Override
  @Nullable
  protected PsiClass getSelectedFromTreeUserObject(DefaultMutableTreeNode node) {
    Object userObject = node.getUserObject();
    if (!(userObject instanceof ClassTreeNode)) return null;
    ClassTreeNode descriptor = (ClassTreeNode)userObject;
    return descriptor.getPsiClass();
  }

  @NotNull
  protected List<PsiClass> getClassesByName(final String name,
                                            final boolean checkBoxState,
                                            final String pattern,
                                            final GlobalSearchScope searchScope) {
    List<HaxeComponent> components = HaxeComponentIndex.getItemsByName(name, getProject(), searchScope);
    List<PsiClass> classes = new ArrayList<PsiClass>();

    for (HaxeComponent component : components) {
      if (component instanceof PsiClass) {
        classes.add((PsiClass)component);
      }
    }

    return classes;
  }
}
