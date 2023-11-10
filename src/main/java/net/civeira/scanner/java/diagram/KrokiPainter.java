//package net.civeira.scanner.java.diagram;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//
//public class KrokiPainter {
//  private static final String URL = "https://kroki.io/";
//
//  public void save(Diagram diagram, OutputFormat format, File output) {
//    String encodedSource = diagram.write();
//
//    HttpClient client = HttpClient.newHttpClient();
//    HttpRequest request =
//        HttpRequest.newBuilder().uri(URI.create(URL + "/" + diagram.getInput() + "/" + format))
//            .header("Content-Type", "text/plain")
//            .POST(HttpRequest.BodyPublishers.ofString(encodedSource)).build();
//
//    try {
//      HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
//      if (response.statusCode() == 200) {
//        try (FileOutputStream outputStream = new FileOutputStream(output)) {
//          outputStream.write(response.body());
//        } catch (IOException e) {
//          System.err.println("Error writing to output file: " + e.getMessage());
//        }
//      } else {
//        System.err.println("Error: " + response.statusCode() + " " + encodedSource );
//      }
//    } catch (IOException | InterruptedException e) {
//      e.printStackTrace();
//    }
//  }
//}
