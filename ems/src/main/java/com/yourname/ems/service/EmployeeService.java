package com.yourname.ems.service;

import java.util.List;

import com.yourname.ems.dto.EmployeeRequest;
import com.yourname.ems.dto.EmployeeResponse;
import com.yourname.ems.dto.EmployeeStatsResponse;
import com.yourname.ems.model.Department;
import com.yourname.ems.model.Status;

public interface EmployeeService {

    EmployeeResponse create(EmployeeRequest request);

    EmployeeResponse getById(Long id);

    /** @param department optional filter; @param status optional filter; sortBy/order default to id/asc. */
    List<EmployeeResponse> getAll(Department department, Status status, String sortBy, String order);

    EmployeeResponse update(Long id, EmployeeRequest request);

    EmployeeResponse updateStatus(Long id, Status status);

    void delete(Long id);

    List<EmployeeResponse> search(String keyword);

    List<EmployeeResponse> getByDepartment(Department department);

    EmployeeStatsResponse getStats();
}
