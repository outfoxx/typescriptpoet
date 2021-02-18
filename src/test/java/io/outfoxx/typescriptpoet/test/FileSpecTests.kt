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

import io.outfoxx.typescriptpoet.ClassSpec
import io.outfoxx.typescriptpoet.FileSpec
import io.outfoxx.typescriptpoet.ModuleSpec
import io.outfoxx.typescriptpoet.SymbolSpec
import io.outfoxx.typescriptpoet.TypeAliasSpec
import io.outfoxx.typescriptpoet.TypeName
import io.outfoxx.typescriptpoet.tag
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("FileSpec Tests")
class FileSpecTests {

  @Test
  @DisplayName("Tags on builders can be retrieved on builders and built specs")
  fun testTags() {
    val testBuilder = FileSpec.builder("Test")
      .tag(5)
    val testSpec = testBuilder.build()

    assertThat(testBuilder.tags[Integer::class] as? Int, equalTo(5))
    assertThat(testSpec.tag(), equalTo(5))
  }

  @Test
  @DisplayName("Generates header file comment")
  fun testComment() {
    val testFile =
      FileSpec.builder("test.ts")
        .addComment(
          """
            A file header comment that
            spans multiple lines.
          """.trimIndent()
        )
        .build()

    assertThat(
      testFile.toString(),
      equalTo(
        """
          // A file header comment that
          // spans multiple lines.

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates named imports")
  fun testImportedTypes() {
    val typeName = TypeName.namedImport("Observable", "rxjs/observable")

    val testFile =
      FileSpec.builder("test.ts")
        .addTypeAlias(
          TypeAliasSpec.builder("Test", typeName)
            .build()
        )
        .build()

    val out = StringBuilder()
    testFile.writeTo(out)

    assertThat(
      out.toString(),
      equalTo(
        """
          import {Observable} from 'rxjs/observable';
          
          
          type Test = Observable;
          
        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates star imports")
  fun testStarImports() {
    val typeName = TypeName.symbolized(
      SymbolSpec.importsAll("stuff", "stuff/types")
    )

    val testFile =
      FileSpec.builder("test.ts")
        .addTypeAlias(
          TypeAliasSpec.builder("Test", typeName)
            .build()
        )
        .build()

    val out = StringBuilder()
    testFile.writeTo(out)

    assertThat(
      out.toString(),
      equalTo(
        """
          import * as stuff from 'stuff/types';
          
          
          type Test = stuff;
          
        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates augment imports")
  fun testAugmentImports() {
    val typeName1 = TypeName.namedImport("Observable", "rxjs/observable")
    val typeName2 = TypeName.symbolized(
      SymbolSpec.augmented("flatMap", "rxjs/operators/flatMap", "Observable")
    )

    val testFile =
      FileSpec.builder("test.ts")
        .addTypeAlias(
          TypeAliasSpec.builder("Test1", typeName1)
            .build()
        )
        .addTypeAlias(
          TypeAliasSpec.builder("Test2", typeName2)
            .build()
        )
        .build()

    val out = StringBuilder()
    testFile.writeTo(out)

    assertThat(
      out.toString(),
      equalTo(
        """
          import {Observable} from 'rxjs/observable';
          import 'rxjs/operators/flatMap';
          
          
          type Test1 = Observable;
          
          type Test2 = flatMap;
          
        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates side effect imports")
  fun testSideEffectImports() {
    val typeName = TypeName.symbolized(
      SymbolSpec.sideEffect("describe", "mocha")
    )

    val testFile =
      FileSpec.builder("test.ts")
        .addTypeAlias(
          TypeAliasSpec.builder("Test", typeName)
            .build()
        )
        .build()

    val out = StringBuilder()
    testFile.writeTo(out)

    assertThat(
      out.toString(),
      equalTo(
        """
          import 'mocha';
          
          
          type Test = describe;
          
        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates renamed imports on collision")
  fun testCollisionRenames() {
    val typeName1 = TypeName.namedImport("Test", "test1")
    val typeName2 = TypeName.namedImport("Test", "test2")
    val typeName3 = TypeName.namedImport("Another", "test1")

    val testFile =
      FileSpec.builder("test.ts")
        .addTypeAlias(
          TypeAliasSpec.builder("LocalTest1", typeName1)
            .build()
        )
        .addTypeAlias(
          TypeAliasSpec.builder("LocalTest2", typeName2)
            .build()
        )
        .addTypeAlias(
          TypeAliasSpec.builder("Another", typeName3)
            .build()
        )
        .build()

    val out = StringBuilder()
    testFile.writeTo(out)

    assertThat(
      out.toString(),
      equalTo(
        """
          import {Another as Another_, Test} from 'test1';
          import {Test as Test_} from 'test2';
          
          
          type LocalTest1 = Test;

          type LocalTest2 = Test_;
          
          type Another = Another_;
          
        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates relative references for nested modules")
  fun testRelativeModuleReferences() {
    val nestedTypeName = TypeName.implicit("Test").nested("Nested")

    val testFile =
      FileSpec.builder("test.ts")
        .addClass(
          ClassSpec.builder("Test")
            .build()
        )
        .addModule(
          ModuleSpec.builder("Test")
            .addClass(
              ClassSpec.builder("Nested")
                .build()
            )
            .addClass(
              ClassSpec.builder("SubNested")
                .superClass(nestedTypeName)
                .build()
            )
            .build()
        )
        .build()

    val out = StringBuilder()
    testFile.writeTo(out)

    assertThat(
      out.toString(),
      equalTo(
        """
          
          class Test {
          }
          
          namespace Test {
          
            class Nested {
            }
          
            class SubNested extends Nested {
            }
          
          }
          
        """.trimIndent()
      )
    )
  }
}
