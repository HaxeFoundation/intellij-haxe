/*
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
package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HaxeAnnotation /* extends Annotation -- can't, it's final :( */ {

  ArrayList<IntentionAction> intentions = new ArrayList<>();

  HighlightSeverity severity;
  TextRange range;
  String message;
  String tooltip;

  public HaxeAnnotation(@NotNull HighlightSeverity severity,
                        @NotNull TextRange range,
                        @Nullable String message,
                        @Nullable String tooltip) {
    this.severity = severity;
    this.range = range;
    this.message = message;
    this.tooltip = tooltip;
  }

  public HighlightSeverity getSeverity() {
    return severity;
  }

  public TextRange getRange() {
    return range;
  }

  public String getMessage() {
    return message;
  }

  public String getTooltip() {
    return tooltip;
  }

  public List<IntentionAction> getFixes() {
    return intentions;
  }

  public HaxeAnnotation withFix(HaxeFixer action) {
    intentions.add(action);
    return this;
  }

  public HaxeAnnotation withFix(IntentionAction action) {
    intentions.add(action);
    return this;
  }

  public HaxeAnnotation withFixes(List<? extends HaxeFixer> actions) {
    intentions.addAll(actions);
    return this;
  }

  public Annotation toAnnotation() {
    String tooltip = this.tooltip;
    if (null == tooltip && null != message && !message.isEmpty()) {
      tooltip = XmlStringUtil.wrapInHtml(XmlStringUtil.escapeString(message));
    }
    Annotation anno = new Annotation(range.getStartOffset(), range.getEndOffset(), severity, message, tooltip);

    for (IntentionAction action : intentions) {
      anno.registerFix(action);
    }
    return anno;
  }

}
