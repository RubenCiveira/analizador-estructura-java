package net.civeira.scanner.java.diagram;

public enum InputType {
  MERMAID("mermaid"),
  PLANTUML("plantuml");

  private final String type;

  InputType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return type;
  }
}
