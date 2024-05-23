package pt.up.fe.comp2024;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2024.analysis.JmmAnalysisImpl;
import pt.up.fe.comp2024.backend.JasminBackendImpl;
import pt.up.fe.comp2024.optimization.JmmOptimizationImpl;
import pt.up.fe.comp2024.parser.JmmParserImpl;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

import java.util.Map;

public class Launcher {

    public static void main(String[] args) {
//        SpecsSystem.programStandardInit();
//
//        Map<String, String> config = CompilerConfig.parseArgs(args);
//
//        var inputFile = CompilerConfig.getInputFile(config).orElseThrow();
//        if (!inputFile.isFile()) {
//            throw new RuntimeException("Option '-i' expects a path to an existing input file, got '" + args[0] + "'.");
//        }
//        String code = SpecsIo.read(inputFile);
//
//        // Parsing stage
//        JmmParserImpl parser = new JmmParserImpl();
//        JmmParserResult parserResult = parser.parse(code, config);
//        TestUtils.noErrors(parserResult.getReports());
//
//        // Print AST
//        System.out.println(parserResult.getRootNode().toTree());
//
//        // Semantic Analysis stage
//        JmmAnalysisImpl sema = new JmmAnalysisImpl();
//        JmmSemanticsResult semanticsResult = sema.semanticAnalysis(parserResult);
//        TestUtils.noErrors(semanticsResult.getReports());
//
//
//        // Optimization stage
//        JmmOptimizationImpl ollirGen = new JmmOptimizationImpl();
//        OllirResult ollirResult = ollirGen.toOllir(semanticsResult);
//        TestUtils.noErrors(ollirResult.getReports());
//
//        // Print OLLIR code
//        System.out.println(ollirResult.getOllirCode());

//        // Code generation stage

        OllirResult ollirResult = new OllirResult("import io;\n" +
                "\n" +
                "Arithmetic_and{\n" +
                "\n" +
                "   .construct Arithmetic_and().V {\n" +
                "       invokespecial(this, \"<init>\").V;\n" +
                "   }\n" +
                "\n" +
                "   .method public static main(args.array.String).V {\n" +
                "      tmp0.Arithmetic_and :=.Arithmetic_and new(Arithmetic_and).Arithmetic_and;\n" +
                "      invokespecial(tmp0.Arithmetic_and, \"<init>\").V;\n" +
                "      c.Arithmetic_and :=.Arithmetic_and tmp0.Arithmetic_and;\n" +
                "      if(1.bool) goto true_0;\n" +
                "      tmp1.bool :=.bool 0.bool;\n" +
                "      goto end_0;\n" +
                "      true_0:\n" +
                "      tmp2.bool :=.bool invokevirtual(c.Arithmetic_and, \"p\", 1.i32).bool;\n" +
                "      tmp1.bool :=.bool tmp2.bool;\n" +
                "      end_0:\n" +
                "      a.bool :=.bool tmp1.bool;\n" +
                "      ret.V ;\n" +
                "   }\n" +
                "\n" +
                "   .method p(value.i32).bool {\n" +
                "      invokestatic(io, \"print\", value.i32).V;\n" +
                "      ret.bool 1.bool;\n" +
                "   }\n" +
                "}", null);
        JasminBackendImpl jasminGen = new JasminBackendImpl();
        JasminResult jasminResult = jasminGen.toJasmin( ollirResult);
        TestUtils.noErrors(jasminResult.getReports());
//
        // Print Jasmin code
        System.out.println(jasminResult.getJasminCode());
    }

}
