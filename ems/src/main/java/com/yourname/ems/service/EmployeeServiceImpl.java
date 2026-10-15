package com.yourname.ems.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.yourname.ems.config.AppProperties;
import com.yourname.ems.dto.DepartmentStats;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeRepository repository;
    private final EmployeeMapper mapper;
    private final AppProperties props;

    public EmployeeServiceImpl(EmployeeRepository repository, EmployeeMapper mapper, AppProperties props) {
        this.repository = repository;
        this.mapper = mapper;
        this.props = props;
    }

    @Override
    public EmployeeResponse create(EmployeeRequest request) {
        log.debug("Creating employee in department {}", request.department());
        validateSalary(request.salary());
        String email = EmployeeMapper.normalizeEmail(request.email());
        if (repository.existsByEmail(email)) {
            throw new DuplicateResourceException("An employee with the given email already exists");
        }
        Employee saved = repository.save(mapper.toEntity(request));
        log.info("Employee created with id {}", saved.getId());
        return mapper.toResponse(saved);
    }

    @Override
    public EmployeeResponse getById(Long id) {
        log.debug("Fetching employee with id {}", id);
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public List<EmployeeResponse> getAll(Department department, Status status, String sortBy, String order) {
        log.debug("Listing employees: department={}, status={}, sortBy={}, order={}",
                department, status, sortBy, order);
        Comparator<Employee> comparator = comparatorFor(sortBy);
        String direction = order == null ? "asc" : order.trim().toLowerCase(Locale.ROOT);
        if (direction.equals("desc")) {
            comparator = comparator.reversed();
        } else if (!direction.equals("asc")) {
            throw new InvalidRequestException("Invalid order '" + order + "'. Allowed values: asc, desc");
        }
        return repository.findAll().stream()
                .filter(e -> department == null || e.getDepartment() == department)
                .filter(e -> status == null || e.getStatus() == status)
                .sorted(comparator)
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        log.debug("Updating employee with id {}", id);
        Employee employee = findOrThrow(id);
        validateSalary(request.salary());
        String email = EmployeeMapper.normalizeEmail(request.email());
        repository.findByEmail(email)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("An employee with the given email already exists");
                });
        mapper.updateEntity(employee, request);
        Employee saved = repository.save(employee);
        log.info("Employee updated with id {}", id);
        return mapper.toResponse(saved);
    }

    @Override
    public EmployeeResponse updateStatus(Long id, Status status) {
        Employee employee = findOrThrow(id);
        employee.setStatus(status);
        Employee saved = repository.save(employee);
        log.info("Employee {} status set to {}", id, status);
        return mapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found with id " + id);
        }
        repository.deleteById(id);
        log.info("Employee deleted with id {}", id);
    }

    @Override
    public List<EmployeeResponse> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new InvalidRequestException("Search keyword must not be blank");
        }
        String needle = keyword.trim().toLowerCase(Locale.ROOT);
        log.debug("Searching employees (keyword length {})", needle.length());
        return repository.findAll().stream()
                .filter(e -> e.getFirstName().toLowerCase(Locale.ROOT).contains(needle)
                        || e.getLastName().toLowerCase(Locale.ROOT).contains(needle)
                        || (e.getFirstName() + " " + e.getLastName()).toLowerCase(Locale.ROOT).contains(needle)
                        || e.getEmail().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparing(Employee::getId))
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<EmployeeResponse> getByDepartment(Department department) {
        return getAll(department, null, "id", "asc");
    }

    @Override
    public EmployeeStatsResponse getStats() {
        List<Employee> all = repository.findAll();
        List<DepartmentStats> stats = Arrays.stream(Department.values())
                .map(dept -> {
                    List<Employee> inDept = all.stream().filter(e -> e.getDepartment() == dept).toList();
                    BigDecimal average = inDept.isEmpty()
                            ? BigDecimal.ZERO.setScale(2)
                            : inDept.stream().map(Employee::getSalary)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .divide(BigDecimal.valueOf(inDept.size()), 2, RoundingMode.HALF_UP);
                    return new DepartmentStats(dept, inDept.size(), average);
                })
                .toList();
        return new EmployeeStatsResponse(all.size(), stats);
    }

    // ---------- helpers ----------

    private Employee findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    /** Business rule driven by the app.max-salary configuration property. */
    private void validateSalary(BigDecimal salary) {
        if (salary.compareTo(props.maxSalary()) > 0) {
            throw new InvalidRequestException("Salary must not exceed " + props.maxSalary().toPlainString());
        }
    }

    private Comparator<Employee> comparatorFor(String sortBy) {
        String key = sortBy == null || sortBy.isBlank() ? "id" : sortBy.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "id" -> Comparator.comparing(Employee::getId);
            case "firstname" -> Comparator.comparing(Employee::getFirstName, String.CASE_INSENSITIVE_ORDER);
            case "lastname" -> Comparator.comparing(Employee::getLastName, String.CASE_INSENSITIVE_ORDER);
            case "email" -> Comparator.comparing(Employee::getEmail);
            case "department" -> Comparator.comparing(Employee::getDepartment);
            case "designation" -> Comparator.comparing(Employee::getDesignation, String.CASE_INSENSITIVE_ORDER);
            case "salary" -> Comparator.comparing(Employee::getSalary);
            case "dateofjoining" -> Comparator.comparing(Employee::getDateOfJoining);
            case "status" -> Comparator.comparing(Employee::getStatus);
            default -> throw new InvalidRequestException("Invalid sortBy '" + sortBy + "'. Allowed values: "
                    + "id, firstName, lastName, email, department, designation, salary, dateOfJoining, status");
        };
    }
}
