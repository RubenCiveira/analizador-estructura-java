package net.civeira.scanner.java.diagram;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
