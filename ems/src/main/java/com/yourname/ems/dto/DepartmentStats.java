package com.yourname.ems.dto;

import java.math.BigDecimal;

import com.yourname.ems.model.Department;

public record DepartmentStats(Department department, long headcount, BigDecimal averageSalary) {
}
