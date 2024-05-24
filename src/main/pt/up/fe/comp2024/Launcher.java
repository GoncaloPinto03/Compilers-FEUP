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

        OllirResult ollirResult = new OllirResult("import ioPlus;\n" +
                "IfWhileNested {\n" +
                "\n" +
                "    .construct IfWhileNested().V {\n" +
                "        invokespecial(this, \"<init>\").V;\n" +
                "    }\n" +
                "\n" +
                "    .method public func(a.i32).i32 {\n" +
                "flag.bool :=.bool 1.bool;\n" +
                "i.i32 :=.i32 0.i32;\n" +
                "if (i.i32 <.bool a.i32) goto whilebody_1;\n" +
                "goto endwhile_1;\n" +
                "whilebody_1:\n" +
                "\n" +
                "if (flag.bool) goto ifbody_0;\n" +
                "    invokestatic(ioPlus, \"printResult\", 2.i32).V;\n" +
                "goto endif_0;\n" +
                "ifbody_0:\n" +
                "    invokestatic(ioPlus, \"printResult\", 1.i32).V;\n" +
                "endif_0:\n" +
                "flag.bool :=.bool !.bool flag.bool;\n" +
                "i.i32 :=.i32 i.i32 +.i32 1.i32;\n" +
                "if (i.i32 <.bool a.i32) goto whilebody_1;\n" +
                "endwhile_1:\n" +
                "ret.i32 1.i32;\n" +
                "    }\n" +
                "    .method public static main(args.array.String).V {\n" +
                "d.IfWhileNested :=.IfWhileNested new(IfWhileNested).IfWhileNested;\n" +
                "invokespecial(d.IfWhileNested,\"<init>\").V;\n" +
                "a.i32 :=.i32 invokevirtual(d.IfWhileNested, \"func\", 3.i32).i32;\n" +
                "\n" +
                "ret.V;\n" +
                "    }\n" +
                "\n" +
                "}", null);
        JasminBackendImpl jasminGen = new JasminBackendImpl();
        JasminResult jasminResult = jasminGen.toJasmin( ollirResult);
        TestUtils.noErrors(jasminResult.getReports());
//
        // Print Jasmin code
        System.out.println(jasminResult.getJasminCode());
    }

}
