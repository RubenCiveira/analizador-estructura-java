package net.civeira.scanner.java.diagrams;

import java.util.Optional;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

public interface TypeSearchCallback {

  boolean canHandle(String fullType);

  Optional<TypeDeclaration<?>> findTypeForSequence(SecuenceDiagramInfo secuenceDiagramInfo,
      String fullType, MethodCallExpr mc, String retorno);

}
