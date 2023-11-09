package net.civeira.scanner.java.diagram;

import java.io.File;
import lombok.Getter;

public class LocalDiagram extends Diagram {

  @Getter
  private final File file;

  public LocalDiagram(InputType input, String name, File file) {
    super(input, name);
    this.file = file;
  }
}
