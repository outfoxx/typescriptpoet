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

import io.outfoxx.typescriptpoet.CodeBlock.Companion.joinToCode

/** A generated `class` declaration. */
class ClassSpec
private constructor(
  builder: Builder
) : TypeSpec<ClassSpec, ClassSpec.Builder>(builder) {

  override val name = builder.name
  val tsDoc = builder.tsDoc.build()
  val decorators = builder.decorators.toImmutableList()
  val modifiers = builder.modifiers.toImmutableSet()
  val typeVariables = builder.typeVariables.toImmutableList()
  val superClass = builder.superClass
  val mixins = builder.mixins.toImmutableList()
  val propertySpecs = builder.propertySpecs.toImmutableList()
  val constructor = builder.constructor
  val functionSpecs = builder.functionSpecs.toImmutableList()
  val useConstructorPropertiesAutomatically = builder.useConstructorPropertiesAutomatically

  override fun emit(codeWriter: CodeWriter) {

    val constructorProperties: Map<String, PropertySpec> =
      if (useConstructorPropertiesAutomatically)
        constructorProperties()
      else
        emptyMap()

    codeWriter.emitTSDoc(tsDoc)
    codeWriter.emitDecorators(decorators, false)
    codeWriter.emitModifiers(modifiers, setOf(Modifier.PUBLIC))
    codeWriter.emit("class")
    codeWriter.emitCode(CodeBlock.of(" %L", name))
    codeWriter.emitTypeVariables(typeVariables)

    val superClass = if (superClass != null) CodeBlock.of("extends %T", superClass) else CodeBlock.empty()
    val mixins = mixins.map { CodeBlock.of("%T", it) }.let {
      if (it.isNotEmpty()) it.joinToCode(prefix = "implements ") else CodeBlock.empty()
    }

    val parents = (listOf(superClass) + mixins).filter { it.isNotEmpty() }
    if (parents.any { it.isNotEmpty() }) {
      codeWriter.emitCode(parents.joinToCode(separator = " ", prefix = " "))
    }

    codeWriter.emit(" {\n")
    codeWriter.indent()

    // Non-static properties.
    for (propertySpec in propertySpecs) {
      if (constructorProperties.containsKey(propertySpec.name)) {
        continue
      }
      codeWriter.emit("\n")
      propertySpec.emit(
        codeWriter, setOf(Modifier.PUBLIC), asStatement = true,
        compactOptionalAllowed = !useConstructorPropertiesAutomatically,
      )
    }

    // Write the constructor manually, allowing the replacement
    // of property specs with constructor parameters
    constructor?.let {
      codeWriter.emit("\n")

      if (it.decorators.isNotEmpty()) {
        codeWriter.emit(" ")
        codeWriter.emitDecorators(it.decorators, false)
        codeWriter.emit("\n")
      }

      if (it.modifiers.isNotEmpty()) {
        codeWriter.emitModifiers(it.modifiers)
      }

      codeWriter.emit("constructor")

      val body = constructor.body

      // Emit constructor parameters & property specs that can be replaced with parameters
      it.parameters.emit(
        codeWriter, rest = it.restParameter,
        constructorProperties = constructorProperties
      ) { param, isRest, optionalAllowed ->

        var property = constructorProperties[param.name]
        if (property != null && !isRest) {

          // Ensure the parameter always has a modifier (that makes it a property in TS)
          if (
            property.modifiers.none { mod ->
              mod.isOneOf(
                Modifier.PUBLIC,
                Modifier.PRIVATE,
                Modifier.PROTECTED,
                Modifier.READONLY
              )
            }
          ) {
            // Add default public modifier
            property = property.toBuilder().addModifiers(Modifier.PUBLIC).build()
          }
          property.emit(codeWriter, setOf(), compactOptionalAllowed = false, withInitializer = false)
          param.emitDefaultValue(codeWriter)
        } else {
          param.emit(
            codeWriter,
            isRest = isRest,
            optionalAllowed = optionalAllowed && !useConstructorPropertiesAutomatically,
          )
        }
      }

      codeWriter.emit(" {\n")
      codeWriter.indent()
      codeWriter.emitCode(body)
      codeWriter.unindent()
      codeWriter.emit("}\n")
    }

    // Constructors.
    for (funSpec in functionSpecs) {
      if (!funSpec.isConstructor) continue
      codeWriter.emit("\n")
      funSpec.emit(codeWriter, name, setOf(Modifier.PUBLIC))
    }

    // Functions (static and non-static).
    for (funSpec in functionSpecs) {
      if (funSpec.isConstructor) continue
      codeWriter.emit("\n")
      funSpec.emit(codeWriter, name, setOf(Modifier.PUBLIC))
    }

    codeWriter.unindent()

    if (!hasNoBody) {
      codeWriter.emit("\n")
    }
    codeWriter.emit("}\n")
  }

  /** Returns the properties that can be declared inline as constructor parameters. */
  private fun constructorProperties(): Map<String, PropertySpec> =
    propertySpecs.filter { it.name == it.initializer?.toString() }.map { it.name to it }.toMap()

  private val hasNoBody: Boolean
    get() {
      if (propertySpecs.isNotEmpty()) {
        val constructorProperties = constructorProperties()
        propertySpecs
          .filterNot { constructorProperties.containsKey(it.name) }
          .forEach { _ -> return false }
      }
      return constructor == null && functionSpecs.isEmpty()
    }

  fun toBuilder(): Builder {
    val builder = Builder(name)
    builder.tsDoc.add(tsDoc)
    builder.decorators += decorators
    builder.modifiers += modifiers
    builder.typeVariables += typeVariables
    builder.superClass = superClass
    builder.mixins += mixins
    builder.propertySpecs += propertySpecs
    builder.constructor = constructor
    builder.functionSpecs += functionSpecs
    return builder
  }

  class Builder(
    name: String
  ) : TypeSpec.Builder<ClassSpec, Builder>(name) {

    internal val tsDoc = CodeBlock.builder()
    internal val decorators = mutableListOf<DecoratorSpec>()
    internal val modifiers = mutableListOf<Modifier>()
    internal val typeVariables = mutableListOf<TypeName.TypeVariable>()
    internal var superClass: TypeName? = null
    internal val mixins = mutableListOf<TypeName>()
    internal val propertySpecs = mutableListOf<PropertySpec>()
    internal var constructor: FunctionSpec? = null
    internal val functionSpecs = mutableListOf<FunctionSpec>()
    internal var useConstructorPropertiesAutomatically = true

    fun addTSDoc(format: String, vararg args: Any) = apply {
      tsDoc.add(format, *args)
    }

    fun addTSDoc(block: CodeBlock) = apply {
      tsDoc.add(block)
    }

    fun addDecorators(decoratorSpecs: Iterable<DecoratorSpec>) = apply {
      decorators += decoratorSpecs
    }

    fun addDecorator(decoratorSpec: DecoratorSpec) = apply {
      decorators += decoratorSpec
    }

    fun addModifiers(vararg modifiers: Modifier) = apply {
      this.modifiers += modifiers
    }

    fun addTypeVariables(typeVariables: Iterable<TypeName.TypeVariable>) = apply {
      this.typeVariables += typeVariables
    }

    fun addTypeVariable(typeVariable: TypeName.TypeVariable) = apply {
      typeVariables += typeVariable
    }

    fun superClass(superClass: TypeName) = apply {
      check(this.superClass == null) { "superclass already set to ${this.superClass}" }
      this.superClass = superClass
    }

    fun addMixins(mixins: Iterable<TypeName>) = apply {
      this.mixins += mixins
    }

    fun addMixin(mixin: TypeName) = apply {
      mixins += mixin
    }

    fun constructor(constructor: FunctionSpec?) = apply {
      if (constructor != null) {
        require(constructor.isConstructor) {
          "expected a constructor but was ${constructor.name}; use FunctionSpec.constructorBuilder when building"
        }
      }
      this.constructor = constructor
    }

    fun addProperties(propertySpecs: Iterable<PropertySpec>) = apply {
      this.propertySpecs += propertySpecs
    }

    fun addProperty(propertySpec: PropertySpec) = apply {
      propertySpecs += propertySpec
    }

    fun addProperty(name: String, type: TypeName, optional: Boolean = false, vararg modifiers: Modifier) =
      addProperty(PropertySpec.builder(name, type, optional, *modifiers).build())

    fun addFunctions(functionSpecs: Iterable<FunctionSpec>) = apply {
      functionSpecs.forEach { addFunction(it) }
    }

    fun addFunction(functionSpec: FunctionSpec) = apply {
      require(!functionSpec.isConstructor) { "Use the 'constructor' method for the constructor" }
      this.functionSpecs += functionSpec
    }

    fun allowUsingConstructorPropertiesAutomatically(value: Boolean = true) = apply {
      this.useConstructorPropertiesAutomatically = value
    }

    override fun build(): ClassSpec {
      val isAbstract = modifiers.contains(Modifier.ABSTRACT)
      for (functionSpec in functionSpecs) {
        require(isAbstract || !functionSpec.modifiers.contains(Modifier.ABSTRACT)) {
          "non-abstract type $name cannot declare abstract function ${functionSpec.name}"
        }
      }

      return ClassSpec(this)
    }
  }

  companion object {

    @JvmStatic
    fun builder(name: String) = Builder(name)

    @JvmStatic
    fun builder(name: TypeName) = Builder("$name")
  }
}
