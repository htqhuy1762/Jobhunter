package vn.hoidanit.companyservice.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.hoidanit.companyservice.annotation.PageableDefault;
import vn.hoidanit.companyservice.domain.Company;
import vn.hoidanit.companyservice.domain.response.RestResponse;
import vn.hoidanit.companyservice.dto.ResultPaginationDTO;
import vn.hoidanit.companyservice.service.CompanyService;
import vn.hoidanit.companyservice.util.SecurityUtil;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {
    private final CompanyService companyService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_HR', 'ROLE_ADMIN')")
    public ResponseEntity<RestResponse<Company>> createCompany(@Valid @RequestBody Company reqCompany) {
        log.info("User {} is creating company: {}", SecurityUtil.getCurrentUserInfo(), reqCompany.getName());
        Company company = companyService.createCompany(reqCompany);
        return RestResponse.created(company, "Create company successfully");
    }

    @GetMapping
    public ResponseEntity<RestResponse<ResultPaginationDTO>> getCompany(
            @Filter Specification<Company> spec,
            @PageableDefault(page = 1, size = 10, sort = "id", direction = "desc") Pageable pageable) {

        ResultPaginationDTO result = companyService.getAllCompanies(spec, pageable);
        return RestResponse.ok(result, "Fetch companies successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<Company>> getCompanyById(@PathVariable("id") long id) {
        Optional<Company> company = companyService.findById(id);
        return RestResponse.ok(company.orElse(null), "Fetch company by id successfully");
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('ROLE_HR', 'ROLE_ADMIN')")
    public ResponseEntity<RestResponse<Company>> updateCompany(@Valid @RequestBody Company reqCompany) {
        log.info("User {} is updating company ID: {}", SecurityUtil.getCurrentUserInfo(), reqCompany.getId());
        Company updatedCompany = companyService.updateCompany(reqCompany);
        return RestResponse.ok(updatedCompany, "Update company successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<RestResponse<Void>> deleteCompany(@PathVariable("id") long id) {
        log.warn("User {} is attempting to delete company ID: {}", SecurityUtil.getCurrentUserInfo(), id);
        companyService.deleteCompany(id);
        return RestResponse.ok(null, "Delete company successfully");
    }
}
