package org.idea.plugin.pofgen.generation

import com.intellij.psi._

/**
 * @author sigito
 */
class PofSerializerUtils {
  val READ_PREFIX: String = "read"

  def addWriteMethod(collector: StringBuilder, writerClass: PsiClass, writer: String, instance: String, field: SerializableField): Unit = {
    val writeMethod = selectWriteMethod(writerClass, field.psiField.getType)

    collector ++= writer ++= "." ++= writeMethod.getName ++= "("
    collector ++= field.indexName ++= ", "
    collector ++= instance ++= "." ++= field.getter.getName ++= "()"
    collector ++= ");"
  }

  def addReadMethod(collector: StringBuilder, readerClass: PsiClass, reader: String, instance: String, field: SerializableField): Unit = {
    val readMethod = selectReadMethod(readerClass, field.psiField.getType)

    collector ++= instance ++= "." ++= field.setter.getName ++= "("

    if (!field.psiField.getType.isAssignableFrom(readMethod.getReturnType))
    // add cast
      collector ++= "(" ++= field.typeName ++= ") "

    // read and set field
    collector ++= reader ++= "." ++= readMethod.getName ++= "("
    collector ++= field.indexName ++= ")"
    collector ++= ");"
  }

  def selectWriteMethod(writerClass: PsiClass, fieldType: PsiType): PsiMethod = {
    // default write method
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
            valueParameterType == field
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

  def selectReadMethod(readerClass: PsiClass, fieldType: PsiType): PsiMethod = {
    val readObjectMethod = readerClass.findMethodsByName("readObject", true).head

    def findMethod(field: PsiType): Option[PsiMethod] =
      readerClass.getMethods filter (_.getName.startsWith("read")) find (_.getReturnType == field)

    findMethod(fieldType) match {
      case Some(selectedMethod) => selectedMethod
      case None =>
        val superTypes = fieldType.getSuperTypes
        (readObjectMethod /: superTypes) {
          (selectedMethod, fieldType) =>
            findMethod(fieldType).getOrElse(selectedMethod)
        }
    }
  }
}

object PofSerializerUtils extends PofSerializerUtils
