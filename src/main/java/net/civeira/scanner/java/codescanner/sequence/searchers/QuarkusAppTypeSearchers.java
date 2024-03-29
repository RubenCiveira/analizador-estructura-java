package net.civeira.scanner.java.codescanner.sequence.searchers;

import java.util.Optional;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import lombok.RequiredArgsConstructor;
import net.civeira.scanner.java.Project;
import net.civeira.scanner.java.codescanner.sequence.SecuenceDiagramInfo;
import net.civeira.scanner.java.codescanner.sequence.TypeSearchCallback;

@RequiredArgsConstructor
public class QuarkusAppTypeSearchers implements TypeSearchCallback {
  private final Project sequence;

  @Override
  public boolean canHandle(String fullType) {
    if (fullType.endsWith(".RestContextService")) {
      return true;
    }
    return false;
  }

  @Override
  public Optional<TypeDeclaration<?>> findTypeForSequence(SecuenceDiagramInfo secuenceDiagramInfo,
      String fullType, MethodCallExpr mc, String retorno) {
    Optional<TypeDeclaration<?>> response = Optional.empty();
    if ( fullType.endsWith(".RestContextService") && "getCurrentActorRequest".equals(mc.getNameAsString()) ) {
      secuenceDiagramInfo.addParticipant("RestContextService");
      secuenceDiagramInfo.addCallback("RestContextService", "getCurrentActorRequest(headers, request)");
      secuenceDiagramInfo.addStep("ref over RestContextService\n    buildActor\nend ref");
      secuenceDiagramInfo.addIncomeReturn("RestContextService", "actor");
    } else {
      response = Optional.ofNullable( sequence.types.get(fullType) );
    }
    return response;
  }
}
