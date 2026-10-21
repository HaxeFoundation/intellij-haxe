/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2019 Eric Bishton
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
package com.intellij.plugins.haxe.ide.refactoring.introduceVariable;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.util.HaxeSuggestedNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeIntroduceOperation {
  private final Project myProject;
  private Editor myEditor;
  private PsiFile myFile;
  private String myName;
  private String myActionName;
  private Boolean myReplaceAll;
  private PsiElement myElement;
  private PsiElement myInitializer;
  private List<PsiElement> myOccurrences = Collections.emptyList();
  private Collection<String> mySuggestedNames;
  private HaxeSuggestedNames mySuggestion;
  private boolean nameWasAutoSelectedFromSuggestions;

  public HaxeIntroduceOperation(Project project,
                                Editor editor,
                                PsiFile file,
                                String name,
                                String actionName
                                ) {
    myProject = project;
    myEditor = editor;
    myFile = file;
    myName = name;
    myActionName = actionName;
  }

  public String getName() {
    return myName;
  }

  public void setName(@Nullable String name) {
    myName = name;
    nameWasAutoSelectedFromSuggestions = false;
  }

  /** Takes the first suggestion as the name; the in-place template then offers all of them. */
  public void suggestName() {
    assert null == myName : "Name has already been assigned.";

    if (null != mySuggestedNames && !mySuggestedNames.isEmpty()) {
      setName(mySuggestedNames.iterator().next());
      nameWasAutoSelectedFromSuggestions = true;
    }
  }

  public boolean isNameSuggested() {
    return nameWasAutoSelectedFromSuggestions;
  }

  public Project getProject() {
    return myProject;
  }

  public Editor getEditor() {
    return myEditor;
  }

  /**
   * Updates the current editor when the operation changes the editor that the operation is occurring in, such as when
   * an embedded element (e.g. regular expression) is moved (e.g. the operation wraps the REGEX_FILE) and the new
   * element is not in the embedded editor where the operation started.
   */
  public Editor updateEditor(Editor editor) {
    return myEditor = editor;
  }

  public PsiFile getFile() {
    return myFile;
  }

  /**
   * Updates the current file when the operation changes the jfile that the operation is occurring in, such as when
   * an embedded element (e.g. regular expression) is moved (e.g. the operation wraps the REGEX_FILE) and the new
   * element is not in the embedded file where the operation started.
   *
   * @param file
   * @return
   */
  public PsiFile updateFile(PsiFile file) {
    return myFile = file;
  }

  public PsiElement getElement() {
    return myElement;
  }

  public void setElement(PsiElement element) {
    myElement = element;
  }

  public Boolean isReplaceAll() {
    return myReplaceAll;
  }

  public void setReplaceAll(boolean replaceAll) {
    myReplaceAll = replaceAll;
  }

  public PsiElement getInitializer() {
    return myInitializer;
  }

  public void setInitializer(PsiElement initializer) {
    myInitializer = initializer;
  }

  public List<PsiElement> getOccurrences() {
    return myOccurrences;
  }

  public void setOccurrences(List<PsiElement> occurrences) {
    myOccurrences = occurrences;
  }

  public Collection<String> getSuggestedNames() {
    return mySuggestedNames;
  }

  /** Sets the suggested names together with their description, under which the chosen name is remembered. */
  public void setSuggestion(@NotNull HaxeSuggestedNames suggestion) {
    mySuggestion = suggestion;
    mySuggestedNames = suggestion.names();
  }

  /** Remembers {@code chosenName} for the next similar value. */
  public void recordChosenName(@Nullable String chosenName) {
    if (mySuggestion != null && chosenName != null) mySuggestion.recordChosen(chosenName);
  }
}
