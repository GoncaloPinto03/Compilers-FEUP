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
        addVisit (Kind.BINARY_EXPR, this::visitBinaryExpr );
        addVisit(Kind.CONDITION_STM, this::visitConditionStm);
        addVisit(Kind.IDENTIFIER, this::visitBooleanExpr);
        addVisit(Kind.NEGATION, this::visitNegation);
        //addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
        //addVisit(Kind.THIS, this::visitThis);
        //addVisit(Kind.ASSIGN_STMT, this::visitAssign);
        addVisit(Kind.LENGTH, this::visitLength);

    }

    private Type getNodeType (JmmNode node, SymbolTable table){
        if(node.getKind().equals("VarRefExpr")){
            String var = node.get("name");
            List<Symbol> params=table.getParameters(this.currentMethod);
            List<Symbol> locals=table.getLocalVariables(this.currentMethod);
            List<Symbol> fields=table.getFields();


            if(params != null){
                for(Symbol arg: params){
                    if(arg.getName().equals(var))
                        return arg.getType();
                }
            }
            if(locals != null){
                for(Symbol arg: locals){
                    if(arg.getName().equals(var))
                        return arg.getType();
                }
            }

            if(fields != null){
                for(Symbol arg: fields){
                    if(arg.getName().equals(var))
                        return arg.getType();
                }
            }

        }

        if(node.getKind().equals("BinaryExpr")){
            String operator = node.get("op");
            if(operator.equals("+") || operator.equals("-") || operator.equals("*") || operator.equals("/")){
                return new Type("int", false);
            }
            if(operator.equals("&&") || operator.equals("||")){
                return new Type("boolean", false);
            }

        }
        return new Type(node.getKind(), false);
    }

    private Void visitLength(JmmNode node, SymbolTable table){
        JmmNode array = node.getChild(0);
        Type arrayType = TypeUtils.getExprType(array, table);
        if(!arrayType.isArray()){
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



    private Void visitArrayAccess(JmmNode array, SymbolTable table) {
        //Type arrayType = TypeUtils.getExprType(array, table);
        //Type indexType = TypeUtils.getExprType(array.getChild(1), table);
        JmmNode accessNode = array.getChildren().get(0);
        JmmNode indexNode = array.getChildren().get(1);

        Type accessNodeType = getNodeType(accessNode, table);
        Type indexNodeType = getNodeType(indexNode, table);


        if(!accessNodeType.isArray()){
            String message = "Invalid array access";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(array),
                    NodeUtils.getColumn(array),
                    message, null)
            );
        }

        if(!indexNodeType.getName().equals("IntegerLiteral")){
            String message = "Invalid array access index, should be an integer.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(array),
                    NodeUtils.getColumn(array),
                    message, null)
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
        if (!(leftType.getName().equals("IntegerLiteral") || leftType.getName().equals("int")) || leftType.isArray()) {
            String message =("The type of left operand of binary expression is not compatible with the operation.");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        }

        if (!(rightType.getName().equals("IntegerLiteral") || rightType.getName().equals("int")) || rightType.isArray()) {
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





}
