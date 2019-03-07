package net.florianschoppmann.asana;

import com.asana.models.ResultBodyCollection;
import com.asana.requests.CollectionRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class CollectionRequestIterable<T> implements Iterable<T> {
    private final CollectionRequest<T> request;
    private final ResultBodyCollection<T> firstPage;

    private CollectionRequestIterable(CollectionRequest<T> request, ResultBodyCollection<T> firstPage) {
        this.request = request;
        this.firstPage = firstPage;
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return new IteratorImpl<>(request, firstPage);
    }

    static <T> CollectionRequestIterable<T> of(CollectionRequest<T> request) throws IOException {
        activateOptions(request, 0, null);
        return new CollectionRequestIterable<>(request, request.executeRaw());
    }

    /**
     * Activate options for Asana Rest Client.
     *
     * Note that it is safe to set option to null. Method {@link com.asana.Client#request(com.asana.requests.Request)}
     * will not include such options.
     */
    private static <T> void activateOptions(CollectionRequest<T> request, int nextPos, @Nullable String offset) {
        request.query("offset", offset);
        long pageSize = maxValueIfNullOrNegative((Number) request.options.get("page_size"));
        long itemLimit = maxValueIfNullOrNegative((Number) request.options.get("item_limit"));
        if (pageSize < Long.MAX_VALUE || itemLimit < Long.MAX_VALUE) {
            request.query("limit", Math.min(pageSize, itemLimit - nextPos));
        } else {
            request.query("limit", null);
        }
    }

    private static long maxValueIfNullOrNegative(@Nullable Number number) {
        long longValue = number == null
            ? Long.MAX_VALUE
            : number.longValue();
        return longValue < 0
            ? Long.MAX_VALUE
            : longValue;
    }

    private static class IteratorImpl<T> implements Iterator<T> {
        private final CollectionRequest<T> request;
        @Nullable private String offset;
        @Nullable private Iterator<T> listIt;

        private int nextPos = 0;
        private ResultBodyCollection<T> currentPage;

        private IteratorImpl(CollectionRequest<T> request, @Nullable ResultBodyCollection<T> firstPage) {
            this.request = request;
            currentPage = firstPage;
            if (currentPage != null) {
                listIt = currentPage.data.iterator();
            }
        }

        @Override
        public boolean hasNext() {
            return listIt != null && listIt.hasNext();
        }

        @Nonnull
        @Override
        public T next() {
            if (listIt == null) {
                throw new NoSuchElementException("Reached end of collection returned by Asana Rest API.");
            }

            T result = listIt.next();
            ++nextPos;
            if (!listIt.hasNext()) {
                activateOptions(request, nextPos, offset);
                try {
                    currentPage = request.executeRaw();
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
                if (currentPage != null && currentPage.nextPage != null) {
                    offset = currentPage.nextPage.offset;
                    listIt = currentPage.data.iterator();
                } else {
                    offset = null;
                    listIt = null;
                }
            }
            return result;
        }
    }
}
