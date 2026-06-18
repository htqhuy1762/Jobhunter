package vn.hoidanit.searchservice.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import vn.hoidanit.searchservice.domain.JobDocument;

@Repository
public interface JobSearchRepository extends ElasticsearchRepository<JobDocument, Long> {
}

