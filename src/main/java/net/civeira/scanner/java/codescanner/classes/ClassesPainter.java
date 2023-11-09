package net.civeira.scanner.java.codescanner.classes;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.github.javaparser.ast.body.TypeDeclaration;
import net.civeira.scanner.java.Project;
import net.civeira.scanner.java.diagram.InputType;
import net.civeira.scanner.java.diagram.LocalDiagram;


public class ClassesPainter {
  public final Project project;

  public ClassesPainter(Project project) {
    this.project = project;
  }

  public List<LocalDiagram> generateClassDia(File base) {
    List<LocalDiagram> diagrams = new ArrayList<>();
    for (Entry<String, TypeDeclaration<?>> entry : this.project.types.entrySet()) {
      LocalDiagram dia = new LocalDiagram(InputType.PLANTUML, entry.getKey(),
          new File(base, entry.getKey().replace(".", "/") + ".puml"));
      dia.setContent(generateDiagram(entry.getKey(), true, Arrays.asList(entry.getValue())));
      diagrams.add(dia);
    }
    return diagrams;
  }

  public List<LocalDiagram> generatePackagesDia(File base) {
    List<LocalDiagram> diagrams = new ArrayList<>();
    Map<String, List<TypeDeclaration<?>>> paquetes = new HashMap<>();
    for (Entry<String, TypeDeclaration<?>> entry : this.project.types.entrySet()) {
      // Para cada paquete => pinto el diagrama de clases
      project.packageName(entry.getValue()).ifPresent(pack -> {
        if (!paquetes.containsKey(pack)) {
          paquetes.put(pack, new ArrayList<>());
        }
        paquetes.get(pack).add(entry.getValue());
      });
    }
    for (Entry<String, List<TypeDeclaration<?>>> entry : paquetes.entrySet()) {
      // Dibujo las clases del paquetes
      LocalDiagram dia = new LocalDiagram(InputType.PLANTUML, entry.getKey(),
          new File(base, entry.getKey().replace(".", "/") + ".puml"));
      dia.setContent(generateDiagram(entry.getKey(), false, entry.getValue()));
      diagrams.add(dia);
    }
    return diagrams;
  }

  private String generateDiagram(String pack, boolean forClass, List<TypeDeclaration<?>> types) {
    return new ClassDiagramInfo(project, forClass, types).scan(pack);
  }
}
