package org.idea.plugin.pofgen

import com.intellij.psi._
import com.intellij.psi.util.{PsiTreeUtil, PsiClassUtil, PsiTypesUtil}
import com.intellij.openapi.module.{ModuleUtilCore, ModuleUtil, Module}
import com.intellij.psi.search.GlobalSearchScope

/**
 * @author sigito
 */
class PofSerializerUtils {
  val READ_PREFIX: String = "read"

  def addWriteMethod(collector: StringBuilder, writerClass: PsiClass, writer: String, instanceName: String, field: SerializableField): Unit = {
    val writeMethod = selectWriteMethod(writerClass, field.psiField.getType)

    collector ++= writer ++= "." ++= writeMethod.getName ++= "("
    collector ++= field.indexName ++= ", "
    collector ++= instanceName ++= "." ++= field.getter.getName ++= "());"
  }

  def addReadMethod(collector: StringBuilder, instance: String, field: SerializableField, reader: String = "pofReader"): Unit = {
    val fieldType = field.psiField.getType
    val methodName = READ_PREFIX + methodSuffix(fieldType)
    addReadMethod(collector, methodName, instance, field, reader)
  }

  private def methodSuffix(psiType: PsiType): String = {
    psiType match {
      case primitiveType: PsiPrimitiveType => primitiveType.getCanonicalText.capitalize
      case _ => {
        "Object"
      }
    }
  }

  private def addReadMethod(collector: StringBuilder, methodName: String, instance: String, field: SerializableField, reader: String): Unit = {
    collector ++= instance ++= "." ++= field.setter.getName ++= "("
    // cast
    collector ++= "(" ++= field.typeName ++= ") "
    // read field
    collector ++= reader ++= "." ++= methodName ++= "("
    collector ++= field.indexName ++= ")"
    collector ++= ");"
  }

  private def selectWriteMethod(writerClass: PsiClass, fieldType: PsiType): PsiMethod = {
    // default one
    val writeObjectMethod = writerClass.findMethodsByName("writeObject", true).head

    def findMethod(field: PsiType): Option[PsiMethod] = {
      writerClass.getMethods filter (_.getName.startsWith("write")) find {
        method =>
          val parameters = method.getParameterList.getParameters
          if (parameters.length < 2) false
          else {
            // get second, first one is for index
            val valueParameter: PsiParameter = parameters.tail.head
            val valueParameterType = valueParameter.getType
            valueParameterType == fieldType
          }
      }
    }

    findMethod(fieldType) match {
      case Some(selectedMethod) => selectedMethod
      case None =>
        val superTypes = fieldType.getSuperTypes
        (writeObjectMethod /: superTypes) {
          (selectedMethod, fieldType) =>
            findMethod(fieldType).getOrElse(selectedMethod)
        }
    }
  }
}

  object PofSerializerUtils extends PofSerializerUtils
