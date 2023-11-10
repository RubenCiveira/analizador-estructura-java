package net.civeira.scanner.java.codescanner.sequence;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import net.civeira.scanner.java.Project;
import net.civeira.scanner.java.codescanner.sequence.coders.StreamCoderCallback;
import net.civeira.scanner.java.codescanner.sequence.searchers.QuarkusAppTypeSearchers;
import net.civeira.scanner.java.codescanner.sequence.searchers.SqlTypeSearchers;
import net.civeira.scanner.java.diagram.InputType;
import net.civeira.scanner.java.diagram.LocalDiagram;

public class SecuencePainter {
  private static final int DEEP = 1;
  public final Project project;
  public List<TypeSearchCallback> searchers = new ArrayList<>();
  public List<CodeSpecificCallback> specificators = new ArrayList<>();

  public SecuencePainter(Project project) {
    this.project = project;
    searchers.add(new SqlTypeSearchers());
    searchers.add(new QuarkusAppTypeSearchers(project));
    specificators.add(new StreamCoderCallback());
  }

  public List<LocalDiagram> generateSequencesDia(File base) {
    List<LocalDiagram> diagrams = new ArrayList<>();
    for (Entry<String, TypeDeclaration<?>> entry : this.project.types.entrySet()) {
      TypeDeclaration<?> typeDeclaration = entry.getValue();
      for (MethodDeclaration methodDeclaration : typeDeclaration.getMethods()) {
        methodDeclaration.getBody().ifPresent(body -> {
          if (methodDeclaration.isPublic()) {
            String txt = "";
            List<String> entities = new ArrayList<>();
            entities.add("participant " + typeDeclaration.getNameAsString());
            List<String> sequences = new ArrayList<>();
            SecuenceDiagramInfo info =
                new SecuenceDiagramInfo(new ArrayList<>(), entities, sequences, new ArrayList<>(), "result", this,
                    entry.getKey(), methodDeclaration, typeDeclaration, "Start", "", DEEP, false);
            scan(info);
            if (sequences.size() > 1 && !sequences.get(1).endsWith(" Start: result\n")) {
              clearEntities(entities, "System.out");
              clearEntities(entities, "System.err");
              // @formatter:off
              txt += "" 
                  + "@startuml "+methodDeclaration.getNameAsString()+"\n"
                  + "actor  Start\n" 
                  + String.join("\n", entities)+ "\n\n" 
                  + "Start -> " + typeDeclaration.getNameAsString() + " : " + methodDeclaration.getNameAsString() + "\n"
                  + String.join("", sequences) 
                  + "@enduml\n";
              // @formatter:on
              LocalDiagram dg =
                  new LocalDiagram(InputType.PLANTUML, methodDeclaration.getNameAsString(),
                      new File(base,
                          this.project.files.get(entry.getKey()) + "/" + typeDeclaration.getNameAsString() + "_"
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

  private void clearEntities(List<String> entities, String name) {
    String entity = "participant "+name+"";
    if( entities.contains(entity) ) {
      entities.remove(entity);
      entities.add(entity);
    }
  }
  
  private void scan(SecuenceDiagramInfo info) {
    scan(null, null, info);
  }
  /* default */ void scan(TypeDeclaration<?> from, MethodCallExpr mc, SecuenceDiagramInfo info) {
    if( null != from && null != mc ) {
      info.setActivation(from.getNameAsString(), mc);
    }
    Optional<BlockStmt> bodyStmt = info.getMethodDeclaration().getBody();
    bodyStmt.ifPresent(body -> addStatements(body.getStatements(), info));
    if( !bodyStmt.isPresent() ) {
      info.addReturnCallback();
    }
  }

  /* default */ void addExpression(Expression exp, String retorno, SecuenceDiagramInfo info) {
    if (exp instanceof ObjectCreationExpr) {
      info.addSelfCallback( retorno + " = " + exp.toString() );
    } else if (exp instanceof MethodCallExpr) {
      MethodCallExpr mc = (MethodCallExpr) exp;
      Optional<SecuenceDiagramInfo> lookup = info.lookup(mc, retorno);
      if( lookup.isPresent() ) {
        SecuenceDiagramInfo seq = lookup.get();
        if( seq.getMethodDeclaration() == info.getMethodDeclaration() ) {
          // TODO: add note of recursión
        } else {
          seq.addStep("group#Gold #LightBlue " + mc.getNameAsString() + "");
          scan(info.getTypeDeclaration(), mc, seq.descentJustified(retorno));
          seq.addStep("end");
        }
      } else {
          boolean handled = false;
          for(CodeSpecificCallback spe: specificators) {
            if( spe.canHandle(mc) ) {
              spe.handle( mc );
              handled = true;
            }
          }
          if( !handled && !mc.getScope().isPresent() ) {
            info.addSelfCallback(mc);
          }
      }
    } else if (exp instanceof AssignExpr) {
      AssignExpr assign = (AssignExpr) exp;
      addExpression(assign.getValue(), assign.getTarget().toString(), info);
    } else if (exp instanceof VariableDeclarationExpr) {
      VariableDeclarationExpr vd = (VariableDeclarationExpr) exp;
      for (VariableDeclarator variableDeclarator : vd.getVariables()) {
        info.newContextVariable(variableDeclarator);
        variableDeclarator.getInitializer().ifPresent(init -> {
          addExpression(init, variableDeclarator.getNameAsString(), info);
        });
      }
    } else if ( exp instanceof NameExpr ) {
      // Un nombre suelto no aporta nada
    } else {
      info.addSelfCallback(exp.toString(), retorno);
    }
  }

  private void addIfStatement(IfStmt theIf, SecuenceDiagramInfo info) {
    addStatement(theIf.getThenStmt(), info);
    theIf.getElseStmt().ifPresent(theElse -> {
      if (theElse instanceof IfStmt) {
        IfStmt inner = (IfStmt) theElse;
        info.addStep("else " + inner.getCondition());
        addIfStatement(inner, info.descentJustified());
      } else {
        info.addStep("else");
        addStatement(theElse, info.descentJustified());
      }
    });
  }

  private void addStatements(NodeList<Statement> mc, SecuenceDiagramInfo info) {
    for (Statement statement : mc) {
      addStatement(statement, info);
    }
  }

  private void addStatement(Statement mc, SecuenceDiagramInfo info) {
    if (mc instanceof IfStmt) {
      IfStmt theIf = (IfStmt) mc;
      info.addStep("alt " + theIf.getCondition());
      addIfStatement(theIf, info.descentJustified());
      info.addStep("end");
    } else if (mc instanceof ForeachStmt) {
      ForeachStmt fs = (ForeachStmt) mc;
      VariableDeclarator var = fs.getVariable().getVariable(0);
      SecuenceDiagramInfo descend = info.descentJustified();
      descend.newContextVariable(var);
      info.addStep("loop " + var.getNameAsString() + " = " + var.getInitializer());
      addStatement(fs.getBody(), descend);
      info.addStep("end");
    } else if (mc instanceof ForStmt) {
      ForStmt fs = (ForStmt) mc;
      info.addStep("loop " + fs.getCompare());
      addStatement(fs.getBody(), info.descentJustified());
      info.addStep("end");
    } else if (mc instanceof WhileStmt) {
      WhileStmt fs = (WhileStmt) mc;
      info.addStep("loop " + fs.getCondition());
      addStatement(fs.getBody(), info.descentJustified());
      info.addStep("end");
    } else if (mc instanceof DoStmt) {
      DoStmt fs = (DoStmt) mc;
      info.addStep("loop " + fs.getCondition());
      addStatement(fs.getBody(), info.descentJustified());
      info.addStep("end");
    } else if (mc instanceof SwitchStmt) {
      SwitchStmt sw = (SwitchStmt) mc;
      boolean first = true;
      for (SwitchEntryStmt switchEntryStmt : sw.getEntries()) {
        if (first) {
          info.addStep("alt " + switchEntryStmt.getLabel());
        } else {
          info.addStep("else " + switchEntryStmt.getLabel().map(lb -> lb.toString()).orElse(""));
        }
        addStatements(switchEntryStmt.getStatements(), info.descentJustified());
        info.addStep("end");
      }
    } else if (mc instanceof ReturnStmt) {
      ReturnStmt rs = (ReturnStmt) mc;
      rs.getExpression().ifPresent(exp -> {
        addExpression(exp, info.getResult(), info);
      });
      info.addReturnCallback();
    } else if (mc instanceof BlockStmt) {
      BlockStmt block = (BlockStmt) mc;
      addStatements(block.getStatements(), info.descendInline());
    } else if (mc instanceof ExpressionStmt) {
      ExpressionStmt exp = (ExpressionStmt) mc;
      addExpression(exp.getExpression(), "", info);
    } else if (mc instanceof BreakStmt) {
      info.addSelfCallback("break");
    } else if (mc instanceof TryStmt ) {
      TryStmt tr = (TryStmt)mc;
      info.addStep("group try");
      SecuenceDiagramInfo tryContext = info.tryContext();
      for (Expression node : tr.getResources()) {
        addExpression(node, "", tryContext);
      }
      addStatements(tr.getTryBlock().getStatements(), tryContext);
      for (CatchClause catchClause : tr.getCatchClauses()) {
        info.addStep("else " + catchClause.getParameter()  );
        addStatements(catchClause.getBody().getStatements(), info.descentJustified());
      }
      tr.getFinallyBlock().ifPresent( fb -> {
        info.addStep("finally");
        addStatements(fb.getStatements(), info.descentJustified());
      });
      info.addStep("end");
    } else if (mc instanceof ThrowStmt) {
      ThrowStmt th = (ThrowStmt)mc;
      info.addStep("group#Gold #Pink throws");
      boolean handled = false;
      for (SecuenceDiagramInfo step : info.getStack()) {
        if( step.isHandleCatch() ) {
          info.addStep( info.getTypeDeclaration().getNameAsString() + " --> " + step.getTypeDeclaration().getNameAsString() + " : " + th.toThrowStmt().map(Object::toString).orElse("throw")  );
          handled = true;
          break;
        }
      }
      if( !handled ) {
        info.addStep( info.getTypeDeclaration().getNameAsString() + " --> Start !!: " + label( th.toThrowStmt().map(Object::toString).orElse("throw") ) );
      }
      info.addStep("end");
    } else {
      // FIXME: ver que hacer
      // System.out.println("Tengo a " + mc.getClass() + ": " + mc);
    }
  }
  
  private String label(String str) {
    int limit = 30;
    return  str.length() > limit ? str.substring(0, limit - 3) + "..." : str;
  }
}
