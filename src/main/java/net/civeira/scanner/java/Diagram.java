package net.civeira.scanner.java;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.civeira.scanner.java.kroki.InputType;

@RequiredArgsConstructor
public class Diagram {
  private StringBuilder builder = new StringBuilder();

  @Getter
  private final InputType input;
  @Getter
  private final String name;
  
  public void setContent(String str) {
    builder.append(str);
  }

  public String write() {
    return builder.toString();
  }
}
