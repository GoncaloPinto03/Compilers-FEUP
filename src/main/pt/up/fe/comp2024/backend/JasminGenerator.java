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
        code.append(".class ").append(className).append(NL).append(NL);

        // generate superClass name, if it exists
        String superClass = classUnit.getSuperClass() == null ?
                "java/lang/Object" :
                classUnit.getSuperClass();
        code.append(".super ").append(superClass).append(NL);

        // traverse fields of the class
        for (var field : ollirResult.getOllirClass().getFields()) {
            String fieldType = decideElementTypeForParamOrField(field.getFieldType().getTypeOfElement()).equals("A") ? // 'A' stands for string
                    "Ljava/lang/String;" :
                    decideElementTypeForParamOrField(field.getFieldType().getTypeOfElement());

            String fieldAccess;
            /* nao sei se podemos fazer assim ou se só fazemos isto para o PUBLIC
            fieldAccess = field.getFieldAccessModifier().name().toLowerCase();
            */
            if (field.getFieldAccessModifier().name().equals("PUBLIC")) {
                fieldAccess = "public";
            } else {
                fieldAccess = "";
            }

            if (field.isStaticField())
                fieldAccess += " static";
            if (field.isFinalField())
                fieldAccess += " final";

            code.append(".field ")
                .append(fieldAccess)
                .append(' ')
                .append(field.getFieldName())
                .append(fieldType)
                .append(NL);
        }

        // generate a single constructor method
        var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial""" + superClass + """
                    /<init>()V
                    return
                .end method
                """;
        code.append(defaultConstructor);

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

        String methodName = method.getMethodName();

        String methodRestriction = "";
        if (method.isStaticMethod())
            methodRestriction = "static ";
        if (method.isFinalMethod())
            methodRestriction = "final ";

        code.append("\n.method ").append(modifier).append(methodRestriction).append(methodName).append('(');

        // traverse the method params
        for (var param : method.getParams()) {
          String paramType = decideElementTypeForParamOrField(param.getType().getTypeOfElement());

          // handle special cases
          if (param.getType().getTypeOfElement().equals("STRING"))
              paramType = "Ljava/lang/String;";
          if (param.getType().getTypeOfElement().equals("ARRAYREF"))
              paramType = "[Ljava/lang/String;";

          code.append(paramType);
        }
        code.append(')');

        // return type
        String retType = decideElementTypeForParamOrField(method.getReturnType().getTypeOfElement());
        if (method.getReturnType().getTypeOfElement().equals("STRING"))
            retType = "Ljava/lang/String;";
        if (method.getReturnType().getTypeOfElement().equals("ARRAYREF"))
            retType = "[Ljava/lang/String;";

        code.append(retType).append(NL);

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

        String operandType = decideElementTypeForParamOrField(operand.getType().getTypeOfElement());
        switch (operandType.toLowerCase()) {
            case "i" -> code.append("istore ").append(reg).append(NL);
            case "z" -> code.append("zstore ").append(reg).append(NL);
            case "a" -> code.append("astore ").append(reg).append(NL);
            case "" -> code.append("store ").append(reg).append(NL);
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
        return "iload " + reg + NL;
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
        if (returnInst.getOperand() != null) {
            code.append(generators.apply(returnInst.getOperand()));

            ElementType elementType = returnInst.getReturnType().getTypeOfElement();
            decideElementTypeForRetStmt(code, elementType);
        } else {
            code.append("\n\t\treturn");
        }

        return code.toString();
    }

    private void decideElementTypeForRetStmt(StringBuilder code, ElementType elementType) {
        String retType = switch (elementType) {
            case INT32 -> "ireturn";
            case BOOLEAN -> "zreturn";
            case ARRAYREF, OBJECTREF, CLASS, STRING, THIS -> "areturn";
            case VOID -> "return";
            default -> throw new IllegalArgumentException("Unsupported return type: " + elementType);
        };;
        code.append(retType).append(NL);
    }

    private String decideElementTypeForParamOrField(ElementType elementType) {
        String type = switch (elementType) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case ARRAYREF, OBJECTREF, CLASS, STRING, THIS -> "A";
            case VOID -> "V";
            default -> throw new IllegalArgumentException("Unsupported return type: " + elementType);
        };;
        return type;
    }
}



// O QUE ESTÁ EM BAIXO É DO RICARDO

//package pt.up.fe.comp2024.backend;
//
//import org.specs.comp.ollir.*;
//import org.specs.comp.ollir.tree.TreeNode;
//import pt.up.fe.comp.jmm.ollir.OllirResult;
//import pt.up.fe.comp.jmm.report.Report;
//import pt.up.fe.specs.util.classmap.FunctionClassMap;
//import pt.up.fe.specs.util.exceptions.NotImplementedException;
//import pt.up.fe.specs.util.utilities.StringLines;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import static org.specs.comp.ollir.OperandType.INT32;
//
///**
// * Generates Jasmin code from an OllirResult.
// * <p>
// * One JasminGenerator instance per OllirResult.
// */
//public class JasminGenerator {
//
//    private static final String NL = "\n";
//    private static final String TAB = "   ";
//
//    private final OllirResult ollirResult;
//
//    List<Report> reports;
//
//    String code;
//
//    Method currentMethod;
//
//    private final FunctionClassMap<TreeNode, String> generators;
//
//    public JasminGenerator(OllirResult ollirResult) {
//        this.ollirResult = ollirResult;
//
//        reports = new ArrayList<>();
//        code = null;
//        currentMethod = null;
//
//        this.generators = new FunctionClassMap<>();
//        generators.put(ClassUnit.class, this::generateClassUnit);
//        generators.put(Method.class, this::generateMethod);
//        generators.put(AssignInstruction.class, this::generateAssign);
//        generators.put(SingleOpInstruction.class, this::generateSingleOp);
//        generators.put(LiteralElement.class, this::generateLiteral);
//        generators.put(Operand.class, this::generateOperand);
//        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
//        generators.put(ReturnInstruction.class, this::generateReturn);
//    }
//    public List<Report> getReports() {
//        return reports;
//    }
//
//    public String build() {
//
//        // This way, build is idempotent
//        if (code == null) {
//            code = generators.apply(ollirResult.getOllirClass());
//        }
//
//        return code;
//    }
//
//
//    private String generateClassUnit(ClassUnit classUnit) {
//
//        var code = new StringBuilder();
//
//        // generate class name
//        var className = ollirResult.getOllirClass().getClassName();
//        code.append(".class ").append(className).append(NL).append(NL);
//
//        String superclass = classUnit.getSuperClass() != null ?
//                classUnit.getSuperClass().toString():
//                "java/lang/Object";
//        String superCode = ".super " + superclass;
//
//        code.append(superCode).append(NL);
//
//
//        String c;
//
//        for (var field : ollirResult.getOllirClass().getFields()) {
//            switch(field.getFieldType().getTypeOfElement()) {
//                case INT32 -> c = "I";
//                case BOOLEAN -> c = "Z";
//                case VOID -> c = "V";
//                case STRING -> c = "Ljava/lang/String;";
//                default -> c = "";
//            }
//
//            String access;
//
//            if (field.getFieldAccessModifier().name().equals("PUBLIC")) {
//                access = "public";
//            }
//            else {
//                access = "";
//            }
//
//            String field_restriction;
//            if (field.isFinalField()) { field_restriction = " final"; }
//            else if (field.isStaticField()) { field_restriction = " static"; } else { field_restriction = ""; }
//
//            String field_code = ".field " + access + field_restriction + " " + field.getFieldName() + " " + c + NL;
//
//            code.append(field_code);
//
//        }
//
//        // generate a single constructor method
//        var defaultConstructor = """
//                ;default constructor
//                .method public <init>()V
//                    aload_0
//                    invokespecial""" + " " + superclass + """
//                /<init>()V
//                    return
//                .end method
//                """;
//        code.append(defaultConstructor);
//
//        // generate code for all other methods
//        for (var method : ollirResult.getOllirClass().getMethods()) {
//
//            // Ignore constructor, since there is always one constructor
//            // that receives no arguments, and has been already added
//            // previously
//            if (method.isConstructMethod()) {
//                continue;
//            }
//
//            code.append(generators.apply(method));
//        }
//
//        return code.toString();
//    }
//
//
//    private String generateMethod(Method method) {
//
//        // set method
//        currentMethod = method;
//
//        var code = new StringBuilder();
//
//        // calculate modifier
//        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
//                method.getMethodAccessModifier().name().toLowerCase() + " " :
//                "";
//
//        String method_restriction;
//        if (method.isFinalMethod()) { method_restriction = "final "; }
//        else if (method.isStaticMethod()) { method_restriction = "static "; } else { method_restriction = ""; }
//
//        var methodName = method.getMethodName();
//
//        code.append("\n.method ").append(modifier).append(method_restriction).append(methodName).append("(");
//
//        //gets the parameters types
//        for (Element argument : method.getParams()) {
//            switch(argument.getType().getTypeOfElement()) {
//                case INT32:
//                    code.append("I");
//                    break;
//                case BOOLEAN:
//                    code.append("Z");
//                    break;
//                case VOID:
//                    code.append("V");
//                    break;
//                case STRING:
//                    code.append("Ljava/lang/String;");
//                    break;
//                case ARRAYREF:
//                    code.append("[Ljava/lang/String;");
//                    break;
//                default:
//                    break;
//            }
//        }
//
//        code.append(")");
//
//        var returnType = method.getReturnType().getTypeOfElement();
//
//        switch (returnType) {
//            case INT32:
//                code.append("I\n");
//                break;
//            case BOOLEAN:
//                code.append("Z\n");
//                break;
//            case VOID:
//                code.append("V\n");
//                break;
//            case STRING:
//                code.append("Ljava/lang/String;\n");
//                break;
//            case ARRAYREF:
//                code.append("[Ljava/lang/String;\n");
//                break;
//            default:code.append("\n");
//        }
//
//
//        // Add limits
//        code.append(TAB).append(".limit stack 99").append(NL);
//        code.append(TAB).append(".limit locals 99").append(NL);
//
//        for (var inst : method.getInstructions()) {
//           /* if (inst.getInstType().toString().equals("RETURN")) {
//                code.append("ireturn\n");
//                break;
//            }*/
//            var instCode = StringLines.getLines(generators.apply(inst)).stream()
//                    .collect(Collectors.joining(NL + TAB, TAB, NL));
//
//            code.append(instCode);
//        }
//
//        code.append(".end method\n");
//
//        // unset method
//        currentMethod = null;
//
//        return code.toString();
//    }
//
//    private String generateAssign(AssignInstruction assign) {
//        var code = new StringBuilder();
//
//        // generate code for loading what's on the right
//        code.append(generators.apply(assign.getRhs()));
//
//        // store value in the stack in destination
//        var lhs = assign.getDest();
//
//        if (!(lhs instanceof Operand)) {
//            throw new NotImplementedException(lhs.getClass());
//        }
//
//        var operand = (Operand) lhs;
//
//        // get register
//        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
//
//        code.append("istore ").append(reg).append(NL);
//
//        return code.toString();
//    }
//
//    private String generateSingleOp(SingleOpInstruction singleOp) {
//        return generators.apply(singleOp.getSingleOperand());
//    }
//
//    private String generateLiteral(LiteralElement literal) {
//        return "ldc " + literal.getLiteral() + NL;
//    }
//
//    private String generateOperand(Operand operand) {
//        // get register
//        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
//        return "iload " + reg + NL;
//    }
//
//    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
//        var code = new StringBuilder();
//
//        // load values on the left and on the right
//        code.append(generators.apply(binaryOp.getLeftOperand()));
//        code.append(generators.apply(binaryOp.getRightOperand()));
//
//        // apply operation
//        var op = switch (binaryOp.getOperation().getOpType()) {
//            case ADD -> "iadd";
//            case MUL -> "imul";
//            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
//        };
//
//        code.append(op).append(NL);
//
//        return code.toString();
//    }
//
//    private String generateReturn(ReturnInstruction returnInst) {
//        var code = new StringBuilder();
//
//
//        if (returnInst.getElementType() == ElementType.VOID) {
//            code.append("\nreturn").append(NL);
//        }
//        else {
//            code.append(generators.apply(returnInst.getOperand()));
//            code.append("\nireturn").append(NL);
//        }
//
//        return code.toString();
//    }
//
//}
