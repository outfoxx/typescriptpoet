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

import java.nio.file.Path

object FileModules {

  fun importPath(directory: Path, importer: String, import: String): String {
    return if (import.startsWith("!")) {
      // Ensure two generated files use proper relative import path
      val importerPath = directory.resolve(importer).toAbsolutePath().normalize()
      val importerDir = importerPath.parent ?: importerPath
      val importPath = directory.resolve(import.drop(1)).toAbsolutePath().normalize()
      val importedPath = importerDir.relativize(importPath).normalize().toString().replace('\\', '/')
      if (importedPath.startsWith("."))
        importedPath
      else
        "./$importedPath"
    } else {
      import
    }
  }
}
