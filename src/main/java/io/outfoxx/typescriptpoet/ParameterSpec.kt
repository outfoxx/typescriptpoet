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

package io.outfoxx.typescriptpoet

import kotlin.math.min

class ParameterSpec private constructor(
  builder: Builder
) : Taggable(builder.tags.toImmutableMap()) {

  val name = builder.name
  val optional = builder.optional
  val decorators = builder.decorators.toImmutableList()
  val modifiers = builder.modifiers.toImmutableSet()
  val type = builder.type
  val defaultValue = builder.defaultValue

  internal fun emit(
    codeWriter: CodeWriter,
    includeType: Boolean = true,
    isRest: Boolean = false,
    optionalAllowed: Boolean = false,
    scope: List<String>
  ) {
    codeWriter.emitDecorators(decorators, true, scope)
    codeWriter.emitModifiers(modifiers)
    if (isRest) {
      codeWriter.emitCode("... ")
    }
    codeWriter.emitCode(CodeBlock.of("%L", name), scope)
    if (includeType) {
      if (optional && optionalAllowed) {
        codeWriter.emitCode("?")
      }
      codeWriter.emitCode(CodeBlock.of(": %T", type), scope)
      if (optional && !optionalAllowed) {
        codeWriter.emitCode(" | undefined")
      }
    }
    emitDefaultValue(codeWriter, scope)
  }

  internal fun emitDefaultValue(codeWriter: CodeWriter, scope: List<String>) {
    if (defaultValue != null) {
      codeWriter.emitCode(CodeBlock.of(" = %[%L%]", defaultValue), scope)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildCodeString { emit(this, scope = emptyList()) }

  fun toBuilder(name: String = this.name, type: TypeName = this.type): Builder {
    val builder = Builder(name, type, optional)
    builder.decorators += decorators
    builder.modifiers += modifiers
    builder.defaultValue = defaultValue
    return builder
  }

  class Builder internal constructor(
    internal val name: String,
    internal val type: TypeName,
    val optional: Boolean = false
  ) : Taggable.Builder<Builder>() {

    internal val decorators = mutableListOf<DecoratorSpec>()
    internal val modifiers = mutableListOf<Modifier>()
    internal var defaultValue: CodeBlock? = null

    fun addDecorators(decoratorSpecs: Iterable<DecoratorSpec>) = apply {
      decorators += decoratorSpecs
    }

    fun addDecorator(decoratorSpec: DecoratorSpec) = apply {
      decorators += decoratorSpec
    }

    fun addDecorator(decorator: SymbolSpec) = apply {
      decorators += DecoratorSpec.builder(decorator).build()
    }

    fun addModifiers(vararg modifiers: Modifier) = apply {
      this.modifiers += modifiers
    }

    fun addModifiers(modifiers: Iterable<Modifier>) = apply {
      this.modifiers += modifiers
    }

    fun defaultValue(format: String, vararg args: Any?) = defaultValue(
      CodeBlock.of(format, *args)
    )

    fun defaultValue(codeBlock: CodeBlock) = apply {
      check(this.defaultValue == null) { "initializer was already set" }
      this.defaultValue = codeBlock
    }

    fun build() = ParameterSpec(this)
  }

  companion object {

    @JvmStatic
    fun builder(name: String, type: TypeName, optional: Boolean = false, vararg modifiers: Modifier): Builder {
      require(name.isName) { "not a valid name: $name" }
      return Builder(name, type, optional).addModifiers(*modifiers)
    }
  }
}

internal fun List<ParameterSpec>.emit(
  codeWriter: CodeWriter,
  enclosed: Boolean = true,
  rest: ParameterSpec? = null,
  constructorProperties: Map<String, PropertySpec> = emptyMap(),
  scope: List<String>,
  emitBlock: (ParameterSpec, Boolean, Boolean, List<String>) -> Unit =
    { param, isRest, optionalAllowed, pscope ->
      param.emit(codeWriter, optionalAllowed = optionalAllowed, isRest = isRest, scope = pscope)
    }
) = with(codeWriter) {
  val params = this@emit + if (rest != null) listOf(rest) else emptyList()
  if (enclosed) emit("(")
  if (size < 5 && all { constructorProperties[it.name]?.decorators?.isEmpty() ?: it.decorators.isEmpty() }) {
    params.forEachIndexed { index, parameter ->
      val optionalAllowed = subList(min(index + 1, size), size).all { it.optional }
      if (index > 0) emit(", ")
      emitBlock(parameter, rest === parameter, optionalAllowed, scope)
    }
  } else {
    emit("\n")
    indent(2)
    params.forEachIndexed { index, parameter ->
      val optionalAllowed = subList(min(index + 1, size), size).all { it.optional }
      if (index > 0) emit(",\n")
      emitBlock(parameter, rest === parameter, optionalAllowed, scope)
    }
    unindent(2)
    emit("\n")
  }
  if (enclosed) emit(")")
}
