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

abstract class TypeSpec<T : TypeSpec<T, B>, B : TypeSpec.Builder<T, B>>
protected constructor(
  builder: Builder<T, B>
) : Taggable(builder.tags.toImmutableMap()) {

  abstract val name: String

  internal abstract fun emit(codeWriter: CodeWriter)

  override fun toString() = buildCodeString { emit(this) }

  abstract class Builder<T : TypeSpec<T, B>, B : Builder<T, B>>(
    internal val name: String
  ) : Taggable.Builder<B>() {

    abstract fun build(): T
  }
}
