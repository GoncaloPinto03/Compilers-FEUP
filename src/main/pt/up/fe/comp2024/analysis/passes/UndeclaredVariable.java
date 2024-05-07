package pt.up.fe.comp2024.analysis.passes;

import com.sun.source.doctree.SystemPropertyTree;
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

import java.util.*;

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
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.NEGATION, this::visitNegationExpr);
        addVisit(Kind.METHOD_CALL, this::visitMethodCall);
        addVisit(Kind.NEW_CLASS, this::visitNewClass);
        addVisit("String", this::dealWithType);
        addVisit("Double", this::dealWithType);
        addVisit("Boolean", this::dealWithType);
        addVisit("Int", this::dealWithType);
        addVisit("Integer", this::dealWithType);
        addVisit("Id", this::dealWithType);
        addVisit("Identifier", this::dealWithType);
        addVisit(Kind.CONDITION_STM, this::visitBooleanExpr);
        addVisit(Kind.THIS, this::visitThisExpr);


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

    private Void dealWithType(JmmNode node, SymbolTable table){
        node.put("type", node.getKind());
        return null;
    }


    private Void visitBooleanExpr(JmmNode node, SymbolTable table){
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
            return null;
        }

        if(retType.getName().equals("imported")){
            return null;
        }

        if(retType.hasAttribute("vararg") && stmt.getKind().equals("ArrayAccess")){
            String message = "Invalid return type, varargs cannot be used in array access";
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


    private Void visitThisExpr (JmmNode node, SymbolTable table){
        node.put("type", table.getClassName());
        return null;
    }



    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        List<JmmNode> params=method.getChildren("ParamDeclaration");

        var nrVarags = params.stream().filter(
                param->param.getChild(0).getKind().equals("VARARG")
        ).count();

        if (nrVarags > 0) {
            if (nrVarags > 1) {
                String message = "Invalid number of varargs";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message, null)
                );
            }
            var pos = 0;
            for (var paramsAux : method.getChildren("ParamDeclaration")) {
                if (paramsAux.getChild(0).getKind().equals("VARARG")) {
                    pos = method.getChildren().indexOf(paramsAux)-1;
                }
            }
            JmmNode lastParamNode = method.getChild(method.getChildren().size()-1);
            if (method.getChildren("ParamDeclaration").size() - 1 != pos) {
                String message = "Vararg must be the last parameter";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message, null)
                );
            }
            if (!lastParamNode.getChild(0).getKind().equals("VARARG") && !lastParamNode.getChild(0).getKind().equals("ArrayAccess") ) {

                String message = "Vararg must be the last parameter";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message, null)
                );

            }
        }

        // check if method is imported or extemded
        if (table.getMethods().contains(currentMethod) || table.getImports().contains(currentMethod)) {
            return null;
        } else {
            String message = "Method not declared";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message, null)
            );
        }

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

    private Void visitArrayLiteral(JmmNode arrayLiteral, SymbolTable table) {
        // Get the expected type of the array elements (assuming it's the type of the first element)
        // This part might need to be adjusted based on how you're handling type information in your AST
        if (arrayLiteral.getNumChildren() == 0) return null; // Handle empty array initializations gracefully

        JmmNode lhsNode = arrayLiteral.getChildren().get(0); // Assuming left-hand side is the first child
        JmmNode rhsNode = arrayLiteral.getChildren().get(1); // Assuming right-hand side is the second child

        Type lhsType = TypeUtils.getExprType(lhsNode, table);
        Type rhsType = TypeUtils.getExprType(rhsNode, table);

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

        // Check if the right-hand side is an array literal and the left-hand side is not an array
        if (rhsNode.getKind().equals("ARRAY_LITERAL") && !lhsType.isArray()) {
            String message = "Cannot assign an array literal to a non-array variable.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayLiteral),
                    NodeUtils.getColumn(arrayLiteral),
                    message,
                    null
            ));
        }

        // Check if types are compatible for other cases (e.g., variable assignment)
        if (!TypeUtils.areTypesAssignable(rhsType, lhsType)) {
            String message = String.format("Type mismatch: cannot assign %s to %s", rhsType, lhsType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayLiteral),
                    NodeUtils.getColumn(arrayLiteral),
                    message,
                    null
            ));
        }
        return null;
    }


    private Void visitArrayAccess(JmmNode array, SymbolTable table) {
        JmmNode arrayNode = array.getChildren().get(0);
        JmmNode indexNode = array.getChildren().size() > 1 ? array.getChildren().get(1) : null;

        Type arrayType = arrayNode != null ? TypeUtils.getExprType(arrayNode, table) : null;
        Type indexType = indexNode != null ? TypeUtils.getExprType(indexNode, table) : null;

        // Check if the array node is actually an array
        if (!arrayType.isArray()){
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
        if (!indexType.getName().equals("int") || indexType.isArray() || indexType.hasAttribute("vararg")) {
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


    private Void visitNegationExpr(JmmNode node, SymbolTable table){
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
    }

    private Void visitBinaryExpr(JmmNode node, SymbolTable table) {
        JmmNode leftExpr = node.getChild(0);
        JmmNode rightExpr = node.getChild(1);


        Type leftType = TypeUtils.getExprType(leftExpr,table);
        Type rightType = TypeUtils.getExprType(rightExpr,table);

        // Check if the types are compatible for the binary operation
        if (!leftType.getName().equals("int") ||!rightType.getName().equals("int") || leftType.isArray()) {
            String message =("The type of operand of binary expression is not compatible with the operation.");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        } else if (leftType.getName().equals("null") || rightType.getName().equals("null")){
            return null;
        } else if (leftType.getName().equals("int") && rightType.getName().equals("int") && !leftType.isArray() && !rightType.isArray() ) {
            return null;
        }
        else{
            String message =("The type of operand of binary expression is not compatible with the operation.");
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
        JmmNode lhsNode = assignStmt.getChildren().get(0);
        JmmNode rhsNode = assignStmt.getChildren().get(1);

        Type lhsType = TypeUtils.getExprType(lhsNode, table);
        Type rhsType = TypeUtils.getExprType(rhsNode, table);

        if(rhsType == null){
            return null;
        }

        if(lhsType.getName().equals(rhsType.getName()) && lhsType.isArray() == rhsType.isArray()){
            return null;
        }

        if(TypeUtils.importedClass(lhsType.getName(), table) && TypeUtils.importedClass(rhsType.getName(), table)){
            return null;
        }


        String extendedClass = table.getSuper();
        var aux = table.getClassName();
        if(lhsType.getName().equals(extendedClass) && rhsType.getName().equals(aux)){
            return null;
        }

        if(lhsType.getName().equals(rhsType.getName()) && lhsType.isArray() != rhsType.isArray()){
            String message = "Type mismatch: cannot assign array to an int";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null
            ));

        }

        if(rhsType.getName().equals("ArrayLiteral") && lhsType.isArray()){
            return null;
        }

        if (rhsNode.getKind().equals("MethodCall")) {
            var aux3 = rhsNode.getJmmChild(0);
            if (table.getImports().contains(TypeUtils.getExprType(aux3, table).getName())) {
                return null;
            }
        }

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



    private Void visitNewClass(JmmNode node, SymbolTable table) {
        if(table.getImports().stream().noneMatch(name -> name.equals(node.get("value"))) && !(node.get("value").equals(table.getClassName()))){
            String message = "Class is not defined";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
        }

        node.put("type", node.get("value"));
        node.put("isArray", "false");
        return null;
    }
    private Void visitMethodCall (JmmNode method, SymbolTable table){

        if(table.getMethods().contains(method.get("value")) || table.getImports().contains(method.get("value"))){
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    "Method not declared",
                    null)
            );
        }

        if(!table.getMethods().contains(method.get("value")) && !table.getImports().contains(method.get("value"))){

            if (method.getNumChildren() > 0) {
                JmmNode node = method.getChildren().get(0);

                Type nodeType = TypeUtils.getExprType(node, table);
                if (table.getSuper() != null && table.getImports() != null) {
                    if (table.getSuper().contains(nodeType.getName()) || table.getImports().contains(nodeType.getName())) {
                        return null;
                    } else if (table.getClassName().equals(nodeType.getName())) {
                        return null;
                    }
                }
            } else if (table.getClassName().equals(method.get("value"))) {
                return null;
            }
            if (method.getKind().equals("MethodCall")) {
                JmmNode rhsNode = method.getChildren().get(0);
                Type rhsNodeType = TypeUtils.getExprType(rhsNode, table);
                if (rhsNodeType != null && table.getImports().contains(rhsNodeType.getName())) {
                    return null;
                }
            }
            if (table.getImports().contains(method.getJmmChild(0).get("name"))){
                return null;
            }
            String message = "Method not declared";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message, null)
            );
        }

        return null;
    }



}
