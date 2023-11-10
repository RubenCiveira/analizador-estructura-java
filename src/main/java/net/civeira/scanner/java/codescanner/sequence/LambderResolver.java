package net.civeira.scanner.java.codescanner.sequence;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

public interface LambderResolver {

  boolean canHandle(MethodCallExpr mc);
  
  String resolveAsVariable(MethodCallExpr mc, Expression expression, SecuencePainter seq, SecuenceDiagramInfo dia);
}
