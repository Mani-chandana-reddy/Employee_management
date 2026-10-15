package com.yourname.ems.repository;

import java.util.List;
import java.util.Optional;

import com.yourname.ems.model.Employee;

/** Data-access contract. Phase 9 replaces the in-memory implementation with JPA. */
public interface EmployeeRepository {

    Employee save(Employee employee);

    Optional<Employee> findById(Long id);

    Optional<Employee> findByEmail(String email);

    List<Employee> findAll();

    boolean existsById(Long id);

    boolean existsByEmail(String email);

    void deleteById(Long id);
}
