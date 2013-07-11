package org.idea.plugin.pofgen;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sigito
 */
public class GeneratePofAction extends AnAction {
    public static final String POF_SERIALIZER = "com.tangosol.io.pof.PofSerializer";
    public static final String POF_SERIALIZER_SUFFIX = "PofSerializer";

    public static final String SERIALIZE_METHOD = "serialize";
    public static final String POF_WRITER = "com.tangosol.io.pof.PofWriter pofWriter";

    public static final String DESERIALIZE_METHOD = "deserialize";
    public static final String POF_READER = "com.tangosol.io.pof.PofReader";

    public void actionPerformed(AnActionEvent e) {
        PsiClass psiClass = getPsiClassFromContext(e);
        GenerateDialog generateDialog = new GenerateDialog(psiClass);
        generateDialog.show();
        if (generateDialog.isOK()) {
            generateSerializer(psiClass, generateDialog.getSerializeFields());
        }
    }

    private void generateSerializer(final PsiClass psiClass, final List<PsiField> fields) {
        new WriteCommandAction.Simple(psiClass.getProject(), psiClass.getContainingFile()) {

            @Override
            protected void run() throws Throwable {
                generateSerializerContent(psiClass, fields);
            }
        }.execute();
    }

    private void generateSerializerContent(PsiClass psiClass, List<PsiField> fields) {
        List<SerializableField> serializableFields = processFields(psiClass, fields);

//        PsiPackage psiPackage = PsiTreeUtil.getParentOfType(psiClass, PsiPackage.class);
        // create pof serializer class
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        PsiClass serializerClass = getSerializerClass(psiClass, elementFactory);

        // create write index constants
        addIndexConstants(elementFactory, serializerClass, serializableFields);

        // create serialize method
        addSerializeMethod(elementFactory, psiClass, serializerClass, serializableFields);

        // todo optimize layout
        JavaCodeStyleManager.getInstance(psiClass.getProject()).shortenClassReferences(serializerClass);

        PsiDirectory parent = psiClass.getContainingFile().getParent();
        PsiFile containingFile = serializerClass.getContainingFile();
        containingFile.setName(serializerClass.getName() + ".java");
        parent.add(containingFile);
    }

    private void addSerializeMethod(PsiElementFactory elementFactory, PsiClass serializingClass, PsiClass serializerClass, List<SerializableField> serializableFields) {
        PsiClass pofWriterClass = findClass(POF_WRITER, serializingClass);

        StringBuilder code = new StringBuilder();
        code.append("public void serialize(com.tangosol.io.pof.PofWriter pofWriter, java.lang.Object o) throws java.io.IOException {");
        String sourceClassName = serializingClass.getQualifiedName();
        String sourceName = StringUtil.decapitalize(serializerClass.getName());
        code.append(sourceClassName).append(' ').append(sourceName);
        code.append(" = (").append(sourceClassName).append(") ").append("o;");

        // write every field
        for (SerializableField field : serializableFields) {
            code.append("pofWriter").append('.').append(selectMethod(null, field)).append('(');
            code.append(field.getIndexName()).append(", ");
            code.append(sourceName).append('.').append(field.getGetter().getName()).append("()").append(");");
        }

        // write remainder
        code.append("pofWriter").append(".writeRemainder(null);");
        code.append("}");

        PsiMethod serializeMethod = elementFactory.createMethodFromText(code.toString(), serializerClass);
        serializerClass.add(serializeMethod);
    }

    private String selectMethod(PsiParameter pofWriter, SerializableField field) {
        // todo
        return "writeObject";
    }

    private void addIndexConstants(PsiElementFactory elementFactory, PsiClass serializerClass, List<SerializableField> serializableFields) {
        for (SerializableField serializableField : serializableFields) {
            // create static field
            PsiField indexConstant = elementFactory.createField(serializableField.getIndexName(), PsiType.INT);
            indexConstant.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
            indexConstant.getModifierList().setModifierProperty(PsiModifier.STATIC, true);
            indexConstant.getModifierList().setModifierProperty(PsiModifier.FINAL, true);
            indexConstant.setInitializer(elementFactory.createExpressionFromText(String.valueOf(serializableField.getIndex()), serializerClass));

            serializerClass.add(indexConstant);
        }
    }

    private PsiClass getSerializerClass(PsiClass psiClass, PsiElementFactory elementFactory) {
        PsiClass serializerClass = elementFactory.createClass(psiClass.getName() + POF_SERIALIZER_SUFFIX);
        PsiJavaCodeReferenceElement implementsReferenceElement = elementFactory.createReferenceFromText(POF_SERIALIZER, serializerClass);
        serializerClass.getImplementsList().add(implementsReferenceElement);
        serializerClass.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
        return serializerClass;
    }

    private List<SerializableField> processFields(PsiClass psiClass, List<PsiField> fields) {
        List<SerializableField> serializableFields = new ArrayList<SerializableField>();

        int index = 0;
        for (PsiField field : fields) {
            serializableFields.add(new SerializableField(field, index++));
        }

        return serializableFields;
    }

    private PsiPackage getPackage(PsiClass psiClass) {
        PsiJavaFile containingFile = (PsiJavaFile) psiClass.getContainingFile();
        return JavaPsiFacade.getInstance(psiClass.getProject()).findPackage(containingFile.getPackageName());
    }

    private PsiClass findClass(String className, PsiClass context) {
        // todo test
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(context.getProject());
        Module module = ModuleUtil.findModuleForPsiElement(context);
        PsiClass foundClass = psiFacade.findClass(className, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module));
        return psiFacade.findClass(className, GlobalSearchScope.EMPTY_SCOPE);

    }

    @Override
    public void update(AnActionEvent e) {
        PsiClass psiClass = getPsiClassFromContext(e);
        e.getPresentation().setEnabled(psiClass != null);
    }

    private PsiClass getPsiClassFromContext(AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (psiFile == null || editor == null) {
            return null;
        }
        int offset = editor.getCaretModel().getOffset();
        PsiElement elementAt = psiFile.findElementAt(offset);
        return PsiTreeUtil.getParentOfType(elementAt, PsiClass.class);
    }
}
