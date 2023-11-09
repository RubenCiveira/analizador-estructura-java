package net.civeira.scanner.java;

import java.io.File;
import java.io.IOException;
import java.util.List;
import net.civeira.scanner.java.adoc.Builder;
import net.civeira.scanner.java.dbscanner.DbScanner;
import net.civeira.scanner.java.diagram.Diagram;
import net.civeira.scanner.java.diagram.InputType;
import net.civeira.scanner.java.diagram.KrokiPainter;
import net.civeira.scanner.java.diagram.OutputFormat;

public class Main {
  public static void main(String[] args) throws Exception {
    uu();
    // Diagram dia = ejemplo();
//    List<Diagram> dia = ddbb();
//    KrokiPainter painter = new KrokiPainter();
//    for (Diagram dg : dia) {
//      painter.save(dg, OutputFormat.PNG, new File("esp.png"));
//    }
  }

  public static void uu() throws IOException {
    // /Users/ruben.civeiraiglesia/Documents/Proyectos/i+d/generado
    // /Users/ruben.civeiraiglesia/eclipse-workspace/rata
    Builder build = new Builder();
//    build.build("test", "pruebas",
//        new File("/Users/ruben.civeiraiglesia/eclipse-workspace/rata/src"),
//        new File("/Users/ruben.civeiraiglesia/Documents/Proyectos/i+d/generado"));
    build.build("test", "pruebas",
        new File("/Users/ruben.civeiraiglesia/Documents/Proyectos/i+d/typology/typology-back"),
        new File("/Users/ruben.civeiraiglesia/Documents/Proyectos/i+d/generado"));

  }

  public static List<Diagram> ddbb() {
    DbScanner scanner = new DbScanner();
    return scanner.generate("jdbc:postgresql://localhost:5432/postgres?currentSchema=agora",
        "agorauser", "agorapass");
  }

  public static Diagram ejemplo() {
    Diagram dia = new Diagram(InputType.PLANTUML, "sample");
    dia.setContent("""
        skinparam ranksep 20
        skinparam dpi 125
        skinparam packageTitleAlignment left

        rectangle "Main" {
          (main.view)
          (singleton)
        }
        rectangle "Base" {
          (base.component)
          (component)
          (model)
        }
        rectangle "<b>main.ts</b>" as main_ts

        (component) ..> (base.component)
        main_ts ==> (main.view)
        (main.view) --> (component)
        (main.view) ...> (singleton)
        (singleton) ---> (model)
                    """);
    return dia;
  }
}
