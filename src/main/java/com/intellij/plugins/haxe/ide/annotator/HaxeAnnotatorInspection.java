/*
 * Copyright 2020 Eric Bishton
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
package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.UnfairLocalInspectionTool;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.*;

/**
 * An "inspection" that will show up as an entry in the inspection settings dialog,
 * but really doesn't do any inspecting of its own.  The whole point is to get
 * fine-grained control of the semantic annotator without having to create all-new UI; so
 * that the user doesn't have to look in an odd place to turn on/off specific
 * annotations.
 *
 * Implementation Note:
 * {@link UnfairLocalInspectionTool} is a marker class that denotes that an inspection doesn't do its own work.
 */
public class HaxeAnnotatorInspection extends LocalInspectionTool implements UnfairLocalInspectionTool {

  @NonNls

  protected final HighlightDisplayKey inspectionToolKey;
  protected final String shortName;
  protected final String displayName;

  /** The description that will be displayed in the settings dialog.  If this is NULL, then
   *  {@code resources/inspectionDescriptions/<shortName>.html} is expected to exist and
   *  contain the description.
   */
  protected final String description;
  protected final boolean enabledByDefault;

  HaxeAnnotatorInspection(@NotNull @PropertyKey(resourceBundle = HaxeBundle.BUNDLE) String displayNameKey) {
    this(null, displayNameKey, null, true);
  }

  HaxeAnnotatorInspection(@NotNull @PropertyKey(resourceBundle = HaxeBundle.BUNDLE) String displayNameKey,
                          @Nullable @PropertyKey(resourceBundle = HaxeBundle.BUNDLE) String descriptionKey) {
    this(null, displayNameKey, descriptionKey, true);
  }

  /** This constructor expects {@code resources/inspectionDescriptions/<shortname>.html} to exist. */
  HaxeAnnotatorInspection(@NonNls @Nullable String shortName,
                          @NotNull @PropertyKey(resourceBundle = HaxeBundle.BUNDLE) String displayNameKey,
                          boolean enabledByDefault) {
    this(shortName, displayNameKey, null, enabledByDefault);
  }

  /**
   * Creates a new inspection that shows up in the Settings->Inspections panel.
   *
   * @param shortName - The (code) name of the inspection.  If null, will be taken from the name of the class.
   * @param displayNameKey - The (HaxeBundle) resource key for displayed name of the inspection.
   * @param descriptionKey - The (HaxeBundle) resource key for the long description of the inspection.
   *                         May be null.  If so, then an html description file named
   *                         {@code resources/inspectionDescriptions/<shortname>.html} is expected to exist.
   *                         If neither exists, an internationalized version of "description not found" will
   *                         be displayed in the description panel.
   * @param enabledByDefault - Whether the inspection is enabled by default.
   */
  HaxeAnnotatorInspection(@NonNls @Nullable String shortName,
                          @NotNull @PropertyKey(resourceBundle = HaxeBundle.BUNDLE) String displayNameKey,
                          @Nullable @PropertyKey(resourceBundle = HaxeBundle.BUNDLE) String descriptionKey,
                          boolean enabledByDefault) {
    super();
    this.enabledByDefault = enabledByDefault;
    this.shortName = shortName != null && !shortName.isEmpty() ? shortName : getClass().getSimpleName();
    this.displayName = HaxeBundle.message(displayNameKey);
    this.description = (null != descriptionKey) ? HaxeBundle.message(descriptionKey) : null;

    this.inspectionToolKey = HighlightDisplayKey.findOrRegister(this.shortName, displayName);
  }


  public boolean isEnabled(@Nullable PsiElement element) {
    if (null == element) return false;
    InspectionProfileManager mgr = InspectionProfileManager.getInstance(element.getProject());
    InspectionProfile profile = mgr.getCurrentProfile();
    return profile.isToolEnabled(inspectionToolKey, element);
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getGroupDisplayName() {
    return HaxeBundle.message(getGroupKey());
  }

  // @Override - Not until 2019.3
  @Nullable
  public String getGroupKey() {
    return "inspections.semantic.annotation.group.name";
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String[] getGroupPath() {
    return new String[]{ HaxeBundle.message("inspections.group.name"), getGroupDisplayName() };
  }

  @Override
  public boolean isEnabledByDefault() {
    return enabledByDefault;
  }

  @NotNull
  @Override
  public String getShortName() {
    return shortName;
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Nullable
  @Override
  public String getStaticDescription() {
    return description;
  }
}
