package uk.co.onsdigital.discovery.csv.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.fasterxml.jackson.dataformat.csv.CsvSchema.emptySchema;

/**
 * Streams and filters a CSV stream according to a set of dimension filters.
 */
public class CsvStreamer implements Closeable {
    private final CsvMapper mapper = new CsvMapper().configure(CsvParser.Feature.WRAP_AS_ARRAY, true);

    private final MappingIterator<String[]> iterator;
    private final Predicate<String[]> filter;
    private final String[] header;

    CsvStreamer(final Reader reader, Map<String, String[]> filters) throws IOException {
        this.iterator = mapper.readerFor(String[].class).with(emptySchema()).readValues(reader);
        this.header = iterator.next();
        this.filter = compileFilter(header, filters);
    }

    private static Predicate<String[]> compileFilter(final String[] header, final Map<String, String[]> dimensions) {
        final List<Filter> filters = new ArrayList<>(dimensions.size());
        for (Map.Entry<String, String[]> dimension : dimensions.entrySet()) {
            final int index = column(header, dimension.getKey());
            filters.add(new Filter(index, new HashSet<>(Arrays.asList(dimension.getValue()))));
        }

        return row -> filters.stream().allMatch(f -> f.test(row));
    }

    private static int column(String[] header, String column) {
        return IntStream.range(0, header.length).filter(i -> header[i].equalsIgnoreCase(column)).findAny()
                .orElseThrow(() -> new IllegalArgumentException("No such dimension: " + column));
    }

    public Stream<String> lines() throws IOException {
        Iterable<String[]> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false)
                .filter(filter)
                .map(this::rowToString);
    }

    public String getHeader() {
        try {
            return mapper.writeValueAsString(header);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public void close() throws IOException {
        iterator.close();
    }

    private String rowToString(String...row) {
        try {
            return mapper.writeValueAsString(row);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class Filter implements Predicate<String[]> {
        private final int index;
        private final Set<String> options;

        Filter(int index, Set<String> options) {
            this.index = index;
            this.options = options;
        }

        @Override
        public boolean test(String[] row) {
            return row != null && row.length >= index && options.contains(row[index]);
        }
    }
}
