/*
 * Copyright 2017 Outfox, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.outfoxx.typescriptpoet.test

import io.outfoxx.typescriptpoet.FunctionSpec
import io.outfoxx.typescriptpoet.TypeName.Companion.STRING
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CodeWriter Tests")
class CodeWriterTests {

  @Test
  fun `test long line wrapping`() {
    val testFunc = FunctionSpec.builder("test")
      .returns(STRING)
      .addStatement("return X(aaaaa,%Wbbbbb,%Wccccc,%Wddddd,%Weeeee,%Wfffff,%Wggggg,%Whhhhh,%Wiiiii,%Wjjjjj,%Wkkkkk,%Wlllll,%Wmmmmm,%Wnnnnn,%Wooooo,%Wppppp,%Wqqqqq)")
      .build()

    MatcherAssert.assertThat(
      testFunc.toString(),
      CoreMatchers.equalTo(
        """
            function test(): string {
              return X(aaaaa, bbbbb, ccccc, ddddd, eeeee, fffff, ggggg, hhhhh, iiiii, jjjjj, kkkkk, lllll,
                  mmmmm, nnnnn, ooooo, ppppp, qqqqq);
            }

        """.trimIndent()
      )
    )
  }
}
