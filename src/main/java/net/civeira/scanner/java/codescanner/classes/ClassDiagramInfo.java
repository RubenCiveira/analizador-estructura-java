package net.civeira.scanner.java.codescanner.classes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAbstractModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import lombok.RequiredArgsConstructor;
import net.civeira.scanner.java.Project;

@RequiredArgsConstructor
public class ClassDiagramInfo {
  private final Map<String, List<String>> forPackage = new HashMap<>();
  private final Map<String, List<String>> forPackageRel = new HashMap<>();
  private final Project project;
  private final boolean allWithMethods;
  private final List<TypeDeclaration<?>> types;
  private final List<String> painted = new ArrayList<>();

  public String scan(String pack) {
    for (TypeDeclaration<?> typeDeclaration : types) {
      typeDiagram(typeDeclaration, true);
    }
    String thePack = allWithMethods ? packName(pack) : pack;
    String content = "";
    String rels = "";
    for (Entry<String, List<String>> entry : forPackage.entrySet()) {
      // Origen.
      content += "package " + entry.getKey() + " " + (!entry.getKey().equals(thePack) ? "#DDDDDD" : "") + " {\n";
      for (String string : entry.getValue()) {
        content += string;
      }
      content += "}\n\n";
      for (String rel : forPackageRel.get(entry.getKey())) {
        rels += rel;
      }
    }
    return "@startuml\n" + content + rels + "@enduml\n";
  }

