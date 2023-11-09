package net.civeira.scanner.java.diagram;

public enum OutputFormat {
  PNG("png");

  private final String type;

  OutputFormat(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return type;
  }

}
