package net.civeira.scanner.java.adoc;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.civeira.scanner.java.Project;
import net.civeira.scanner.java.codescanner.classes.ClassesPainter;
import net.civeira.scanner.java.codescanner.sequence.SecuencePainter;
import net.civeira.scanner.java.diagram.LocalDiagram;

public class Builder {

  public void buildFromMaven(String group, String id, File input, File output) throws IOException {
    build(group, id, new File(input, "src/main/java"), output);
  }

  public void build(String group, String id, File input, File output) throws IOException {
    maven(group, id, output);
    Project seq = new Project();
    SecuencePainter painter = new SecuencePainter(seq);
    ClassesPainter cpainter = new ClassesPainter(seq);
    scan(seq, input, new File(output, "src/main/docs/java"));
    for (LocalDiagram diagram : painter.generateSequencesDia( new File(output, "src/main/docs/diagrams/sequence") )) {
      write(diagram.write(), diagram.getFile());
    }
    for (LocalDiagram diagram : cpainter.generatePackagesDia( new File(output, "src/main/docs/diagrams/packages") )) {
      write(diagram.write(), diagram.getFile());
    }
    for (LocalDiagram diagram : cpainter.generateClassDia( new File(output, "src/main/docs/diagrams/classes") )) {
      write(diagram.write(), diagram.getFile());
    }
  }

