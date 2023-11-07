package net.civeira.scanner.java.diagrams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.Type;
import lombok.RequiredArgsConstructor;
import net.civeira.scanner.java.LocalDiagram;
import net.civeira.scanner.java.kroki.InputType;

public class Sequence {
  /* default */ Map<String, TypeDeclaration<?>> types = new HashMap<>();
  /* default */ Map<String, String> files = new HashMap<>();
  /* default */ Map<String, CompilationUnit> units = new HashMap<>();

  public void scan(File java) throws IOException {
    CompilationUnit cu = JavaParser.parse(java);
    String pack = cu.getPackageDeclaration().map(pck -> pck.getNameAsString() + ".").orElse("");
    for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
      types.put(pack + typeDeclaration.getNameAsString(), typeDeclaration);
      files.put(pack + typeDeclaration.getNameAsString(), pack.replace(".", "/"));
      units.put(pack + typeDeclaration.getNameAsString(), cu);
    }
  }

  public List<LocalDiagram> generateSequences(File base) {
    List<LocalDiagram> diagrams = new ArrayList<>();
    for (Entry<String, TypeDeclaration<?>> entry : types.entrySet()) {
      TypeDeclaration<?> typeDeclaration = entry.getValue();
      for (MethodDeclaration methodDeclaration : typeDeclaration.getMethods()) {
        SequenceExtractor extractor = new SequenceExtractor("result", this, entry.getKey(), methodDeclaration,
            typeDeclaration, "Start");
        methodDeclaration.getBody().ifPresent(body -> {
          if (methodDeclaration.isPublic()) {
            NodeList<Statement> statements = body.getStatements();

            // Generamos un diagrama de secuencia
            String txt = "";
            List<String> entities = new ArrayList<>();
            entities.add("participant " + typeDeclaration.getNameAsString() + "\n");
            List<String> sequences = new ArrayList<>();
            extractor.addStatements(statements, entities, sequences);
            if (sequences.size() > 1 && !sequences.get(1).endsWith(" Start: result\n")) {
              // @formatter:off
              txt += "" 
                  + "@startuml\n" 
                  + "actor  Start\n" 
                  + String.join("", entities)+ "\n" 
                  + "Start -> " + typeDeclaration.getNameAsString() + " : " + methodDeclaration.getNameAsString() + "\n"
                  + String.join("", sequences) 
                  + "@enduml\n";
              // @formatter:on
              LocalDiagram dg =
                  new LocalDiagram(InputType.PLANTUML, methodDeclaration.getNameAsString(),
                      new File(base,
                          files.get(entry.getKey()) + "/" + typeDeclaration.getNameAsString() + "_"
                              + methodDeclaration.getNameAsString() + ".puml"));
              dg.setContent(txt);
              diagrams.add(dg);
            }
          }
        });
      }
    }
    return diagrams;
  }
}


@RequiredArgsConstructor
class SequenceExtractor {
  private final String result;
  private final Sequence seq;
  private final String pack;
  private final MethodDeclaration methodDeclaration;
  private final TypeDeclaration<?> typeDeclaration;
  private final String back;

