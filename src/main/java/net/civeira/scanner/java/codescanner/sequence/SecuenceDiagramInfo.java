package net.civeira.scanner.java.codescanner.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.Type;
import lombok.Getter;

public class SecuenceDiagramInfo {
  @Getter
  private final List<SecuenceDiagramInfo> stack;
  private final List<String> entities;
  private final List<String> sequences;
  private final List<VariableDeclarator> vds;
  @Getter
  private final String result;
  private final SecuencePainter seq;
  private final String pack;
  @Getter
  private final MethodDeclaration methodDeclaration;
  @Getter
  private final TypeDeclaration<?> typeDeclaration;
  private final String back;
  private final String tabs;
  private final int deep;
  @Getter
  private final boolean handleCatch;

  public SecuenceDiagramInfo(List<SecuenceDiagramInfo> stack, List<String> entities,
      List<String> sequences, List<VariableDeclarator> vds, String result, SecuencePainter seq,
      String pack, MethodDeclaration methodDeclaration, TypeDeclaration<?> typeDeclaration,
      String back, String tabs, int deep, boolean handleCatch) {
    super();
    this.stack = stack;
    this.entities = entities;
    this.sequences = sequences;
    this.vds = vds;
    this.result = result;
    this.seq = seq;
    this.pack = pack;
    this.methodDeclaration = methodDeclaration;
    this.typeDeclaration = typeDeclaration;
    this.back = back;
    this.tabs = tabs;
    this.deep = deep;
    this.handleCatch = handleCatch;
    this.stack.add(this);
  }

  public void addEntity(String as, String str) {
    String entity = as + " " + str;
    if (!entities.contains(entity)) {
      entities.add(entity);
    }
  }

  public void addParticipant(String str) {
    addEntity("participant", str);
  }

  public void addStep(String str) {
    sequences.add("" + tabs + str.trim() + "\n");
  }

  public void addSelfCallback(MethodCallExpr mc) {
    addSelfCallback(mc, null);
  }

  public void addSelfCallback(String name) {
    addSelfCallback(name, null);
  }

  public void addSelfCallback(MethodCallExpr mc, String back) {
    addSelfCallback(methodCall(mc), back);
  }

  public void addSelfCallback(String name, String back) {
    addStep(typeDeclaration.getNameAsString() + " -> " + typeDeclaration.getNameAsString() + ": "
        + (null == back || "".equals(back.trim()) ? name : back));
  }

  public void setActivation(String from, MethodCallExpr mc) {
    addStep(
        from + " -> " + typeDeclaration.getNameAsString() + ": " + methodCall(mc) );
  }

  public void addIncomeReturn(String from, String name) {
    addStep(from + " --> " + typeDeclaration.getNameAsString() + " : " + name);
  }
  
  public void addCallback(String to, MethodCallExpr mc) {
    addCallback(to, mc, null);
  }

  public void addCallback(String to, String name) {
    addCallback(to, name, null);
  }

  public void addCallback(String to, MethodCallExpr mc, String back) {
    addCallback(to, methodCall(mc), back);
  }

  public void addCallback(String to, String name, String back) {
    addStep(typeDeclaration.getNameAsString() + " -> " + to + ": " + name);
    if (null != back && !"".equals(back.trim()) && !to.equals(typeDeclaration.getNameAsString())) {
      addStep(to + " --> " + typeDeclaration.getNameAsString() + ": " + back);
    }
  }

  public void addReturnCallback() {
    addStep(typeDeclaration.getNameAsString() + " --> " + back + ": " + result);
  }

  public SecuenceDiagramInfo tryContext() {
    return new SecuenceDiagramInfo(new ArrayList<>(stack), entities, sequences,
        new ArrayList<>(vds), result, seq, pack, methodDeclaration, typeDeclaration, back,
        tabs + "  ", deep, true);
  }

  public SecuenceDiagramInfo descendInline() {
    return new SecuenceDiagramInfo(new ArrayList<>(stack), entities, sequences,
        new ArrayList<>(vds), result, seq, pack, methodDeclaration, typeDeclaration, back, tabs,
        deep, false);
  }
  public SecuenceDiagramInfo descentJustified() {
    return descentJustified(result);
  }
  
