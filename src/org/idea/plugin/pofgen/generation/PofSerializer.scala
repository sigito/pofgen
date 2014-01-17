package org.idea.plugin.pofgen.generation

import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi._
import com.intellij.psi.util.PsiTypesUtil
import scala.collection.convert.WrapAsScala._


/**
 * @author sigito
 */
class PofSerializer(context: GenerationContext,
                    entityClass: EntityClass) {
  import context._

  private val SERIALIZER_CLASS_NAME: String = "com.tangosol.io.pof.PofSerializer"

  private val serializerInterface: PsiClass = findClass(SERIALIZER_CLASS_NAME)

  private val pofSerializerUtils = PofSerializerUtils()

  // serialize and deserialize methods
  private val (serialize: PsiMethod, deserialize: PsiMethod) = {
    val methods = serializerInterface.getMethods
    assert(methods.length == 2)

    if (methods(0).getName == "serialize") (methods(0), methods(1))
    else (methods(1), methods(0))
  }

  def createClass(): PsiClass = {
    // create new class for serializer
    implicit val serializer = elementFactory.createClass(s"${entityClass.name}PofSerializer")
    serializer.getModifierList.setModifierProperty(PsiModifier.PUBLIC, true)

    // implement PofSerializer
    val serializerInterfaceRef = elementFactory.createClassReferenceElement(serializerInterface)
    serializer.getImplementsList.add(serializerInterfaceRef)

    // add constant field to serializer class
    entityClass.fields foreach (f => serializer.add(createIndex(f.indexName, f.index)))
    serializer.add(createSerializeMethod)
    serializer.add(createDeserializeMethod)
    serializer
  }

  private def createSerializeMethod(implicit serializer: PsiClass): PsiMethod = {
    // implement method
    val serializeMethod = OverrideImplementUtil.overrideOrImplementMethod(serializer, serialize, false).head
    val body = serializeMethod.getBody

    val writerClass = findClass("com.tangosol.io.pof.PofWriter")

    // declare serialize object instance and cast
    val instanceClassName = entityClass.fullName
    val instanceName = StringUtil.decapitalize(entityClass.name)
    val castInit = elementFactory.createExpressionFromText(s"($instanceClassName) o", body)
    val instanceVar = elementFactory.createVariableDeclarationStatement(instanceName, PsiTypesUtil.getClassType(entityClass.clazz), castInit)
    body add instanceVar

    // write every field
    for {
      field <- entityClass.fields
      methodCall = pofSerializerUtils.writeMethodCall(writerClass, "pofWriter", instanceName, field)
      statement = elementFactory.createStatementFromText(methodCall + ';', body)
    } body add statement

    // write remainder
    body add elementFactory.createStatementFromText("pofWriter.writeRemainder(null);", body)

    serializeMethod
  }

  private def createDeserializeMethod(implicit serializer: PsiClass): PsiMethod = {
    val readerClass = findClass("com.tangosol.io.pof.PofReader")

    val instanceClassName = entityClass.fullName
    val instanceName = StringUtil.decapitalize(entityClass.name)

    val code = new StringBuilder("public ") ++= instanceClassName ++= " deserialize(com.tangosol.io.pof.PofReader pofReader) throws java.io.IOException {"

    entityClass.fields.foreach {
      field =>
        val (readMethod, returnType) = pofSerializerUtils.readMethodCall(readerClass, "pofReader", field)

        code ++= field.typeName ++= " " ++= field.name ++= " = "

        if (!field.psiField.getType.isAssignableFrom(returnType))
        // add cast
          code ++= "(" ++= field.typeName ++= ") "

        code ++= readMethod ++= ";"
    }

    // declare deserialize object instance and initialize
    code ++= instanceClassName ++= " " ++= instanceName ++= " = "
    code ++= "new " ++= entityClass.constructor.getName ++= "("
    entityClass.constructorFields.map(entityClass.fields(_).name).addString(code, ", ")
    code ++= ")"
    code ++= ";"

    // set other fields with setters
    entityClass.restFields.foreach {
      fieldIndex =>
        val field = entityClass.fields(fieldIndex)
        field.setter match {
          case Some(setter) => code ++= instanceName ++= "." ++= setter.getName ++= "(" ++= field.name ++= ");"
          case None => code ++= "// ERROR: no setter for field '" ++= field.name ++= "'\n"
        }
    }

    // read remainder
    code ++= "pofReader.readRemainder();"

    code ++= "return " ++= instanceName ++= ";"
    code ++= "}"
    elementFactory.createMethodFromText(code.toString(), serializer)
  }

  private def createIndex(indexName: String, index: Int)(implicit serializer: PsiClass): PsiField = {
    // create static field
    val indexConstant = elementFactory.createField(indexName, PsiType.INT)
    indexConstant.getModifierList.setModifierProperty(PsiModifier.PRIVATE, true)
    indexConstant.getModifierList.setModifierProperty(PsiModifier.STATIC, true)
    indexConstant.getModifierList.setModifierProperty(PsiModifier.FINAL, true)
    indexConstant.setInitializer(elementFactory.createExpressionFromText(index.toString, serializer))
    indexConstant
  }
}
