package com.ariseontech.joindesk.page.repo;

import com.ariseontech.joindesk.page.domain.Page;
import com.ariseontech.joindesk.project.domain.Project;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.util.List;

@Repository
public class PageCustomRepo {
    @PersistenceContext
    private EntityManager entityManager;

    /*
        System.out.println(pageCustomRepo.count("WHERE (to_tsvector(title) || to_tsvector(content)) @@ to_tsquery('doc')"));
        pageCustomRepo.filter("WHERE (to_tsvector(title) || to_tsvector(content)) @@ to_tsquery('doc')").forEach(System.out::println);
     */

    public List filter(String query, Project project) {
        String sql = "select * from {h-schema}page where content_vector @@ to_tsquery('" + query + "') and project = " + project.getId();
        System.out.println(sql);
        return entityManager.createNativeQuery(sql, Page.class).getResultList();
    }

    public BigInteger count(String query, Project project) {
        String sql = "select count(*) from {h-schema}page where content_vector @@ to_tsquery('" + query + "') and project = " + project.getId();
        return (BigInteger) entityManager.createNativeQuery(sql).getSingleResult();
    }

    @Transactional
    public void update(Page page) {
        entityManager.createNativeQuery("update {h-schema}page set content_vector = (to_tsvector(title) || to_tsvector(content)) where id=" + page.getId()).executeUpdate();
    }

    @Transactional
    public void setHasChild(Page page, boolean val) {
        entityManager.createNativeQuery("update {h-schema}page set has_child = " + val + " where id=" + page.getId()).executeUpdate();
    }

    @Transactional
    public void updateAll() {
        entityManager.createNativeQuery("update {h-schema}page set content_vector = (to_tsvector(title) || to_tsvector(content))").executeUpdate();
    }
}
