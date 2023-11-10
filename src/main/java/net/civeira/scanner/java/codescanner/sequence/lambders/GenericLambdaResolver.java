package net.civeira.scanner.java.codescanner.sequence.lambders;

import java.util.Arrays;
import java.util.List;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.stmt.Statement;
import net.civeira.scanner.java.codescanner.sequence.LambderResolver;
import net.civeira.scanner.java.codescanner.sequence.SecuenceDiagramInfo;
import net.civeira.scanner.java.codescanner.sequence.SecuencePainter;

public class GenericLambdaResolver implements LambderResolver {
  private static final List<String> KNOWNS = Arrays.asList("ifPresent", "then", "otherwise",
      "orElseThrow", "orElseGet", "forEach", "findOne", "count", "list", "agregate",
      "retrieveLastFromStore", "retrieveFromStore", "collect", "map", "flatMap", "filter");

  @Override
  public boolean canHandle(MethodCallExpr mc) {
    String name = mc.getNameAsString();
    boolean result = KNOWNS.contains(name);
    if (!result) {
      System.err.println(">> Error con " + name);
    }
    return result;
  }

  @Override
  public String resolveAsVariable(MethodCallExpr mc, Expression exp, SecuencePainter seq,
      SecuenceDiagramInfo dia) {
    // TODO Auto-generated method stub
    if (exp instanceof LambdaExpr) {
      return lambda(mc, (LambdaExpr) exp, seq, dia);
    }
    if (exp instanceof MethodReferenceExpr) {
      return reference(mc, (MethodReferenceExpr) exp, seq, dia);
    }
    // dia.addSelfCallback("ll = " + exp.toString());
    return exp.toString();
  }

  private String lambda(MethodCallExpr mc, LambdaExpr exp, SecuencePainter seq,
      SecuenceDiagramInfo parent) {
    if( mc.getNameAsString().equals("ifPresent") ) {
      parent.addStep("alt " + mc.getScope().map(sc -> sc.toString()).orElse("..") );
    } else {
      parent.addStep("group " + mc);
    }
    SecuenceDiagramInfo dia = parent.descentJustified("lambda");
    for (Node node : exp.getBody().getChildNodes()) {
      if (node instanceof Statement) {
        seq.addStatement((Statement) node, dia);
      } else if (node instanceof Expression) {
        seq.addExpression((Expression) node, "lambda", dia);
      } else {
        System.err.println("=======" + node.getClass() + ", " + node.toString());
      }
    }
    parent.addStep("end");
    return "lambda";
  }

  private String reference(MethodCallExpr mc, MethodReferenceExpr exp, SecuencePainter seq,
      SecuenceDiagramInfo parent) {
    if( mc.getNameAsString().equals("ifPresent") ) {
      parent.addStep("alt " + mc.getScope().map(sc -> sc.toString()).orElse("..") );
    } else {
      parent.addStep("group " + mc.getNameAsString() + " " + mc.getScope().map(sc -> sc.toString()).orElse(".."));
    }
    String participant = parent.lookupForVariable( exp.getScope().toString() );
    // FIXME: tenemos que poder llamar hacia el nuevo participante que se ha contruido.
    parent.addCallback( participant, exp.toString() );
    parent.addStep("end");
    return "lambda";// exp.toString();
  }
}
