package org.idea.plugin.pofgen

import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.openapi.ui.{LabeledComponent, DialogWrapper}
import com.intellij.psi.{PsiModifier, PsiField, PsiClass}
import com.intellij.ui.components.JBList
import com.intellij.ui.{CollectionListModel, ToolbarDecorator}
import javax.swing.{ListCellRenderer, JList, JComponent, JPanel}
import scala.collection.convert.WrapAsScala._

/**
 * @author sigito
 */
class GenerateDialog protected[pofgen](psiClass: PsiClass) extends DialogWrapper(psiClass.getProject) {
  setTitle("Select Fields to Use For Serialization")

  private val fieldsListModel: CollectionListModel[PsiField] = {
    // filter static and transient fields
    val entityFields = psiClass.getAllFields filterNot {
      field =>
        val modifiers = field.getModifierList
        // exclude static fields
        modifiers.hasModifierProperty(PsiModifier.STATIC) ||
        // exclude transient fields
        modifiers.hasModifierProperty(PsiModifier.TRANSIENT)
    }
    new CollectionListModel[PsiField](entityFields: _*)
  }

  private val component: JComponent = {
    val fieldsList: JList[_] = new JBList(fieldsListModel).asInstanceOf[JList[_]]
    fieldsList.setCellRenderer(new DefaultPsiElementCellRenderer().asInstanceOf[ListCellRenderer[_ >: Any]])

    val decorator = ToolbarDecorator.createDecorator(fieldsList)
    decorator.disableAddAction

    val panel: JPanel = decorator.createPanel
    LabeledComponent.create(panel, "Fields to be used for serialization:")
  }

  init()

  override def createCenterPanel(): JComponent = component

  /**
   * Returns sequence of selected fields
   * @return
   */
  def selectedFields: IndexedSeq[PsiField] = fieldsListModel.getItems.toIndexedSeq
}
