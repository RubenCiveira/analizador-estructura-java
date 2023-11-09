package net.civeira.scanner.java.dbscanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import net.civeira.scanner.java.diagram.Diagram;
import net.civeira.scanner.java.diagram.InputType;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Column;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;
import schemacrawler.schemacrawler.LimitOptionsBuilder;
import schemacrawler.schemacrawler.LoadOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.tools.utility.SchemaCrawlerUtility;
import us.fatehi.utility.LoggingConfig;
import us.fatehi.utility.datasource.DatabaseConnectionSource;
import us.fatehi.utility.datasource.DatabaseConnectionSources;
import us.fatehi.utility.datasource.MultiUseUserCredentials;


public class DbScanner {

  static {
    // System.setProperty("SC_WITHOUT_DATABASE_PLUGIN","postgresql");
  }

  public List<Diagram> generate(String connection, String user, String pass) {
    List<Diagram> diagrams = new ArrayList<>();
    String txt = "";
    String links = "";

    new LoggingConfig(Level.OFF);

    final LimitOptionsBuilder limitOptionsBuilder = LimitOptionsBuilder.builder()
        .includeTables(tableFullName -> !tableFullName.contains("_PK"));
    final LoadOptionsBuilder loadOptionsBuilder =
        LoadOptionsBuilder.builder().withSchemaInfoLevel(SchemaInfoLevelBuilder.standard());
    final SchemaCrawlerOptions options = SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
        .withLimitOptions(limitOptionsBuilder.toOptions())
        .withLoadOptions(loadOptionsBuilder.toOptions());

    final DatabaseConnectionSource dataSource = DatabaseConnectionSources
        .newDatabaseConnectionSource(connection, new MultiUseUserCredentials(user, pass));

    final Catalog catalog = SchemaCrawlerUtility.getCatalog(dataSource, options);

    for (final Schema schema : catalog.getSchemas()) {
      Collection<Table> tables = catalog.getTables(schema);
      if (!tables.isEmpty()) {
        for (final Table table : tables) {
          txt += "  table( " + table.getName() + " ) {\n";
          for (final Column column : table.getColumns()) {
            Column referencedColumn = column.getReferencedColumn();
            if (column.isPartOfPrimaryKey()) {
              txt += "    primary_key( " + column.getName() + " ): " + column.getType() + "\n";
            } else if (null != referencedColumn) {
              Table parent = referencedColumn.getParent();
              links += "" + parent.getName() + " }|--|| " + table.getName() + ": "
                  + column.getName() + "\n";
              txt +=
                  "    foreign_key( " + column.getName() + " ): " + column.getType() + " <<FK>>\n";
            } else {
              txt += "    column( " + column.getName() + " ): " + column.getType() + "\n";
            }
          }
          txt += "" + "  }\n" + "";
        }
        Diagram dg = new Diagram(InputType.PLANTUML, "db");
        dg.setContent("@startuml\n" + ""
            + "!define primary_key(x) <b><color:#b8861b><&key></color> x</b>\n"
            + "!define foreign_key(x) <color:#aaaaaa><&key></color> x\n"
            + "!define column(x) <color:#efefef><&media-record></color> x\n"
            + "!define table(x) entity x << (T, white) >>\n" + "" + txt + links + "\n@enduml\n");
        diagrams.add(dg);
      }
    }
    return diagrams;
  }
}
