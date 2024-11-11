package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.model.HaxeGenericParamModel;

public interface HaxeTypeParameterDeclaration extends HaxeClass {

  String getName();

  String getQualifiedName();

  HaxeNamedComponent getOwner();

  HaxeGenericParamModel getModel();

}
