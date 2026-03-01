/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.editor;

import com.intellij.codeInsight.AbstractEnterActionTestCase;

import com.intellij.plugins.haxe.util.HaxeTestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

/**
 * @author winmain
 */
public class HaxeEnterActionTest extends AbstractEnterActionTestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        HaxeTestUtils.cleanupUnexpiredAppleUITimers(this::addSuppressedException);
        super.tearDown();
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return HaxeTestUtils.BASE_TEST_DATA_PATH;
    }

    @Override
    protected void doTest() throws Exception {
        doTest("hx");
    }

    @Test
    public void testEnterInAbstract() throws Throwable {
        doTextTest("hx",
                """
                        abstract Test {
                            var a;<caret>
                        }""",
                """
                        abstract Test {
                            var a;
                           \s
                        }""");
    }

    @Test
    public void testEnterInClass() throws Throwable {
        doTextTest("hx",
                """
                        class Test {
                            var a;<caret>
                        }""",
                """
                        class Test {
                            var a;
                           \s
                        }""");
    }

    @Test
    public void testEnterInEnum() throws Throwable {
        doTextTest("hx",
                """
                        enum Test {
                            FOO;<caret>
                        }""",
                """
                        enum Test {
                            FOO;
                           \s
                        }""");
    }

    @Test
    public void testEnterInExternClass() throws Throwable {
        doTextTest("hx",
                """
                        extern class Test {
                            var a;<caret>
                        }""",
                """
                        extern class Test {
                            var a;
                           \s
                        }""");
    }

    @Test
    public void testEnterInInterface() throws Throwable {
        doTextTest("hx",
                """
                        interface Test {
                            function qwe():Void;<caret>
                        }""",
                """
                        interface Test {
                            function qwe():Void;
                           \s
                        }""");
    }

    @Test
    public void testEnterAfterDocumentationStart() throws Throwable {
        doTextTest("hx",
                """
                        class Test {
                            /**<caret>
                            function foo():Void {}
                        }
                        """,
                """
                        class Test {
                            /**
                              \s
                            **/
                            function foo():Void {}
                        }
                        """);
    }

    @Test
    public void testEnterAfterDocumentationStartWhenClosed() throws Throwable {
        doTextTest("hx",
                """
                        class Test {
                            /**<caret>
                            **/
                            function foo():Void {}
                        }
                        """,
                // TODO formatting should probably indent this the same way as testEnterAfterDocumentationStart
                """
                        class Test {
                            /**
                           \s
                            **/
                            function foo():Void {}
                        }
                        """);
    }

    @Test
    public void testEnterAfterDocumentationStartOnLineWithContent() throws Throwable {
        doTextTest("hx",
                """
                        class Test {/**<caret>
                            function foo():Void {}
                        }
                        """,
                // TODO formatting
                """
                       class Test {/**
                         \s
                       **/
                           function foo():Void {}
                       }
                       """);
    }


}
