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
        //addVisit(Kind.ARRAY_LITERAL, this::visitArrayLiteral );
        addVisit("Negation", this::visitNegationExpr);
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
        return new Type(node.getKind(), false);
    }



    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("name");

        // Var is a field, return
        if (table.getFields().stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Var is a parameter, return
        if (table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Var is a declared variable, return
        if (table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
            return null;
        }

        // Create error report
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

    private Void visitArrayAccess(JmmNode array, SymbolTable table) {
        JmmNode arrayExpression = array.getChild(0); // Get the expression representing the array
        JmmNode indexExpression = array.getChild(1); // Get the expression representing the index

        // Get the types of the array and index expressions
        Type arrayType = getNodeType(arrayExpression, table);
        Type indexType = getNodeType(indexExpression, table);

        // Check if the array expression represents an array type
        if (!arrayType.isArray()) {
            var message = String.format("Invalid array access - '%s'. Must be an array type.", arrayType.getName());
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(array),
                    NodeUtils.getColumn(array),
                    message, null)
            );
        }

        // Check if the index expression represents an integer type
        if (!indexType.getName().equals("int") && (!indexType.getName().equals("IntegerLiteral"))) {
            var message = String.format("Invalid array index '%s'. Must be of type int.", indexType.getName());
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(indexExpression),
                    NodeUtils.getColumn(indexExpression),
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
        // Get the left and right expressions of the binary expression
        JmmNode leftExpression = node.getChild(0);
        JmmNode rightExpression = node.getChild(1);

        // Get the types of the left and right expressions
        Type leftType = getNodeType(leftExpression, table);
        Type rightType = getNodeType(rightExpression, table);

        // Check if the types are compatible for the binary operation
        // For the sake of this example, let's assume we're only dealing with arithmetic expressions
        if (!leftType.getName().equals("int")) {
            var message = String.format("Left operand of binary expression must be of type int, found '%s'.", leftType.getName());
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message, null)
            );
        }

        if (!rightType.getName().equals("int")) {
            var message = String.format("Right operand of binary expression must be of type int, found '%s'.", rightType.getName());
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
