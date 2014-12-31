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
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.ui.LayeredIcon;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Created by srikanthg on 12/30/14.
 */
final public class HaxeTypeHierarchyNodeDescriptor extends HierarchyNodeDescriptor {
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

  @Nullable
  public final AbstractHaxePsiClass getHaxeClass() {
    return (myElement instanceof AbstractHaxePsiClass) ? (AbstractHaxePsiClass) myElement : null;
  }

  public final boolean isValid() {
    final AbstractHaxePsiClass haxePsiClass = getHaxeClass();
    return haxePsiClass != null && haxePsiClass.isValid();
  }

  public final boolean update() {
    boolean changes = super.update();

    final AbstractHaxePsiClass haxePsiClass = getHaxeClass();

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

    myHighlightedText.getEnding().addText(haxePsiClass.getName(), classNameAttributes);
    myHighlightedText.getEnding().addText(" (" + haxePsiClass.getQualifiedName() + ", " +
                                                 haxePsiClass.getContainingFile() + ")",
                                          HierarchyNodeDescriptor.getPackageNameAttributes());
    myName = myHighlightedText.getText();

    if (!Comparing.equal(myHighlightedText, oldText)) {
      changes = true;
    }
    return changes;
  }
}
