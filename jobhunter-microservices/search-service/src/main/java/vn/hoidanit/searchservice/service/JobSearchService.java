package vn.hoidanit.searchservice.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import vn.hoidanit.searchservice.domain.JobDocument;
import vn.hoidanit.searchservice.dto.SearchJobRequest;
import vn.hoidanit.searchservice.dto.SearchJobResponse;

@Service
@RequiredArgsConstructor
public class JobSearchService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "salary", "createdAt", "updatedAt", "startDate", "endDate");

    private final ElasticsearchOperations operations;

    public SearchJobResponse search(SearchJobRequest request) {
        String sortBy = sanitizeSortField(request.normalizedSortBy());
        Sort.Direction direction = "asc".equalsIgnoreCase(request.normalizedSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(request.normalizedPage() - 1, request.normalizedSize(),
                Sort.by(direction, sortBy));

        Criteria criteria = buildCriteria(request);
        Query query = criteria == null
                ? new StringQuery("{\"match_all\": {}}")
                : new CriteriaQuery(criteria);
        query.setPageable(pageable);

        SearchHits<JobDocument> hits = operations.search(query, JobDocument.class);
        List<SearchJobResponse.Item> items = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toResponseItem)
                .toList();

        long total = hits.getTotalHits();
        int pages = request.normalizedSize() == 0
                ? 0
                : (int) Math.ceil((double) total / request.normalizedSize());

        SearchJobResponse.Meta meta = new SearchJobResponse.Meta(
                request.normalizedPage(),
                request.normalizedSize(),
                total,
                pages);

        return new SearchJobResponse(meta, items);
    }

    private String sanitizeSortField(String sortBy) {
        if (!StringUtils.hasText(sortBy) || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            return "updatedAt";
        }
        return sortBy;
    }

    private Criteria buildCriteria(SearchJobRequest request) {
        Criteria criteria = null;

        if (StringUtils.hasText(request.getKeyword())) {
            Criteria keywordCriteria = new Criteria("name").matches(request.getKeyword())
                    .or(new Criteria("description").matches(request.getKeyword()));
            criteria = addAnd(criteria, keywordCriteria);
        }

        if (StringUtils.hasText(request.getLocation())) {
            criteria = addAnd(criteria, new Criteria("location").is(request.getLocation()));
        }

        if (StringUtils.hasText(request.getLevel())) {
            criteria = addAnd(criteria, new Criteria("level").is(request.getLevel()));
        }

        if (request.getCompanyId() != null) {
            criteria = addAnd(criteria, new Criteria("companyId").is(request.getCompanyId()));
        }

        if (request.getActive() != null) {
            criteria = addAnd(criteria, new Criteria("active").is(request.getActive()));
        }

        if (request.getMinSalary() != null || request.getMaxSalary() != null) {
            Criteria salaryCriteria = new Criteria("salary");
            if (request.getMinSalary() != null) {
                salaryCriteria = salaryCriteria.greaterThanEqual(request.getMinSalary());
            }
            if (request.getMaxSalary() != null) {
                salaryCriteria = salaryCriteria.lessThanEqual(request.getMaxSalary());
            }
            criteria = addAnd(criteria, salaryCriteria);
        }

        return criteria;
    }

    private Criteria addAnd(Criteria existing, Criteria toAdd) {
        return existing == null ? toAdd : existing.and(toAdd);
    }

    private SearchJobResponse.Item toResponseItem(JobDocument job) {
        return new SearchJobResponse.Item(
                job.getId(),
                job.getName(),
                job.getDescription(),
                job.getLocation(),
                job.getSalary(),
                job.getQuantity(),
                job.getLevel(),
                job.getCompanyId(),
                job.getActive(),
                job.getUpdatedAt());
    }
}



