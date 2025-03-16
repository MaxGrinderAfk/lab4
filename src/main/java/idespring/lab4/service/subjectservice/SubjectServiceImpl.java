package idespring.lab4.service.subjectservice;

import idespring.lab4.exceptions.EntityNotFoundException;
import idespring.lab4.model.Subject;
import idespring.lab4.repository.subjectrepo.SubjectRepository;
import java.util.List;
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
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository subjectRepository;
    private static final String NOTFOUND = "Subject not found with id: ";
    private static final Logger logger = LoggerFactory.getLogger(SubjectServiceImpl.class);

    @Autowired
    public SubjectServiceImpl(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Override
    @Cacheable(value = "subjects",
            key = "#namePattern + '-' + (#sort != null ? #sort : 'default')",
            unless = "#result == null or #result.isEmpty()")
    public List<Subject> readSubjects(String namePattern, String sort) {
        long start = System.nanoTime();
        logger.info("Fetching subjects from database for namePattern: "
                + "{}, sort: {}", namePattern, sort);

        List<Subject> subjects;
        if (namePattern != null) {
            subjects = subjectRepository.findByNameContaining(namePattern);
        } else if ("asc".equalsIgnoreCase(sort)) {
            subjects = subjectRepository.findAllByOrderByNameAsc();
        } else {
            subjects = subjectRepository.findAll();
        }

        long end = System.nanoTime();
        logger.info("Execution time for readSubjects: {} ms", (end - start) / 1_000_000);
        return subjects;
    }

    @Override
    @Cacheable(value = "subjects", key = "#id", unless = "#result == null")
    public Subject findById(Long id) {
        long start = System.nanoTime();
        logger.info("Fetching subject from database for id: {}", id);

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOTFOUND + id));

        long end = System.nanoTime();
        logger.info("Execution time for findById: {} ms", (end - start) / 1_000_000);
        return subject;
    }

    @Override
    @Cacheable(value = "subjects", key = "#name")
    public Subject findByName(String name) {
        long start = System.nanoTime();
        logger.info("Fetching subject from database for name: {}", name);

        Subject subject = subjectRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Subject not found with name: " + name));

        long end = System.nanoTime();
        logger.info("Execution time for findByName: {} ms", (end - start) / 1_000_000);
        return subject;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = "subjects", key = "#result.id"),
                    @CachePut(value = "subjects", key = "#result.name")
            },
            evict = {
                    @CacheEvict(value = "subjects", key = "'exists-' + #result.name")
            }
    )
    public Subject addSubject(Subject subject) {
        long start = System.nanoTime();
        logger.info("Saving subject: {}", subject.getName());

        Subject savedSubject = subjectRepository.save(subject);

        long end = System.nanoTime();
        logger.info("Execution time for addSubject: {} ms", (end - start) / 1_000_000);
        return savedSubject;
    }

    @Override
    @CacheEvict(value = "subjects", key = "#id")
    @Transactional
    public void deleteSubject(Long id) {
        logger.info("Deleting subject with id: {}", id);
        if (!subjectRepository.existsById(id)) {
            throw new EntityNotFoundException(NOTFOUND + id);
        }
        subjectRepository.deleteById(id);
        logger.info("Subject with id {} deleted", id);
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "subjects", key = "#name"),
                    @CacheEvict(value = "subjects", key = "'exists-' + #name")
            }
    )
    @Transactional
    public void deleteSubjectByName(String name) {
        logger.info("Deleting subject with name: {}", name);
        if (!subjectRepository.existsByName(name)) {
            throw new EntityNotFoundException("Subject not found with name: " + name);
        }
        subjectRepository.deleteByName(name);
        logger.info("Subject with name {} deleted", name);
    }

    @Override
    @Cacheable(value = "subjects", key = "'exists-' + #name")
    public boolean existsByName(String name) {
        logger.info("Checking existence of subject with name: {}", name);
        return subjectRepository.existsByName(name);
    }
}
