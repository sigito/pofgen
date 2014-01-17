package org.idea.plugin.pofgen.generation

import com.intellij.psi._

/**
 * @author sigito
 */
class PofSerializerUtils {
  private var cachedReadMethods: Map[PsiType, PsiMethod] = Map.empty
  private var cachedWriteMethods: Map[PsiType, PsiMethod] = Map.empty

  def writeMethodCall(writerClass: PsiClass, writer: String, instance: String, field: EntityField): String = {
    val writeMethod = selectWriteMethod(writerClass, field.psiField.getType)
    s"$writer.${writeMethod.getName}(${field.indexName}, $instance.${field.getter.getName}())"
  }

  def readMethodCall(readerClass: PsiClass, reader: String, field: EntityField): (String, PsiType) = {
    val readMethod = selectReadMethod(readerClass, field.psiField.getType)
    (s"$reader.${readMethod.getName}(${field.indexName})", readMethod.getReturnType)
  }

  def selectWriteMethod(writerClass: PsiClass, fieldType: PsiType): PsiMethod = {
    cachedWriteMethods.get(fieldType) match {
      case Some(selectedMethod) => selectedMethod
      case None =>
        // default write method
        val writeObjectMethod = writerClass.findMethodsByName("writeObject", true)(0)

        def findMethod(field: PsiType): Option[PsiMethod] = {
          writerClass.getMethods filter (_.getName.startsWith("write")) find {
            method =>
              val parameters = method.getParameterList.getParameters
              if (parameters.length < 2) false
              else {
                // get second, first one is for index
                val valueParameter: PsiParameter = parameters(1)
                val valueParameterType = valueParameter.getType
                valueParameterType == field
              }
          }
        }

        val selectedMethod = findMethod(fieldType) match {
          case Some(writeMethod) => writeMethod
          case None =>
            val superTypes = fieldType.getSuperTypes
            (writeObjectMethod /: superTypes) {
              (selectedMethod, fieldType) =>
                findMethod(fieldType).getOrElse(selectedMethod)
            }
        }

        cachedWriteMethods += (fieldType -> selectedMethod)
        selectedMethod
    }
  }

  def selectReadMethod(readerClass: PsiClass, fieldType: PsiType): PsiMethod = {
    cachedReadMethods.get(fieldType) match {
      case Some(readMethod) => readMethod
      case None =>
        val readObjectMethod = readerClass.findMethodsByName("readObject", true)(0)

        def findMethod(field: PsiType): Option[PsiMethod] =
          readerClass.getMethods filter (_.getName.startsWith("read")) find (_.getReturnType == field)

        val selectedMethod = findMethod(fieldType) match {
          case Some(readMethod) => readMethod
          case None =>
            val superTypes = fieldType.getSuperTypes
            (readObjectMethod /: superTypes) {
              (selectedMethod, fieldType) =>
                findMethod(fieldType).getOrElse(selectedMethod)
            }
        }

        cachedReadMethods += (fieldType -> selectedMethod)
        selectedMethod
    }
  }
}

object PofSerializerUtils {
  def apply(): PofSerializerUtils = new PofSerializerUtils
}