  private void typeDiagram(Node typeDeclaration, boolean withRel) {
    String lpack = project.packageName(typeDeclaration).orElse("_");
    if (!forPackage.containsKey(lpack)) {
      forPackage.put(lpack, new ArrayList<>());
      forPackageRel.put(lpack, new ArrayList<>());
    }
    List<String> rels = new ArrayList<>();
    String content = "";// forPackage.get(lpack);
    String name;
    if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
      name = ((ClassOrInterfaceDeclaration) typeDeclaration).getNameAsString();
    } else if (typeDeclaration instanceof NodeWithName) {
      name = ((NodeWithName<?>) typeDeclaration).getNameAsString();
    } else {
      name = typeDeclaration.toString();
    }
    boolean debug = name.equals("TypologyDtoMapper");
    String fname = lpack + "." + name;
    if( painted.contains(fname) ) {
      return;
    }
    if( debug ) {
      System.out.println("look en " + lpack);
    }
    painted.add( fname );
    if (typeDeclaration instanceof BodyDeclaration) {
      BodyDeclaration<?> body = (BodyDeclaration<?>) typeDeclaration;
      if (body.isAnnotationDeclaration()) {
        content += "  annotation " + name + "\n";
      } else if (body.isEnumDeclaration()) {
        content += "  enum " + name + "{\n";
        EnumDeclaration dec = (EnumDeclaration) typeDeclaration;
        for (EnumConstantDeclaration enumConstantDeclaration : dec.getEntries()) {
          content += "    " + enumConstantDeclaration.getNameAsString() + "\n";
        }
        content += "  }\n";
      }
    }
    if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
      ClassOrInterfaceDeclaration cl = (ClassOrInterfaceDeclaration) typeDeclaration;
      if (withRel) {
        for (ClassOrInterfaceType ext : cl.getImplementedTypes()) {
          rels.add( buildParent(lpack, name, cl, ext.getNameAsString(), true) );
        }
        for (ClassOrInterfaceType ext : cl.getExtendedTypes()) {
          rels.add(  buildParent(lpack, name, cl, ext.getNameAsString(), false) );
        }
      }
      content += "  " + classSterotype(cl) + "{\n";
      if (withRel || allWithMethods ) {
        for (FieldDeclaration fieldDeclaration : cl.getFields()) {
          for (VariableDeclarator variableDeclarator : fieldDeclaration.getVariables()) {
            content +=
                "    " + visibility(fieldDeclaration) + variableDeclarator.getNameAsString() + "\n";
            String fullName = project.typeName(variableDeclarator.getTypeAsString(), cl);
            if (!project.isPrimitive(fullName)) {
              rels.add( buildAggr(lpack, name, cl, variableDeclarator.getTypeAsString(), withRel) );
            }
          }
        }
        for (ConstructorDeclaration constructorDeclaration : cl.getConstructors()) {
          content += "    " + visibility(constructorDeclaration) + name + "(";
          for (Parameter parameter : constructorDeclaration.getParameters()) {
            if (content.endsWith("(")) {
              content += parameter.getNameAsString();
            } else {
              content += ", " + parameter.getNameAsString();
            }
            String fullName = project.typeName(parameter.getTypeAsString(), cl);
            if (!project.isPrimitive(fullName)) {
              rels.add( buildUses(lpack, name, cl, parameter.getTypeAsString(), withRel) );
            }
          }
          content += ")\n";
        }
        for (MethodDeclaration methodDeclaration : cl.getMethods()) {
          content +=
              "    " + visibility(methodDeclaration) + methodDeclaration.getNameAsString() + "(";
          for (Parameter parameter : methodDeclaration.getParameters()) {
            if (content.endsWith("(")) {
              content += parameter.getNameAsString();
            } else {
              content += ", " + parameter.getNameAsString();
            }
            String fullName = project.typeName(parameter.getTypeAsString(), cl);
            if (!project.isPrimitive(fullName)) {
              rels.add( buildUses(lpack, name, cl, parameter.getTypeAsString(), withRel) );
            }
          }
          content += ")\n";
        }
      }
      content += "  }\n";
    }
    
    List<String> prevContent = forPackage.get(lpack);
    List<String> prevRel = forPackageRel.get(lpack);
    for(String rel: rels) {
      if( !prevRel.contains(rel) ) {
        prevRel.add( rel );
      }
    }
    if( !prevContent.contains(content) ) {
      prevContent.add( content );
    }
  }

  private String buildParent(String lpack, String name, ClassOrInterfaceDeclaration cl, String ext,
      boolean asImplement) {
    return buildRelation(lpack, name, cl, ext, " --|> ", asImplement);
  }

  private String buildAggr(String lpack, String name, ClassOrInterfaceDeclaration cl, String ext,
      boolean asImplement) {
    return buildRelation(lpack, name, cl, ext, " --o ", asImplement);
  }

  private String buildUses(String lpack, String name, ClassOrInterfaceDeclaration cl, String ext,
      boolean asImplement) {
    return buildRelation(lpack, name, cl, ext, " --> ", asImplement);
  }

  private String buildRelation(String lpack, String name, ClassOrInterfaceDeclaration cl,
      String ext, String mod, boolean asImplement) {
    String fullName = project.typeName(ext, cl);
    Optional<TypeDeclaration<?>> type = project.type(fullName);
    if (type.isPresent()) {
      TypeDeclaration<?> rel = type.get();
      String relPack = project.packageName(rel).orElse("_");
      if( lpack.equals(relPack) || allWithMethods) {
        String on = project.packageName(rel).orElse("_");
        if (!on.equals(lpack) || allWithMethods) {
          typeDiagram(rel, false);
        }
        return name + mod + rel.getNameAsString() + "\n";
      } else {
        return "";
      }
    } else if( allWithMethods ) {
      return name + mod
          + addExternalType(asImplement || cl.isInterface() ? "interface" : "abstract", fullName)
          + "\n";
    } else {
      return "";
    }
  }

  private String addExternalType(String of, String fullName) {
    int dot = fullName.lastIndexOf('.');
    String in = dot > 0 ? fullName.substring(0, dot) : "_";
    String name = (dot > 0 ? fullName.substring(dot + 1) : fullName);
    String add = of + " " + name;
    if (!forPackage.containsKey(in)) {
      forPackage.put(in, new ArrayList<>());
      forPackageRel.put(in, new ArrayList<>());
    }
    List<String> prev = forPackage.get(in);
    if( !prev.contains(add ) ) {
      prev.add( add + "\n");
    }
    return name;
  }

  @SuppressWarnings("rawtypes")
  private String visibility(NodeWithAccessModifiers node) {
    String mod = "";
    if (node instanceof NodeWithAbstractModifier) {
      if (((NodeWithAbstractModifier) node).isAbstract()) {
        mod += "{abstract}";
      }
    }
    if (node instanceof NodeWithStaticModifier) {
      if (((NodeWithStaticModifier) node).isStatic()) {
        mod += "{static}";
      }
    }
    if (node.isPrivate()) {
      mod += "-";
    } else if (node.isProtected()) {
      mod += "#";
    } else if (node.isPublic()) {
      mod += "+";
    } else {
      mod += "~";
    }
    return mod;
  }

  private String classSterotype(ClassOrInterfaceDeclaration cl) {
    String content;
    String name = cl.getNameAsString();
    if (cl.getAnnotationByName("Data").isPresent()) {
      content = "struct " + name + "";
    } else if (cl.isInterface()) {
      content = "interface " + name + "";
    } else {
      content = "class " + name + "";
    }
    return content;
  }
  
  private String packName(String name) {
    int dot = name.lastIndexOf('.');
    return dot > 0 ? name.substring(0, dot) : "";
  }
}
