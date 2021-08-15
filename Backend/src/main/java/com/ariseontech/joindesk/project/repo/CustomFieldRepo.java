package com.ariseontech.joindesk.project.repo;

import com.ariseontech.joindesk.issues.domain.IssueType;
import com.ariseontech.joindesk.project.domain.CustomField;
import com.ariseontech.joindesk.project.domain.CustomFieldType;
import com.ariseontech.joindesk.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface CustomFieldRepo extends JpaRepository<CustomField, Long> {

    Set<CustomField> findByNameOrderByNameAsc(String name);

    Set<CustomField> findByProjectOrderByNameAsc(Project project);

    Set<CustomField> findByProjectAndKeyOrderByNameAsc(Project project, String key);

    Set<CustomField> findByProjectAndIssueTypesOrderByNameAsc(Project project, IssueType issueType);

    Set<CustomField> findByTypeOrderByNameAsc(CustomFieldType type);

    CustomField findByProjectAndId(Project project, Long fieldId);
}
