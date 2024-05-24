package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.specs.comp.ollir.OperationType.*;


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
    int stackLimit = 0;
    int maxStackLimit = 0;


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
        generators.put(GotoInstruction.class, inst -> "goto " + ((GotoInstruction) inst).getLabel() + NL);
        generators.put(CondBranchInstruction.class, this::generateCondBranch);
        generators.put(UnaryOpInstruction.class, inst -> {
            var unaryOp = (UnaryOpInstruction) inst;
            var code = new StringBuilder();
            code.append(generators.apply(unaryOp.getOperand()));
            code.append("iconst_1").append(NL);
            code.append("ixor").append(NL);
            return code.toString();
        });
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }

    private int getStackLimit() {
        return maxStackLimit;
    }
    private void updateStackLimit() {
        if (stackLimit >= maxStackLimit)
            maxStackLimit = stackLimit;
    }

    private String generateCondBranch(CondBranchInstruction condBranchInstruction) {
        var code = new StringBuilder();

        code.append(generators.apply(condBranchInstruction.getOperands().get(0)));
        if (condBranchInstruction.getCondition() instanceof BinaryOpInstruction) {
            code.append(generators.apply(condBranchInstruction.getOperands().get(1)));

            ((BinaryOpInstruction) condBranchInstruction.getCondition()).getOperation().getOpType();

            var op = switch (((BinaryOpInstruction) condBranchInstruction.getCondition()).getOperation().getOpType()) {
                case LTH -> "if_icmplt";
                case GTH -> "if_icmpgt";
                case EQ -> "if_icmpeq";
                case NEQ -> "if_icmpne";
                case LTE -> "if_icmple";
                case GTE -> "if_icmpge";
                default -> throw new NotImplementedException(((BinaryOpInstruction) condBranchInstruction.getCondition()).getOperation().getOpType());
            };

            code.append(op).append(" ").append(condBranchInstruction.getLabel()).append(NL);
        }
        else if (condBranchInstruction.getCondition() instanceof SingleOpInstruction) {
            code.append("ifne ").append(condBranchInstruction.getLabel()).append(NL);
        }

        return code.toString();
    }

    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = classUnit.getClassName();
        className.replace('.', '/');
        var classType = switch (classUnit.getClassAccessModifier()) {
            case PUBLIC, DEFAULT -> "public ";
            case PRIVATE -> "private ";
            case PROTECTED -> "protected ";
        };
        code.append(".class ").append(classType).append(className).append(NL).append(NL);

        String superclass = classUnit.getSuperClass() != null ? classUnit.getSuperClass() : "java/lang/Object";

        if (superclass.equals("Object"))
            superclass = "java/lang/Object";

        code.append(".super ").append(superclass).append(NL);

        for (var field : classUnit.getFields()) {
            String fieldType = decideElementTypeForParamOrField(field.getFieldType());

            String fieldAccess = "";
            if (field.getFieldAccessModifier().name().equals("PUBLIC"))
                fieldAccess = "public";

            if (field.isFinalField())
                fieldAccess += " final";
            if (field.isStaticField())
                fieldAccess += " static";

            code.append(".field ").append(fieldAccess).append(" ").append(field.getFieldName()).append(" ").append(fieldType).append(NL);
        }

        boolean hasExplicitConstructors = classUnit.getMethods().stream()
                .anyMatch(Method::isConstructMethod);
        if (!hasExplicitConstructors) {
            code.append(";default constructor");
        }

        for (var method : classUnit.getMethods()) {

            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

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
            String elementType = decideElementTypeForParamOrField(argument.getType());


            code.append(elementType);
            if (argument.getType().getTypeOfElement().equals(ElementType.OBJECTREF))
                code.append(';');
        }

        code.append(")");

        var returnType = decideElementTypeForParamOrField(method.getReturnType());


        code.append(returnType).append(NL);


        int limitsStack = getStackLimit();
        int limitsLocals = calculateLocalsLimit(method);

        // Add limits
        code.append(TAB).append(".limit stack 98").append(NL);
        code.append(TAB).append(".limit locals ").append(limitsLocals).append(NL);
        var label = "";
        for (var inst : method.getInstructions()) {
            for (var labels : method.getLabels(inst)) {
                label = labels;
                code.append(label).append(":").append(NL);
            }
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        currentMethod = null;

        return code.toString();
    }


    public static int calculateLocalsLimit(Method method) {
        int maxLocals = method.isStaticMethod() ? -1 : 0;

        HashMap<String, Descriptor> varTable = method.getVarTable();

        for (Descriptor descriptor : varTable.values()) {
            int virtualReg = descriptor.getVirtualReg();
            maxLocals = Math.max(maxLocals, virtualReg);
        }

        return maxLocals + 1;
    }


    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        boolean isArrayIndex = false;

        var lhs = assign.getDest();

        var operand = (Operand) lhs;
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();


        if(lhs instanceof ArrayOperand){
            isArrayIndex = true;
            code.append("aload ").append(reg).append(NL);
            for(Element index : ((ArrayOperand) lhs).getIndexOperands()){
                code.append(generators.apply(index));
            }
        }

        code.append(generators.apply(assign.getRhs()));

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        if(isArrayIndex)
            code.append("iastore").append(NL);
        else {
            switch (operand.getType().getTypeOfElement()) {
                case INT32, BOOLEAN -> {
                    code.append("istore ").append(reg).append(NL);
                }
                case STRING, OBJECTREF, ARRAYREF, CLASS, THIS -> {
                    code.append("astore ").append(reg).append(NL);
                }
                case VOID -> code.append("store ").append(reg).append(NL);
                default ->
                        throw new NotImplementedException("Unsupported assign type: " + operand.getType().getTypeOfElement());
            }
        }

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return switch (literal.getType().getTypeOfElement()) {
            case INT32 -> {
                int value = Integer.parseInt(literal.getLiteral());
                if (value >= 0 && value <= 5) {

                    yield "iconst_" + value + NL;
                } else if (value >= -128 && value <= 127) {

                    yield "bipush " + value + NL;
                } else if (value >= -32768 && value <= 32767) {

                    yield "sipush " + value + NL;
                } else {

                    yield "ldc " + value + NL;
                }
            }
            case BOOLEAN -> {
                yield literal.getLiteral().equals("1") ? "iconst_1" + NL : "iconst_0" + NL;
            }
            case STRING -> {
                yield "ldc \"" + literal.getLiteral() + "\"" + NL;
            }
            default -> throw new NotImplementedException(literal.getType().getTypeOfElement());
        };
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        if(operand instanceof ArrayOperand){
            var arrayOperand = (ArrayOperand) operand;
            var code = new StringBuilder();
            code.append("aload ").append(reg).append(NL);
            for(Element index : arrayOperand.getIndexOperands()){
                code.append(generators.apply(index));
            }
            code.append("iaload ").append(NL);
            return code.toString();
        } else{
            return switch (operand.getType().getTypeOfElement()) {
                case INT32, BOOLEAN -> {

                    yield "iload " + reg + NL;
                }
                case STRING, OBJECTREF, ARRAYREF, CLASS, THIS -> {

                    yield "aload " + reg + NL;
                }
                default -> throw new NotImplementedException("Unsupported type: " + operand.getType().getTypeOfElement());
            };
        }
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

        if (returnInst.getElementType() == ElementType.VOID) {
            code.append("\nreturn").append(NL);
        } else if (returnInst.getElementType() == ElementType.BOOLEAN) {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("\nireturn").append(NL);
        } else if (returnInst.getElementType() == ElementType.ARRAYREF) {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("\nareturn").append(NL);
        } else if (returnInst.getElementType() == ElementType.CLASS) {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("\nareturn").append(NL);
        } else if (returnInst.getElementType() == ElementType.THIS) {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("\nareturn").append(NL);
        } else if (returnInst.getElementType() == ElementType.STRING) {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("\nareturn").append(NL);
        } else if (returnInst.getElementType() == ElementType.OBJECTREF) {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("\nareturn").append(NL);
        } else if (returnInst.getElementType() == ElementType.CLASS) {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("\nareturn").append(NL);
        } else if (returnInst.getElementType() == ElementType.INT32) {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("\nireturn").append(NL);
        } else {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("\nireturn").append(NL);
        }
        return code.toString();
    }

    private String generateCall(CallInstruction callInstruction) {
        var code = new StringBuilder();
        var operand = (Operand) callInstruction.getOperands().get(0);

        switch (callInstruction.getInvocationType()) {
            case invokespecial -> code.append(invokeSpecial(callInstruction));
            case invokevirtual -> code.append(invokeVirtual(callInstruction));
            case invokestatic -> code.append(invokeStatic(callInstruction));
            case invokeinterface -> code.append(invokeInterface(callInstruction));
            case arraylength -> {
                code.append(generators.apply(callInstruction.getOperands().get(0)));
                code.append("arraylength").append(NL);
            }
            case NEW -> {
                if (operand.getName().equals("array")) {
                    code.append(generators.apply(callInstruction.getOperands().get(1)));
                    code.append("newarray int").append(NL);
                } else {
                    code.append("new ").append(getClassNameForElementType((ClassType) operand.getType())).append(NL).append("dup").append(NL);
                }
            }

        }

        if (!callInstruction.getReturnType().getTypeOfElement().equals(ElementType.VOID))
            if (usesResultOf(callInstruction)) {
                code.append("pop").append(NL);
            }
        return code.toString();
    }

    private boolean usesResultOf(Instruction inst) {
        if (currentMethod == null) {
            return false;
        }

        List<Instruction> instructions = currentMethod.getInstructions();
        int index = instructions.indexOf(inst);

        if (index == -1 || index == instructions.size() - 1) {
            return false;
        }

        for (int i = index + 1; i < instructions.size(); i++) {
            Instruction nextInst = instructions.get(i);
            if (nextInst.getInstType().equals(InstructionType.ASSIGN)) {
                return true;
            }
        }

        return false;
    }


    private String invokeSpecial(CallInstruction callInstruction) {
        var code = new StringBuilder();
        code.append(generators.apply(callInstruction.getOperands().get(0)));
        String className = getClassNameForElementType((ClassType) callInstruction.getCaller().getType());
        code.append("invokespecial ").append(className).append("/<init>");

        code.append("(");
        for (Element element : callInstruction.getArguments()) {
            var elementType = element.getType();
            code.append(decideElementTypeForParamOrField(elementType));
        }
        code.append(")");

        var returnType = callInstruction.getReturnType();
        code.append(decideElementTypeForParamOrField(returnType));

        return code.append("\n").toString();
    }

    private String invokeVirtual(CallInstruction callInstruction) {
        var code = new StringBuilder();
        code.append(generators.apply(callInstruction.getOperands().get(0)));

        for (Element op : callInstruction.getArguments()) {
            code.append(generators.apply(op));
        }

        var callerClassName = (ClassType) callInstruction.getCaller().getType();
        var literal = (LiteralElement) callInstruction.getOperands().get(1);

        code.append("invokevirtual " + getClassNameForElementType(callerClassName) + "/");
        code.append(literal.getLiteral().replace("\"", ""));

        code.append("(");
        for (Element element : callInstruction.getArguments()) {
            var elementType = element.getType();
            code.append(decideElementTypeForParamOrField(elementType));
        }
        code.append(")");

        var returnType = callInstruction.getReturnType();
        code.append(decideElementTypeForParamOrField(returnType));

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
            var elementType = element.getType();
            code.append(decideElementTypeForParamOrField(elementType));
        }
        code.append(")");

        var returnType = callInstruction.getReturnType();
        code.append(decideElementTypeForParamOrField(returnType));

        return code.append("\n").toString();
    }

    private String invokeInterface(CallInstruction callInstruction) {
        var code = new StringBuilder();
        int numArgs = callInstruction.getArguments().size();

        code.append(generators.apply(callInstruction.getOperands().get(0)));
        for (Element op : callInstruction.getArguments()) {
            code.append(generators.apply(op));
        }

        var callerName = ((Operand) callInstruction.getOperands().get(0)).getName();
        code.append("invokeinterface ").append(callerName).append("/");

        var literal = (LiteralElement) callInstruction.getOperands().get(1);
        code.append(literal.getLiteral().replace("\"", ""));

        code.append("(");
        for (Element element : callInstruction.getArguments()) {
            var elementType = element.getType();
            code.append(decideElementTypeForParamOrField(elementType));
        }
        code.append(")").append(" ").append(numArgs + 1);

        var returnType = callInstruction.getReturnType();
        code.append(decideElementTypeForParamOrField(returnType));

        return code.append("\n").toString();
    }

    private String generatePutField(PutFieldInstruction putFieldInstruction) {
        var code = new StringBuilder();

        var callerType = (ClassType) putFieldInstruction.getOperands().get(0).getType();
        var field = (Operand) putFieldInstruction.getOperands().get(1);

        code.append(generators.apply(putFieldInstruction.getOperands().get(0))).append(generators.apply(putFieldInstruction.getOperands().get(2)));
        code.append("putfield ").append(callerType.getName()).append("/").append(field.getName()).append(" ");

        code.append(decideElementTypeForParamOrField(field.getType()));
        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getFieldInstruction) {
        var code = new StringBuilder();

        var callerType = (ClassType) getFieldInstruction.getOperands().get(0).getType();
        var field = (Operand) getFieldInstruction.getOperands().get(1);

        code.append(generators.apply(getFieldInstruction.getOperands().get(0)));
        code.append("getfield ").append(callerType.getName()).append("/").append(field.getName()).append(" ");

        code.append(decideElementTypeForParamOrField(field.getType()));
        return code.append("\n").toString();
    }

    private String decideElementTypeForParamOrField(Type type) {
        if (type instanceof ArrayType aType) {
            return "[" + decideElementTypeForParamOrField(aType.getElementType());
        }
        if (type instanceof ClassType cType) {
            return "L" + getClassNameForElementType(cType);
        }

        return switch (type.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case STRING -> "Ljava/lang/String;";
            case VOID -> "V";
            default -> throw new IllegalArgumentException("Unsupported return type: " + type.getTypeOfElement());
        };
    }

    private String getClassNameForElementType(ClassType classType) {
        ClassUnit classUnit = ollirResult.getOllirClass();
        String name = null;

        if (classUnit.getClassName().equals(classType.getName())) {
            name = classUnit.getClassName();
        } else {
            for (String imprt : classUnit.getImports()) {
                if (!imprt.contains("."))
                    name = imprt;
                String[] imprtSplit = imprt.split("\\.");
                if (imprtSplit[imprtSplit.length - 1].equals(classType.getName())) {
                    name = imprt;
                    break;
                }
            }
        }

        return name.replace('.', '/');
    }

}