package org.idea.plugin.pofgen;

import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author sigito
 */
public class GenerateDialog extends DialogWrapper {

    private final CollectionListModel<PsiField> serializeFields;
    private final LabeledComponent<JPanel> component;
    private final ToolbarDecorator decorator;

    protected GenerateDialog(PsiClass psiClass) {
        super(psiClass.getProject());
        setTitle("Select Fields to Use For Serialization");

        serializeFields = new CollectionListModel<PsiField>(psiClass.getAllFields());
        JList fieldsList = new JBList(serializeFields);
        fieldsList.setCellRenderer(new DefaultPsiElementCellRenderer());
        decorator = ToolbarDecorator.createDecorator(fieldsList);
        decorator.disableAddAction();
        JPanel panel = decorator.createPanel();
        component = LabeledComponent.create(panel, "Fields to be used for serailization:");

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return component;
    }

    public List<PsiField> getSerializeFields() {
        return serializeFields.getItems();
    }
}