  public SecuenceDiagramInfo descentJustified(String retorno) {
    return new SecuenceDiagramInfo(new ArrayList<>(stack), entities, sequences,
        new ArrayList<>(vds), retorno, seq, pack, methodDeclaration, typeDeclaration, back,
        tabs + "  ", deep, false);
  }

  public void newContextVariable(VariableDeclarator vc) {
    vds.add(vc);
  }

  public Optional<SecuenceDiagramInfo> lookup(MethodCallExpr mc, String retorno) {
    return lookup(mc, retorno, true);
  }
  public Optional<SecuenceDiagramInfo> lookup(MethodCallExpr mc, String retorno, boolean initial) {
    Optional<SecuenceDiagramInfo> result = Optional.empty();
    Optional<Expression> scope = mc.getScope();
    if (scope.isPresent()) {
      Expression expression = scope.get();
      result = lookupScope(mc, expression, retorno, initial);
    } else {
      addSelfCallback(mc, retorno);
      result = jumpTo(mc);
    }
    return result;
  }

  private Optional<SecuenceDiagramInfo> lookupScope(MethodCallExpr mc, Expression expression,
      String retorno, boolean initial) {
    Optional<SecuenceDiagramInfo> result = Optional.empty();
    if (expression instanceof NameExpr) {
      result = lookup(mc, retorno, (NameExpr) expression);
    } else if (expression instanceof MethodCallExpr) {
      //  addSelfCallback(mc);
      MethodCallExpr childMc = (MethodCallExpr)expression;
      Optional<SecuenceDiagramInfo> lookup = lookup( childMc, retorno, false );
      if( lookup.isPresent() ) {
        addCallback( lookup.get().typeDeclaration.getNameAsString(), childMc, retorno);
      } else {
        if( initial ) {
          // Si es la llamada inicial => resolvemos y pintamos
          addSelfCallback( methodCall( childMc ) + "." + mc.getNameAsString() + args(mc)  );
        } else {
          // Si no es la llamada inicial => vamos resolviendo para facilitar los datos
          methodCall( childMc );
        }
      }
    } else if (expression instanceof EnclosedExpr) {
      EnclosedExpr enclosed = (EnclosedExpr) expression;
      result = lookupScope(mc, enclosed.getInner(), retorno, true);
    } else if (expression instanceof CastExpr) {
      CastExpr cast = (CastExpr) expression;
      result = lookupForType(mc, cast.getType().asString(), retorno);
    } else {
      String of = typeName( expression.toString() );
      addParticipant(of);
      addCallback(of, mc, retorno);
    }
    return result;
  }

  private Optional<SecuenceDiagramInfo> lookup(MethodCallExpr mc, String retorno, NameExpr name) {
    Type type = null;
    String varName = typeName(name.getName().asString());
    for (VariableDeclarator variableDeclarator : vds) {
      if (variableDeclarator.getName().asString().equals(varName)) {
        type = variableDeclarator.getType();
        break;
      }
    }
    boolean traze = name.getName().asString().equals("args[1]");
    if( traze ) {
      System.out.println("Tengo que pintar a " + name + " frente a " + varName );
    }
    if (null == type) {
      for (Parameter parameter : methodDeclaration.getParameters()) {
        if (parameter.getName().asString().equals(varName)) {
          type = parameter.getType();
          break;
        }
      }
    }
    if (null == type) {
      for (FieldDeclaration fieldDeclaration : typeDeclaration.getFields()) {
        for (VariableDeclarator variableDeclarator : fieldDeclaration.getVariables()) {
          if (variableDeclarator.getName().asString().equals(varName)) {
            type = variableDeclarator.getType();
            break;
          }
        }
      }
    }
    return null != type ? lookupForType(mc, type.asString(), retorno)
        : lookupForType(mc, varName, retorno);
  }

