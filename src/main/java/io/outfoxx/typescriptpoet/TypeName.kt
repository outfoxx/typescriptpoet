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

import io.outfoxx.typescriptpoet.TypeName.TypeVariable.Bound
import io.outfoxx.typescriptpoet.TypeName.TypeVariable.Bound.Combiner
import io.outfoxx.typescriptpoet.TypeName.TypeVariable.Bound.Combiner.UNION

/**
 * Name of any possible type that can be referenced
 *
 */
sealed class TypeName {

  internal abstract fun emit(codeWriter: CodeWriter)

  data class Standard
  internal constructor(
    val base: SymbolSpec,
  ) : TypeName() {

    fun nested(name: String) = Standard(base.nested(name))

    fun enclosingTypeName() = base.enclosing()?.let { Standard(it) }

    fun topLevelTypeName() = Standard(base.topLevel())

    fun simpleName() = base.value.split(".").last()

    fun simpleNames(): List<String> {
      val names = base.value.split(".")
      return names.subList(1, names.size)
    }

    val isTopLevelTypeName: Boolean get() = base.isTopLevelSymbol

    fun parameterized(vararg typeArgs: TypeName) = parameterizedType(this, *typeArgs)

    override fun emit(codeWriter: CodeWriter) {
      val fullPath = base.value.split(".")
      val relativePath = fullPath.dropCommon(codeWriter.currentScope())
      val relativeName =
        if (relativePath.isNotEmpty()) {
          relativePath.joinToString(".")
        } else {
          fullPath.last()
        }

      if (relativeName == base.value) {
        codeWriter.emitSymbol(base)
      } else {
        codeWriter.emitSymbol(SymbolSpec.implicit(relativeName))
      }
    }

    override fun toString() = buildCodeString { emit(this) }
  }

  data class Parameterized
  internal constructor(
    val rawType: Standard,
    val typeArgs: List<TypeName>
  ) : TypeName() {

    override fun emit(codeWriter: CodeWriter) {
      rawType.emit(codeWriter)
      codeWriter.emit("<")
      typeArgs.forEachIndexed { idx, typeArg ->
        typeArg.emit(codeWriter)

        if (idx < typeArgs.size - 1) {
          codeWriter.emit(", ")
        }
      }
      codeWriter.emit(">")
    }
  }

  data class TypeVariable
  internal constructor(
    val name: String,
    val bounds: List<Bound>
  ) : TypeName() {

    data class Bound(
      val type: TypeName,
      val combiner: Combiner = UNION,
      val modifier: Modifier?
    ) {

      enum class Combiner(
        val symbol: String
      ) {

        UNION("|"),
        INTERSECT("&")
      }

      enum class Modifier(
        val keyword: String
      ) {

        KEY_OF("keyof")
      }
    }

    override fun emit(codeWriter: CodeWriter) {
      codeWriter.emit(name)
    }

    override fun toString() = buildCodeString { emit(this) }
  }

  data class Anonymous
  internal constructor(
    val members: List<Member>
  ) : TypeName() {

    data class Member(
      val name: String,
      val type: TypeName,
      val optional: Boolean
    )

    override fun emit(codeWriter: CodeWriter) {
      codeWriter.emit("{ ")
      members.forEachIndexed { idx, member ->
        codeWriter.emit(member.name)
        if (member.optional) {
          codeWriter.emit("?")
        }
        codeWriter.emit(": ")
        member.type.emit(codeWriter)

        if (idx != members.size - 1) {
          codeWriter.emit(", ")
        }
      }
      codeWriter.emit(" }")
    }

    override fun toString() = buildCodeString { emit(this) }
  }

  data class Tuple
  internal constructor(
    val memberTypes: List<TypeName>
  ) : TypeName() {

    override fun emit(codeWriter: CodeWriter) {
      codeWriter.emit("[")
      memberTypes.forEachIndexed { idx, memberType ->
        memberType.emit(codeWriter)

        if (idx != memberTypes.size - 1) {
          codeWriter.emit(", ")
        }
      }
      codeWriter.emit("]")
    }

    override fun toString() = buildCodeString { emit(this) }
  }

  data class Intersection
  internal constructor(
    val typeRequirements: List<TypeName>
  ) : TypeName() {

    override fun emit(codeWriter: CodeWriter) {
      typeRequirements.forEachIndexed { idx, typeRequirement ->
        typeRequirement.emit(codeWriter)

        if (idx != typeRequirements.size - 1) {
          codeWriter.emit(" & ")
        }
      }
    }

    override fun toString() = buildCodeString { emit(this) }
  }

