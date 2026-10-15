package com.yourname.ems.controller;

import java.net.URI;
import java.util.List;

import com.yourname.ems.dto.EmployeeRequest;
import com.yourname.ems.dto.EmployeeResponse;
import com.yourname.ems.dto.EmployeeStatsResponse;
import com.yourname.ems.dto.StatusUpdateRequest;
import com.yourname.ems.model.Department;
import com.yourname.ems.model.Status;
import com.yourname.ems.service.EmployeeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employees", description = "Create, view, update, search and deactivate employee records")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Operation(summary = "Create an employee")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employee created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse created = employeeService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/employees/" + created.id())).body(created);
    }

    @Operation(summary = "List employees",
            description = "Supports optional filters (department, status) and sorting (sortBy, order).")
    @ApiResponse(responseCode = "200", description = "List of employees")
    @GetMapping
    public List<EmployeeResponse> getAll(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order) {
        Department dept = department == null ? null : Department.from(department);
        Status st = status == null ? null : Status.from(status);
        return employeeService.getAll(dept, st, sortBy, order);
    }

    @Operation(summary = "Search employees by first name, last name or email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching employees"),
            @ApiResponse(responseCode = "400", description = "Keyword missing or blank")
    })
    @GetMapping("/search")
    public List<EmployeeResponse> search(@RequestParam String keyword) {
        return employeeService.search(keyword);
    }

    @Operation(summary = "Headcount per department and average salary")
    @ApiResponse(responseCode = "200", description = "Statistics")
    @GetMapping("/stats")
    public EmployeeStatsResponse stats() {
        return employeeService.getStats();
    }

    @Operation(summary = "List employees of a department")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employees in the department"),
            @ApiResponse(responseCode = "400", description = "Unknown department")
    })
    @GetMapping("/department/{dept}")
    public List<EmployeeResponse> getByDepartment(@PathVariable("dept") String dept) {
        return employeeService.getByDepartment(Department.from(dept));
    }

    @Operation(summary = "Get an employee by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee found"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable Long id) {
        return employeeService.getById(id);
    }

    @Operation(summary = "Fully update an employee")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Employee not found"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @Operation(summary = "Activate or deactivate an employee (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @PatchMapping("/{id}/status")
    public EmployeeResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return employeeService.updateStatus(id, request.status());
    }

    @Operation(summary = "Permanently delete an employee")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Employee deleted"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        employeeService.delete(id);
    }
}
