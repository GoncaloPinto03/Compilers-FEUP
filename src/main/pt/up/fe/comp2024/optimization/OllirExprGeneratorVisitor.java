package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(METHOD_CALL, this::visitMethodCall);
        addVisit(NEW_CLASS, this::visitNewClass);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(ARRAY_DECLARATION, this::visitArrayDeclaration);
        addVisit(INTEGER_LITERAL, this::visitIntegerLiteral);
        addVisit(IF_STM, this::visitIfStmt);
        addVisit(WHILE_STM, this::visitWhileStmt);
        addVisit(FOR_STMT, this::visitForStmt);
        addVisit(BRACKETS, this::visitBrackets);
        addVisit(IDENTIFIER, this::visitIdentifier);
        addVisit(NEGATION, this::visitNegation);
        addVisit(EXPR_STMT, this::visitExprStmt);
        addVisit(LENGTH, this::visitLength);
        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = OllirExprResult.EMPTY;
        if (node.getNumChildren() > 1) {
            rhs = visit(node.getJmmChild(1));
        }

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // Generate temporary variables for complex expressions if necessary
        String lhsCode = lhs.getCode();
        if (lhsCode.contains("invokevirtual") || lhsCode.contains("invokestatic")) {
            String lhsTemp = OptUtils.getTemp() + OptUtils.toOllirType(node.getJmmChild(0));
            computation.append(lhsTemp).append(SPACE)
                    .append(ASSIGN).append(OptUtils.toOllirType(node.getJmmChild(0))).append(SPACE)
                    .append(lhsCode);
            lhsCode = lhsTemp;
        }

        String rhsCode = rhs.getCode();
        if (rhsCode.contains("invokevirtual") || rhsCode.contains("invokestatic")) {
            String rhsTemp = OptUtils.getTemp() + OptUtils.toOllirType(table.getReturnType(node.getJmmChild(1).get("value")));
            computation.append(rhsTemp).append(SPACE)
                    .append(ASSIGN).append(OptUtils.toOllirType(table.getReturnType(node.getJmmChild(1).get("value")))).append(SPACE)
                    .append(rhsCode);
            rhsCode = rhsTemp;
        }

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhsCode).append(SPACE)
                .append(node.get("op")).append(OptUtils.toOllirType(resType)).append(SPACE)
                .append(rhsCode).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitAssignStmt(JmmNode node, Void unused) {
        var lhs = visit(node.getJmmChild(0));
        var rhs = OllirExprResult.EMPTY;
        if (node.getNumChildren() > 1) {
            rhs = visit(node.getJmmChild(1));
        }

        StringBuilder computation = new StringBuilder();

        // Compute code for children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // Generate temporary variables for complex expressions if necessary
        String lhsCode = lhs.getCode();
        if (lhsCode.contains("invokevirtual") || lhsCode.contains("invokestatic")) {
            String lhsTemp = OptUtils.getTemp() + OptUtils.toOllirType(node.getJmmChild(0));
            computation.append(lhsTemp).append(SPACE)
                    .append(ASSIGN).append(OptUtils.toOllirType(node.getJmmChild(0))).append(SPACE)
                    .append(lhsCode).append(END_STMT);
            lhsCode = lhsTemp;
        }

        String rhsCode = rhs.getCode();
        if (rhsCode.contains("invokevirtual") || rhsCode.contains("invokestatic")) {
            String rhsTemp = OptUtils.getTemp() + OptUtils.toOllirType(TypeUtils.getExprType(node.getJmmChild(1), table));
            computation.append(rhsTemp).append(SPACE)
                    .append(ASSIGN).append(OptUtils.toOllirType(TypeUtils.getExprType(node.getJmmChild(1), table))).append(SPACE)
                    .append(rhsCode);
            rhsCode = rhsTemp;
        }

        // Type of the left-hand side
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);

        // Generate the assignment code
        StringBuilder code = new StringBuilder();
        code.append(lhsCode).append(SPACE)
                .append(ASSIGN).append(typeString).append(SPACE)
                .append(rhsCode).append(END_STMT);

        computation.append(code);

        return new OllirExprResult(code.toString(), computation);
    }



    private OllirExprResult visitMethodCall(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        String functionName = node.get("value");

        if (node.getJmmChild(0).getAttributes().contains("name")) {
            if (checkIfImport(node.getJmmChild(0).get("name"))) {
                code.append("invokestatic(");
                code.append(node.getJmmChild(0).get("name"));
            } else {

                code.append("invokevirtual(");
                if (node.getJmmChild(0).getKind().equals("VarRefExpr")) {
                    code.append(node.getJmmChild(0).get("name")).append(".");
                    code.append(table.getClassName());
                } else if (node.getJmmChild(0).getKind().equals("This")){
                    code.append("this").append(".");
                    code.append(table.getClassName());
                } else {
                    code.append(node.getJmmChild(0).get("value")).append(".");
                    code.append(table.getClassName());
                }
            }
        } else {

            code.append("invokevirtual(");
            if (node.getJmmChild(0).getKind().equals("VarRefExpr")) {
                code.append(node.getJmmChild(0).get("name")).append(".");
                code.append(table.getClassName());
            } else if (node.getJmmChild(0).getKind().equals("This")){
                code.append("this").append(".");
                code.append(table.getClassName());
            } else {
                code.append(node.getJmmChild(0).get("value")).append(".");
                code.append(table.getClassName());
            }
        }

        code.append(", \"");
        code.append(functionName);
        code.append("\"");

        StringBuilder aux = new StringBuilder();

        for (int i = 1; i < node.getNumChildren(); i++) {
            code.append(", ");
            // check if the child is a literal or a function variable
            if (!isLiteralOrFunctionVariable(node.getJmmChild(i))) {
                var child = visit(node.getJmmChild(i));
                aux.append(child.getCode()).append(END_STMT);
                // append aux ate the beginning of the code
                code.insert(0, aux);
                code.append(OptUtils.getCurrentTemp()).append(".i32");
            } else {
                code.append(visit(node.getJmmChild(i)).getCode());
            }
        }

        if (table.getReturnType(node.get("value")) != null) {
            code.append(")").append(OptUtils.toOllirType(table.getReturnType(functionName)));
        } else {
            code.append(").V");
        }

        code.append(END_STMT);

        return new OllirExprResult(code.toString());

    }

