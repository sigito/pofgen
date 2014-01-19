package org.idea.plugin.pofgen.generation

import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi._
import com.intellij.psi.util.PsiTypesUtil
import scala.collection.convert.WrapAsScala._
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl


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
    val method = OverrideImplementUtil.overrideOrImplementMethod(serializer, serialize, false).head
    val body = method.getBody
    // cleanup body, can contain template lines
    body.deleteChildRange(body.getFirstBodyElement, body.getLastBodyElement)

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

    method
  }

  private def createDeserializeMethod(implicit serializer: PsiClass): PsiMethod = {
    val method = OverrideImplementUtil.overrideOrImplementMethod(serializer, deserialize, false).head
    val body = method.getBody
    // cleanup body, can contain template lines
    body.deleteChildRange(body.getFirstBodyElement, body.getLastBodyElement)

    val readerClass = findClass("com.tangosol.io.pof.PofReader")

    val instanceName = StringUtil.decapitalize(entityClass.name)

    entityClass.fields.foreach {
      field =>
        def cast(returnType: PsiType): String =
          if (field.psiField.getType.isAssignableFrom(returnType)) ""
          else s"(${field.typeName})"

        val (readMethod, returnType) = pofSerializerUtils.readMethodCall(readerClass, "pofReader", field)
        val readWithCast = elementFactory.createExpressionFromText(s"${cast(returnType)}$readMethod", body)
        val readStatement = elementFactory.createVariableDeclarationStatement(field.name, field.psiField.getType, readWithCast)
        body add readStatement
    }

    // declare deserialize object instance and initialize
    val newInstance = elementFactory.createExpressionFromText(constructorCall, body)
    val instance = elementFactory.createVariableDeclarationStatement(instanceName, PsiTypesUtil.getClassType(entityClass.clazz), newInstance)
    body add instance

    // set other fields with setters
    entityClass.restFields.map(entityClass.fields(_)).foreach {
      field =>
        field.setter match {
          case Some(setter) =>
            val setterCall = elementFactory.createStatementFromText(s"$instanceName.${setter.getName}(${field.name});", body)
            body add setterCall
          case None =>
            // no setter, writer error message
            // add comment on new line
            body add PsiParserFacade.SERVICE.getInstance(project).createWhiteSpaceFromText("\n") // I wonder if there is some other way to insert new line
            body add elementFactory.createCommentFromText(s"// ERROR: no setter for field '${field.name}'.", body)
        }
    }

    // read remainder
    body add elementFactory.createStatementFromText("pofReader.readRemainder();", body)
    // return value
    body add elementFactory.createStatementFromText(s"return $instanceName;", body)

    method
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

  private def constructorCall: String = {
    val code = StringBuilder.newBuilder
    code ++= "new " ++= entityClass.constructor.getName ++= "("
    entityClass.constructorFields.map(entityClass.fields(_).name).addString(code, ", ")
    code ++= ")"
    code.toString()
  }
}
