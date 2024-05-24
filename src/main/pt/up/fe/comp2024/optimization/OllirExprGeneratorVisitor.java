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
        addVisit(LENGTH, this::visitArrayLength);
        addVisit(ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(ARRAY_ASSIGN, this::visitArrayAssignmentStm);
        addVisit(BINARY_EXPR_AND, this::visitBinExprAnd);
        addVisit("This", this::visitThis);
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

    private OllirExprResult visitBinExprAnd(JmmNode node, Void unused) {
        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        // Build the computation string for the AND operation
        StringBuilder computation = new StringBuilder();
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

        // Create a temporary variable to hold the result of the AND operation
        String resultTemp = OptUtils.getTemp() + ".bool";

        // Build the OLLIR code for the AND operation
        StringBuilder ollirCode = new StringBuilder();
        ollirCode.append("if (").append(lhsCode).append(") goto ").append(OptUtils.getAndTrue()).append(";\n");
        ollirCode.append(resultTemp).append(SPACE).append(ASSIGN).append(".bool 0.bool;\n");
        ollirCode.append("goto ").append(OptUtils.getAndEnd()).append(";\n");
        ollirCode.append(OptUtils.getCurrentAndTrue()).append(":\n");
        ollirCode.append(computation);
        ollirCode.append(resultTemp).append(SPACE).append(ASSIGN).append(OptUtils.toOllirType(table.getReturnType(node.getJmmChild(1).get("value")))).append(SPACE).append(rhsCode).append(END_STMT);
        ollirCode.append(OptUtils.getCurrentAndEnd()).append(SPACE).append(":\n");

        return new OllirExprResult(resultTemp, ollirCode.toString());
    }

    private OllirExprResult visitAssignStmt(JmmNode node, Void unused) {
        var lhs = visit(node.getJmmChild(0));
        var rhs = OllirExprResult.EMPTY;
        String lhsCode = lhs.getCode();
        if (node.getNumChildren() > 1) {
            if (node.getJmmChild(1).getKind().equals("NewClass")) {
                lhsCode = OptUtils.getTemp() + OptUtils.toOllirType(TypeUtils.getExprType(node.getJmmChild(0), table));
                rhs = visit(node.getJmmChild(1));
            } else {
                rhs = visit(node.getJmmChild(1));
            }
        }

        StringBuilder computation = new StringBuilder();

        // Compute code for children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // Generate temporary variables for complex expressions if necessary
        if (!node.getJmmChild(1).getKind().equals("NewClass")) {
            lhsCode = lhs.getCode();
        }
        if (lhsCode.contains("invokevirtual") || lhsCode.contains("invokestatic")) {
            String lhsTemp = OptUtils.getTemp() + OptUtils.toOllirType(node.getJmmChild(0));
            computation.append(lhsTemp).append(SPACE)
                    .append(ASSIGN).append(OptUtils.toOllirType(node.getJmmChild(0))).append(SPACE)
                    .append(lhsCode).append(END_STMT);
            lhsCode = lhsTemp;
        }

        String rhsCode = rhs.getCode();
        if (rhsCode.contains("invokevirtual") || rhsCode.contains("invokestatic")) {
            String rhsTemp = OptUtils.getTemp() + OptUtils.toOllirType(TypeUtils.getExprType(node.getJmmChild(0), table));
            computation.append(rhsTemp).append(SPACE)
                    .append(ASSIGN).append(OptUtils.toOllirType(TypeUtils.getExprType(node.getJmmChild(0), table))).append(SPACE)
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
        StringBuilder computation = new StringBuilder();

        JmmNode receiverNode = node.getJmmChild(0);
        boolean isStatic = false;

        // Compute the receiver of the method call
        String receiverCode;
        if (receiverNode.getAttributes().contains("name")) {
            String receiverName = receiverNode.get("name");
            if (checkIfImport(receiverName)) {
                isStatic = true;
                receiverCode = receiverName;
            } else {
                receiverCode = receiverName + "." + TypeUtils.getExprType(receiverNode, table).getName();
            }
        } else {
            if ("This".equals(receiverNode.getKind())) {
                receiverCode = "this." + table.getClassName();
            } else {
                receiverCode = receiverNode.get("value") + "." + table.getClassName();
            }
        }

        // Generate temporary variable for receiver if necessary
        if (receiverCode.contains("invokevirtual") || receiverCode.contains("invokestatic")) {
            String receiverTemp = OptUtils.getTemp() + OptUtils.toOllirType(receiverNode);
            computation.append(receiverTemp).append(SPACE)
                    .append(ASSIGN).append(OptUtils.toOllirType(receiverNode)).append(SPACE)
                    .append(receiverCode).append(END_STMT);
            receiverCode = receiverTemp;
        }

        // Determine whether to use invokevirtual or invokestatic
        if (isStatic) {
            code.append("invokestatic(").append(receiverCode);
        } else {
            code.append("invokevirtual(").append(receiverCode);
        }

        code.append(", \"").append(functionName).append("\"");

        // Compute arguments
        for (int i = 1; i < node.getNumChildren(); i++) {
            code.append(", ");
            JmmNode argNode = node.getJmmChild(i);
            var argResult = visit(argNode);

            // Generate temporary variables for complex arguments if necessary
            String argCode = argResult.getCode();
            if (argCode.contains("invokevirtual") || argCode.contains("invokestatic")) {
                String argTemp = OptUtils.getTemp() + OptUtils.toOllirType(argNode);
                computation.append(argTemp).append(SPACE)
                        .append(ASSIGN).append(OptUtils.toOllirType(argNode)).append(SPACE)
                        .append(argCode).append(END_STMT);
                argCode = argTemp;
            }
            code.insert(0, argResult.getComputation());
            code.append(argCode);
        }

        // Determine the return type
        Type returnType = TypeUtils.getExprType(node.getParent().getJmmChild(0), table);
        if (returnType != null) {
            code.append(")").append(OptUtils.toOllirType(returnType));
        } else {
            code.append(").V");
        }

        code.append(END_STMT);

        // Prepend the computation for receiver and arguments
        computation.append(code);

        return new OllirExprResult(computation.toString());
    }


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
        code.append(OptUtils.getCurrentTemp()).append(".").append(node.get("value"));
        code.append(", \"<init>\")").append(".V").append(END_STMT);

        code.append(node.getParent().getJmmChild(0).get("name")).append(OptUtils.toOllirType(node)).append(" := ");
        code.append(OptUtils.toOllirType(node)).append(SPACE);
        code.append(OptUtils.getCurrentTemp()).append(OptUtils.toOllirType(node));

        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitArrayDeclaration(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        StringBuilder temp = new StringBuilder();

        temp.append(OptUtils.getTemp()).append(" := .").append(OptUtils.toOllirType(node)).append(SPACE);
        temp.append(visit(node.getJmmChild(0)).getCode()).append(END_STMT);


        code.append("new(array, ");
        code.append(OptUtils.getCurrentTemp()).append(".i32");
        code.append(").array.");
        code.append(OptUtils.toOllirType(node));

        return new OllirExprResult(temp.toString(), code.toString());
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
        var aux = visit(node.getJmmChild(0));
        code.append(aux.getCode());
        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitArrayLength(JmmNode node, Void unused) {
        // Assuming you have this method for array length handling
        String arrayCode = visit(node.getJmmChild(0)).getCode();
        String temp = OptUtils.getTemp() + ".i32";
        StringBuilder computation = new StringBuilder();
        computation.append(temp).append(" :=.i32 arraylength(").append(arrayCode).append(").i32;\n");
        return new OllirExprResult(temp, computation.toString());
    }


    private OllirExprResult visitArrayAccess(JmmNode node, Void unused) {
        // Visitar as expressões para o array e o índice
        var arrayExpr = visit(node.getJmmChild(0));
        var indexExpr = visit(node.getJmmChild(1));

        // Construir a string de computação para o acesso ao array
        StringBuilder computation = new StringBuilder();
        computation.append(arrayExpr.getComputation());
        computation.append(indexExpr.getComputation());

        // Gerar variáveis temporárias para expressões complexas se necessário
        String arrayCode = arrayExpr.getCode();
        if (arrayCode.contains("invokevirtual") || arrayCode.contains("invokestatic")) {
            String arrayTemp = OptUtils.getTemp() + OptUtils.toOllirType(node.getJmmChild(0));
            computation.append(arrayTemp).append(SPACE)
                    .append(ASSIGN).append(OptUtils.toOllirType(node.getJmmChild(0))).append(SPACE)
                    .append(arrayCode);
            arrayCode = arrayTemp;
        }

        String indexCode = indexExpr.getCode();
        if (indexCode.contains("invokevirtual") || indexCode.contains("invokestatic")) {
            String indexTemp = OptUtils.getTemp() + OptUtils.toOllirType(node.getJmmChild(1));
            computation.append(indexTemp).append(SPACE)
                    .append(ASSIGN).append(OptUtils.toOllirType(node.getJmmChild(1))).append(SPACE)
                    .append(indexCode);
            indexCode = indexTemp;
        }

        // Criar uma variável temporária para armazenar o resultado do acesso ao array
        String resultTemp = OptUtils.getTemp() + "." + OptUtils.toOllirType(node.getJmmChild(1));

        // Construir o código OLLIR para o acesso ao array
        StringBuilder ollirCode = new StringBuilder();
        ollirCode.append(resultTemp).append(SPACE).append(ASSIGN).append(SPACE).append(".")
                .append(OptUtils.toOllirType(node.getJmmChild(1))).append(SPACE)
                .append(arrayCode).append("[").append(indexCode).append("]").append(".i32").append(END_STMT);

        // Adicionar o código de computação
        computation.append(ollirCode);

        return new OllirExprResult(resultTemp, computation.toString());
    }

    private OllirExprResult visitArrayAssignmentStm(JmmNode node, Void unused) {
        // Visitar as expressões para o array e o índice
        var arrayExpr = visit(node.getJmmChild(0));
        var indexExpr = visit(node.getJmmChild(1));

        // Construir a string de computação para a atribuição ao array
        StringBuilder computation = new StringBuilder();

        computation.append("a.array.i32[").append(arrayExpr.getCode()).append("]").append(".i32").append(ASSIGN).append(".i32").append(SPACE).append(indexExpr.getCode()).append(END_STMT);
        //a.array.i32[0.i32].i32 :=.i32 1.i32;


        return new OllirExprResult(computation.toString());
    }


    private OllirExprResult visitThis(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        code.append("this.").append(table.getClassName());
        return new OllirExprResult(code.toString());
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
