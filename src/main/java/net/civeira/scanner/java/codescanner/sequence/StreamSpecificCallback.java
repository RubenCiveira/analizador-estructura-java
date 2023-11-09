package net.civeira.scanner.java.codescanner.sequence;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

public class StreamSpecificCallback {

  // @Override
  public boolean buildDiagram(MethodCallExpr exp, Expression scope,
      MethodDeclaration methodDeclaration, TypeDeclaration<?> typeDeclaration,
      SecuenceDiagramInfo info) {
    // String code = scope.toString();
    
//    info.addStep("" + typeDeclaration.getNameAsString() + " -> " + typeDeclaration.getNameAsString()
//        + " : " + exp.getNameAsString() + args(exp));
    return false;
  }
}
