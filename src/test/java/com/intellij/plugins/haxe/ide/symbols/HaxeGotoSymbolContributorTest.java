/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
package com.intellij.plugins.haxe.ide.symbols;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The three contributors plugin.xml registers as gotoSymbolContributor, driven over one fixture. */
@DisplayName("Navigation: goto symbol contributors")
public class HaxeGotoSymbolContributorTest extends HaxeLightFixtureTestCase {

  private static final String BASIC_SYMBOLS_HX_NAME = "BasicSymbols.hx";
  /** One expected symbol name per line. */
  private static final String BASIC_SYMBOLS_TXT_NAME = "BasicSymbols.txt";

  private static final List<ChooseByNameContributor> CONTRIBUTORS = List.of(
    new HaxeGotoClassSymbolContributor(), new HaxeGotoFieldSymbolContributor(), new HaxeGotoMethodSymbolContributor());

  @Override
  protected String getBasePath() {
    return "/indexers/symbols/";
  }

  @Test
  @DisplayName("basic symbols")
  public void testBasicSymbols() throws IOException {
    myFixture.configureByFile(BASIC_SYMBOLS_HX_NAME);
    List<String> expected = expectedSymbols();

    Set<String> offered = new LinkedHashSet<>();
    for (ChooseByNameContributor contributor : CONTRIBUTORS) {
      Collections.addAll(offered, contributor.getNames(getProject(), false));
    }

    assertTrue(offered.containsAll(expected), "offered " + offered + ", expected " + expected);
  }

  /** One symbol of each kind: class, field, method. */
  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"Bar1", "c", "foo2"})
  @DisplayName("items by name")
  public void testItemsByName(String name) {
    myFixture.configureByFile(BASIC_SYMBOLS_HX_NAME);

    List<NavigationItem> items = new ArrayList<>();
    for (ChooseByNameContributor contributor : CONTRIBUTORS) {
      Collections.addAll(items, contributor.getItemsByName(name, name, getProject(), false));
    }

    assertFalse(items.isEmpty(), "no contributor navigates to " + name);
  }

  private List<String> expectedSymbols() throws IOException {
    Path listing = Path.of(getTestDataPath(), BASIC_SYMBOLS_TXT_NAME);
    return Files.readAllLines(listing).stream()
      .map(String::trim)
      .filter(line -> !line.isEmpty())
      .toList();
  }
}
