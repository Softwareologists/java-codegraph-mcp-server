package tech.softwareologists.core;

import java.util.List;

/**
 * Simple container for paginated query results.
 */
public class QueryResult<T> {
    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final int total;

    public QueryResult(List<T> items, int page, int pageSize, int total) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotal() {
        return total;
    }
}
