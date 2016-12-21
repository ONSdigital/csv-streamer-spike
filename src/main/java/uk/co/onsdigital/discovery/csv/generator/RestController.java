package uk.co.onsdigital.discovery.csv.generator;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static spark.Spark.get;
import static spark.Spark.halt;

/**
 * Provides a simple REST API for streaming a CSV file.
 */
public class RestController {

    public static void main(String...args) {
        get("/data/:filename", (request, response) -> {
            final String filename = request.params(":filename") + ".csv";
            try (Reader in = reader(filename);
                 PrintWriter out = response.raw().getWriter()) {
                final CsvStreamer streamer = new CsvStreamer(in, request.queryMap().toMap());
                response.status(200);
                response.type("text/csv");
                response.header("Content-Encoding", "gzip");
                response.header("Content-Disposition", "attachment; filename=" + filename);
                out.print(streamer.getHeader());
                streamer.lines().forEach(out::print);
            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
                e.printStackTrace();
                halt(500, e.getMessage());
            }
            return response;
        });
    }

    private static Reader reader(String filename) {
        return new InputStreamReader(RestController.class.getResourceAsStream("/" + filename), StandardCharsets.ISO_8859_1);
    }

}
