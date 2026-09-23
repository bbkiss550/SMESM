package com.smeservicemanager.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @EntityGraph(attributePaths = "phones")
    Optional<Customer> findOneWithPhonesById(Long id);

    @EntityGraph(attributePaths = "phones")
    @Query("select distinct c from Customer c left join c.phones p where c.status = 'A' and (:q = '' or lower(c.customerCode) like lower(concat('%', :q, '%')) or lower(c.name) like lower(concat('%', :q, '%')) or lower(coalesce(c.companyName, '')) like lower(concat('%', :q, '%')) or lower(coalesce(c.lineId, '')) like lower(concat('%', :q, '%')) or p.phone like concat('%', :q, '%'))")
    Page<Customer> search(@Param("q") String query, Pageable pageable);

    @EntityGraph(attributePaths = "phones")
    List<Customer> findTop50ByStatusOrderByNameAsc(String status);

    boolean existsByCustomerCode(String code);
}
