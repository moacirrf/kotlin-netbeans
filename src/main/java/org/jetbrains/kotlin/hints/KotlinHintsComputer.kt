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
package org.jetbrains.kotlin.hints

import org.jetbrains.kotlin.diagnostics.netbeans.parser.KotlinParserResult
import org.jetbrains.kotlin.hints.intentions.*
import org.jetbrains.kotlin.psi.*
import org.netbeans.modules.csl.api.Hint

class KotlinHintsComputer(val parserResult: KotlinParserResult) : KtVisitor<Unit, Any?>() {

    val hints = arrayListOf<Hint>()
    
    override fun visitKtFile(ktFile: KtFile, data: Any?) {
        ktFile.acceptChildren(this)
    }
    
    override fun visitKtElement(element: KtElement, data: Any?) {
        hints.addAll(element.inspections())
        
        element.acceptChildren(this)
    }
    
    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: Any?) {
        getSmartCastHover(expression, parserResult)?.let { hints.add(it) }
    }
    
    private fun KtElement.inspections() = listOf(
            RemoveEmptyPrimaryConstructorInspection(parserResult, this),
            RemoveEmptyClassBodyInspection(parserResult, this),
            ConvertToStringTemplateInspection(parserResult, this),
            ConvertTryFinallyToUseCallInspection(parserResult, this),
            RemoveEmptySecondaryConstructorInspection(parserResult, this),
            ReplaceSizeCheckWithIsNotEmptyInspection(parserResult, this)
    )
            .filter(Inspection::isApplicable)
            .map { it.hint(parserResult.snapshot.source.fileObject) }
        
}