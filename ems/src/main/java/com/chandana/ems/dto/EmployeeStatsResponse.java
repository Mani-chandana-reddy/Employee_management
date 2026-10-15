package com.yourname.ems.dto;

import java.util.List;

public record EmployeeStatsResponse(long totalEmployees, List<DepartmentStats> departments) {
}
