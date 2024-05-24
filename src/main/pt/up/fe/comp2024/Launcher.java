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
                "ArrayVarargs{\n" +
                "\n" +
                "   .construct ArrayVarargs().V {\n" +
                "       invokespecial(this, \"<init>\").V;\n" +
                "   }\n" +
                "\n" +
                "   .method public static main(args.array.String).V {\n" +
                "      tmp0.ArrayVarargs :=.ArrayVarargs new(ArrayVarargs).ArrayVarargs;\n" +
                "      invokespecial(tmp0.ArrayVarargs, \"<init>\").V;\n" +
                "      a.ArrayVarargs :=.ArrayVarargs tmp0.ArrayVarargs;\n" +
                "      invokevirtual(a.ArrayVarargs, \"bar\").i32;\n" +
                "      ret.V ;\n" +
                "   }\n" +
                "\n" +
                "   .method foo(a.array.i32).i32 {\n" +
                "      tmp1.i32 :=.i32 a.array.i32[0.i32].i32;\n" +
                "      ret.i32 tmp1.i32;\n" +
                "   }\n" +
                "\n" +
                "   .method bar().i32 {\n" +
                "      tmp2.array.i32 :=.array.i32 new(array, 3.i32).array.i32;\n" +
                "      __varargs_array_0.array.i32 :=.array.i32 tmp2.array.i32;\n" +
                "      __varargs_array_0.array.i32[0.i32].i32 :=.i32 1.i32;\n" +
                "      __varargs_array_0.array.i32[1.i32].i32 :=.i32 2.i32;\n" +
                "      __varargs_array_0.array.i32[2.i32].i32 :=.i32 3.i32;\n" +
                "      res.i32 :=.i32 invokevirtual(this.ArrayVarargs, \"foo\", __varargs_array_0.array.i32).i32;\n" +
                "      invokestatic(io, \"println\", res.i32).V;\n" +
                "      tmp3.array.i32 :=.array.i32 new(array, 1.i32).array.i32;\n" +
                "      __varargs_array_1.array.i32 :=.array.i32 tmp3.array.i32;\n" +
                "      __varargs_array_1.array.i32[0.i32].i32 :=.i32 4.i32;\n" +
                "      res.i32 :=.i32 invokevirtual(this.ArrayVarargs, \"foo\", __varargs_array_1.array.i32).i32;\n" +
                "      invokestatic(io, \"println\", res.i32).V;\n" +
                "      ret.i32 res.i32;\n" +
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
