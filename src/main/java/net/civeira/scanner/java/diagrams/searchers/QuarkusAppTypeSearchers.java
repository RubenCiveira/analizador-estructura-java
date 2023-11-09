package net.civeira.scanner.java.diagrams.searchers;

import java.util.Optional;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import lombok.RequiredArgsConstructor;
import net.civeira.scanner.java.diagrams.SecuenceDiagramInfo;
import net.civeira.scanner.java.diagrams.Sequence;
import net.civeira.scanner.java.diagrams.TypeSearchCallback;

@RequiredArgsConstructor
public class QuarkusAppTypeSearchers implements TypeSearchCallback {
  private final Sequence sequence;

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
      secuenceDiagramInfo.addCallback("RestContextService", "getCurrentActorRequest");
      secuenceDiagramInfo.addStep("RestContextService -> RestContextService : buildActor");
      secuenceDiagramInfo.addIncomeReturn("RestContextService", "actor");
    } else {
      response = Optional.ofNullable( sequence.types.get(fullType) );
    }
    return response;
  }
}
