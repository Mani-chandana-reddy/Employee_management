package com.yourname.ems.mapper;

import com.yourname.ems.dto.EmployeeRequest;
import com.yourname.ems.dto.EmployeeResponse;
import com.yourname.ems.model.Employee;

import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public Employee toEntity(EmployeeRequest request) {
        Employee employee = new Employee();
        updateEntity(employee, request);
        return employee;
    }

    /** Copies all request fields onto an existing entity (id and status are untouched). */
    public void updateEntity(Employee employee, EmployeeRequest request) {
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(normalizeEmail(request.email()));
        employee.setPhone(request.phone());
        employee.setDepartment(request.department());
        employee.setDesignation(request.designation().trim());
        employee.setSalary(request.salary());
        employee.setDateOfJoining(request.dateOfJoining());
    }

    public EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(e.getId(), e.getFirstName(), e.getLastName(), e.getEmail(), e.getPhone(),
                e.getDepartment(), e.getDesignation(), e.getSalary(), e.getDateOfJoining(), e.getStatus());
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
