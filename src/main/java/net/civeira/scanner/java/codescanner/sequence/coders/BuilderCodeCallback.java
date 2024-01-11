package net.civeira.scanner.java.codescanner.sequence.coders;

import com.github.javaparser.ast.expr.MethodCallExpr;
import net.civeira.scanner.java.codescanner.sequence.CodeSpecificCallback;
import net.civeira.scanner.java.codescanner.sequence.SecuenceDiagramInfo;

public class BuilderCodeCallback implements CodeSpecificCallback {

  @Override
  public boolean canHandle(MethodCallExpr mc) {
    return mc.getScope().isPresent() && mc.toString().contains(".builder().")
        && mc.getName().toString().equals("build") && mc.getArguments().isEmpty();
  }

  @Override
  public void handle(MethodCallExpr mc, SecuenceDiagramInfo info, String retorno) {
    String note = String.join(").\n", mc.toString().split("\\)\\.)"));
    info.addSelfCallback(retorno);
    info.addRightNote( note );
  }

}
