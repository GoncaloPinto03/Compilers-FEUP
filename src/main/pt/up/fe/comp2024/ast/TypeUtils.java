package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TypeUtils {

    private String currentMethod;
    private static String currentClass;



    public void setCurrentMethod(String method){
        this.currentMethod = method;
    }

    public void setCurrentClass(String _class){currentClass = _class;}

    public String getCurrentMethod(){return this.currentMethod;}

    public static String getCurrentClass(){ return
            currentClass;}

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
            case BINARY_EXPR_AND -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case IDENTIFIER, NEGATION -> new Type("boolean", false);
            case PARENTESIS -> getExprType(expr.getChild(0), table);
            case NEW_CLASS -> new Type(expr.get("value"), false);
            case THIS -> new Type(table.getClassName(), false);
            case METHOD_CALL -> getReturnType(expr, table);
            case ARRAY_DECLARATION -> new Type(INT_TYPE_NAME, true);
            case ARRAY_ACCESS -> new Type(INT_TYPE_NAME, false);
            case ARRAY_LITERAL -> new Type(INT_TYPE_NAME, true);
            case LENGTH -> new Type(INT_TYPE_NAME, false);
            case VARARG -> new Type("vararg", true);

            default ->
                    new Type("undefined", false);
        };

        //return type;
    }

    public static Boolean importedClass(String className, SymbolTable table) {
        return table.getImports().stream()
                .anyMatch(importDecl -> {
                    String[] segments = importDecl.split("\\.");
                    return segments[segments.length - 1].equals(className);
                });
    }

    private static Type getBinExprType(JmmNode binaryExpr) {

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "-", "/" -> new Type(INT_TYPE_NAME, false);
            case "==" , "!=" , "<=",  ">=", "<" , ">", "&&", "||" -> new Type("boolean", false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {

        String varName = varRefExpr.get("name");

        var methodNode = varRefExpr.getAncestor(Kind.METHOD_DECLARATION).get();
        var symbol = table.getLocalVariables(methodNode.get("name")).stream().filter(var -> var.getName().equals(varName)).findAny().orElse(null);
        if(symbol == null) {
            symbol = table.getParameters(methodNode.get("name")).stream().filter(var -> var.getName().equals(varName)).findAny().orElse(null);
        }
        if(symbol == null){
            symbol = table.getFields().stream().filter(var -> var.getName().equals(varName)).findAny().orElse(null);
        }
        if(symbol == null){
            //symbol = table.getImports().stream().filter(var -> var.g.equals(varName)).findAny().orElse(null);
        }
        if(symbol == null){
            return null;
        }

        return symbol.getType();

    }

    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {

        // verify if the types are the same
        if (sourceType.equals(destinationType)) {
            return true;
        }

        // verify if the target is a generalization of the source
        if (destinationType.hasAttribute("superclass")) {
            return destinationType.getObject("superclass", String.class)
                    .equals(sourceType.print());
        }

        return false;
    }

    public static Type getReturnType(JmmNode methodCall, SymbolTable table) {
        String methodName = methodCall.get("value");
        JmmNode x = methodCall.getChild(0);
        Type classType = getExprType(x, table);
        if(classType==null){
            return null;
        }
        if (Objects.equals(classType.getName(), "this") || Objects.equals(classType.getName(), table.getClassName())) {
            return table.getReturnType(methodName);
        } else {
//            if (!importedClass(classType.getName(), table)) {
//                return null;
//            }
            for (String importDecl : table.getImports()) {
                String[] segments = importDecl.split("\\.");
                if (segments[segments.length - 1].equals(classType.getName())) {
                    return classType;
                }

            }
            return new Type("undefined", false);
        }

    }

}