//    private Boolean isLiteralOrFunctionVariable(JmmNode jmmNode){
//        return Objects.equals(jmmNode.getKind(), "Integer") || Objects.equals(jmmNode.getKind(), "Boolean") || Objects.equals(jmmNode.getKind(), "This") || (Objects.equals(jmmNode.getKind(), "Identifier") && Objects.equals(jmmNode.get("field"), "false"))
//                || Objects.equals(jmmNode.getKind(), "Grouping") && isLiteralOrFunctionVariable(jmmNode.getJmmChild(0));
//    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = "";
        if (type == null) {
            ollirType = "";
        } else {
            ollirType = OptUtils.toOllirType(type);
        }

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    private OllirExprResult visitNewClass(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        code.append("new(");
        code.append(node.get("value"));
        code.append(")");
        code.append(OptUtils.toOllirType(node)).append(END_STMT);

        code.append("invokespecial(");
        code.append(node.getParent().getJmmChild(0).get("name")).append(".").append(node.get("value"));
        code.append(", \"<init>\")").append(".V");

        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitArrayDeclaration(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        code.append("new(array, ");
        code.append(visit(node.getJmmChild(0)).getCode());
        code.append(").array.");
        code.append(OptUtils.toOllirType(node));

        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitIntegerLiteral(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitIfStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        code.append(visit(node.getJmmChild(0)).getComputation());

//        JmmNode Expression = node.getJmmChild(0);
//        JmmNode Statement1 = node.getJmmChild(1);
//        JmmNode Statement2 = node.getJmmChild(2);
//
//        boolean noNext = false;
//
//        Expression.put("true", OptUtils.getIfLabel());
//        Expression.put("false", OptUtils.getEndIfLabel());"

        var currNode = node.getJmmChild(1);

        if (node.getJmmChild(1).getKind().equals("BRACKETS")) {
            if (node.getNumChildren() == 3) {
                code.append("if(");
                code.append(visit(node.getJmmChild(0)).getCode());
                code.append(") goto ").append(OptUtils.getIfLabel()).append(";\n");
                code.append(visit(node.getJmmChild(2).getJmmChild(0)).getCode());
                code.append("goto ").append(OptUtils.getEndIfLabel()).append(";\n");

                code.append(OptUtils.getCurrentIfLabel()).append(":\n");
                code.append(visit(node.getJmmChild(1).getJmmChild(0)).getCode());
                code.append(OptUtils.getCurrentEndIfLabel()).append(":\n");

            } else {
                code.append("if(");
                code.append(visit(node.getJmmChild(0)).getCode());
                code.append(") goto ").append(OptUtils.getIfLabel()).append(";\n");
                var stmt = visit(node.getJmmChild(1)).getCode();
                code.append(visit(node.getJmmChild(1)).getCode());
                code.append(OptUtils.getTemp()).append(":\n");
            }
        }

        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitBrackets(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        code.append(visit(node.getJmmChild(0)).getCode());
        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitWhileStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        code.append(OptUtils.getWhileCondLabel()).append(":\n");
        var aux2 = visit(node.getJmmChild(0));
        code.append(aux2.getComputation());
        code.append("if(");
        code.append(aux2.getCode());
        code.append(") goto ").append(OptUtils.getWhileLoopLabel()).append(";\n");
        code.append("goto ").append(OptUtils.getWhileEndLabel()).append(";\n");

        code.append(OptUtils.getCurrentWhileLoopLabel()).append(":\n");
        if (node.getJmmChild(1).getKind().equals("BRACKETS")) {
            code.append(visit(node.getJmmChild(1).getJmmChild(0)).getCode());
        } else {
            code.append(visit(node.getJmmChild(1)).getCode());
        }
        code.append("goto ").append(OptUtils.getCurrentWhileLoopLabel()).append(";\n");


        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitForStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        code.append(visit(node.getJmmChild(0)).getCode());
        code.append(visit(node.getJmmChild(1)).getCode());
        code.append(visit(node.getJmmChild(2)).getCode());
        code.append(visit(node.getJmmChild(3)).getCode());
        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitIdentifier(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        if (node.get("value").equals("true")) {
            code.append("1.bool");
        } else if (node.get("value").equals("false")) {
            code.append("0.bool");
        }
        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitNegation(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        code.append("!.bool ");
        if (node.getJmmChild(0).get("value").equals("true")) {
            code.append("0.bool");
        } else {
            code.append("1.bool");
        }
        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitExprStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        code.append(visit(node.getJmmChild(0)).getCode());
        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitLength(JmmNode node, Void unused) {
        if (!isLiteralOrFunctionVariable(node)) {
            StringBuilder code = new StringBuilder();
            code.append(OptUtils.getTemp()).append(".i32").append(" := ").append(".i32");
            code.append(" arraylength(");
            code.append(visit(node.getJmmChild(0)).getCode());
            code.append(").i32");
            return new OllirExprResult(code.toString());
        } else {
            StringBuilder code = new StringBuilder();
            code.append("arraylength(");
            code.append(visit(node.getJmmChild(0)).getCode());
            code.append(").i32");
            return new OllirExprResult(code.toString());
        }
    }

    private boolean isLiteralOrFunctionVariable(JmmNode jmmNode){
        return jmmNode.getKind().equals("VarRefExpr") || jmmNode.getKind().equals("MethodCall") || jmmNode.getKind().equals("IntegerLiteral") || jmmNode.getKind().equals("This") || (jmmNode.getKind().equals("Identifier") || (jmmNode.getKind().equals("Increment") || (jmmNode.getKind().equals("BinaryExpr") || (jmmNode.getKind().equals("Negation") || (jmmNode.getKind().equals("NewClass") && jmmNode.get("field").equals("false"))
                || jmmNode.getKind().equals("Grouping") && isLiteralOrFunctionVariable(jmmNode.getJmmChild(0));
    }

    private boolean checkIfImport(String name) {
        for (var importID : table.getImports()) {
            if (importID.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
