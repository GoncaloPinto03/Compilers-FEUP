package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";

    private static final String BOOLEAN_TYPE = "boolean";
    private String currentMethod;
    private String currentClass;


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
            case IDENTIFIER, NEGATION -> new Type(BOOLEAN_TYPE, false);
            case PARENTESIS, ARRAY_ACCESS -> getExprType(expr.getJmmChild(0), table);
            case NEW_OBJECT -> new Type(expr.get("object"), false);
            case THIS -> new Type(table.getClassName(), false);
            //case METHODCALL -> getReturns (expr, table);
            case ARRAY_DECLARATION -> new Type(INT_TYPE_NAME, true);
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
            case "+", "*", "-" -> new Type(INT_TYPE_NAME, false);
            case "!" -> new Type(BOOLEAN_TYPE, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }



    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {

        String varName = varRefExpr.get("name");

        var methodNode = varRefExpr.getAncestor(Kind.METHOD_DECLARATION).get();
        var symbol = table.getLocalVariables(methodNode.get("name")).stream().filter(var -> var.getName().equals(varName)).findAny().orElse(null);
        if(symbol == null){
            symbol = table.getFields().stream().filter(var -> var.getName().equals(varName)).findAny().get();
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
