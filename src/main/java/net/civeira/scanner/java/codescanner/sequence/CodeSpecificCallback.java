package net.civeira.scanner.java.codescanner.sequence;

import com.github.javaparser.ast.expr.MethodCallExpr;

public interface CodeSpecificCallback {

  boolean canHandle(MethodCallExpr mc);

  void handle(MethodCallExpr mc);

}
