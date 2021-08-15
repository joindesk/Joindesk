package com.ariseontech.joindesk.wiki.repo;

import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.wiki.domain.WikiFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface WikiFolderRepo extends JpaRepository<WikiFolder, Long> {

    Set<WikiFolder> findByProjectOrderByTitleAsc(Project project);

    Set<WikiFolder> findByProjectAndParentOrderByTitleAsc(Project project, WikiFolder parent);

    int countByProjectAndParent(Project project, WikiFolder parent);

    Set<WikiFolder> findTop10ByProjectAndTitleIgnoreCaseContainingOrderByTitleAsc(Project project, String title);

    WikiFolder findByProjectAndProjectDefaultTrue(Project project);
}
