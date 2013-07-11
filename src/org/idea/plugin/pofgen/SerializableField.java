package org.idea.plugin.pofgen;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PropertyUtil;

/**
 * @author sigito
 */
public class SerializableField {
    private final PsiField psiField;
    private final PsiMethod getter;
    private final PsiMethod setter;

    private final String indexName;
    private final int index;

    public SerializableField(PsiField psiField, int index) {
        this.psiField = psiField;
        this.getter = PropertyUtil.findGetterForField(psiField);
        this.setter = PropertyUtil.findSetterForField(psiField);

        this.indexName = psiField.getName().toUpperCase();
        this.index = index;
    }

    public PsiField getPsiField() {
        return psiField;
    }

    public PsiMethod getGetter() {
        return getter;
    }

    public PsiMethod getSetter() {
        return setter;
    }

    public String getIndexName() {
        return indexName;
    }

    public int getIndex() {
        return index;
    }
}
