package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.List;
import java.util.Optional;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(Kind.ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.CONDITION_STM, this::visitConditionStm);
        addVisit(Kind.NEGATION, this::visitNegation);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
        addVisit(Kind.LENGTH, this::visitLength);
        addVisit(Kind.ARRAY_LITERAL, this::visitArrayLiteral);
        addVisit(Kind.NEW_OBJECT, this::visitNewObject); // Register the visitor for new object nodes
    }


    private Type getNodeType(JmmNode node, SymbolTable table) {
        if (node.getKind().equals("VarRefExpr")) {
            String var = node.get("name");
            List<Symbol> params = table.getParameters(this.currentMethod);
            List<Symbol> locals = table.getLocalVariables(this.currentMethod);
            List<Symbol> fields = table.getFields();


            if (params != null) {
                for (Symbol arg : params) {
                    if (arg.getName().equals(var))
                        return arg.getType();
                }
            }
            if (locals != null) {
                for (Symbol arg : locals) {
                    if (arg.getName().equals(var))
                        return arg.getType();
                }
            }

            if (fields != null) {
                for (Symbol arg : fields) {
                    if (arg.getName().equals(var))
                        return arg.getType();
                }
            }

        }

        if (node.getKind().equals("BinaryExpr")) {
            String operator = node.get("op");
            if (operator.equals("+") || operator.equals("-") || operator.equals("*") || operator.equals("/")) {
                return new Type("int", false);
            }
            if (operator.equals("&&") || operator.equals("||")) {
                return new Type("boolean", false);
            }

        }
        return new Type(node.getKind(), false);
    }

    private Void visitLength(JmmNode node, SymbolTable table) {
        JmmNode array = node.getChild(0);
        Type arrayType = TypeUtils.getExprType(array, table);
        if (!arrayType.isArray()) {
            String message = "Invalid length operation";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        }
        return null;
    }

    private Void visitReturnStmt(JmmNode node, SymbolTable table) {
        JmmNode stmt = node.getChildren().get(0);
        Type retType = TypeUtils.getExprType(stmt, table);
        Type methodType = table.getReturnType(currentMethod);
        if(retType == null){
            String message = "Invalid return type";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        }


        if(!retType.getName().equals(methodType.getName())){
            String message = "Invalid return type";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        }
        if (!methodType.getName().equals(retType.getName()) || methodType.isArray() != retType.isArray()) {
            String message = "Invalid return type";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        }
        return null;
    }




    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");

        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("name");

        // Check if the variable is a field
        if (table.getFields().stream()
                .anyMatch(field -> field.getName().equals(varRefName))) {
            return null; // Variable is a field, return
        }

        // Check if the variable is a parameter
        if (table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null; // Variable is a parameter, return
        }

        // Check if the variable is a local variable
        if (table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
            return null; // Variable is a local variable, return
        }

        // Check if the variable is an imported class or package
        if (table.getImports().stream()
                .anyMatch(importDecl -> importDecl.endsWith(varRefName))) {
            return null; // Variable is an imported class or package, return
        }


        // Variable does not exist, create error report
        var message = String.format("Variable '%s' does not exist.", varRefName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varRefExpr),
                NodeUtils.getColumn(varRefExpr),
                message,
                null)
        );

        return null;
    }


    private Void visitNegation (JmmNode node, SymbolTable table){
        JmmNode exp = node.getChildren().get(0);
        Type type = TypeUtils.getExprType(exp, table);

        if(!type.getName().equals("boolean") || type.isArray()){
            String message = "Negation can only be applied to a boolean expression";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        }

        return null;
    }



    private Void visitBooleanExpr (JmmNode node, SymbolTable table){
        JmmNode leftExpr = node.getChild(0);
        JmmNode rightExpr = node.getChild(1);


        Type leftType = TypeUtils.getExprType(leftExpr,table);
        Type rightType = TypeUtils.getExprType(rightExpr,table);

        if(!leftType.getName().equals("boolean") || !rightType.getName().equals("boolean")){
            String message = "Invalid";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        }
        return null;
    }




    private Void visitArrayLiteral(JmmNode arrayLiteral, SymbolTable table) {
        // Get the expected type of the array elements (assuming it's the type of the first element)
        // This part might need to be adjusted based on how you're handling type information in your AST
        if (arrayLiteral.getNumChildren() == 0) return null; // Handle empty array initializations gracefully

        Type expectedType = TypeUtils.getExprType(arrayLiteral.getChildren().get(0), table);

        for (JmmNode element : arrayLiteral.getChildren()) {
            Type elementType = TypeUtils.getExprType(element, table);
            if (!TypeUtils.areTypesAssignable(elementType, expectedType)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(element),
                        NodeUtils.getColumn(element),
                        String.format("Array initialization type mismatch: expected %s, found %s", expectedType, elementType),
                        null
                ));
            }
        }
        return null;
    }

    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        if (sourceType.isArray() != destinationType.isArray()) return false;
        if (sourceType.getName().equals(destinationType.getName())) return true;

        // Expand this section to handle type promotions or specific cases like int to double, etc.
        return false;
    }




    private Void visitArrayAccess(JmmNode array, SymbolTable table) {
        // Get the node representing the array and the node representing the index
        JmmNode arrayNode = array.getChildren().get(0);
        JmmNode indexNode = array.getChildren().get(1);

        // Retrieve the types of the array node and the index node
        Type arrayType = TypeUtils.getExprType(arrayNode, table);
        Type indexType = TypeUtils.getExprType(indexNode, table);

        // Check if the array node is actually an array
        if (!arrayType.isArray()) {
            String message = "Invalid array access: '" + arrayNode.get("name") + "' is not an array.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayNode),
                    NodeUtils.getColumn(arrayNode),
                    message,
                    null)
            );
        }

        // Check if the index node is an integer
        if (!indexType.getName().equals("int") || indexType.isArray()) {
            String message = "Invalid array index type: Index must be an integer.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(indexNode),
                    NodeUtils.getColumn(indexNode),
                    message,
                    null)
            );
        }

        return null;
    }









    private Void visitConditionStm (JmmNode node, SymbolTable table){
        JmmNode condition = node.getChild(0);
        Type conditionType = TypeUtils.getExprType(condition, table);
        if(!conditionType.getName().equals("boolean")){
            String message = "Invalid";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        }
        return null;
    }


    /*private Void visitNegationExpr(JmmNode node, SymbolTable table){
        node.put("type", "boolean");
        if(!node.getChild(0).get("type").equals("boolean")){
            String message = "Invalid";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        }

        return null;
    }*/

    /*private Void visitReturnStmt(JmmNode node, SymbolTable table) {
        JmmNode stmt = node.getChildren().get(0);
        Type retType = TypeUtils.getExprType(stmt, table);
        Type methodType = table.getReturnType(currentMethod);

        if(retType.getName().equals("imported")){
            return null;
        }
        if(!methodType.getName().equals(retType.getName()) || methodType.isArray() != retType.isArray()){
            String message = "Invalid return type";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        }


        return null;
    }*/




    private Void visitBinaryExpr(JmmNode node, SymbolTable table) {
        JmmNode leftExpr = node.getChild(0);
        JmmNode rightExpr = node.getChild(1);


        Type leftType = TypeUtils.getExprType(leftExpr,table);
        Type rightType = TypeUtils.getExprType(rightExpr,table);

        // Check if the types are compatible for the binary operation
        if (!leftType.getName().equals("IntegerLiteral") || leftType.isArray()) {
            String message =("The type of left operand of binary expression is not compatible with the operation.");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        }

        if (!rightType.getName().equals("IntegerLiteral") || rightType.isArray()) {
            String message =("The type of right operand of binary expression is not compatible with the operation.");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        }

        return null;
    }


    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        JmmNode lhsNode = assignStmt.getChildren().get(0); // Assuming left-hand side is the first child
        JmmNode rhsNode = assignStmt.getChildren().get(1); // Assuming right-hand side is the second child

        Type lhsType = TypeUtils.getExprType(lhsNode, table);
        Type rhsType = TypeUtils.getExprType(rhsNode, table);

        // Check if types are compatible
        if (!TypeUtils.areTypesAssignable(rhsType, lhsType)) {
            String message = String.format("Type mismatch: cannot assign %s to %s", rhsType, lhsType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null
            ));
        }
        return null;
    }

    private Void visitIdUsage(JmmNode idNode, SymbolTable table) {
        String identifier = idNode.get("name");

        // Check if it's a variable or a field already declared
        if (isDeclaredLocally(identifier, table)) {
            return null; // It's a local or field variable, so it's fine
        }

        // Check if it's an imported class
        if (!table.getImports().contains(identifier)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(idNode),
                    NodeUtils.getColumn(idNode),
                    "Class '" + identifier + "' is not imported and not declared in the local file.",
                    null
            ));
        }
        return null;
    }

    private boolean isDeclaredLocally(String identifier, SymbolTable table) {
        return table.getLocalVariables(currentMethod).stream().anyMatch(v -> v.getName().equals(identifier)) ||
                table.getFields().stream().anyMatch(f -> f.getName().equals(identifier));
    }
    private Void visitClassUsage(JmmNode classUsageNode, SymbolTable table) {
        String className = classUsageNode.get("name");

        // Check if it's an imported class or declared locally
        if (!table.getImports().contains(className) && !className.equals(table.getClassName())) {
            String message = "Class '" + className + "' is not imported or declared.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(classUsageNode),
                    NodeUtils.getColumn(classUsageNode),
                    message,
                    null
            ));
        }
        return null;
    }


    private Void visitNewObject(JmmNode newNode, SymbolTable table) {
        String className = newNode.get("className"); // Assuming the node stores class name in this attribute

        // Check if class is imported or declared locally
        if (!table.getImports().contains(className) && !className.equals(table.getClassName())) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(newNode),
                    NodeUtils.getColumn(newNode),
                    "Class '" + className + "' is not imported or declared in the current scope.",
                    null
            ));
        }
        // You might also want to check constructor parameters here
        return null;
    }

}
