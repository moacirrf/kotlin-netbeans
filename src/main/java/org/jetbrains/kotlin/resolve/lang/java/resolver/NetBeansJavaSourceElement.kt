/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.resolve.lang.java.resolver

import org.jetbrains.kotlin.resolve.lang.java.structure.NetBeansJavaElement
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.JavaElement

/*

  @author Alexander.Baratynski
  Created on Aug 26, 2016
*/

class NetBeansJavaSourceElement(override val javaElement: JavaElement) : JavaSourceElement {

    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
    
    fun getElementBinding() = (javaElement as NetBeansJavaElement<*>).elementHandle
    
}