package com.example.gradingsystem.repository;

import com.example.gradingsystem.model.Assignment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRepository  extends JpaRepository<Assignment, Long> {
    Optional<byte[]> findRubricById(Long id);
}