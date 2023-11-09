package net.civeira.scanner.java.diagrams.coders;

import com.github.javaparser.ast.expr.MethodCallExpr;
import net.civeira.scanner.java.diagrams.CodeSpecificCallback;

public class StreamCoderCallback implements CodeSpecificCallback {

  @Override
  public boolean canHandle(MethodCallExpr mc) {
//    System.out.println("\nMiro a ver si " + mc);
    return false;
  }

  @Override
  public void handle(MethodCallExpr mc) {
    // TODO Auto-generated method stub
    
  }

}