  data class Union
  internal constructor(
    val typeChoices: List<TypeName>
  ) : TypeName() {

    override fun emit(codeWriter: CodeWriter) {
      typeChoices.forEachIndexed { idx, typeChoice ->
        typeChoice.emit(codeWriter)

        if (idx != typeChoices.size - 1) {
          codeWriter.emit(" | ")
        }
      }
    }

    override fun toString() = buildCodeString { emit(this) }
  }

  data class Lambda
  internal constructor(
    private val parameters: Map<String, TypeName> = emptyMap(),
    private val returnType: TypeName = VOID
  ) : TypeName() {

    override fun emit(codeWriter: CodeWriter) {
      codeWriter.emit("(")
      parameters.entries.forEachIndexed { idx, entry ->
        val (name, type) = entry

        codeWriter.emit(name)
        codeWriter.emit(": ")
        type.emit(codeWriter)

        if (idx != parameters.size - 1) {
          codeWriter.emit(", ")
        }
      }
      codeWriter.emit(") => ")
      returnType.emit(codeWriter)
    }

    override fun toString() = buildCodeString { emit(this) }
  }

  companion object {

    val NULL = implicit("null")
    val UNDEFINED = implicit("undefined")
    val NEVER = implicit("never")
    val VOID = implicit("void")
    val ANY = implicit("any")

    val BOOLEAN = implicit("boolean")
    val NUMBER = implicit("number")
    val BIGINT = implicit("bigint")
    val STRING = implicit("string")
    val OBJECT = implicit("object")
    val SYMBOL = implicit("symbol")

    val BOOLEAN_CLASS = implicit("Boolean")
    val NUMBER_CLASS = implicit("Number")
    val BIGINT_CLASS = implicit("BigInt")
    val STRING_CLASS = implicit("String")
    val OBJECT_CLASS = implicit("Object")
    val SYMBOL_CLASS = implicit("Symbol")

    val FUNCTION = implicit("Function")
    val ERROR = implicit("Error")
    val REGEXP = implicit("RegExp")
    val MATH = implicit("Math")
    val DATE = implicit("Date")

    val SET = implicit("Set")
    val WEAK_SET = implicit("WeakSet")
    val MAP = implicit("Map")
    val WEAK_MAP = implicit("WeakMap")

    val ARRAY = implicit("Array")
    val INT8_ARRAY = implicit("Int8Array")
    val UINT8_ARRAY = implicit("Uint8Array")
    val UINT8_CLAMPED_ARRAY = implicit("Uint8ClampedArray")
    val INT16_ARRAY = implicit("Int16Array")
    val UINT16_ARRAY = implicit("Uint16Array")
    val INT32_ARRAY = implicit("Int32Array")
    val UINT32_ARRAY = implicit("Uint32Array")
    val FLOAT32_ARRAY = implicit("Float32Array")
    val FLOAT64_ARRAY = implicit("Float64Array")
    val BIG_INT64_ARRAY = implicit("BigInt64Array")
    val BIG_UINT64_ARRAY = implicit("BigUint64Array")

    val ARRAY_BUFFER = implicit("ArrayBuffer")
    val SHARED_ARRAY_BUFFER = implicit("SharedArrayBuffer")
    val ATOMICS = implicit("Atomics")
    val DATA_VIEW = implicit("DataView")
    val JSON = implicit("JSON")

    val PROMISE = implicit("Promise")
    val GENERATOR = implicit("Generator")

    /**
     * Any class/enum/primitive/etc type name
     *
     * @param exportedName The name of the symbol exported from module `from`
     * @param from The module the that `exportedName` is exported from
     */
    @JvmStatic
    fun namedImport(exportedName: String, from: String): Standard {
      return Standard(SymbolSpec.importsName(exportedName, from))
    }

    /**
     * Any class/enum/primitive/etc type name
     *
     * @param name Name for the implicit type, will be symbolized
     */
    @JvmStatic
    fun implicit(name: String): Standard {
      return Standard(SymbolSpec.implicit(name))
    }

    /**
     * Any class/enum/primitive/etc type name
     *
     * @param symbolSpec Serialized symbol spec
     */
    @JvmStatic
    fun standard(symbolSpec: String): Standard {
      return Standard(SymbolSpec.from(symbolSpec))
    }

    /**
     * Any class/enum/primitive/etc type name
     *
     * @param symbolSpec Symbol spec
     */
    @JvmStatic
    fun standard(symbolSpec: SymbolSpec): Standard {
      return Standard(symbolSpec)
    }

    /**
     * Type name for the generic Array type
     *
     * @param elementType Element type of the array
     * @return Type name of the new array type
     */
    @JvmStatic
    fun arrayType(elementType: TypeName): TypeName {
      return parameterizedType(
        ARRAY, elementType
      )
    }

    /**
     * Type name for the generic Set type
     *
     * @param elementType Element type of the set
     * @return Type name of the new set type
     */
    @JvmStatic
    fun setType(elementType: TypeName): TypeName {
      return parameterizedType(
        SET, elementType
      )
    }

    /**
     * Type name for the generic Map type
     *
     * @param keyType Key type of the map
     * @param valueType Value type of the map
     * @return Type name of the new map type
     */
    @JvmStatic
    fun mapType(keyType: TypeName, valueType: TypeName): TypeName {
      return parameterizedType(
        MAP, keyType, valueType
      )
    }

    /**
     * Parameterized type that represents a concrete
     * usage of a generic type
     *
     * @param rawType Generic type to invoke with arguments
     * @param typeArgs Names of the provided type arguments
     * @return Type name of the new parameterized type
     */
    @JvmStatic
    fun parameterizedType(rawType: Standard, vararg typeArgs: TypeName): Parameterized {
      return Parameterized(rawType, typeArgs.toList())
    }

    /**
     * Type variable represents a single variable type in a
     * generic type or function.
     *
     * @param name The name of the variable as it will be used in the definition
     * @param bounds Bound constraints that will be required during instantiation
     * @return Type name of the new type variable
     */
    @JvmStatic
    fun typeVariable(name: String, vararg bounds: Bound): TypeVariable {
      return TypeVariable(name, bounds.toList())
    }

    /**
     * Factory for type variable bounds
     */
    @JvmStatic
    fun bound(
      type: TypeName,
      combiner: Combiner = UNION,
      modifier: Bound.Modifier? = null
    ): Bound {
      return Bound(type, combiner, modifier)
    }

    /**
     * Factory for type variable bounds
     */
    @JvmStatic
    fun unionBound(type: TypeName, keyOf: Boolean = false): Bound {
      return bound(type, UNION, if (keyOf) Bound.Modifier.KEY_OF else null)
    }

    /**
     * Factory for type variable bounds
     */
    @JvmStatic
    fun intersectBound(type: TypeName, keyOf: Boolean = false): Bound {
      return bound(type, Combiner.INTERSECT, if (keyOf) Bound.Modifier.KEY_OF else null)
    }

    /**
     * Anonymous type name (e.g. `{ length: number, name: string }`)
     *
     * @param members Member pairs to define the anonymous type
     * @return Type name representing the anonymous type
     */
    @JvmStatic
    fun anonymousType(members: List<Anonymous.Member>): Anonymous {
      return Anonymous(members)
    }

    /**
     * Anonymous type name (e.g. `{ length?: number, name: string }`)
     *
     * @param members Member pairs to define the anonymous type (all properties are required)
     * @return Type name representing the anonymous type
     */
    @JvmStatic
    fun anonymousType(vararg members: Pair<String, TypeName>): Anonymous {
      return anonymousType(members.map { Anonymous.Member(it.first, it.second, false) })
    }

    /**
     * Tuple type name (e.g. `[number, boolean, string]`}
     *
     * @param memberTypes Each argument represents a distinct member type
     * @return Type name representing the tuple type
     */
    @JvmStatic
    fun tupleType(vararg memberTypes: TypeName): Tuple {
      return Tuple(memberTypes.toList())
    }

    /**
     * Intersection type name (e.g. `Person & Serializable & Loggable`)
     *
     * @param typeRequirements Requirements of the intersection as individual type names
     * @return Type name representing the intersection type
     */
    @JvmStatic
    fun intersectionType(vararg typeRequirements: TypeName): Intersection {
      return Intersection(typeRequirements.toList())
    }

    /**
     * Union type name (e.g. `int | number | any`)
     *
     * @param typeChoices All possible choices allowed in the union
     * @return Type name representing the union type
     */
    @JvmStatic
    fun unionType(vararg typeChoices: TypeName): Union {
      return Union(typeChoices.toList())
    }

    /** Returns a lambda type with `returnType` and parameters of listed in `parameters`. */
    @JvmStatic
    fun lambda(parameters: Map<String, TypeName> = emptyMap(), returnType: TypeName) =
      Lambda(parameters, returnType)

    /** Returns a lambda type with `returnType` and parameters of listed in `parameters`. */
    @JvmStatic
    fun lambda(vararg parameters: Pair<String, TypeName> = emptyArray(), returnType: TypeName) =
      Lambda(parameters.toMap(), returnType)
  }
}
