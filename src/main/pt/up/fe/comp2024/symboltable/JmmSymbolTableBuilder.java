package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.specs.util.SpecsCheck;
import java.util.*;
import static pt.up.fe.comp2024.ast.Kind.*;

import java.util.List;
import java.util.Map;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.specs.util.SpecsCheck;
import java.util.ArrayList;
import java.util.HashMap;

public class JmmSymbolTableBuilder {

    public static JmmSymbolTable build(JmmNode root) {
        var classDecl = root.getChildren(Kind.CLASS_DECLARATION).get(0);
        SpecsCheck.checkArgument(Kind.CLASS_DECLARATION.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");
        var imports = buildImports(root);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        String superClass = classDecl.getOptional("sname").orElse(null);
        List<Symbol> fields = buildFields(classDecl); // Convert ExtendedSymbol to Symbol

        return new JmmSymbolTable(className, methods, imports, returnTypes, params, locals, superClass, fields);
    }

    private static List<String> buildImports(JmmNode root) {
        List<String> imports = new ArrayList<>();
        root.getChildren("ImportDeclaration").stream()
                .map(importNode -> dealWithImport(importNode))
                .forEach(imports::add);
        return imports;
    }

    private static String dealWithImport(JmmNode importNode) {
        String importList = importNode.get("importValue");
        String[] imports = importList.split(", ");
        String lastImport = imports[imports.length - 1]; // Obtém o último elemento
        if (lastImport.startsWith("[")) { // Verifica se o último elemento começa com '[A'
            lastImport = lastImport.substring(1); // Remove os dois primeiros caracteres ('[A')
        }
        if (lastImport.endsWith("]")) { // Verifica se o último elemento termina com ']'
            lastImport = lastImport.substring(0, lastImport.length() - 1); // Remove o último caractere (']')
        }
        return lastImport;
    }


    private static List<String> buildMethods(JmmNode classDecl) {
        return classDecl.getChildren(Kind.METHOD_DECLARATION).stream()
                .map(method -> method.get("name"))
                .toList();
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();
        for (var method : classDecl.getChildren(Kind.METHOD_DECLARATION)) {
            String methName = method.get("name");
            JmmNode retType = method.getChildren(Kind.TYPE).get(0);
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
        for (var methodDecl : classDecl.getChildren(Kind.METHOD_DECLARATION)) {
            List<Symbol> paramList = new ArrayList<>();
            for (var param : methodDecl.getChildren("Param")) {
                String name = param.get("name");
                Type type = getType(param.getChildren(Kind.TYPE).get(0));
                paramList.add(new Symbol(type, name));
            }
            params.put(methodDecl.get("name"), paramList);
        }
        return params;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();
        classDecl.getChildren(Kind.METHOD_DECLARATION).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));
        return map;
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        List<Symbol> symbols = new ArrayList<>();
        for (var variable : classDecl.getChildren("VarDeclaration")) {
            String name = variable.get("name");
            Type type = getType(variable.getChildren(Kind.TYPE).get(0));
            symbols.add(new Symbol(type, name));
        }
        return symbols;
    }

    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        List<Symbol> symbols = new ArrayList<>();
        for (var variable : methodDecl.getChildren("VarDeclaration")) {
            String name = variable.get("name");
            Type type = getType(variable.getChildren(Kind.TYPE).get(0));
            symbols.add(new Symbol(type, name));
        }
        return symbols;
    }

    private static List<Symbol> convertToSymbols(List<ExtendedSymbol> extendedSymbols) {
        List<Symbol> symbols = new ArrayList<>();
        for (ExtendedSymbol extendedSymbol : extendedSymbols) {
            symbols.add(extendedSymbol.getSymbol());
        }
        return symbols;
    }

    private static class ExtendedSymbol {
        private final Symbol symbol;
        private final String accessModifier;

        public ExtendedSymbol(Symbol symbol, String accessModifier) {
            this.symbol = symbol;
            this.accessModifier = accessModifier;
        }

        public Symbol getSymbol() {
            return symbol;
        }

        public String getAccessModifier() {
            return accessModifier;
        }
    }
}