  private void scan(Project seq, File file, File content) {
    File[] javas = file.listFiles(filter -> {
      return filter.getName().endsWith(".java");
    });
    if (null != javas) {
      for (File java : javas) {
        try {
          seq.scanJava(java);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }
    File[] dirs = file.listFiles(File::isDirectory);
    if (null != dirs) {
      for (File dir : dirs) {
        scan(seq, dir, new File(content, dir.getName()));
      }
    }
  }

  private void maven(String group, String id, File output) throws IOException {
    // @formatter:off
    String xml = ""
        + "<?xml version=\"1.0\"?>\n"
        + "<project xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
        + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
        + "  <modelVersion>4.0.0</modelVersion>\n"
        + "  <groupId>"+group+"</groupId>\n"
        + "  <artifactId>"+id+"</artifactId>\n"
        + "  <version>0.0.2-SNAPSHOT</version>\n"
        + "  <properties>\n"
        + "    <asciidoctor.maven.plugin.version>2.2.2</asciidoctor.maven.plugin.version>\n"
        + "    <gem.home>${basedir}/../develop/bin/rubygems/</gem.home>\n"
        + "    <gem.path>${basedir}/../develop/bin/rubygems/</gem.path>\n"
        + "    <kroki.server>https://kroki.io</kroki.server>\n"
        + "    <net.kroki.server>https://kroki.io</net.kroki.server>\n"
        + "    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n"
        + "    <project.reporting.sourceEncoding>UTF-8</project.reporting.sourceEncoding>\n"
        + "  </properties>\n"
        + "  <repositories>\n"
        + "    <repository>\n"
        + "      <id>mavengems</id>\n"
        + "      <name>Paquetes binarios de ruby para maven</name>\n"
        + "      <url>mavengem:https://rubygems.org</url>\n"
        + "    </repository>\n"
        + "  </repositories>\n"
        + "  <build>\n"
        + "    <extensions>\n"
        + "      <extension>\n"
        + "        <groupId>org.torquebox.mojo</groupId>\n"
        + "        <artifactId>mavengem-wagon</artifactId>\n"
        + "        <version>1.0.3</version>\n"
        + "      </extension>\n"
        + "    </extensions>\n"
        + "    <plugins>\n"
        + "      <plugin>\n"
        + "        <groupId>de.saumya.mojo</groupId>\n"
        + "        <artifactId>gem-maven-plugin</artifactId>\n"
        + "        <version>2.0.1</version>\n"
        + "        <executions>\n"
        + "          <execution>\n"
        + "            <id>install-gems-for-asciidoctor</id>\n"
        + "            <goals>\n"
        + "              <goal>sets</goal>\n"
        + "            </goals>\n"
        + "            <configuration>\n"
        + "              <gems>\n"
        + "                <asciidoctor-pdf>2.1.2</asciidoctor-pdf>\n"
        + "                <asciidoctor-kroki>0.6.0</asciidoctor-kroki>\n"
        + "              </gems>\n"
        + "              <jrubyVersion>9.2.5.0</jrubyVersion>\n"
        + "            </configuration>\n"
        + "          </execution>\n"
        + "        </executions>\n"
        + "        <configuration />\n"
        + "      </plugin>\n"
        + "    </plugins>\n"
        + "  </build>\n"
        + "  <profiles>\n"
        + "    <profile>\n"
        + "      <id>pdf</id>\n"
        + "      <build>\n"
        + "        <plugins>\n"
        + "          <plugin>\n"
        + "            <groupId>org.asciidoctor</groupId>\n"
        + "            <artifactId>asciidoctor-maven-plugin</artifactId>\n"
        + "            <version>2.2.2</version>\n"
        + "            <executions>\n"
        + "              <execution>\n"
        + "                <id>convert-to-pdf</id>\n"
        + "                <phase>compile</phase>\n"
        + "                <goals>\n"
        + "                  <goal>process-asciidoc</goal>\n"
        + "                </goals>\n"
        + "                <configuration>\n"
        + "                  <attributes>\n"
        + "                    <source-highlighter>coderay</source-highlighter>\n"
        + "                    <kroki-server-url>${kroki.server}</kroki-server-url>\n"
        + "                    <imagesdir>../images</imagesdir>\n"
        + "                    <kroki-fetch-diagram>true</kroki-fetch-diagram>\n"
        + "                    <toclevels>3</toclevels>\n"
        + "                    <kroki-default-format>png</kroki-default-format>\n"
        + "                    <icons>font</icons>\n"
        + "                    <kroki-http-method>post</kroki-http-method>\n"
        + "                    <toclevels>3</toclevels>\n"
        + "                    <icons>font</icons>\n"
        + "                    <toc>right</toc>\n"
        + "                    <allow-uri-read>true</allow-uri-read>\n"
        + "                    <pdf-theme>./</pdf-theme>\n"
        + "                    <pdf-themesdir>./</pdf-themesdir>\n"
        + "                  </attributes>\n"
        + "                  <baseDir>${project.build.directory}/tmp/books</baseDir>\n"
        + "                  <sourceDirectory>${project.build.directory}/tmp/books/</sourceDirectory>\n"
        + "                  <preserveDirectories>true</preserveDirectories>\n"
        + "                  <relativeBaseDir>true</relativeBaseDir>\n"
        + "                  <requires>\n"
        + "                    <require>asciidoctor-kroki</require>\n"
        + "                    <require>asciidoctor-pdf</require>\n"
        + "                  </requires>\n"
        + "                  <outputDirectory>${project.build.directory}/tmp/out</outputDirectory>\n"
        + "                  <gemPath>${gem.home}</gemPath>\n"
        + "                  <backend>pdf</backend>\n"
        + "                  <allow-uri-read>true</allow-uri-read>\n"
        + "                </configuration>\n"
        + "              </execution>\n"
        + "            </executions>\n"
        + "            <dependencies>\n"
        + "              <dependency>\n"
        + "                <groupId>org.jruby</groupId>\n"
        + "                <artifactId>jruby</artifactId>\n"
        + "                <version>9.3.8.0</version>\n"
        + "              </dependency>\n"
        + "              <dependency>\n"
        + "                <groupId>org.asciidoctor</groupId>\n"
        + "                <artifactId>asciidoctorj</artifactId>\n"
        + "                <version>2.5.7</version>\n"
        + "              </dependency>\n"
        + "              <dependency>\n"
        + "                <groupId>org.asciidoctor</groupId>\n"
        + "                <artifactId>asciidoctorj-pdf</artifactId>\n"
        + "                <version>2.3.3</version>\n"
        + "              </dependency>\n"
        + "            </dependencies>\n"
        + "            <configuration />\n"
        + "          </plugin>\n"
        + "          <plugin>\n"
        + "            <artifactId>maven-resources-plugin</artifactId>\n"
        + "            <version>3.2.0</version>\n"
        + "            <executions>\n"
        + "              <execution>\n"
        + "                <id>copy-resources-all</id>\n"
        + "                <phase>validate</phase>\n"
        + "                <goals>\n"
        + "                  <goal>copy-resources</goal>\n"
        + "                </goals>\n"
        + "                <configuration>\n"
        + "                  <resources>\n"
        + "                    <resource>\n"
        + "                      <directory>${basedir}/src/main/docs</directory>\n"
        + "                      <filtering>false</filtering>\n"
        + "                    </resource>\n"
        + "                  </resources>\n"
        + "                  <outputDirectory>${project.build.directory}/tmp</outputDirectory>\n"
        + "                  <overwrite>true</overwrite>\n"
        + "                </configuration>\n"
        + "              </execution>\n"
        + "              <execution>\n"
        + "                <id>copy-pdf-files</id>\n"
        + "                <phase>compile</phase>\n"
        + "                <goals>\n"
        + "                  <goal>copy-resources</goal>\n"
        + "                </goals>\n"
        + "                <configuration>\n"
        + "                  <resources>\n"
        + "                    <resource>\n"
        + "                      <directory>${project.build.directory}/tmp/out</directory>\n"
        + "                      <filtering>false</filtering>\n"
        + "                    </resource>\n"
        + "                  </resources>\n"
        + "                  <outputDirectory>${project.build.directory}/pdf</outputDirectory>\n"
        + "                  <overwrite>true</overwrite>\n"
        + "                </configuration>\n"
        + "              </execution>\n"
        + "            </executions>\n"
        + "            <configuration />\n"
        + "          </plugin>\n"
        + "        </plugins>\n"
        + "      </build>\n"
        + "    </profile>\n"
        + "    <profile>\n"
        + "      <id>html</id>\n"
        + "      <activation />\n"
        + "      <build>\n"
        + "        <plugins>\n"
        + "          <plugin>\n"
        + "              <artifactId>maven-clean-plugin</artifactId>\n"
        + "              <version>2.4.1</version>\n"
        + "              <configuration>\n"
        + "                  <filesets>\n"
        + "                      <fileset>\n"
        + "                          <directory>pub/html</directory>\n"
        + "                          <includes>\n"
        + "                              <include>**</include>\n"
        + "                          </includes>\n"
        + "                          <followSymlinks>false</followSymlinks>\n"
        + "                      </fileset>\n"
        + "                  </filesets>\n"
        + "              </configuration>\n"
        + "          </plugin>        \n"
        + "          <plugin>\n"
        + "            <groupId>org.asciidoctor</groupId>\n"
        + "            <artifactId>asciidoctor-maven-plugin</artifactId>\n"
        + "            <version>2.2.2</version>\n"
        + "            <executions>\n"
        + "              <execution>\n"
        + "                <id>convert-to-html</id>\n"
        + "                <phase>compile</phase>\n"
        + "                <goals>\n"
        + "                  <goal>process-asciidoc</goal>\n"
        + "                </goals>\n"
        + "                <configuration>\n"
        + "                  <attributes>\n"
        + "                    <source-highlighter>coderay</source-highlighter>\n"
        + "                    <kroki-server-url>${kroki.server}</kroki-server-url>\n"
        + "                    <imagesdir>./images</imagesdir>\n"
        + "                    <kroki-fetch-diagram>true</kroki-fetch-diagram>\n"
        + "                    <toclevels>3</toclevels>\n"
        + "                    <kroki-default-format>png</kroki-default-format>\n"
        + "                    <icons>font</icons>\n"
        + "                    <kroki-http-method>post</kroki-http-method>\n"
        + "                    <toc>right</toc>\n"
        + "                  </attributes>\n"
        + "                  <sourceDirectory>${basedir}/src/main/docs/books/</sourceDirectory>\n"
        + "                  <preserveDirectories>true</preserveDirectories>\n"
        + "                  <relativeBaseDir>true</relativeBaseDir>\n"
        + "                  <outputDirectory>${project.build.directory}/html</outputDirectory>\n"
        + "                  <requires>\n"
        + "                    <require>asciidoctor-kroki</require>\n"
        + "                  </requires>\n"
        + "                  <gemPath>${gem.home}</gemPath>\n"
        + "                  <backend>html</backend>\n"
        + "                </configuration>\n"
        + "              </execution>\n"
        + "            </executions>\n"
        + "            <dependencies>\n"
        + "              <dependency>\n"
        + "                <groupId>org.jruby</groupId>\n"
        + "                <artifactId>jruby</artifactId>\n"
        + "                <version>9.3.8.0</version>\n"
        + "              </dependency>\n"
        + "              <dependency>\n"
        + "                <groupId>org.asciidoctor</groupId>\n"
        + "                <artifactId>asciidoctorj</artifactId>\n"
        + "                <version>2.5.7</version>\n"
        + "              </dependency>\n"
        + "              <dependency>\n"
        + "                <groupId>org.asciidoctor</groupId>\n"
        + "                <artifactId>asciidoctorj-pdf</artifactId>\n"
        + "                <version>2.3.3</version>\n"
        + "              </dependency>\n"
        + "            </dependencies>\n"
        + "            <configuration />\n"
        + "          </plugin>\n"
        + "          <plugin>\n"
        + "            <artifactId>maven-resources-plugin</artifactId>\n"
        + "            <version>3.2.0</version>\n"
        + "            <executions>\n"
        + "              <execution>\n"
        + "                <id>copy-resources-all</id>\n"
        + "                <phase>validate</phase>\n"
        + "                <goals>\n"
        + "                  <goal>copy-resources</goal>\n"
        + "                </goals>\n"
        + "                <configuration>\n"
        + "                  <resources>\n"
        + "                    <resource>\n"
        + "                      <directory>${basedir}/src/main/docs/images</directory>\n"
        + "                      <filtering>false</filtering>\n"
        + "                    </resource>\n"
        + "                  </resources>\n"
        + "                  <outputDirectory>${project.build.directory}/html/images</outputDirectory>\n"
        + "                  <overwrite>true</overwrite>\n"
        + "                </configuration>\n"
        + "              </execution>\n"
        + "            </executions>\n"
        + "            <configuration />\n"
        + "          </plugin>\n"
        + "          <plugin>\n"
        + "              <artifactId>maven-resources-plugin</artifactId>\n"
        + "              <version>3.0.2</version>\n"
        + "              <executions>\n"
        + "                  <execution>\n"
        + "                      <id>copy-resource-one</id>\n"
        + "                      <phase>package</phase>\n"
        + "                      <goals>\n"
        + "                          <goal>copy-resources</goal>\n"
        + "                      </goals>\n"
        + "                      <configuration>\n"
        + "                          <outputDirectory>${basedir}/pub/html</outputDirectory>\n"
        + "                          <resources>\n"
        + "                              <resource>\n"
        + "                                  <directory>${project.build.directory}/html</directory>\n"
        + "                                  <includes>\n"
        + "                                      <include>**</include>\n"
        + "                                  </includes>\n"
        + "                              </resource>\n"
        + "                          </resources>\n"
        + "                      </configuration>\n"
        + "                  </execution>\n"
        + "              </executions>\n"
        + "          </plugin>\n"
        + "        </plugins>\n"
        + "      </build>\n"
        + "    </profile>\n"
        + "  </profiles>\n"
        + "</project>\n"
        + "";
    // @formatter:on
    write(xml, new File(output, "pom.xml"));
  }

  private void write(String content, File file) throws IOException {
    if (!file.exists()) {
      file.getParentFile().mkdirs();
    }
    String mark = "# AUTOGENERATED\n";
    if( file.getName().endsWith(".puml") ) {
      mark = "' AUTOGENERATED\n";
    } else if( file.getName().endsWith(".adoc") ) {
      mark = "// AUTOGENERATED\n";
    }
    boolean write = true;
    if( file.exists() ) {
      String all = new String( Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      write = all.startsWith(mark);
    }
    if( write ) {
      content = mark + content;
      Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }
  }
}
