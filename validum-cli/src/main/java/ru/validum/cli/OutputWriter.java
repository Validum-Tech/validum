package ru.validum.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.validum.core.export.CsvExporter;
import ru.validum.core.sql.SqlDialect;
import ru.validum.core.sql.SqlExporter;

public final class OutputWriter {

  private static final ObjectMapper JSON = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .enable(SerializationFeature.INDENT_OUTPUT);

  private final Path outputPath;
  private final OutputFormat format;
  private final SqlDialect dialect;
  private final String table;
  private final boolean withDdl;

  public OutputWriter(Path outputPath, OutputFormat format) {
    this(outputPath, format, SqlDialect.POSTGRESQL, "validum_data", true);
  }

  public OutputWriter(
    Path outputPath, OutputFormat format, SqlDialect dialect, String table, boolean withDdl) {
    this.outputPath = outputPath;
    this.format = format;
    this.dialect = dialect;
    this.table = table;
    this.withDdl = withDdl;
  }

  /** Фабрика для CLI: принимает строковые опции и не тащит диалект в командные классы. */
  public static OutputWriter of(
    Path outputPath, OutputFormat format, String dialect, String table, boolean withDdl) {
    return new OutputWriter(
      outputPath, format, SqlDialect.fromString(dialect), table, withDdl);
  }

  public void write(List<Map<String, String>> rows, Long seed) throws IOException {
    String content = switch (format) {
      case JSON -> toJsonWrapper(rows, seed);
      case JSON_ARRAY -> JSON.writeValueAsString(rows);
      case CSV -> CsvExporter.toCsv(rows);
      case SQL -> SqlExporter.script(table, rows, dialect, withDdl);
    };
    writeContent(content);
  }

  private void writeContent(String content) throws IOException {
    if (outputPath == null) {
      PrintStream out = System.out;
      out.print(content);
      if (!content.endsWith("\n") && !content.endsWith("\r\n")) {
        out.println();
      }
    } else {
      Files.writeString(outputPath, content, StandardCharsets.UTF_8);
      System.err.println("Записано в файл: " + outputPath.toAbsolutePath());
    }
  }

  private static String toJsonWrapper(List<Map<String, String>> rows, Long seed)
    throws IOException {
    Map<String, Object> wrapper = new LinkedHashMap<>();
    wrapper.put("count", rows.size());
    wrapper.put("seed", seed);
    wrapper.put("items", rows);
    return JSON.writeValueAsString(wrapper);
  }
}
