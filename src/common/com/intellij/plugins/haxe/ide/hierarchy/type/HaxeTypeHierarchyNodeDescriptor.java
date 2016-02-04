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

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.util.CompositeAppearance;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.ide.hierarchy.HaxeHierarchyNodeDescriptor;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeExternClassDeclaration;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.ui.LayeredIcon;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Created by srikanthg on 12/30/14.
 */
final public class HaxeTypeHierarchyNodeDescriptor extends HaxeHierarchyNodeDescriptor {

  public HaxeTypeHierarchyNodeDescriptor(final Project project,
                                         final HierarchyNodeDescriptor parentDescriptor,
                                         final PsiClass haxePsiClass,
                                         final boolean isBase) {
    super(project, parentDescriptor, haxePsiClass, isBase);
  }

  @Nullable
  @Override
  public PsiFile getContainingFile() {
    return super.getContainingFile();
  }

  public final boolean update() {
    boolean changes = super.update();

    final HaxeClass haxePsiClass = getHaxeClass();

    if (haxePsiClass == null) {
      final String invalidPrefix = IdeBundle.message("node.hierarchy.invalid");
      if (!myHighlightedText.getText().startsWith(invalidPrefix)) {
        myHighlightedText.getBeginning().addText(invalidPrefix, HierarchyNodeDescriptor.getInvalidPrefixAttributes());
      }
      return true;
    }

    if (changes && myIsBase) {
      final LayeredIcon icon = new LayeredIcon(2);
      icon.setIcon(getIcon(), 0);
      icon.setIcon(AllIcons.Hierarchy.Base, 1, -AllIcons.Hierarchy.Base.getIconWidth() / 2, 0);
      setIcon(icon);
    }

    final CompositeAppearance oldText = myHighlightedText;

    myHighlightedText = new CompositeAppearance();

    TextAttributes classNameAttributes = null;
    if (myColor != null) {
      classNameAttributes = new TextAttributes(myColor, null, null, null, Font.PLAIN);
    }

    final PsiFile psiFile = haxePsiClass.getContainingFile();
    final String noExtensionFilename = FileUtil.getNameWithoutExtension(psiFile.getName());
    final boolean isNonPrimaryClassOfTheFile = ((! (haxePsiClass instanceof HaxeExternClassDeclaration)) &&
                                                (! (noExtensionFilename.equals(haxePsiClass.getName()))) &&
                                                (HaxeResolveUtil.findComponentDeclaration(psiFile, noExtensionFilename) != null));

    String packageScopeStr = "";
    final String packageStr = HaxeResolveUtil.getPackageName(psiFile);
    if (! packageStr.equals("")) {
      packageScopeStr = "  (" + packageStr + ") ";
    }

    String fileScopeStr = ""; // for classes that are in a file that doesn't match their name, display filename too
    if (isNonPrimaryClassOfTheFile || "".equals(packageScopeStr)) {
      fileScopeStr = "  (" + psiFile + ") ";
    }

    myHighlightedText.getEnding().addText(haxePsiClass.getName(), classNameAttributes);
    myName = myHighlightedText.getText();
    myHighlightedText.getEnding().addText(packageScopeStr + fileScopeStr, HierarchyNodeDescriptor.getPackageNameAttributes());

    if (!Comparing.equal(myHighlightedText, oldText)) {
      changes = true;
    }
    return changes;
  }
}
