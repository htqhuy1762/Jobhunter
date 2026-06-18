package vn.hoidanit.searchservice.dto;

import org.springframework.util.StringUtils;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchJobRequest {

    private String keyword;
    private String location;
    private String level;
    private Long companyId;
    private Boolean active;
    private Double minSalary;
    private Double maxSalary;
    private int page;
    private int size;
    private String sortBy;
    private String sortDirection;

    public int normalizedPage() {
        return Math.max(1, page);
    }

    public int normalizedSize() {
        return Math.min(100, Math.max(1, size));
    }

    public String normalizedSortDirection() {
        return "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";
    }

    public String normalizedSortBy() {
        if (!StringUtils.hasText(sortBy)) {
            return "updatedAt";
        }
        return sortBy;
    }
}

