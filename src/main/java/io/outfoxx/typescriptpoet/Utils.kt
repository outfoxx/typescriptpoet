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

import java.lang.Character.isISOControl
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashMap
import java.util.LinkedHashSet

internal object NullAppendable : Appendable {

  override fun append(charSequence: CharSequence) = this
  override fun append(charSequence: CharSequence, start: Int, end: Int) = this
  override fun append(c: Char) = this
}

internal fun String.parentSegment(separator: String = "."): String? {
  val parts = split(separator)
  val parent = parts.dropLast(1).joinToString(separator)
  return if (parent.isEmpty()) null else parent
}

internal fun String.topLevelSegment(separator: String = "."): String {
  return split(separator).first()
}

internal fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> = Collections.unmodifiableMap(LinkedHashMap(this))

internal fun <T> Collection<T>.toImmutableList(): List<T> = Collections.unmodifiableList(ArrayList(this))

internal fun <T> Collection<T>.toImmutableSet(): Set<T> = Collections.unmodifiableSet(LinkedHashSet(this))

internal fun <T> List<T>.dropCommon(other: List<T>): List<T> {
  if (size < other.size) return this
  var lastDiff = 0
  for (idx in other.indices) {
    if (get(idx) != other[idx]) {
      break
    }
    lastDiff = idx + 1
  }
  return subList(lastDiff, size)
}

internal fun requireExactlyOneOf(modifiers: Set<Modifier>, vararg mutuallyExclusive: Modifier) {
  val count = mutuallyExclusive.count(modifiers::contains)
  require(count == 1) {
    "modifiers $modifiers must contain one of ${mutuallyExclusive.contentToString()}"
  }
}

internal fun requireNoneOrOneOf(modifiers: Set<Modifier>, vararg mutuallyExclusive: Modifier) {
  val count = mutuallyExclusive.count(modifiers::contains)
  require(count <= 1) {
    "modifiers $modifiers must contain none or only one of ${mutuallyExclusive.contentToString()}"
  }
}

internal fun requireNoneOf(modifiers: Set<Modifier>, vararg forbidden: Modifier) {
  require(forbidden.none(modifiers::contains)) {
    "modifiers $modifiers must contain none of ${forbidden.contentToString()}"
  }
}

internal fun <T> T.isOneOf(t1: T, t2: T, t3: T? = null, t4: T? = null, t5: T? = null, t6: T? = null) =
  this == t1 || this == t2 || this == t3 || this == t4 || this == t5 || this == t6

// see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
internal fun characterLiteralWithoutDoubleQuotes(c: Char) = when {
  c == '\b' -> "\\b" // \u0008: backspace (BS)
  c == '\t' -> "\\t" // \u0009: horizontal tab (HT)
  c == '\n' -> "\\n" // \u000a: linefeed (LF)
  c == '\r' -> "\\r" // \u000d: carriage return (CR)
  c == '\"' -> "\\\"" // \u0022: double quote (")
  c == '\'' -> "'" // \u0027: single quote (')
  c == '\\' -> "\\\\" // \u005c: backslash (\)
  isISOControl(c) -> String.format("\\u%04x", c.code)
  else -> c.toString()
}

/** Returns the string literal representing `value`, including wrapping double quotes.  */
internal fun stringLiteralWithQuotes(value: String, multiLineIndent: String): String {
  return value.split("\n").joinToString(" +\n$multiLineIndent") { line ->
    val result = StringBuilder(line.length + 32)
    result.append('\'')
    for (c in line) {
      // Trivial case: single quote must be escaped.
      if (c == '\'') {
        result.append("\\\'")
        continue
      }
      // Trivial case: dollar sign must be escaped.
      if (c == '$') {
        result.append("\\$")
        continue
      }
      // Trivial case: double quotes must not be escaped.
      if (c == '\"') {
        result.append('"')
        continue
      }
      // Default case: just let character literal do its work.
      result.append(characterLiteralWithoutDoubleQuotes(c))
    }
    result.append('\'')

    result.toString()
  }
}

/** Returns the string template literal representing `value`, including wrapping backticks.  */
internal fun stringTemplateLiteralWithBackticks(value: String, multiLineIndent: String): String {
  return value.split("\n").joinToString("\n$multiLineIndent", prefix = "`", postfix = "`") { line ->
    val result = StringBuilder(line.length + 32)
    for (c in line) {
      // Trivial case: single quote must not be escaped.
      if (c == '\'') {
        result.append("'")
        continue
      }
      // Trivial case: dollar sign must not be escaped.
      if (c == '$') {
        result.append("$")
        continue
      }
      // Trivial case: double quotes must not be escaped.
      if (c == '\"') {
        result.append('"')
        continue
      }
      // Default case: just let character literal do its work.
      result.append(characterLiteralWithoutDoubleQuotes(c))
    }

    result.toString()
  }
}

internal val String.isKeyword get() = KEYWORDS.contains(this)

internal val String.isName get() = split("\\.").none { it.isKeyword }

// https://github.com/JetBrains/kotlin/blob/master/core/descriptors/src/org/jetbrains/kotlin/renderer/KeywordStringsGenerated.java
private val KEYWORDS = setOf(
  "package",
  "as",
  "typealias",
  "class",
  "this",
  "super",
  "val",
  "var",
  "fun",
  "for",
  "null",
  "true",
  "false",
  "is",
  "in",
  "throw",
  "return",
  "break",
  "continue",
  "object",
  "if",
  "try",
  "else",
  "while",
  "do",
  "when",
  "interface",
  "typeof"
)
