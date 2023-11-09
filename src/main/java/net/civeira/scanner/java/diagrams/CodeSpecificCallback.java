package net.civeira.scanner.java.diagrams;

import com.github.javaparser.ast.expr.MethodCallExpr;

public interface CodeSpecificCallback {

  boolean canHandle(MethodCallExpr mc);

  void handle(MethodCallExpr mc);

}
