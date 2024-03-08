package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.specs.util.SpecsCheck;
import java.util.*;
import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {

    public static JmmSymbolTable build(JmmNode root) {
        var classDecl = root.getChildren(CLASS_DECL).get(0);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");
        var imports = buildImports(root);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        String superClass = classDecl.getOptional("sname").orElse(null);
        List<Symbol> fields = buildFields(classDecl);

        return new JmmSymbolTable(className , methods, imports, returnTypes, params, locals, superClass, fields);
    }

    public static String dealWithImport(JmmNode jmmNode){
        String importList = jmmNode.get("importValue");
        String importStr = importList.substring(1, importList.length()-1);
        String _import = String.join(".", importStr.split(", "));

        return _import;
    }
    private static List<String> buildImports(JmmNode root) {

        List<String> imports = new ArrayList<>();

        root.getChildren("ImportDeclaration").stream()
                .forEach(importNode -> {
                    String _import = dealWithImport(importNode);
                    imports.add(_import);
                });

        return imports;
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {

        Map<String, Type> map = new HashMap<>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            String methName = method.get("name");
            JmmNode retType = method.getChildren(TYPE).get(0);
            map.put(methName, getType(retType));

        }

        return map;
    }

    private static Type getType(JmmNode node) {
        boolean isArray = node.getObject("isArray", Boolean.class);
        return new Type(node.get("value"), isArray);
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> params = new HashMap<>();

        for (var methodDecl : classDecl.getChildren(METHOD_DECL)) {
            List<Symbol> paramList = new ArrayList<>();

            for (var param : methodDecl.getChildren("Param")) {
                String name = param.get("name");
                Type type = getType(param.getChildren(TYPE).get(0));
                paramList.add(new Symbol(type, name));
            }
            params.put(methodDecl.get("name"), paramList);
        }
        return params;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }

     private static List<Symbol> buildFields(JmmNode classDecl) {
         List<Symbol> symbols = new ArrayList<>();

         for (var variable : classDecl.getChildren("VarDeclaration")) {
             String name = variable.get("name");
             Type type = getType(variable.getChildren(TYPE).get(0));
             symbols.add(new Symbol(type, name));
         }
         return symbols;
     }

    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        List<Symbol> symbols = new ArrayList<>();

        for (var variable : methodDecl.getChildren("VarDeclaration")) {
            String name = variable.get("name");
            Type type = getType(variable.getChildren(TYPE).get(0));
            symbols.add(new Symbol(type, name));
        }
        return symbols;
    }
}