  private Optional<SecuenceDiagramInfo> lookupForType(MethodCallExpr mc, String baseType,
      String retorno) {
    String type = typeName( baseType );
    Optional<SecuenceDiagramInfo> result = Optional.empty();
    String fullType = seq.project.typeName(type, typeDeclaration);
    boolean handled = false;
    for (TypeSearchCallback searcher : seq.searchers) {
      if (searcher.canHandle(fullType)) {
        result = searcher.findTypeForSequence(this, fullType, mc, retorno)
            .flatMap(tp -> jumpTo(tp, mc));
        handled = true;
        break;
      }
    }
    if (!handled) {
      Optional<TypeDeclaration<?>> otd = seq.project.type(fullType);
      if (otd.isPresent()) {
        TypeDeclaration<?> td = otd.get();
        addEntity( entityType(td), type);
        result = jumpTo(td, mc);
        if( !result.isPresent() ) {
          // TODO: el metodo no existe, seguramente lombok o similar.
          addCallback(type, mc);
        }
      } else {
        // FIXME: hay que ver las clases.
        // System.err.println("No encuentro a " + fullType);
      }
    }
    return result;
  }

  private Optional<SecuenceDiagramInfo> jumpTo(MethodCallExpr mc) {
    return jumpTo(typeDeclaration, mc);
  }

  private Optional<SecuenceDiagramInfo> jumpTo(TypeDeclaration<?> typeDeclaration2,
      MethodCallExpr mc) {
    Optional<SecuenceDiagramInfo> optional = Optional.empty();
    if (deep > 0) {
      for (MethodDeclaration methodDeclaration2 : typeDeclaration2.getMethods()) {
        if (methodDeclaration2.getNameAsString().equals(mc.getNameAsString())) {
          optional = Optional.of(new SecuenceDiagramInfo(new ArrayList<>(stack), entities,
              sequences, new ArrayList<>(vds), result, seq, pack, methodDeclaration2,
              typeDeclaration2, typeDeclaration.getName().asString(), tabs,
              typeDeclaration2 == typeDeclaration ? deep : deep - 1, false));
          break;
        }
      }
    }
    return optional;
  }

  private String methodCall(MethodCallExpr mc) {
    return mc.getScope().map(sc -> sc.toString() + ".").orElse(".") + mc.getNameAsString() + args(mc);
  }
  
  private String args(MethodCallExpr mc) {
    String args = "";
    for (Expression expression : mc.getArguments()) {
      if( expression instanceof MethodCallExpr ) {
        String nn = callName( (MethodCallExpr)expression );
        seq.addExpression(expression, nn, this);
        args += ", " + nn;
      } else {
        args += ", " + expression;
      }
    }
    if( args.length() > 0 ) {
      args = args.substring(2);
    }
    int limit = 30;
    return "(" + (args.length() > limit ? args.substring(0, limit - 3) + "..." : args) + ")";
  }
  
  private String callName(MethodCallExpr expression) {
    String str = expression.getNameAsString();
    int dot = str.indexOf('.');
    if( dot > 0 ) {
      str = str.substring(dot);
    }
    int chap = str.indexOf('(');
    if( chap > 0 ) {
      str = str.substring( chap );
    }
    int limit = 30;
    return  str.length() > limit ? str.substring(0, limit - 3) + "..." : str;
  }
  
  private String entityType(TypeDeclaration<?> td) {
    String result = "participant";
    if( td instanceof ClassOrInterfaceDeclaration ) {
      ClassOrInterfaceDeclaration ci = (ClassOrInterfaceDeclaration)td;
      if( ci.isInterface() || ci.isAbstract() ) {
        result = "boundary";
      } else if ( ci.getAnnotationByName("Data").isPresent() ) {
        result = "entity";
      }
    }
    return result;
  }
  
  private String typeName(String of) {
    int dot_one = of.indexOf('[');
    if( dot_one > 0 ) {
      of = of.substring(0, dot_one);
    }
    int dot_two = of.indexOf('<');
    if( dot_two > 0 ) {
      of = of.substring(0, dot_two);
    }
    return of;
    
  }
}
