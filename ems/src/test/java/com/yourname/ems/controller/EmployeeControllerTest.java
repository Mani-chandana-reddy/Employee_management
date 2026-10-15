package com.yourname.ems.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.yourname.ems.dto.EmployeeResponse;
import com.yourname.ems.exception.DuplicateResourceException;
import com.yourname.ems.exception.ResourceNotFoundException;
import com.yourname.ems.model.Department;
import com.yourname.ems.model.Status;
import com.yourname.ems.service.EmployeeService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    private static final String VALID_BODY = """
            {
              "firstName": "John",
              "lastName": "Doe",
              "email": "john.doe@example.com",
              "phone": "9876543210",
              "department": "IT",
              "designation": "Software Engineer",
              "salary": 75000.00,
              "dateOfJoining": "2024-01-15"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void create_validRequest_returns201WithLocation() throws Exception {
        when(employeeService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/employees/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void create_invalidRequest_returns400WithFieldErrors() throws Exception {
        String invalid = """
                {
                  "firstName": "J",
                  "lastName": "Doe",
                  "email": "not-an-email",
                  "phone": "123",
                  "department": "IT",
                  "designation": "Engineer",
                  "salary": -5,
                  "dateOfJoining": "2024-01-15"
                }
                """;

        mockMvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));
    }

    @Test
    void create_unknownDepartment_returns400() throws Exception {
        String body = VALID_BODY.replace("\"IT\"", "\"MARKETING\"");

        mockMvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void create_duplicateEmail_returns409() throws Exception {
        when(employeeService.create(any()))
                .thenThrow(new DuplicateResourceException("An employee with the given email already exists"));

        mockMvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getById_missing_returns404WithErrorBody() throws Exception {
        when(employeeService.getById(15L)).thenThrow(new ResourceNotFoundException("Employee not found with id 15"));

        mockMvc.perform(get("/api/v1/employees/15"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Employee not found with id 15"))
                .andExpect(jsonPath("$.path").value("/api/v1/employees/15"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getAll_returns200() throws Exception {
        when(employeeService.getAll(any(), any(), any(), any())).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/employees?department=it&sortBy=salary&order=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByDepartment_unknownDepartment_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/employees/department/MARKETING"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_returns200() throws Exception {
        when(employeeService.updateStatus(1L, Status.INACTIVE)).thenReturn(
                new EmployeeResponse(1L, "John", "Doe", "john.doe@example.com", "9876543210", Department.IT,
                        "Software Engineer", new BigDecimal("75000.00"), LocalDate.of(2024, 1, 15),
                        Status.INACTIVE));

        mockMvc.perform(patch("/api/v1/employees/1/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void delete_existing_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/employees/1")).andExpect(status().isNoContent());
    }

    @Test
    void anyRequest_includesRequestIdHeader() throws Exception {
        when(employeeService.getAll(any(), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));
    }

    private EmployeeResponse sampleResponse() {
        return new EmployeeResponse(1L, "John", "Doe", "john.doe@example.com", "9876543210", Department.IT,
                "Software Engineer", new BigDecimal("75000.00"), LocalDate.of(2024, 1, 15), Status.ACTIVE);
    }
}
