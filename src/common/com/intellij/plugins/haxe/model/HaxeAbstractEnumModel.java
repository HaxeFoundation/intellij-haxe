/*
 * Copyright 2018 Ilya Malanin
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
package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxeAbstractBody;
import com.intellij.plugins.haxe.lang.psi.HaxeAbstractClassDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.util.HaxeAbstractEnumUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HaxeAbstractEnumModel extends HaxeAbstractClassModel implements HaxeEnumModel {
  public HaxeAbstractEnumModel(@NotNull HaxeAbstractClassDeclaration haxeClass) {
    super(haxeClass);
  }

  @Override
  public HaxeEnumValueModel getValue(@NotNull String name) {
    HaxeFieldDeclaration value = getValueDeclarationsStream()
      .filter(declaration -> name.equals(declaration.getName()))
      .filter(HaxeAbstractEnumUtil::couldBeAbstractEnumField)
      .findFirst().orElse(null);

    return value != null ? (HaxeEnumValueModel)value.getModel() : null;
  }

  @Override
  public List<HaxeEnumValueModel> getValues() {
    return getValuesStream().collect(Collectors.toList());
  }

  @Override
  public Stream<HaxeEnumValueModel> getValuesStream() {
    return getValueDeclarationsStream()
      .filter(HaxeAbstractEnumUtil::couldBeAbstractEnumField)
      .map(model -> (HaxeEnumValueModel)model.getModel());
  }

  private Stream<HaxeFieldDeclaration> getValueDeclarationsStream() {
    final HaxeAbstractBody body = getAbstractClassBody();

    return body != null ? body.getFieldDeclarationList().stream() : Stream.empty();
  }
}
