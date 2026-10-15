package com.yourname.ems.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Internal domain model. Never returned directly from the API - use EmployeeResponse. */
public class Employee {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Department department;
    private String designation;
    private BigDecimal salary;
    private LocalDate dateOfJoining;
    private Status status = Status.ACTIVE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }

    public LocalDate getDateOfJoining() { return dateOfJoining; }
    public void setDateOfJoining(LocalDate dateOfJoining) { this.dateOfJoining = dateOfJoining; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