  private void lookupVar(MethodCallExpr mc, List<String> entities, List<String> sequences,
      String retorno, String args, NameExpr name, List<VariableDeclarator> vds) {
    Type type = null;
    for (VariableDeclarator variableDeclarator : vds) {
      if (variableDeclarator.getName().asString().equals(name.getName().asString())) {
        type = variableDeclarator.getType();
      }
    }
    if (null == type) {
      for (Parameter parameter : methodDeclaration.getParameters()) {
        if (parameter.getName().asString().equals(name.getName().asString())) {
          type = parameter.getType();
        }
      }
    }
    if (null == type) {
      for (FieldDeclaration fieldDeclaration : typeDeclaration.getFields()) {
        for (VariableDeclarator variableDeclarator : fieldDeclaration.getVariables()) {
          if (variableDeclarator.getName().asString().equals(name.getName().asString())) {
            type = variableDeclarator.getType();
          }
        }
      }
    }
    CompilationUnit compilationUnit = seq.units.get(pack);
    String fullType =
        compilationUnit.getPackageDeclaration().map(pkg -> pkg.getNameAsString() + ".").orElse("")
            + type.asString();
    for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
      if (importDeclaration.toString().endsWith("." + type.asString() + ";")) {
        fullType = importDeclaration.getNameAsString();
      }
    }
    if (seq.types.containsKey(fullType)) {
      TypeDeclaration<?> ref = seq.types.get(fullType);
      for (MethodDeclaration methodDeclaration2 : ref.getMethods()) {
        if (methodDeclaration2.getNameAsString().equals(mc.getNameAsString())) {
          String refName = fullType;
          String refType = ref.getNameAsString();
          methodDeclaration2.getBody().ifPresent(body -> {
            SequenceExtractor linked = new SequenceExtractor(retorno.equals("") ? "result" : retorno, seq, refName, methodDeclaration2, ref,
                typeDeclaration.getNameAsString());
            String entity = "participant " + refType + "\n";
            if (!entities.contains(entity)) {
              entities.add(entity);
            }
            sequences.add("" + typeDeclaration.getNameAsString() + " -> " + refType + " : "
                + mc.getNameAsString() + args + "\n");
            linked.addStatements(body.getStatements(), entities, sequences);
//            if (!retorno.equals("")) {
//              sequences.add("" + refType + " --> " + typeDeclaration.getNameAsString() + " : "
//                  + retorno + "\n");
//            }
          });
        }
      }
    }
  }

  private void addExpression(Expression exp, List<String> entities, List<String> sequences,
      String retorno, List<VariableDeclarator> vds) {
    if (exp instanceof MethodCallExpr) {
      MethodCallExpr mc = (MethodCallExpr) exp;
      Optional<Expression> scope = mc.getScope();
      String args = args(mc);
      if (scope.isPresent()) {
        Expression expression = scope.get();
        if (expression instanceof NameExpr) {
          NameExpr name = (NameExpr) expression;
          lookupVar(mc, entities, sequences, retorno, args, name, vds);
        } else {
          String of = expression.toString();
          String entity = "participant " + of + "\n";
          if (!entities.contains(entity)) {
            entities.add(entity);
          }
          sequences.add("" + typeDeclaration.getNameAsString() + " -> " + of + " : "
              + mc.getNameAsString() + args + "\n");
          if (!retorno.equals("")) {
            sequences.add(
                "" + of + " --> " + typeDeclaration.getNameAsString() + " : " + retorno + "\n");
          }
        }
      } else {
        sequences.add("" + typeDeclaration.getNameAsString() + " -> "
            + typeDeclaration.getNameAsString() + " : "
            + (retorno.equals("") ? "" : retorno + " = ") + mc.getNameAsString() + args + "\n");
      }
    } else if (exp instanceof AssignExpr) {
      AssignExpr assign = (AssignExpr) exp;
      addExpression(assign.getValue(), entities, sequences, assign.getTarget().toString(), vds);
    } else if (exp instanceof VariableDeclarationExpr) {
      VariableDeclarationExpr vd = (VariableDeclarationExpr) exp;
      for (VariableDeclarator variableDeclarator : vd.getVariables()) {
        vds.add(variableDeclarator);
        variableDeclarator.getInitializer().ifPresent(init -> {
          addExpression(init, entities, sequences, variableDeclarator.getNameAsString(), vds);
        });
      }
    } else {
      sequences
          .add("" + typeDeclaration.getNameAsString() + " -> " + typeDeclaration.getNameAsString()
              + (retorno.equals("") ? "" : " : " + retorno + " = ") + exp + "\n");
      // System.out.println("No se con " + exp.getClass( ));
    }
  }

  private void addIfStatement(IfStmt theIf, List<String> entities, List<String> sequences,
      List<VariableDeclarator> scope) {
    addStatement(theIf.getThenStmt(), entities, sequences, scope);
    theIf.getElseStmt().ifPresent(theElse -> {
      if (theElse instanceof IfStmt) {
        IfStmt inner = (IfStmt) theElse;
        sequences.add("else " + inner.getCondition() + "\n");
        addIfStatement(inner, entities, sequences, scope);
      } else {
        sequences.add("else\n");
        addStatement(theElse, entities, sequences, scope);
      }
    });
  }

  /* default */ void addStatements(NodeList<Statement> mc, List<String> entities,
      List<String> sequences) {
    addStatements(mc, entities, sequences, new ArrayList<>());
  }

  /* default */ void addStatements(NodeList<Statement> mc, List<String> entities,
      List<String> sequences, List<VariableDeclarator> vds) {
    for (Statement statement : mc) {
      addStatement(statement, entities, sequences, vds);
    }

  }

  /* default */ void addStatement(Statement mc, List<String> entities, List<String> sequences,
      List<VariableDeclarator> vds) {
    if (mc instanceof IfStmt) {
      IfStmt theIf = (IfStmt) mc;
      sequences.add("alt " + theIf.getCondition() + "\n");
      addIfStatement(theIf, entities, sequences, new ArrayList<>(vds));
      sequences.add("end\n");
    } else if (mc instanceof ForeachStmt) {
      ForeachStmt fs = (ForeachStmt) mc;
      VariableDeclarator var = fs.getVariable().getVariable(0);
      List<VariableDeclarator> invds = new ArrayList<>(vds);
      invds.add(var);
      sequences.add("loop " + var.getNameAsString() + " = " + var.getInitializer() + "\n");
      addStatement(fs.getBody(), entities, sequences, invds);
      sequences.add("end\n");
    } else if (mc instanceof ForStmt) {
      ForStmt fs = (ForStmt) mc;
      sequences.add("loop " + fs.getCompare() + "\n");
      addStatement(fs.getBody(), entities, sequences, new ArrayList<>(vds));
      sequences.add("end\n");
    } else if (mc instanceof WhileStmt) {
      WhileStmt fs = (WhileStmt) mc;
      sequences.add("loop " + fs.getCondition() + "\n");
      addStatement(fs.getBody(), entities, sequences, new ArrayList<>(vds));
      sequences.add("end\n");
    } else if (mc instanceof DoStmt) {
      DoStmt fs = (DoStmt) mc;
      sequences.add("loop " + fs.getCondition() + "\n");
      addStatement(fs.getBody(), entities, sequences, new ArrayList<>(vds));
      sequences.add("end\n");
    } else if (mc instanceof SwitchStmt) {
      SwitchStmt sw = (SwitchStmt) mc;
      boolean first = true;
      for (SwitchEntryStmt switchEntryStmt : sw.getEntries()) {
        if (first) {
          sequences.add("alt " + switchEntryStmt.getLabel() + "\n");
        } else {
          sequences
              .add("else " + switchEntryStmt.getLabel().map(lb -> lb.toString()).orElse("") + "\n");
        }
        ArrayList<VariableDeclarator> inVds = new ArrayList<>(vds);
        for (Statement statement : switchEntryStmt.getStatements()) {
          addStatement(statement, entities, sequences, inVds);
        }
        sequences.add("end\n");
      }
    } else if (mc instanceof ReturnStmt) {
      ReturnStmt rs = (ReturnStmt) mc;
      rs.getExpression().ifPresent(exp -> {
        addExpression(exp, entities, sequences, result, vds);
      });
      sequences.add(typeDeclaration.getNameAsString() + " --> " + back + ": "+ result +"\n");
    } else if (mc instanceof BlockStmt) {
      BlockStmt block = (BlockStmt) mc;
      ArrayList<VariableDeclarator> inVds = new ArrayList<>(vds);
      for (Statement statement : block.getStatements()) {
        addStatement(statement, entities, sequences, inVds);
      }
    } else if (mc instanceof ExpressionStmt) {
      ExpressionStmt exp = (ExpressionStmt) mc;
      addExpression(exp.getExpression(), entities, sequences, "", vds);
    } else if (mc instanceof BreakStmt) {
      sequences.add(typeDeclaration.getNameAsString() + " --> " + typeDeclaration.getNameAsString()
          + ": break\n");
    } else {
      System.out.println("Tengo a " + mc.getClass() + ": " + mc);
    }
  }

  private String args(MethodCallExpr mc) {
    String args = mc.getArguments().toString();
    return "(" + (args.length() > 10 ? args.substring(0, 7) + "..." : args) + ")";
  }
}
