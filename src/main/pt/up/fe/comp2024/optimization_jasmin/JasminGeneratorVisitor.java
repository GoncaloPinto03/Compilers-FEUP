package pt.up.fe.comp2024.optimization_jasmin;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JasminGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final SymbolTable table;


    private JasminExprGeneratorVisitor exprGenerator;

    private String currentMethod;
    private int nextRegister;

    private Map<String, Integer> currentRegisters;

    public JasminGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.exprGenerator = null;
        currentMethod = null;
        nextRegister = -1;
        currentRegisters = null;
    }


    @Override
    protected void buildVisitor() {
        // Using strings to avoid compilation problems in projects that
        // might no longer have the equivalent enums in Kind class.
        addVisit("Program", this::visitProgram);
        addVisit("ClassDecl", this::visitClassDecl);
        addVisit("MethodDecl", this::visitMethodDecl);
        addVisit("AssignStmt", this::visitAssignStmt);
        addVisit("ReturnStmt", this::visitReturnStmt);
        addVisit("IfStmt", this::visitIfStmt); // Add visitor for IfStmt
        addVisit("ElseStmt", this::visitElseStmt); // Add visitor for ElseStmt
        addVisit("Type", this::visitType); // Add visitor for ElseStmt
    }


    private String visitProgram(JmmNode program, Void unused) {

        // Get class decl node
        var classDecl = program.getChild(0);
        SpecsCheck.checkArgument(classDecl.isInstance("ClassDecl"), () -> "Expected a node of type 'ClassDecl', but instead got '" + classDecl.getKind() + "'");

        return visit(classDecl);
    }

    private String visitClassDecl(JmmNode classDecl, Void unused) {
        var code = new StringBuilder();

        // generate class name
//        var className = table.getClassName();
//        code.append(".class ").append(className).append(NL).append(NL);

        // check id
        String extendedClass = classDecl.hasAttribute("sname") ? classDecl.get("sname") : "";
        code.append(extendedClass);

//        String superClass = classDecl.getClass().getSuperclass() != null ? classDecl.getClass().getSuperclass().toString() : "java/lang/Object";
//        code.append(".super ").append(superClass).append(NL);

        // generate a single constructor method
        var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial java/lang/Object/<init>()V
                    return
                .end method
                """;
        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : classDecl.getChildren("MethodDecl")) {
            code.append(visit(method));
        }

        return code.toString();
    }

    private String visitMethodDecl(JmmNode methodDecl, Void unused) {
        var methodName = methodDecl.get("name");


        // set method
        currentMethod = methodName;

        // set next register that can be used
        // if method is static, then can start at 0
        // if method is not static, 0 contains 'this', and must start at 1
        // for the initial language, there are no static methods
        nextRegister = 1;

        // initialize register map and set parameters
        currentRegisters = new HashMap<>();
        for (var param : methodDecl.getChildren("Param")) {
            currentRegisters.put(param.get("name"), nextRegister);
            nextRegister++;
        }

        exprGenerator = new JasminExprGeneratorVisitor(currentRegisters);

        var code = new StringBuilder();

        // calculate modifier
        var modifier = methodDecl.getObject("isPublic", Boolean.class) ? "public " : "";


        // TODO: Hardcoded param types and return type, needs to be expanded
        code.append("\n.method ").append(modifier).append(methodName).append("(I)I").append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var stmt : methodDecl.getChildren("Stmt")) {
            // Get code for statement, split into lines and insert the necessary indentation
            var instCode = StringLines.getLines(visit(stmt)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // reset information
        exprGenerator = null;
        nextRegister = -1;
        currentRegisters = null;
        currentMethod = null;

        return code.toString();
    }

    private String visitAssignStmt(JmmNode assignStmt, Void unused) {
        var code = new StringBuilder();

        // generate code that will put the value on the right on top of the stack
        exprGenerator.visit(assignStmt.getChild(1), code);

        // store value in top of the stack in destination
        var lhs = assignStmt.getChild(0);
        SpecsCheck.checkArgument(lhs.isInstance("VarRefExpr"), () -> "Expected a node of type 'VarRefExpr', but instead got '" + lhs.getKind() + "'");

        var destName = lhs.get("name");

        // get register
        var reg = currentRegisters.get(destName);

        // If no mapping, variable has not been assigned yet, create mapping
        if (reg == null) {
            reg = nextRegister;
            currentRegisters.put(destName, reg);
            nextRegister++;
        }

        // TODO: Hardcoded for int type, needs to be expanded
        code.append("istore ").append(reg).append(NL);

        return code.toString();
    }

    private String visitReturnStmt(JmmNode returnStmt, Void unused) {

        var code = new StringBuilder();

        // TODO: Hardcoded to always return an int type, needs to be expanded

        // generate code that will put the value of the return on the top of the stack
        exprGenerator.visit(returnStmt.getChild(0), code);
        code.append("ireturn").append(NL);

        return code.toString();
    }

    private String visitIfStmt(JmmNode ifStmt, Void unused) {
        var code = new StringBuilder();
        var condition = ifStmt.getChild(0);
        var thenStmt = ifStmt.getChild(1);
        var elseStmt = ifStmt.getChildren().size() > 2 ? ifStmt.getChild(2) : null;

        var thenLabel = "if_true_" + System.nanoTime();
        var elseLabel = "else_" + System.nanoTime();
        var endLabel = "endif_" + System.nanoTime();

        exprGenerator.visit(condition, code);

        code.append("iflt ").append(thenLabel).append(NL);

        if (elseStmt != null) {
            code.append(visit(elseStmt));
            code.append("goto ").append(endLabel).append(NL);
        }

        code.append(thenLabel).append(":").append(NL);
        code.append(visit(thenStmt));

        code.append(endLabel).append(":").append(NL);

        return code.toString();
    }

    private String visitElseStmt(JmmNode elseStmt, Void unused) {
        var code = new StringBuilder();

        for (var stmt : elseStmt.getChildren()) {
            var instCode = StringLines.getLines(visit(stmt)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));
            code.append(instCode);
        }

        return code.toString();
    }

    private String visitType(JmmNode jmmNode, Void unused) {
        return ((Boolean) jmmNode.getObject("isArray")) ? jmmNode.get("value") + "[]" : jmmNode.get("value");
    }
}