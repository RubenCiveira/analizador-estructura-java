package net.civeira.scanner.java.diagrams.searchers;

import java.util.Optional;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import net.civeira.scanner.java.diagrams.SecuenceDiagramInfo;
import net.civeira.scanner.java.diagrams.TypeSearchCallback;

public class SqlTypeSearchers implements TypeSearchCallback {

  @Override
  public boolean canHandle(String fullType) {
    return fullType.startsWith("javax.sql.")
        || fullType.startsWith("java.sql.");
  }

  @Override
  public Optional<TypeDeclaration<?>> findTypeForSequence(SecuenceDiagramInfo secuenceDiagramInfo,
      String fullType, MethodCallExpr mc, String retorno) {
    int last = fullType.lastIndexOf(".") + 1;
    String name = fullType.substring(last);
    secuenceDiagramInfo.addEntity(name.equals("DataSource") ? "database" : "entity", name );
    secuenceDiagramInfo.addCallback( name, mc, retorno );
    return Optional.empty();
  }

}
