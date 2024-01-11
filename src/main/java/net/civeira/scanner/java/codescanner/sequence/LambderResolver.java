package net.civeira.scanner.java.codescanner.sequence;

import com.github.javaparser.ast.expr.MethodCallExpr;

public interface LambderResolver {

  boolean canHandle(MethodCallExpr mc);
  
  void resolveAsVariable(MethodCallExpr mc, SecuencePainter seq, SecuenceDiagramInfo dia);
}
