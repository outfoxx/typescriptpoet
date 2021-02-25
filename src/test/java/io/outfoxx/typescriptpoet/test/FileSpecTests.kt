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
      FileSpec.builder("test")
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
      FileSpec.builder("test")
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
  fun `Generates relative named imports for generated modules`() {
    val typeName = TypeName.namedImport("Observable", "!local/observable")

    val testFile =
      FileSpec.builder("api/test")
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
          import {Observable} from '../local/observable';
          
          
          type Test = Observable;
          
        """.trimIndent()
      )
    )
  }

  @Test
  fun `Generates star imports`() {
    val typeName = TypeName.standard(
      SymbolSpec.importsAll("stuff", "stuff/types")
    )

    val testFile =
      FileSpec.builder("test")
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
  fun `Generates star imports for generated modules`() {
    val typeName = TypeName.standard(
      SymbolSpec.importsAll("stuff", "!stuff/types")
    )

    val testFile =
      FileSpec.builder("api/test")
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
          import * as stuff from '../stuff/types';
          
          
          type Test = stuff;
          
        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates augment imports")
  fun testAugmentImports() {
    val typeName1 = TypeName.namedImport("Observable", "rxjs/observable")
    val typeName2 = TypeName.standard(
      SymbolSpec.augmented("flatMap", "rxjs/operators/flatMap", "Observable")
    )

    val testFile =
      FileSpec.builder("test")
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
    val typeName = TypeName.standard(
      SymbolSpec.sideEffect("describe", "mocha")
    )

    val testFile =
      FileSpec.builder("test")
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
  fun `Generates renamed imports on collision`() {
    val typeName1 = TypeName.namedImport("Test", "test1")
    val typeName2 = TypeName.namedImport("Test", "test2")
    val typeName3 = TypeName.namedImport("Another", "test1")

    val testFile =
      FileSpec.builder("test")
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
  fun `Generates renamed imports on collision for generated modules`() {
    val typeName1 = TypeName.namedImport("Test", "!1/test")
    val typeName2 = TypeName.namedImport("Test", "!2/test")
    val typeName3 = TypeName.namedImport("Another", "!1/test")

    val testFile =
      FileSpec.builder("api/client/test")
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
          import {Another as Another_, Test} from '../../1/test';
          import {Test as Test_} from '../../2/test';
          
          
          type LocalTest1 = Test;

          type LocalTest2 = Test_;
          
          type Another = Another_;
          
        """.trimIndent()
      )
    )
  }

  @Test
  fun `Generates renamed imports on collision for nested types`() {
    val typeName1 = TypeName.namedImport("Test", "test1")
    val typeName2 = TypeName.namedImport("Test.Kind", "test2")
    val typeName3 = TypeName.namedImport("Another.Kind", "test1")

    val testFile =
      FileSpec.builder("test")
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

          type LocalTest2 = Test_.Kind;
          
          type Another = Another_.Kind;
          
        """.trimIndent()
      )
    )
  }

  @Test
  fun `Generates renamed imports on collision of nested types for generated modules`() {
    val typeName1 = TypeName.namedImport("Test", "!client/1/test")
    val typeName2 = TypeName.namedImport("Test.Kind", "!client/2/test")
    val typeName3 = TypeName.namedImport("Another.Kind", "!client/1/test")

    val testFile =
      FileSpec.builder("client/api/test")
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
          import {Another as Another_, Test} from '../1/test';
          import {Test as Test_} from '../2/test';
          
          
          type LocalTest1 = Test;

          type LocalTest2 = Test_.Kind;
          
          type Another = Another_.Kind;
          
        """.trimIndent()
      )
    )
  }

  @Test
  fun `Generates relative references for nested modules`() {
    val nestedTypeName = TypeName.implicit("Test").nested("Nested")

    val testFile =
      FileSpec.builder("test")
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

  @Test
  fun `Generates relative references for generated nested modules`() {
    val nestedTypeName = TypeName.namedImport("Test", "!client/types/test").nested("Nested")

    val testFile =
      FileSpec.builder("client/api/test")
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
