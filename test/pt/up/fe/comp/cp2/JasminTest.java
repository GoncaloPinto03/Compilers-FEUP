package pt.up.fe.comp.cp2;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class JasminTest {

    // new test
    @Test
    public void testHelloWorld() {
        var code = SpecsIo.getResource("pt/up/fe/comp/cp2/apps/HelloWorld.jmm");
        var jasminResult = TestUtils.backend(code, Collections.emptyMap());
        System.out.println(jasminResult.getJasminCode());
        var result = TestUtils.runJasmin(jasminResult.getJasminCode(), Collections.emptyMap());
        assertEquals("Hello, World!", result.strip());
    }

    @Test
    public void ollirToJasminBasic() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminBasic.ollir");
    }

    @Test
    public void ollirToJasminArithmetics() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminArithmetics.ollir");
    }

    @Test
    public void ollirToJasminInvoke() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminInvoke.ollir");
    }

    @Test
    public void ollirToJasminFields() {
        testOllirToJasmin("pt/up/fe/comp/cp2/jasmin/OllirToJasminFields.ollir");
    }

//    @Test
//    public void t() {
//        var jasminCode= ".class Test\n" +
//                ".super java/lang/Object\n" +
//                ";default constructor\n" +
//                ".method public <init>()V\n" +
//                "    aload_0\n" +
//                "    invokespecial java/lang/Object/<init>()V\n" +
//                "    return\n" +
//                ".end method\n" +
//                ".method public static main([Ljava/lang/String;)V\n" +
//                "   .limit stack 99\n" +
//                "   .limit locals 99\n" +
//                "   \n" +
//                "   return\n" +
//                ".end method\n" +
//                ".method public foo()I\n" +
//                "   .limit stack 99\n" +
//                "   .limit locals 99\n" +
//                "   ldc 1\n" +
//                "   istore 1\n" +
//                "   ldc 2\n" +
//                "   istore 2\n" +
//                "   iload 1\n" +
//                "   iload 2\n" +
//                "   iadd\n" +
//                "   istore 3\n" +
//                "   iload 3\n" +
//                "   \n" +
//                "   ireturn\n" +
//                ".end method";
//
//        new JasminResult(jasminCode).run();
//    }


    public static void testOllirToJasmin(String resource, String expectedOutput) {
        JasminResult result = null;

        // If AstToJasmin pipeline, change name of the resource and execute other test
        if (TestUtils.hasAstToJasminClass()) {

            // Rename resource
            var jmmResource = SpecsIo.removeExtension(resource) + ".jmm";

            // Test Jmm resource
            result = TestUtils.backend(SpecsIo.getResource(jmmResource));

        } else {

            var ollirResult = new OllirResult(SpecsIo.getResource(resource), Collections.emptyMap());

            result = TestUtils.backend(ollirResult);
        }

        
        var testName = new File(resource).getName();
        System.out.println(testName + ":\n" + result.getJasminCode());
        var runOutput = result.runWithFullOutput();
        assertEquals("Error while running compiled Jasmin: " + runOutput.getOutput(), 0, runOutput.getReturnValue());
        System.out.println("\n Result: " + runOutput.getOutput());

        if (expectedOutput != null) {
            assertEquals(expectedOutput, runOutput.getOutput());
        }
    }

    public static void testOllirToJasmin(String resource) {
        testOllirToJasmin(resource, null);
    }
}
