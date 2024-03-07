package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;
import java.util.stream.Collectors;

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


        //SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);


        return new JmmSymbolTable(className , methods, imports, returnTypes, params, locals, superClass, fields);

    }

    public static String dealWithImport(JmmNode jmmNode){
        String importList = jmmNode.get("importValue"); // import List comes as String
        String importStr = importList.substring(1, importList.length()-1); // need to remove [ and ]
        String _import = String.join(".", importStr.split(", ")); // split each import and join them

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

    // implment get type
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
                List<String> parameters = param.getObjectAsList("name",String.class);
                for (int i = 0; i < param.getChildren().size(); i++) {
                    var some = param.getChild(i);
                    String type = some.getKind();
                    switch (type) {
                        case "IntArrayType":
                            paramList.add(new Symbol(new Type(some.get("tName"), true), parameters.get(i)));
                            break;
                        case "IntType":
                            paramList.add(new Symbol(new Type(some.get("tName"), false), parameters.get(i)));
                            break;
                        case "BoolType":
                            paramList.add(new Symbol(new Type(some.get("tName"), false), parameters.get(i)));
                            break;
                        case "StringType":
                            paramList.add(new Symbol(new Type(some.get("tName"), false), parameters.get(i)));
                            break;
                        default:
                            paramList.add(new Symbol(new Type(some.get("type"), false), parameters.get(i)));
                            break;
                    }
                }
            }
            params.put(methodDecl.get("name"), new ArrayList<>(paramList));
        }
        return params;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        return null;
        /*
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;

         */
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }

     private static List<Symbol> buildFields(JmmNode classDecl) {
         return classDecl.getChildren(VAR_DECL).stream().map(JmmSymbolTableBuilder::createSymbolFromVarDecl).collect(Collectors.toList());
     }

       private static Symbol createSymbolFromVarDecl(JmmNode varDecl) {
           Type type = new Type(varDecl.getChild(0).get("tname"), varDecl.getChild(0).getObject("isArray", Boolean.class));
            return new Symbol(type, varDecl.get("varName"));
       }

    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        // TODO: Simple implementation that needs to be expanded

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(intType, varDecl.get("name")))
                .toList();
    }

}
