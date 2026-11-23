package com.barangay.zshared.mapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic pagination wrapper DTO representing paginated query results.
 *
 * @param <T> the type of items contained in the page
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private boolean first;

    public int getNumber() {
        return pageNumber;
    }

    public int getSize() {
        return pageSize;
    }

    /**
     * Creates a {@link PageResponse} instance wrapping the provided Spring Data {@link Page}.
     *
     * @param page Spring Data page, may be {@code null}
     * @param <T>  item element type
     * @return a mapped {@link PageResponse}, or {@code null} if page is {@code null}
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        if (page == null) {
            return null;
        }

        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
