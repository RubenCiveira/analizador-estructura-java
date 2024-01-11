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
  private static final List<String> KNOWNS =
      Arrays.asList("ifPresent", "then", "otherwise", "orElseThrow", "orElseGet", "forEach",
          "findone", "count", "list", "agregate", "ifPresentOrElse", "retrievelastFromStore",
          "retrieveFromStore", "collect", "map", "flatMap", "filter");

  @Override
  public boolean canHandle(MethodCallExpr mc) {
    String name = mc.getNameAsString();
    boolean result = KNOWNS.contains(name);
    if (!result) {
      System.err.println("> Error con " + name);
    }
    return result;
  }

  @Override
  public void resolveAsVariable(MethodCallExpr mc, SecuencePainter seq, SecuenceDiagramInfo dia) {
    String in = dia.sanitice(mc.getScope().map(Object::toString).orElse(".."), 30);
    if (mc.getNameAsString().equals("ifPresentOrElse")) {
      dia.addStep("alt " + in);
      exp(mc, mc.getArgument(0), seq, dia);
      dia.addStep("else !" + in);
      exp(mc, mc.getArgument(1), seq, dia);
      dia.addStep("end");
    } else {
      mono(in, mc, seq, dia);
    }
  }

  private void mono(String in, MethodCallExpr mc, SecuencePainter seq, SecuenceDiagramInfo dia) {
    boolean group = true;
    if (mc.getNameAsString().equals("ifPresent")) {
      dia.addStep("alt " + in);
    } else if (mc.getNameAsString().equals("forEach")) {
      dia.addStep("loop " + in);
    } else if (mc.getNameAsString().equals("map")) {
      group = false;
    } else {
      dia.addStep("group " + in);
    }
    for (Expression exp : mc.getArguments()) {
      exp(mc, exp, seq, dia);
    }
    if (group) {
      dia.addStep("end");
    }
  }

  private void exp(MethodCallExpr mc, Expression exp, SecuencePainter seq,
      SecuenceDiagramInfo dia) {
    if (exp instanceof LambdaExpr) {
      lambda(mc, (LambdaExpr) exp, seq, dia);
    }
    if (exp instanceof MethodReferenceExpr) {
      reference(mc, (MethodReferenceExpr) exp, seq, dia);
    }
  }

  private void lambda(MethodCallExpr mc, LambdaExpr exp, SecuencePainter seq,
      SecuenceDiagramInfo parent) {
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
  }

  private void reference(MethodCallExpr mc, MethodReferenceExpr exp, SecuencePainter seq,
      SecuenceDiagramInfo parent) {
    String participant = parent.lookupForVariable(exp.getScope().toString());
    parent.addCallback(participant, exp.toString());
  }
}
