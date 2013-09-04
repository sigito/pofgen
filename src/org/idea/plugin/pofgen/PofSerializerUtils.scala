package org.idea.plugin.pofgen

import com.intellij.psi._

/**
 * @author sigito
 */
class PofSerializerUtils {
  val WRITE_PREFIX: String = "write"
  val READ_PREFIX: String = "read"

  def addWriteMethod(collector: StringBuilder, instanceName: String, field: SerializableField, writer: String = "pofWriter"): Unit = {
    val fieldType = field.psiField.getType
    val methodName = WRITE_PREFIX + methodSuffix(fieldType)
    addWriteMethod(collector, methodName, instanceName, field, writer)
  }

  def addReadMethod(collector: StringBuilder, instance: String, field: SerializableField, reader: String = "pofReader"): Unit = {
    val fieldType = field.psiField.getType
    val methodName = READ_PREFIX + methodSuffix(fieldType)
    addReadMethod(collector, methodName, instance, field, reader)
  }

  private def methodSuffix(psiType: PsiType): String = {
    // todo implement smart method selection
    psiType match {
      case primitiveType: PsiPrimitiveType => primitiveType.getCanonicalText.capitalize
      case _ => "Object"
    }
  }

  private def addWriteMethod(collector: StringBuilder, methodName: String, instance: String, field: SerializableField, writer: String): Unit = {
    collector ++= writer ++= "." ++= methodName ++= "("
    collector ++= field.indexName ++= ", "
    collector ++= instance ++= "." ++= field.getter.getName ++= "()"
    collector ++= ");"
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
}

object PofSerializerUtils extends PofSerializerUtils
