/*
 * Copyright 2017 Eric Bishton
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
package com.intellij.plugins.haxe.compilation;

import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.util.HaxeCommonCompilerUtil;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Runs the compiler and handles its output.
 *
 * Created by ebishton on 6/14/17.
 */
public class HaxeCompilerProcessHandler extends ColoredProcessHandler {

  static final String STDERR_PREFIX = "(stderr) ";

  final HaxeCommonCompilerUtil.CompilationContext context;

  public HaxeCompilerProcessHandler(@NotNull HaxeCommonCompilerUtil.CompilationContext context, @NotNull Process process, /*@NotNull*/ String commandLine, @NotNull Charset charset) {
    super(process, commandLine, charset);
    this.context = context;
  }

  @Override
  protected void onOSProcessTerminated(int exitCode) {
    if (exitCode != 0) {
      List<String> errors = new ArrayList<String>();

      if (processHasSeparateErrorStream()) {
        InputStream stderr = this.getProcess().getErrorStream();


        InputStreamReader reader = new InputStreamReader(stderr);
        Scanner scanner = new Scanner(reader);

        boolean hadOutput = false;
        while (scanner.hasNextLine()) {
          String nextLine = scanner.nextLine();
          errors.add(STDERR_PREFIX + nextLine);
          hadOutput = true;
        }

        if (hadOutput) {
          errors.add( STDERR_PREFIX + " ==== End of stderr output. ==== ");
        } else {
          errors.add("No additional error information available.");
        }
      }
      errors.add("Command exited with an error code : " + exitCode);

      context.handleOutput(errors.toArray(new String[0]));
    }
    super.onOSProcessTerminated(exitCode);
  }

  @Override
  public void coloredTextAvailable(@NotNull String text, @NotNull Key attributes) {
    super.coloredTextAvailable(text, attributes);
    if(text.trim().length()>0) {// avoid empty lines
      context.handleOutput(new String[]{text});
    }
    // TODO mlo: add support for compiler new output format
  }
}
