package tech.softwareologists.core;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class QueryResultTest {
    @Test
    public void constructorAndGetters_storeValues() {
        List<String> list = Arrays.asList("a", "b");
        QueryResult<String> qr = new QueryResult<>(list, 2, 10, 15);
        if (qr.getItems() != list) {
            throw new AssertionError("items mismatch");
        }
        if (qr.getPage() != 2 || qr.getPageSize() != 10 || qr.getTotal() != 15) {
            throw new AssertionError("paging values incorrect");
        }
    }
}
