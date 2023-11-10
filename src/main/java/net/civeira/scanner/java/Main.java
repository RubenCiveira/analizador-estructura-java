package net.civeira.scanner.java;

import java.io.File;
import java.io.IOException;
import java.util.List;
import net.civeira.scanner.java.adoc.Builder;
import net.civeira.scanner.java.dbscanner.DbScanner;
import net.civeira.scanner.java.diagram.Diagram;

public class Main {
  public static void main(String[] args) throws Exception {
    uu();
  }

  public static void uu() throws IOException {
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
}
