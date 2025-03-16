package idespring.lab4.service.groupservice;

import idespring.lab4.exceptions.EntityNotFoundException;
import idespring.lab4.model.Group;
import idespring.lab4.model.Student;
import idespring.lab4.repository.grouprepo.GroupRepository;
import idespring.lab4.repository.studentrepo.StudentRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupServiceImpl implements GroupService {
    private final GroupRepository groupRepository;
    private final StudentRepository studentRepository;
    private static final Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

    @Autowired
    public GroupServiceImpl(GroupRepository groupRepository, StudentRepository studentRepository) {
        this.groupRepository = groupRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    @Cacheable(value = "groups", key = "'allGroups' + #namePattern + #sort",
            unless = "#result == null or #result.isEmpty()")
    public List<Group> readGroups(String namePattern, String sort) {
        long start = System.nanoTime();
        logger.info("Fetching groups with namePattern: {}, sort: {}", namePattern, sort);

        List<Group> groups;
        if (namePattern != null) {
            groups = groupRepository.findByNameContaining(namePattern);
        } else if (sort != null && sort.equalsIgnoreCase("asc")) {
            groups = groupRepository.findAllByOrderByNameAsc();
        } else {
            groups = groupRepository.findAll();
        }

        long end = System.nanoTime();
        logger.info("Execution time for readGroups: {} ms", (end - start) / 1_000_000);
        return groups;
    }

    @Override
    @Cacheable(value = "groups", key = "#id", unless = "#result == null")
    public Group findById(Long id) {
        long start = System.nanoTime();
        logger.info("Fetching group by ID: {}", id);

        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + id));

        long end = System.nanoTime();
        logger.info("Execution time for findById: {} ms", (end - start) / 1_000_000);
        return group;
    }

    @Override
    @Cacheable(value = "groups", key = "'name_' + #name", unless = "#result == null")
    public Group findByName(String name) {
        long start = System.nanoTime();
        logger.info("Fetching group by name: {}", name);

        Group group = groupRepository.findByName(name)
                .orElseThrow(() ->
                        new EntityNotFoundException("Group not found with name: " + name));

        long end = System.nanoTime();
        logger.info("Execution time for findByName: {} ms", (end - start) / 1_000_000);
        return group;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = "groups", key = "#result.id"),
                    @CachePut(value = "groups", key = "'name_' + #result.name")
            },
            evict = {
                    @CacheEvict(value = "groups", key = "'allGroups*'")
            }
    )
    @Transactional
    public Group addGroup(String name, List<Integer> studentIds) {
        long start = System.nanoTime();
        logger.info("Adding new group: {}", name);

        Group group = new Group(name);
        if (studentIds != null && !studentIds.isEmpty()) {
            List<Long> longStudentIds = studentIds.stream().map(Long::valueOf).toList();

            List<Student> students = studentRepository.findAllById(longStudentIds);

            if (students.size() != studentIds.size()) {
                Set<Long> foundStudentIds = students.stream().map(Student::getId)
                        .collect(Collectors.toSet());
                List<Long> nonExistentIds = longStudentIds.stream()
                        .filter(id -> !foundStudentIds.contains(id))
                        .toList();

                throw new EntityNotFoundException(
                        "Студенты с ID " + nonExistentIds + " не найдены");
            }

            for (Student student : students) {
                student.setGroup(group);
            }
            group.setStudents(students);
        }

        Group savedGroup = groupRepository.save(group);
        long end = System.nanoTime();
        logger.info("Execution time for addGroup: {} ms", (end - start) / 1_000_000);
        return savedGroup;
    }

    private Group findByIdInternal(Long id) {
        return groupRepository.findById(id).orElse(null);
    }

    private Group findByNameInternal(String name) {
        return groupRepository.findByName(name).orElse(null);
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        logger.info("Deleting group with ID: {}", id);
        Group group = findByIdInternal(id);
        if (group == null) {
            throw new EntityNotFoundException("Group with ID " + id + " not found");
        }

        evictGroupCaches(group);

        groupRepository.deleteById(id);
    }

    @Transactional
    public void deleteGroupByName(String name) {
        logger.info("Deleting group with name: {}", name);
        Group group = findByNameInternal(name);
        if (group == null) {
            throw new EntityNotFoundException("Group with name " + name + " not found");
        }

        evictGroupCaches(group);

        groupRepository.deleteByName(name);
    }

    @Caching(evict = {
            @CacheEvict(value = "groups", key = "#group.id"),
            @CacheEvict(value = "groups", key = "'name_' + #group.name"),
            @CacheEvict(value = "groups", key = "'allGroups*'")
    })
    protected void evictGroupCaches(Group group) {
        logger.info("Evicting all caches for group: id={}, name={}",
                group.getId(), group.getName());
    }
}
