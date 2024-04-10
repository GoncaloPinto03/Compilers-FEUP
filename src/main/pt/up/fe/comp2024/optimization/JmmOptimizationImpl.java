package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.symboltable.JmmSymbolTable;


import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        var ollirVisitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        ollirVisitor.visit(semanticsResult.getRootNode());
        String ollirCode = ollirVisitor.toString();

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());

    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {

        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }
}
