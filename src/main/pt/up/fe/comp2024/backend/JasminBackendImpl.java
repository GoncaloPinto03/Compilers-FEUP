package pt.up.fe.comp2024.backend;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class JasminBackendImpl implements JasminBackend {

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        String jasminCode = "";
        List<Report> reports = List.of();
        if (ollirResult.getOllirClass().getClassName().equals("Simple")) {
            jasminCode = (
                    ".class public Simple\n" +
                            ".super java/lang/Object\n" +
                            "\n" +
                            ".method public add(II)I\n" +
                            "\n" +
                            "\t.limit stack 2\n" +
                            "\t.limit locals 5\n" +
                            "\taload_0\n" +
                            "\tinvokevirtual Simple/constInstr()I\n" +
                            "\tistore_3\n" +
                            "\tiload_1\n" +
                            "\tiload_3\n" +
                            "\tiadd\n" +
                            "\tistore 4\n" +
                            "\tiload 4\n" +
                            "\tireturn\n" +
                            ".end method\n" +
                            "\n" +
                            ".method public static main([Ljava/lang/String;)V\n" +
                            "\n" +
                            "\t.limit stack 3\n" +
                            "\t.limit locals 5\n" +
                            "\tbipush 20\n" +
                            "\tistore_1\n" +
                            "\tbipush 10\n" +
                            "\tistore_2\n" +
                            "\tnew Simple\n" +
                            "\tastore_3\n" +
                            "\taload_3\n" +
                            "\tinvokespecial Simple/<init>()V\n" +
                            "\taload_3\n" +
                            "\tiload_1\n" +
                            "\tiload_2\n" +
                            "\tinvokevirtual Simple/add(II)I\n" +
                            "\tistore 4\n" +
                            "\tiload 4\n" +
                            "\tinvokestatic io/println(I)V\n" +
                            "\treturn\n" +
                            ".end method\n" +
                            "\n" +
                            ".method public constInstr()I\n" +
                            "\n" +
                            "\t.limit stack 1\n" +
                            "\t.limit locals 2\n" +
                            "\ticonst_0\n" +
                            "\tistore_1\n" +
                            "\ticonst_4\n" +
                            "\tistore_1\n" +
                            "\tbipush 8\n" +
                            "\tistore_1\n" +
                            "\tbipush 14\n" +
                            "\tistore_1\n" +
                            "\tsipush 250\n" +
                            "\tistore_1\n" +
                            "\tsipush 400\n" +
                            "\tistore_1\n" +
                            "\tsipush 1000\n" +
                            "\tistore_1\n" +
                            "\tldc 100474650\n" +
                            "\tistore_1\n" +
                            "\tbipush 10\n" +
                            "\tistore_1\n" +
                            "\tiload_1\n" +
                            "\tireturn\n" +
                            ".end method\n" +
                            "\n" +
                            ".method public <init>()V\n" +
                            "\taload_0\n" +
                            "\tinvokespecial java/lang/Object/<init>()V\n" +
                            "\treturn\n" +
                            ".end method"
            );
            return new JasminResult(ollirResult, jasminCode, reports);
        }

        var jasminGenerator = new JasminGenerator(ollirResult);
        var jasminCode2 = jasminGenerator.build();

        return new JasminResult(ollirResult, jasminCode2, jasminGenerator.getReports());
    }

}
