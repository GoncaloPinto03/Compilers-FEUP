package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;


    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        var classType = switch (ollirResult.getOllirClass().getClassAccessModifier()) {
            case PUBLIC, DEFAULT -> "public ";
            case PRIVATE -> "private ";
            case PROTECTED -> "protected ";
        };
        code.append(".class ").append(classType).append(className).append(NL).append(NL);

        String superclass = ollirResult.getOllirClass().getSuperClass() != null ? ollirResult.getOllirClass().getSuperClass() : "java/lang/Object";

        if (superclass.equals("Object"))
            superclass = "java/lang/Object";

        code.append(".super ").append(superclass).append(NL);

        for (var field : ollirResult.getOllirClass().getFields()) {
            String fieldType = decideElementTypeForParamOrField(field.getFieldType().getTypeOfElement());

            if (fieldType.equals("A"))
                fieldType = "Ljava/lang/String;";

            String fieldAccess = "";
            if (field.getFieldAccessModifier().name().equals("PUBLIC"))
                fieldAccess = "public";

            if (field.isFinalField())
                fieldAccess += " final";
            if (field.isStaticField())
                fieldAccess += " static";

            code.append(".field ").append(fieldAccess).append(" ").append(field.getFieldName()).append(" ").append(fieldType).append(NL);
        }



        boolean hasExplicitConstructors = ollirResult.getOllirClass().getMethods().stream()
                .anyMatch(Method::isConstructMethod);
        if (!hasExplicitConstructors) {
            code.append(";default constructor");
        }

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        // generate a single constructor method
        var defaultConstructor = """
                .method public <init>()V
                    aload_0
                    invokespecial""" + " " + superclass + """
                /<init>()V
                    return
                .end method
                """;
        code.append(defaultConstructor);

        return code.toString();
    }


    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        String methodAccess = "";
        if (method.isFinalMethod())
            methodAccess = "final ";
        if (method.isStaticMethod())
            methodAccess = "static ";

        var methodName = method.getMethodName();

        code.append("\n.method ").append(modifier).append(methodAccess).append(methodName).append("(");

        // traverse method parameters
        for (Element argument : method.getParams()) {
            String elementType = decideElementTypeForParamOrField(argument.getType().getTypeOfElement());

            if (argument.getType().getTypeOfElement().toString().equals("STRING"))
                elementType = "Ljava/lang/String;";
            if (argument.getType().getTypeOfElement().toString().equals("ARRAYREF"))
                elementType = "[Ljava/lang/String;";

            code.append(elementType);
        }

        code.append(")");

        var returnType = decideElementTypeForParamOrField(method.getReturnType().getTypeOfElement());

        if (method.getReturnType().getTypeOfElement().toString().equals("STRING"))
            returnType = "Ljava/lang/String;";
        if (method.getReturnType().getTypeOfElement().toString().equals("ARRAYREF"))
            returnType = "[Ljava/lang/String;";

        code.append(returnType).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        switch (operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> code.append("istore ").append(reg).append(NL);
            case STRING, OBJECTREF -> code.append("astore ").append(reg).append(NL);
            default -> throw new NotImplementedException("Unsupported assign type: " + operand.getType().getTypeOfElement());
        }

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        String loadType = "";
        switch (operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> loadType = "iload " + reg + NL;
            case STRING, OBJECTREF -> loadType = "aload " + reg + NL;
            case THIS -> loadType = "aload_0 " + NL;
        }
        return loadType;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case SUB -> "isub";
            case MUL -> "imul";
            case DIV -> "idiv";
            case SHR -> "ishr";
            case SHL -> "ishl";
            case SHRR -> "iushr";
            case XOR -> "ixor";
            case AND -> "iand";
            case OR -> "ior";
            case LTH -> "if_icmplt";
            case GTH -> "if_icmpgt";
            case EQ -> "if_icmpeq";
            case NEQ -> "if_icmpne";
            case LTE -> "if_icmple";
            case GTE -> "if_icmpge";
            case ANDB -> "iand";
            case ORB -> "ior";
            case NOTB -> "iconst_m1 \n ixor";
            case NOT -> "iconst_1 \n ixor";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        if (returnInst.getElementType() == ElementType.VOID)
            code.append("\nreturn").append(NL);
        else {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("\nireturn").append(NL);
        }

        return code.toString();
    }

    private String generateCall(CallInstruction callInstruction) {
        var code  = new StringBuilder();
        var operand = (Operand) callInstruction.getOperands().get(0);

        switch (callInstruction.getInvocationType()) {
            case invokespecial -> code.append(invokeSpecial(callInstruction));
            case invokevirtual -> code.append(invokeVirtual(callInstruction));
            case invokestatic -> code.append(invokeStatic(callInstruction));
            case invokeinterface -> code.append(invokeInterface(callInstruction));
            case NEW -> code.append("new ").append(operand.getName()).append(NL).append("dup").append(NL);
        }

        return code.toString();
    }

    private String invokeSpecial(CallInstruction callInstruction) {
        var code = new StringBuilder();
        code.append(generators.apply(callInstruction.getOperands().get(0)));
        code.append("invokespecial ").append(ollirResult.getOllirClass().getClassName()).append("/<init>");

        code.append("(");
        for (Element element : callInstruction.getArguments()) {
            var elementType = element.getType().getTypeOfElement();
            decideReturnTypeForInvokeOrPutGetField(code, elementType);
        }
        code.append(")");

        var returnType = callInstruction.getReturnType().getTypeOfElement();
        decideReturnTypeForInvokeOrPutGetField(code, returnType);

        return code.append("\n").toString();
    }

    private String invokeVirtual(CallInstruction callInstruction) {
        var code = new StringBuilder();
        code.append(generators.apply(callInstruction.getOperands().get(0)));

        for (Element op : callInstruction.getArguments()) {
            code.append(generateOperand((Operand) op));
        }

        var callerClassName = (ClassType) callInstruction.getCaller().getType();
        var literal = (LiteralElement) callInstruction.getOperands().get(1);

        code.append("invokevirtual " + callerClassName.getName() + "/");
        code.append(literal.getLiteral().replace("\"", ""));

        code.append("(");
        for (Element element : callInstruction.getArguments()) {
            var elementType = element.getType().getTypeOfElement();
            decideReturnTypeForInvokeOrPutGetField(code, elementType);
        }
        code.append(")");

        var returnType = callInstruction.getReturnType().getTypeOfElement();
        decideReturnTypeForInvokeOrPutGetField(code, returnType);

        return code.append("\n").toString();
    }

    private String invokeStatic(CallInstruction callInstruction) {
        var code = new StringBuilder();
        for (Element op : callInstruction.getArguments()) {
            code.append(generators.apply(op));
        }

        var callerName = ((Operand) callInstruction.getOperands().get(0)).getName();
        code.append("invokestatic ").append(callerName).append("/");

        var literal = (LiteralElement) callInstruction.getOperands().get(1);
        code.append(literal.getLiteral().replace("\"", ""));

        code.append("(");
        for (Element element : callInstruction.getArguments()) {
            var elementType = element.getType().getTypeOfElement();
            decideReturnTypeForInvokeOrPutGetField(code, elementType);
        }
        code.append(")");

        var returnType = callInstruction.getReturnType().getTypeOfElement();
        decideReturnTypeForInvokeOrPutGetField(code, returnType);

        return code.append("\n").toString();
    }

    // A special case is invokeinterface, which takes a <method-spec> and an integer indicating how many arguments the method takes
    private String invokeInterface(CallInstruction callInstruction) {
        var code = new StringBuilder();
        int numArgs = 0;
        for (Element op : callInstruction.getArguments()) {
            numArgs++;
            code.append(generators.apply(op));
        }

        var callerName = ((Operand) callInstruction.getOperands().get(0)).getName();
        code.append("invokeinterface ").append(callerName).append("/");

        var literal = (LiteralElement) callInstruction.getOperands().get(1);
        code.append(literal.getLiteral().replace("\"", ""));

        code.append("(");
        for (Element element : callInstruction.getArguments()) {
            var elementType = element.getType().getTypeOfElement();
            decideReturnTypeForInvokeOrPutGetField(code, elementType);
        }
        code.append(")");

        var returnType = callInstruction.getReturnType().getTypeOfElement();
        decideReturnTypeForInvokeOrPutGetField(code, returnType);
        code.append(" ").append(numArgs);

        return code.append("\n").toString();
    }

    private String generatePutField(PutFieldInstruction putFieldInstruction) {
        var code = new StringBuilder();

        var callerType = (ClassType) putFieldInstruction.getOperands().get(0).getType();
        var field = (Operand) putFieldInstruction.getOperands().get(1);

        code.append(generators.apply(putFieldInstruction.getOperands().get(0))).append(generators.apply(putFieldInstruction.getOperands().get(2)));
        code.append("putfield ").append(callerType.getName()).append("/").append(field.getName()).append(" ");

        decideReturnTypeForInvokeOrPutGetField(code, field.getType().getTypeOfElement());
        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getFieldInstruction) {
        var code = new StringBuilder();

        var callerType = (ClassType) getFieldInstruction.getOperands().get(0).getType();
        var field = (Operand) getFieldInstruction.getOperands().get(1);

        code.append(generators.apply(getFieldInstruction.getOperands().get(0)));
        code.append("getfield ").append(callerType.getName()).append("/").append(field.getName()).append(" ");

        decideReturnTypeForInvokeOrPutGetField(code, field.getType().getTypeOfElement());
        return code.append("\n").toString();
    }

    private void decideReturnTypeForInvokeOrPutGetField(StringBuilder code, ElementType returnType) {
        switch (returnType) {
            case INT32 -> code.append("I");
            case BOOLEAN -> code.append("B");
            case VOID -> code.append("V");
            case STRING -> code.append("Ljava/lang/String;");
        }
    }

    private String decideElementTypeForParamOrField(ElementType elementType) {
        return switch (elementType) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case ARRAYREF, OBJECTREF, CLASS, STRING, THIS -> "A";
            case VOID -> "V";
            default -> throw new IllegalArgumentException("Unsupported return type: " + elementType);
        };
    }
}