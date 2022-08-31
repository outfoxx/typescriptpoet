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
import io.outfoxx.typescriptpoet.CodeBlock
import io.outfoxx.typescriptpoet.CodeWriter
import io.outfoxx.typescriptpoet.DecoratorSpec
import io.outfoxx.typescriptpoet.FunctionSpec
import io.outfoxx.typescriptpoet.Modifier
import io.outfoxx.typescriptpoet.ParameterSpec
import io.outfoxx.typescriptpoet.PropertySpec
import io.outfoxx.typescriptpoet.SymbolSpec
import io.outfoxx.typescriptpoet.TypeName
import io.outfoxx.typescriptpoet.tag
import io.outfoxx.typescriptpoet.toImmutableSet
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItems
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.StringWriter

@DisplayName("ClassSpec Tests")
class ClassSpecTests {

  @Test
  @DisplayName("Tags on builders can be retrieved on builders and built specs")
  fun testTags() {
    val testClassBuilder = ClassSpec.builder("Test")
      .tag(5)
    val testClass = testClassBuilder.build()

    assertThat(testClassBuilder.tags[Integer::class] as? Int, equalTo(5))
    assertThat(testClass.tag(), equalTo(5))
  }

  @Test
  @DisplayName("Generates TSDoc at before class definition")
  fun testGenTSDoc() {
    val testClass = ClassSpec.builder("Test")
      .addTSDoc("this is a comment\n")
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            /**
             * this is a comment
             */
            class Test {
            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates decorators formatted")
  fun testGenDecorators() {
    val testClass = ClassSpec.builder("Test")
      .addDecorator(
        DecoratorSpec.builder("decorate")
          .addParameter(null, "true")
          .addParameter("targetType", "Test2")
          .build()
      )
      .build()
    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            @decorate(true, /* targetType */ Test2)
            class Test {
            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates modifiers in order")
  fun testGenModifiersInOrder() {
    val testClass = ClassSpec.builder("Test")
      .addModifiers(Modifier.ABSTRACT, Modifier.EXPORT)
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            export abstract class Test {
            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates type variables")
  fun testGenTypeVars() {
    val testClass = ClassSpec.builder("Test")
      .addTypeVariable(
        TypeName.typeVariable("X", TypeName.bound(TypeName.implicit("Test2")))
      )
      .addTypeVariable(
        TypeName.typeVariable("Y", TypeName.bound(TypeName.implicit("Test3")), TypeName.intersectBound(TypeName.implicit("Test4")))
      )
      .addTypeVariable(
        TypeName.typeVariable("Z", TypeName.bound(TypeName.implicit("Test5")), TypeName.unionBound(TypeName.implicit("Test6"), true))
      )
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            class Test<X extends Test2, Y extends Test3 & Test4, Z extends Test5 | keyof Test6> {
            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates super class")
  fun testGenSuperClass() {
    val testClass = ClassSpec.builder("Test")
      .superClass(TypeName.implicit("Test2"))
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            class Test extends Test2 {
            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates mixins")
  fun testGenMixins() {
    val testClass = ClassSpec.builder("Test")
      .addMixin(TypeName.implicit("Test2"))
      .addMixin(TypeName.implicit("Test3"))
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            class Test implements Test2, Test3 {
            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates super class & mixins properly formatted")
  fun testGenSuperClassAndMixinsFormatted() {
    val testClass = ClassSpec.builder("Test")
      .superClass(TypeName.implicit("Test2"))
      .addMixin(TypeName.implicit("Test3"))
      .addMixin(TypeName.implicit("Test4"))
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            class Test extends Test2 implements Test3, Test4 {
            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates type vars, super class & mixins properly formatted")
  fun testGenTypeVarsAndSuperClassAndMixinsFormatted() {
    val testClass = ClassSpec.builder("Test")
      .addTypeVariable(
        TypeName.typeVariable("Y", TypeName.bound(TypeName.implicit("Test3")), TypeName.intersectBound(TypeName.implicit("Test4")))
      )
      .superClass(TypeName.implicit("Test2"))
      .addMixin(TypeName.implicit("Test3"))
      .addMixin(TypeName.implicit("Test4"))
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            class Test<Y extends Test3 & Test4> extends Test2 implements Test3, Test4 {
            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates constructor")
  fun testGenConstructor() {
    val testClass = ClassSpec.builder("Test")
      .constructor(
        FunctionSpec.constructorBuilder()
          .addParameter("value", TypeName.NUMBER)
          .build()
      )
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            class Test {

              constructor(value: number) {
              }

            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates constructor with rest parameter")
  fun testGenConstructorRest() {
    val testClass = ClassSpec.builder("Test")
      .constructor(
        FunctionSpec.constructorBuilder()
          .addParameter("value", TypeName.NUMBER)
          .restParameter("all", TypeName.arrayType(TypeName.STRING))
          .build()
      )
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            class Test {

              constructor(value: number, ... all: Array<string>) {
              }

            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates constructor with shorthand properties")
  fun testGenConstructorShorthandProperties() {
    val testClass = ClassSpec.builder("Test")
      .addProperty(
        PropertySpec.builder("value", TypeName.NUMBER, false, Modifier.PRIVATE).initializer("value").build()
      )
      .addProperty(
        PropertySpec.builder("value2", TypeName.STRING, false, Modifier.PUBLIC).initializer("value2").build()
      )
      .addProperty("value3", TypeName.BOOLEAN, true, Modifier.PUBLIC)
      .constructor(
        FunctionSpec.constructorBuilder()
          .addParameter("value", TypeName.NUMBER)
          .addParameter("value2", TypeName.STRING)
          .addParameter("value3", TypeName.BOOLEAN, true)
          .addCode(
            CodeBlock.builder()
              .addStatement("val testing = 'need other code'")
              .addStatement("anotherTestStatement()")
              .addStatement("this.value3 = value3 || testing == ''")
              .build()
          )
          .build()
      )
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            class Test {

              value3: boolean | undefined;

              constructor(private value: number, public value2: string, value3: boolean | undefined) {
                val testing = 'need other code';
                anotherTestStatement();
                this.value3 = value3 || testing == '';
              }

            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates constructor with decorated constructor parameters")
  fun testGenConstructorDecoratedConstructorParameters() {
    val testClass = ClassSpec.builder("Test")
      .addProperty("value", TypeName.NUMBER, false, Modifier.PRIVATE)
      .addProperty("value2", TypeName.STRING, false, Modifier.PUBLIC)
      .addProperty("value3", TypeName.BOOLEAN, true, Modifier.PUBLIC)
      .constructor(
        FunctionSpec.constructorBuilder()
          .addParameter(
            ParameterSpec.builder("value", TypeName.NUMBER)
              .addDecorator(
                DecoratorSpec.builder("MyDec")
                  .addParameter("value", "%S", "test-value")
                  .build()
              )
              .build()
          )
          .addParameter("value2", TypeName.STRING)
          .addParameter(
            ParameterSpec.builder("value3", TypeName.BOOLEAN, true)
              .addDecorator(
                DecoratorSpec.builder("MyDec")
                  .addParameter(null, "%S", "test-value")
                  .build()
              )
              .build()
          )
          .addCode(
            CodeBlock.builder()
              .add("val testing = 'need other code'; this.value = value\n")
              .addStatement("anotherTestStatement()")
              .addStatement("this.value2 = value2")
              .addStatement("this.value3 = value3 || testing == ''")
              .build()
          )
          .build()
      )
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            class Test {

              private value: number;

              value2: string;

              value3: boolean | undefined;

              constructor(
                  @MyDec(/* value */ 'test-value') value: number,
                  value2: string,
                  @MyDec('test-value') value3: boolean | undefined
              ) {
                val testing = 'need other code'; this.value = value
                anotherTestStatement();
                this.value2 = value2;
                this.value3 = value3 || testing == '';
              }

            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates constructor with decorated shorthand properties")
  fun testGenConstructorDecoratedShorthandProperties() {
    val testClass = ClassSpec.builder("Test")
      .addProperty(
        PropertySpec.builder("value", TypeName.NUMBER, false, Modifier.PRIVATE)
          .addDecorator(
            DecoratorSpec.builder("MyDec")
              .addParameter("value", "%S", "test-value")
              .build()
          )
          .initializer("value")
          .build()
      )
      .addProperty(
        PropertySpec.builder("value2", TypeName.STRING, false, Modifier.PUBLIC)
          .initializer("value2")
          .build()
      )
      .addProperty("value3", TypeName.BOOLEAN, true, Modifier.PUBLIC)
      .constructor(
        FunctionSpec.constructorBuilder()
          .addParameter("value", TypeName.NUMBER)
          .addParameter("value2", TypeName.STRING)
          .addParameter("value3", TypeName.BOOLEAN, true)
          .addCode(
            CodeBlock.builder()
              .addStatement("val testing = 'need other code'")
              .addStatement("anotherTestStatement()")
              .addStatement("this.value3 = value3 || testing == ''")
              .build()
          )
          .build()
      )
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            class Test {

              value3: boolean | undefined;

              constructor(
                  @MyDec(/* value */ 'test-value')
                  private value: number,
                  public value2: string,
                  value3: boolean | undefined
              ) {
                val testing = 'need other code';
                anotherTestStatement();
                this.value3 = value3 || testing == '';
              }

            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates property declarations")
  fun testGenProperties() {
    val testClass = ClassSpec.builder("Test")
      .addProperty("value", TypeName.NUMBER, false, Modifier.PRIVATE)
      .addProperty("value2", TypeName.STRING, false, Modifier.PUBLIC)
      .addProperty(
        PropertySpec.builder("value3", TypeName.BOOLEAN, false, Modifier.PUBLIC)
          .initializer("true")
          .build()
      )
      .addProperty(
        PropertySpec.builder("value4", TypeName.NUMBER, false, Modifier.PUBLIC)
          .addDecorator(
            DecoratorSpec.builder("limited")
              .addParameter("min", "5")
              .addParameter("max", "100")
              .build()
          )
          .build()
      )
      .addProperty(
        PropertySpec.builder("value5", TypeName.NUMBER, false, Modifier.PUBLIC)
          .addDecorator(
            DecoratorSpec.builder("dynamic")
              .build()
          )
          .build()
      )
      .addProperty(
        PropertySpec.builder("value5", TypeName.NUMBER, false, Modifier.PUBLIC)
          .addDecorator(
            DecoratorSpec.builder("logged")
              .asFactory()
              .build()
          )
          .build()
      )
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            class Test {

              private value: number;

              value2: string;

              value3: boolean = true;

              @limited(/* min */ 5, /* max */ 100)
              value4: number;

              @dynamic
              value5: number;

              @logged()
              value5: number;

            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates property modifiers static precedes readonly")
  fun testGenPropertiesModifierStaticPrecedesReadonly() {
    val testClass = ClassSpec.builder("Test")
      .addProperty("value", TypeName.NUMBER, false, Modifier.PRIVATE, Modifier.STATIC, Modifier.READONLY)
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            class Test {

              private static readonly value: number;

            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("Generates method definitions")
  fun testGenMethods() {
    val testClass = ClassSpec.builder("Test")
      .addFunction(
        FunctionSpec.builder("test1")
          .addCode("")
          .build()
      )
      .addFunction(
        FunctionSpec.builder("test2")
          .addDecorator(
            DecoratorSpec.builder("validated")
              .addParameter("strict", "true")
              .addParameter("name", "test2")
              .build()
          )
          .addCode("")
          .build()
      )
      .build()

    val out = StringWriter()
    testClass.emit(CodeWriter(out))

    assertThat(
      out.toString(),
      equalTo(
        """
            class Test {

              test1() {
              }

              @validated(/* strict */ true, /* name */ test2)
              test2() {
              }

            }

        """.trimIndent()
      )
    )
  }

  @Test
  @DisplayName("toBuilder copies all fields")
  fun testToBuilder() {
    val testClassBlder = ClassSpec.builder("Test")
      .addTSDoc("this is a comment\n")
      .addDecorator(
        DecoratorSpec.builder("decorate")
          .addParameter(null, "true")
          .addParameter("targetType", "Test2")
          .build()
      )
      .addModifiers(Modifier.ABSTRACT, Modifier.EXPORT)
      .addTypeVariable(
        TypeName.typeVariable("X", TypeName.bound(TypeName.implicit("Test2")))
      )
      .superClass(TypeName.implicit("Test2"))
      .addMixin(TypeName.implicit("Test3"))
      .constructor(
        FunctionSpec.constructorBuilder()
          .addParameter("value", TypeName.NUMBER)
          .build()
      )
      .addProperty("value", TypeName.NUMBER, false, Modifier.PRIVATE)
      .addProperty("value2", TypeName.STRING, false, Modifier.PUBLIC)
      .addFunction(
        FunctionSpec.builder("test1")
          .addCode("")
          .build()
      )
      .build()
      .toBuilder()

    assertThat(testClassBlder.tsDoc.formatParts, hasItems("this is a comment\n"))
    assertThat(testClassBlder.decorators.size, equalTo(1))
    assertThat(testClassBlder.decorators[0].name, equalTo(SymbolSpec.from("decorate")))
    assertThat(testClassBlder.decorators[0].parameters.size, equalTo(2))
    assertThat(testClassBlder.modifiers.toImmutableSet(), equalTo(setOf(Modifier.ABSTRACT, Modifier.EXPORT)))
    assertThat(testClassBlder.typeVariables.size, equalTo(1))
    assertThat(
      testClassBlder.superClass,
      equalTo<TypeName>(
        TypeName.implicit("Test2")
      )
    )
    assertThat(
      testClassBlder.mixins,
      hasItems(
        TypeName.implicit("Test3")
      )
    )
    assertThat(testClassBlder.propertySpecs.map { it.name }, hasItems("value", "value2"))
    assertThat(testClassBlder.constructor, notNullValue())
    assertThat(testClassBlder.functionSpecs.map { it.name }, hasItems("test1"))
  }
}
