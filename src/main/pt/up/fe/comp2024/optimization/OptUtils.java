package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Instruction;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class OptUtils {
    private static int tempNumber = 0;

    private static int ifLabel = 0;

    private static int endIfLabel = 0;

    private static int whileCondLabel = 0;
    private static int whileLoopLabel = 0;

    private static int whileEndLabel = 0;

    private static int and_true = 0;

    private static int and_end = 0;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getIfLabel() {
        return getIfLabel("if");
    }
    public static String getEndIfLabel() {
        return getEndIfLabel("endif");
    }

    public static String getWhileCondLabel() {
        return getWhileCondLabel("whileCond");
    }

    public static String getWhileLoopLabel() {
        return getWhileLoopLabel("whileLoop");
    }

    public static String getWhileEndLabel() {
        return getWhileEndLabel("whileEnd");
    }

    public static String getAndTrue() {
        return getAndTrue("true_");
    }

    public static String getAndEnd() {
        return getAndEnd("end_");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static String getIfLabel(String prefix) {

        return prefix + getNextIfLabel();
    }

    public static String getEndIfLabel(String prefix) {
        return prefix + getNextEndIfLabel();
    }

    public static String getWhileCondLabel(String prefix) {
        return prefix + getNextWhileCondLabel();
    }

    public static String getWhileLoopLabel(String prefix) {
        return prefix + getNextWhileLoopLabel();
    }

    public static String getWhileEndLabel(String prefix) {
        return prefix + getNextWhileEndLabel();
    }


    public static String getAndTrue(String prefix) {
        return prefix + getNextAndTrue();
    }

    public static String getAndEnd(String prefix) {
        return prefix + getNextAndEnd();
    }
    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static int getNextIfLabel() {

        ifLabel += 1;
        return ifLabel;
    }

    public static int getNextEndIfLabel() {
        endIfLabel += 1;
        return endIfLabel;
    }

    public static int getNextWhileCondLabel() {
        whileCondLabel += 1;
        return whileCondLabel;
    }

    public static int getNextWhileLoopLabel() {
        whileLoopLabel += 1;
        return whileLoopLabel;
    }

    public static int getNextWhileEndLabel() {
        whileEndLabel += 1;
        return whileEndLabel;
    }


    public static int getNextAndTrue() {
        and_true += 1;
        return and_true;
    }

    public static int getNextAndEnd() {
        and_end += 1;
        return and_end;
    }
    public static String getCurrentTemp() {

        return "tmp" + tempNumber;
    }

    public static String getCurrentIfLabel() {

        return "if" + ifLabel;
    }

    public static String getCurrentEndIfLabel() {
        return "endif" + endIfLabel;
    }

    public static String getCurrentWhileCondLabel() {
        return "whileCond" + whileCondLabel;
    }

    public static String getCurrentWhileLoopLabel() {
        return "whileLoop" + whileLoopLabel;
    }

    public static String getCurrentWhileEndLabel() {
        return "whileEnd" + whileEndLabel;
    }


    public static String getCurrentAndTrue() {
        return "true_" + and_true;
    }

    public static String getCurrentAndEnd() {
        return "end_" + and_end;
    }
    public static String toOllirType(JmmNode typeNode) {

        if (typeNode.getKind().equals("IntegerLiteral")) {
            return "i32";
        } else if (typeNode.getKind().equals("VarRefExpr")) {
            return "i32";
        }
        // check if it has
        if (typeNode.getAttributes().contains("isArray")) {
            if (typeNode.get("isArray").equals("true")) {
                return ".array" + toOllirType(typeNode.get("value"));
            }
        } else if (typeNode.getKind().equals("ArrayDeclaration")) {
            return toOllirType(typeNode.getChildren().get(0));
        }

        String typeName = typeNode.get("value");

        return toOllirType(typeName);
    }


    public static String toOllirType(Type type) {
        if (type.isArray()) {
            return ".array" + toOllirType(type.getName());
        }
        return toOllirType(type.getName());
    }

    private static String toOllirType(String typeName) {

        String type = "." + switch (typeName) {
            case "int" -> "i32";
            case "boolean" -> "bool";
            case "void" -> "void";
            case "IntegerLiteral" -> "i32";
            default -> typeName;
        };

        return type;
    }


}
