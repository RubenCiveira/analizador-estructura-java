package net.civeira.scanner.java;

import java.io.File;
import lombok.Getter;
import net.civeira.scanner.java.kroki.InputType;

public class LocalDiagram extends Diagram {

  @Getter
  private final File file;

  public LocalDiagram(InputType input, String name, File file) {
    super(input, name);
    this.file = file;
  }
}
