package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.ClassUnit;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class JasminBackendImpl implements JasminBackend {
    ClassUnit classUnit;
    StringBuilder jasminCode;
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        this.classUnit = ollirResult.getOllirClass();

        this.jasminCode = new StringBuilder();

        var jasminGenerator = new JasminGenerator(ollirResult);
        var jasminCode = jasminGenerator.build();

        return new JasminResult(ollirResult, jasminCode, jasminGenerator.getReports());
    }
}