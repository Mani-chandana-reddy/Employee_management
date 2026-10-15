package com.yourname.ems.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.yourname.ems.config.AppProperties;
import com.yourname.ems.dto.EmployeeRequest;
import com.yourname.ems.dto.EmployeeResponse;
import com.yourname.ems.dto.EmployeeStatsResponse;
import com.yourname.ems.exception.DuplicateResourceException;
import com.yourname.ems.exception.InvalidRequestException;
import com.yourname.ems.exception.ResourceNotFoundException;
import com.yourname.ems.mapper.EmployeeMapper;
import com.yourname.ems.model.Department;
import com.yourname.ems.model.Employee;
import com.yourname.ems.model.Status;
import com.yourname.ems.repository.EmployeeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmployeeServiceImpl(repository, new EmployeeMapper(),
                new AppProperties("EMS", "IT", new BigDecimal("1000000")));
    }

    @Test
    void create_savesEmployeeAndDefaultsToActive() {
        when(repository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(repository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EmployeeResponse response = service.create(request("John.Doe@Example.com", new BigDecimal("75000")));

        assertEquals(1L, response.id());
        assertEquals("john.doe@example.com", response.email());
        assertEquals(Status.ACTIVE, response.status());
    }

    @Test
    void create_duplicateEmail_throwsDuplicateResourceException() {
        when(repository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.create(request("john.doe@example.com", new BigDecimal("75000"))));
        verify(repository, never()).save(any());
    }

    @Test
    void create_salaryAboveConfiguredMax_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class,
                () -> service.create(request("john.doe@example.com", new BigDecimal("2000000"))));
        verify(repository, never()).save(any());
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        when(repository.findById(15L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.getById(15L));
        assertEquals("Employee not found with id 15", ex.getMessage());
    }

    @Test
    void update_existingEmployee_updatesFields() {
        Employee existing = employee(1L, "John", "Doe", "john.doe@example.com", Department.IT, "75000");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByEmail("new.mail@example.com")).thenReturn(Optional.empty());
        when(repository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse response = service.update(1L, request("new.mail@example.com", new BigDecimal("90000")));

        assertEquals("new.mail@example.com", response.email());
        assertEquals(0, new BigDecimal("90000").compareTo(response.salary()));
    }

    @Test
    void update_emailBelongingToAnotherEmployee_throwsDuplicateResourceException() {
        Employee existing = employee(1L, "John", "Doe", "john.doe@example.com", Department.IT, "75000");
        Employee other = employee(2L, "Jane", "Roe", "taken@example.com", Department.HR, "60000");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByEmail("taken@example.com")).thenReturn(Optional.of(other));

        assertThrows(DuplicateResourceException.class,
                () -> service.update(1L, request("taken@example.com", new BigDecimal("75000"))));
        verify(repository, never()).save(any());
    }

    @Test
    void updateStatus_deactivatesEmployee() {
        Employee existing = employee(1L, "John", "Doe", "john.doe@example.com", Department.IT, "75000");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse response = service.updateStatus(1L, Status.INACTIVE);

        assertEquals(Status.INACTIVE, response.status());
    }

    @Test
    void search_matchesNameOrEmailCaseInsensitively() {
        when(repository.findAll()).thenReturn(List.of(
                employee(1L, "John", "Doe", "john.doe@example.com", Department.IT, "75000"),
                employee(2L, "Jane", "Johnson", "jane@example.com", Department.HR, "60000"),
                employee(3L, "Bob", "Smith", "bob@example.com", Department.SALES, "50000")));

        List<EmployeeResponse> result = service.search("JOHN");

        assertEquals(2, result.size());
    }

    @Test
    void search_blankKeyword_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class, () -> service.search("  "));
    }

    @Test
    void getAll_filtersByDepartmentAndStatusAndSortsBySalaryDescending() {
        Employee inactive = employee(3L, "Old", "Timer", "old@example.com", Department.IT, "90000");
        inactive.setStatus(Status.INACTIVE);
        when(repository.findAll()).thenReturn(List.of(
                employee(1L, "John", "Doe", "john@example.com", Department.IT, "75000"),
                employee(2L, "Jane", "Roe", "jane@example.com", Department.IT, "85000"),
                employee(4L, "Sam", "Hill", "sam@example.com", Department.HR, "99000"),
                inactive));

        List<EmployeeResponse> result = service.getAll(Department.IT, Status.ACTIVE, "salary", "desc");

        assertEquals(2, result.size());
        assertEquals("Jane", result.get(0).firstName());
        assertEquals("John", result.get(1).firstName());
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void getStats_returnsHeadcountAndAverageSalaryPerDepartment() {
        when(repository.findAll()).thenReturn(List.of(
                employee(1L, "John", "Doe", "john@example.com", Department.IT, "70000"),
                employee(2L, "Jane", "Roe", "jane@example.com", Department.IT, "80000")));

        EmployeeStatsResponse stats = service.getStats();

        assertEquals(2, stats.totalEmployees());
        var it = stats.departments().stream().filter(d -> d.department() == Department.IT).findFirst().orElseThrow();
        assertEquals(2, it.headcount());
        assertEquals(0, new BigDecimal("75000.00").compareTo(it.averageSalary()));
    }

    // ---------- helpers ----------

    private EmployeeRequest request(String email, BigDecimal salary) {
        return new EmployeeRequest("John", "Doe", email, "9876543210", Department.IT,
                "Software Engineer", salary, LocalDate.of(2024, 1, 15));
    }

    private Employee employee(Long id, String first, String last, String email, Department dept, String salary) {
        Employee e = new Employee();
        e.setId(id);
        e.setFirstName(first);
        e.setLastName(last);
        e.setEmail(email);
        e.setPhone("9876543210");
        e.setDepartment(dept);
        e.setDesignation("Engineer");
        e.setSalary(new BigDecimal(salary));
        e.setDateOfJoining(LocalDate.of(2024, 1, 15));
        e.setStatus(Status.ACTIVE);
        return e;
    }
}
