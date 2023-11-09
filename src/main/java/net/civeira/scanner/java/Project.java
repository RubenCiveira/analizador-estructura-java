package net.civeira.scanner.java;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

public class Project {
//  public List<TypeSearchCallback> searchers = new ArrayList<>();
//  public List<CodeSpecificCallback> specificators = new ArrayList<>();
  public Map<String, TypeDeclaration<?>> types = new HashMap<>();
  public Map<String, String> files = new HashMap<>();
  public Map<String, CompilationUnit> units = new HashMap<>();

  public void scanJava(File java) throws IOException {
    CompilationUnit cu = JavaParser.parse(java);
    String pack = cu.getPackageDeclaration().map(pck -> pck.getNameAsString() + ".").orElse("");
    for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
      addType(cu, pack, typeDeclaration);
    }
  }
  
  private void addType(CompilationUnit cu, String pack, TypeDeclaration<?> typeDeclaration) {
    String name = pack + typeDeclaration.getNameAsString();
    types.put(name, typeDeclaration);
    files.put(name, pack.replace(".", "/"));
    units.put(name, cu);
    for (BodyDeclaration<?> bodyDeclaration : typeDeclaration.getMembers()) {
      if( bodyDeclaration instanceof TypeDeclaration ) {
        addType(cu, name, (TypeDeclaration<?>)bodyDeclaration);
      }
    }
  }

  public boolean isPrimitive(String type) {
    return type.indexOf('.') == -1;
  }
  
  public String typeName(String type, TypeDeclaration<?> source) {
    CompilationUnit compilationUnit = compilationUnit(source);
    String fullType = type.substring(0, 1).toLowerCase().equals(type.subSequence(0, 1)) ? type
        : compilationUnit.getPackageDeclaration().map(pkg -> pkg.getNameAsString() + ".").orElse("")
            + type;
    boolean matched = false;
    for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
      if (importDeclaration.getNameAsString().endsWith("." + type + "")) {
        fullType = importDeclaration.getNameAsString();
        matched = true;
        break;
      }
    }
    return !matched && !types.containsKey(fullType) ? type : fullType;
  }
  
  public Optional<TypeDeclaration<?>> type(String fullType) {
    return Optional.ofNullable( types.get(fullType) );
  }

  public Optional<TypeDeclaration<?>> type(String name, TypeDeclaration<?> source) {
    return type( typeName(name, source));
  }
  
  public CompilationUnit compilationUnit(Node typeDeclaration) {
    Node currentNode = typeDeclaration;
    Optional<Node> parentNode = typeDeclaration.getParentNode();
    while (parentNode.isPresent()) {
      currentNode = parentNode.get();
      parentNode = currentNode.getParentNode();
    }
    return (CompilationUnit) currentNode;
  }
  
  public Optional<String> packageName(Node typeDeclaration) {
    return compilationUnit(typeDeclaration).getPackageDeclaration().map(pk -> pk.getNameAsString());
  }
}
