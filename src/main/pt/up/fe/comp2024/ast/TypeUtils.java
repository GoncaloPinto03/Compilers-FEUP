package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

public class TypeUtils {

    private String currentMethod;
    private String currentClass;

    public void setCurrentMethod(String method){
        this.currentMethod = method;
    }

    public void setCurrentClass(String _class){this.currentClass = _class;}

    public String getCurrentMethod(){return this.currentMethod;}

    public String getCurrentClass(){ return this.currentClass;}

    private static final String INT_TYPE_NAME = "int";


    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }


    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());

        return switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case IDENTIFIER, NEGATION -> new Type("boolean", false);
            case PARENTESIS, ARRAY_ACCESS -> getExprType(expr.getJmmChild(0), table);
            case NEW_OBJECT -> new Type(expr.get("object"), false);
            case THIS -> new Type(table.getClassName(), false);
            //case METHODCALL -> getReturns (expr, table);
            case ARRAY_DECLARATION -> new Type(INT_TYPE_NAME, true);
            case METHOD_CALL ->  table.getReturnType(expr.get("value"));
            default ->
                    throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'.");
        };

        //return type;
    }

    public Boolean importedClass(String className, SymbolTable table) {
        return table.getImports().stream().anyMatch(importDecl -> {String[] args = importDecl.split("\\.");
            return args[args.length - 1].equals(className);});
    }

    private static Type getBinExprType(JmmNode binaryExpr) {

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "-", "/" -> new Type(INT_TYPE_NAME, false);
            case "==" , "!=" , "<=",  ">=", "<" , ">" -> new Type("boolean", false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }





    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {

        String varName = varRefExpr.get("name");

        var methodNode = varRefExpr.getAncestor(Kind.METHOD_DECLARATION).get();
        var symbol = table.getLocalVariables(methodNode.get("name")).stream().filter(var -> var.getName().equals(varName)).findAny().orElse(null);
        if(symbol == null){
            symbol = table.getFields().stream().filter(var -> var.getName().equals(varName)).findAny().orElse(null);
        }
        if(symbol == null) {
            symbol = table.getParameters(methodNode.get("name")).stream().filter(var -> var.getName().equals(varName)).findAny().orElse(null);
            return new Type("undefined", false);
        }

        return symbol.getType();


    }



    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
