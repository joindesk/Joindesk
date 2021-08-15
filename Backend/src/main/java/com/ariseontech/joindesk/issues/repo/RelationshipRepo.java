package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface RelationshipRepo extends JpaRepository<Relationship, Long> {

}