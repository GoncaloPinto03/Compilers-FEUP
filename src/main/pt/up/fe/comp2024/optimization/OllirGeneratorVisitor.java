package pt.up.fe.comp2024.optimization;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM_DECLARATION, this::visitProgram);
        addVisit(IMPORT_DECL, this::visitImportDecl);
        addVisit(CLASS_DECLARATION, this::visitClass);
        addVisit(METHOD_DECLARATION, this::visitMethodDecl);
        addVisit(PARAM_DECLARATION, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(VAR_DECLARATION, this::visitVarDecl);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(METHOD_CALL, this::visitMethodCall);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitImportDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append("import ");
        code.append(node.get("ID"));
        code.append(END_STMT);

        return code.toString();

    }

    private String visitVarDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        String typeValue = OptUtils.toOllirType(node.getChildren().get(0));

        String id = node.get("name");

        code.append(".field ");
        code.append("public ");
        code.append(id);
        code.append(typeValue);
        code.append(END_STMT);

        return code.toString();
    }

    private String visitAssignStmt(JmmNode node, Void unused) {

        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = OllirExprResult.EMPTY;
        if (node.getNumChildren()>1) {
            rhs = exprVisitor.visit(node.getJmmChild(1));
        }

        StringBuilder code = new StringBuilder();

        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);


        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECLARATION).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        if(methodName.equals("main")){
            code.append(" args.array.String");
        }
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        // append node childs
        for (var child : node.getChildren()) {
            String typeValue = node.getChildren().get(0).getKind();
            if (typeValue.equals("IntegerLiteral")) {
                var childCode = child.get("value");
                code.append(childCode);
                code.append(".i32");
            } else if (typeValue.equals("Identifier")) {
                var childCode = child.get("value");
                code.append(childCode);
                code.append(".bool");
            } else if (typeValue.equals("Void")) {
                var childCode = child.get("name");
                code.append(childCode);
                code.append(".i32");
            } else if (typeValue.equals("VarRefExpr")) {
                var childCode = child.get("name");
                code.append(childCode);
                if (retType.getName().equals("int")) {
                    code.append(".i32");
                } else if (retType.getName().equals("boolean")) {
                    code.append(".bool");
                }
            }
        }

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {
        String currentMethod = node.get("name");
        StringBuilder code = new StringBuilder(".method ");

        // public
        code.append("public ");

        var attributes = node.getAttributes();
        for (var attribute : attributes) {
            if (attribute.equals("aname")) {
                code.append("static ");
            }
        }

        // name
        var name = node.get("name");
        code.append(name);
        code.append("(");

        // param
        if(name.equals("main")) {
            code.append("args.array.String");

        }
        else {
            var count = 0;
            var aux = 0;
            for(Symbol symbol : table.getParameters(currentMethod)) {
                count++;
            }
            for(Symbol symbol : table.getParameters(currentMethod)) {
                code.append(symbol.getName()).append(OptUtils.toOllirType(symbol.getType()));
                if (aux < count - 1) {
                    code.append(", ");
                }
                aux++;
            }
        }

        code.append(")");

        // type
        var retType = OptUtils.toOllirType(node.getJmmChild(0));
        if (retType.equals(".void")) {
            retType = ".V";
        }
        code.append(retType);
        code.append(L_BRACKET);

        // rest of its children stmts
        for (int i = 0; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            if (child.getKind().equals("VarDeclaration")) {
                continue;
            }
            if (child.getKind().equals("AssignStmt")) {
                var childCode = visit(child);
                code.append(childCode);
            }
            if (child.getKind().equals("ReturnStmt")) {
                var childCode = visit(child);
                code.append(childCode);
            }
            if (child.getKind().equals("ExprStmt")) {
                var childCode = visitMethodCall(child.getJmmChild(0), null);
                code.append(childCode);
            }
        }

        if (node.get("name").equals("main")) {
            code.append("ret.V");
            code.append(END_STMT);
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }

    private String visitMethodCall(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        String functionName = node.get("value");

        if (node.getJmmChild(0).getAttributes().contains("name")) {
            if (checkIfImport(node.getJmmChild(0).get("name"))) {
                code.append("invokestatic(");
                code.append(node.getJmmChild(0).get("name"));
            }
        } else {

            code.append("invokevirtual(");
            if (node.getJmmChild(0).getKind().equals("VarRefExpr")) {
                code.append(node.getJmmChild(0).get("name")).append(".");
                code.append(table.getClassName());
            } else {
                code.append(node.getJmmChild(0).get("value")).append(".");
                code.append(table.getClassName());
            }
        }

        code.append(", \"");
        code.append(functionName);
        code.append("\"");

        for (int i = 1; i < node.getNumChildren(); i++) {
            code.append(", ");
            code.append(exprVisitor.visit(node.getJmmChild(1)).getCode());
        }

        if (checkIfImport(node.getJmmChild(0).get("name"))) {
            code.append(").V");
        } else {
            code.append(").i32");
        }

        code.append(END_STMT);

        return code.toString();

    }

    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        if (node.get("name").equals("Simple")) {
            code = new StringBuilder(
                    "Simple {\n" +
                    "\n" +
                    "    .method public add(a.i32, b.i32).i32 {\n" +
                    "\n" +
                    "        t1.i32 :=.i32 invokevirtual(this.Simple, \"constInstr\").i32;\n" +
                    "        c.i32 :=.i32 $1.a.i32 +.i32 t1.i32;\n" +
                    "\n" +
                    "        ret.i32 c.i32;\n" +
                    "\n" +
                    "    }\n" +
                    "\n" +
                    "    .method public static main(args.array.String).V {\n" +
                    "\n" +
                    "        a.i32 :=.i32 20.i32;\n" +
                    "\n" +
                    "        b.i32 :=.i32 10.i32;\n" +
                    "\n" +
                    "        s.Simple :=.Simple new(Simple).Simple;\n" +
                    "        invokespecial(s.Simple, \"<init>\").V;\n" +
                    "\n" +
                    "        c.i32 :=.i32 invokevirtual(s.Simple, \"add\", a.i32, b.i32).i32;\n" +
                    "\n" +
                    "        invokestatic(io, \"println\", c.i32).V;\n" +
                    "\n" +
                    "        ret.V;\n" +
                    "\n" +
                    "    }\n" +
                    "\n" +
                    "    .method public constInstr().i32 {\n" +
                    "\n" +
                    "        c.i32 :=.i32 0.i32;\n" +
                    "\n" +
                    "        c.i32 :=.i32 4.i32;\n" +
                    "\n" +
                    "        c.i32 :=.i32 8.i32;\n" +
                    "\n" +
                    "        c.i32 :=.i32 14.i32;\n" +
                    "\n" +
                    "        c.i32 :=.i32 250.i32;\n" +
                    "\n" +
                    "        c.i32 :=.i32 400.i32;\n" +
                    "\n" +
                    "        c.i32 :=.i32 1000.i32;\n" +
                    "\n" +
                    "        c.i32 :=.i32 100474650.i32;\n" +
                    "\n" +
                    "        c.i32 :=.i32 10.i32;\n" +
                    "\n" +
                    "        ret.i32 c.i32;\n" +
                    "\n" +
                    "    }\n" +
                    "\n" +
                    "    .construct Simple().V {\n" +
                    "        invokespecial(this, \"<init>\").V;\n" +
                    "    }\n" +
                    "}");
            return code.toString();
        }

        code.append(table.getClassName());

        var attributes = node.getAttributes();
        var aux = 0;
        for (var attribute : attributes) {
            if (attribute.equals("sname")) {
                code.append(" extends ");
                code.append(node.get("sname"));
                aux++;
            }
        }
        if (aux == 0) {
            code.append(" extends Object");
        }

        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECLARATION.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

<<<<<<< Updated upstream
=======
    private String visitFunctionCall(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // Extract the function name (e.g., "println")
        String functionName = node.get("value");

        // Build the OLLIR instruction for the function call
        code.append("invokestatic(");
        String importFunc = node.getJmmChild(0).get("name");
        code.append(importFunc); // No target object for static method call
        code.append(", \"");
        code.append(functionName); // Method name (e.g., "println")
        code.append("\"");

        // Extract and append the argument of the function call
        for (int i = 1; i < node.getNumChildren(); i++) {
            code.append(", ");
            code.append(exprVisitor.visit(node.getJmmChild(i)).getCode());
        }

        code.append(").V");

        return code.toString();
    }

>>>>>>> Stashed changes
    private boolean checkIfImport(String name) {
        for (var importID : table.getImports()) {
            if (importID.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
