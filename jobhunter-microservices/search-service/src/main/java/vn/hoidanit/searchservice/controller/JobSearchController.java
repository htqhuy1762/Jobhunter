package vn.hoidanit.searchservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import vn.hoidanit.searchservice.dto.SearchJobRequest;
import vn.hoidanit.searchservice.dto.SearchJobResponse;
import vn.hoidanit.searchservice.service.JobSearchService;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class JobSearchController {

    private final JobSearchService jobSearchService;

    @GetMapping("/jobs")
    public ResponseEntity<SearchJobResponse> searchJobs(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "location", required = false) String location,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "companyId", required = false) Long companyId,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "minSalary", required = false) Double minSalary,
            @RequestParam(name = "maxSalary", required = false) Double maxSalary,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "updatedAt") String sort,
            @RequestParam(name = "direction", defaultValue = "desc") String direction) {

        SearchJobRequest request = SearchJobRequest.builder()
                .keyword(keyword)
                .location(location)
                .level(level)
                .companyId(companyId)
                .active(active)
                .minSalary(minSalary)
                .maxSalary(maxSalary)
                .page(page)
                .size(size)
                .sortBy(sort)
                .sortDirection(direction)
                .build();

        return ResponseEntity.ok(jobSearchService.search(request));
    }
}

