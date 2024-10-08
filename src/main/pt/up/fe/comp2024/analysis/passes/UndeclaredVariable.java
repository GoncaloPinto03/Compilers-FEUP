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
    private Set<String> declaredFields = new HashSet<>();
    private Set<String> declaredMethods = new HashSet<>();

    private Set<String> importedClasses = new HashSet<>();

    private Set<String> localVariables = new HashSet<>();

    private boolean isCurrentMethodStatic;



    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(Kind.ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.BINARY_EXPR_AND, this::visitBinaryExpr);
        addVisit(Kind.NEGATION, this::visitNegation);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
        addVisit(Kind.LENGTH, this::visitLength);
        addVisit(Kind.ARRAY_LITERAL, this::visitArrayLiteral);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.NEGATION, this::visitNegationExpr);
        addVisit(Kind.METHOD_CALL, this::visitMethodCall);
        addVisit(Kind.NEW_CLASS, this::visitNewClass);
        addVisit("String", this::dealWithType);
        addVisit("Double", this::dealWithType);
        addVisit("Boolean", this::dealWithType);
        addVisit("ClassDeclaration", this::dealClassDecl);
        addVisit("Int", this::dealWithType);
        addVisit("Integer", this::dealWithType);
        addVisit("Id", this::dealWithType);
        addVisit("Identifier", this::dealWithType);
        addVisit(Kind.CONDITION_STM, this::visitBooleanExpr);
        addVisit(Kind.THIS, this::visitThisExpr);
        addVisit("IfStm", this::visitIfStm);
        addVisit("WhileStm", this::visitWhileStm);
        addVisit(Kind.VAR_DECL, this::visitVarDeclaration);
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

    private Void visitImportDecl(JmmNode importNode, SymbolTable table) {
        String importName = importNode.get("ID");


        String[] importParts = importName.split("\\.");
        String className = importParts[importParts.length - 1];
        System.out.println(className);

        if (importedClasses.contains(className)) {
            String message = "Duplicate import declaration: " + className;
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(importNode),
                    NodeUtils.getColumn(importNode),
                    message, null)
            );
        } else {
            importedClasses.add(className);
        }
        return null;
    }


    private Void visitVarDeclaration (JmmNode node, SymbolTable table){
        String varName = node.get("name");
        if (localVariables.contains(varName)) {
            String message = "Duplicate variable declaration: " + varName;
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        } else {
            localVariables.add(varName);
        }

        JmmNode varNode = node.getChild(0);
        if ("VARARG".equals(varNode.getKind())) {
            String message = "Invalid variable declaration: varargs cannot be used in locals";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        }
        return null;
    }

    private Void dealClassDecl(JmmNode node, SymbolTable table){
        for (JmmNode field : node.getChildren("VarDeclaration")) {
            String fieldName = field.get("name");
            if (declaredFields.contains(fieldName)) {
                String message = "Duplicate field declaration: " + fieldName;
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(field),
                        NodeUtils.getColumn(field),
                        message, null)
                );
            } else {
                declaredFields.add(fieldName);
            }

            Type fieldType = TypeUtils.getExprType(field, table);
            if(fieldType.hasAttribute("VARARG")){
                String message = "Invalid field declaration: varargs cannot be used in fields";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(field),
                        NodeUtils.getColumn(field),
                        message, null)
                );
            }
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

        if (methodType.getName().equals("VARARG")) {
            String message = "Invalid return type: cannot return VARARG";
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

    private Void visitIfStm(JmmNode node, SymbolTable table) {
        JmmNode expr = node.getChild(0);

        Type exprType = TypeUtils.getExprType(expr, table);
        if(exprType.isArray() || !exprType.getName().equals("boolean")){
            String message = "Invalid if statement";
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
        if (isCurrentMethodStatic || "main".equals(currentMethod)) {
            String message = "Invalid use of 'this' in a static context.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
            return null;
        }
        node.put("type", table.getClassName());
        return null;
    }



    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        String methodName = method.get("name");
        List<JmmNode> params=method.getChildren("ParamDeclaration");
        Set <String> paramsSet = new HashSet<>();
        localVariables.clear();

        if(method.getChild(0).getKind().equals("VARARG")){
            String message = "Invalid return type: cannot return VARARG";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message, null)
            );
        }

        if(method.getAttributes().contains("isStatic")){
            isCurrentMethodStatic = true;
        } else {
            isCurrentMethodStatic = false;
        }


        if (declaredMethods.contains(methodName)) {
            String message = "Duplicate method declaration: " + methodName;
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message, null)
            );
        } else {
            declaredMethods.add(methodName);
        }


        for (JmmNode param : params) {
            if (paramsSet.contains(param.get("name"))) {
                String message = "Duplicate parameter name";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(param),
                        NodeUtils.getColumn(param),
                        message, null)
                );
            }
            paramsSet.add(param.get("name"));
        }



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
                return null;
            }
            var pos = 0;
            for (var paramsAux : method.getChildren("ParamDeclaration")) {
                if (paramsAux.getChild(0).getKind().equals("VARARG")) {
                    pos = method.getChildren().indexOf(paramsAux)-1;
                }
            }
            if (method.getChildren("ParamDeclaration").size() - 1 != pos) {
                String message = "Vararg must be the last parameter";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message, null)
                );
                return null;
            }

        }

        // check if method is imported or extended
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

        // Check if the variable is a local variable
        if (table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
            return null;
        }

        // Check if the variable is a parameter
        if (table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Check if the variable is a field
        if (table.getFields().stream().anyMatch(field -> field.getName().equals(varRefName))) {
            if (isCurrentMethodStatic) {
                String message = "Cannot access instance field '" + varRefName + "' from static method '" + currentMethod + "'.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varRefExpr),
                        NodeUtils.getColumn(varRefExpr),
                        message,
                        null)
                );
            }
            return null;
        }


        // Check if the variable is an imported class or package
        if (table.getImports().stream()
                .anyMatch(importDecl -> importDecl.endsWith(varRefName))) {
            return null;
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

        if (arrayLiteral.getNumChildren() == 0) return null;

        JmmNode lhsNode = arrayLiteral.getChildren().get(0);
        JmmNode rhsNode = arrayLiteral.getChildren().get(1);

        Type lhsType = TypeUtils.getExprType(lhsNode, table);
        Type rhsType = TypeUtils.getExprType(rhsNode, table);

        if(lhsType.getName().equals("boolean") || rhsType.getName().equals("boolean")){
            String message = "boolean in array init ";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayLiteral),
                    NodeUtils.getColumn(arrayLiteral),
                    message, null)
            );
        }

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

        if (rhsType.getName().equals("ArrayLiteral") && lhsType.isArray()){
            return null;
        }

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

        if(arrayType == null){
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(array),
                    NodeUtils.getColumn(array),
                    "Not declared",
                    null)
            );
            return  null;
        }


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


    private Void visitWhileStm (JmmNode node, SymbolTable table){
        JmmNode expr = node.getChild(0);

        Type exprType = TypeUtils.getExprType(expr, table);

        if(exprType.isArray() || !exprType.getName().equals("boolean")){
            String message = "Invalid while statement";
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
        if(!(node.getChild(0).get("value").equals("true")) && !(node.getChild(0).get("value").equals("false"))){
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

        Type leftType = TypeUtils.getExprType(leftExpr, table);
        Type rightType = TypeUtils.getExprType(rightExpr, table);

        String operator = node.get("op");

        if (leftType == null || rightType == null) {
            return null;
        }

        boolean isIntOperation = operator.equals("+") || operator.equals("-") ||
                operator.equals("*") || operator.equals("/") ||
                operator.equals("%") || operator.equals("<") ||
                operator.equals(">") || operator.equals("<=") ||
                operator.equals(">=");

        boolean isBooleanOperation = operator.equals("&&") || operator.equals("||");

        if (isIntOperation) {
            if (!leftType.getName().equals("int") || !rightType.getName().equals("int") || leftType.isArray() || rightType.isArray()) {
                String message = "The type of operand of binary expression is not compatible with the integer operation.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        message, null)
                );
            }
        } else if (isBooleanOperation) {
            if (!leftType.getName().equals("boolean") || !rightType.getName().equals("boolean")) {
                String message = "The type of operand of binary expression is not compatible with the boolean operation.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        message, null)
                );
            }
        } else {
            String message = "Unknown binary operation.";
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

        if(rhsType == null || lhsType == null){
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

        dealClassDecl(node,table);

        if (table.getImports().stream().noneMatch(name -> name.equals(node.get("value"))) &&
                !(node.get("value").equals(table.getClassName()))) {
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

        if (method.getChild(0).getKind().equals("This")) {
            return null;
        }


        var test = TypeUtils.getExprType(method.getChild(0), table);

        if(table.getMethods().contains(method.get("value")) || table.getImports().contains(method.get("value"))) {
            if (!table.getImports().contains(test.getName())) {
                if (!table.getClassName().equals(test.getName())) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(method),
                            NodeUtils.getColumn(method),
                            "Method not declared",
                            null)
                    );
                }
            }
        }

        if(table.getImports().contains(method.get("value"))){
            return null;
        }

        if(table.getMethods().contains(method.get("value"))){
            return null;
        }

        String methodName = method.get("value");

        for (String importedClass : table.getImports()) {
            String[] parts = importedClass.split("\\.");

            String lastPart = parts[parts.length - 1];

            if (lastPart.equals(methodName)) {
                return null;
            }
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
