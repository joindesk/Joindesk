package com.ariseontech.joindesk.wiki.repo;

import com.ariseontech.joindesk.project.domain.Project;
import com.ariseontech.joindesk.wiki.domain.Wiki;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.util.List;

@Repository
public class WikiCustomRepo {
    @PersistenceContext
    private EntityManager entityManager;

    /*
        System.out.println(wikiCustomRepo.count("WHERE (to_tsvector(title) || to_tsvector(content)) @@ to_tsquery('doc')"));
        wikiCustomRepo.filter("WHERE (to_tsvector(title) || to_tsvector(content)) @@ to_tsquery('doc')").forEach(System.out::println);
     */

    public List filter(String query, Project project) {
        String sql = "select * from {h-schema}wiki where content_vector @@ to_tsquery('" + query + "') and project = " + project.getId();
        System.out.println(sql);
        return entityManager.createNativeQuery(sql, Wiki.class).getResultList();
    }

    public BigInteger count(String query, Project project) {
        String sql = "select count(*) from {h-schema}wiki where content_vector @@ to_tsquery('" + query + "') and project = " + project.getId();
        return (BigInteger) entityManager.createNativeQuery(sql).getSingleResult();
    }

    @Transactional
    public void update(Wiki wiki) {
        entityManager.createNativeQuery("update {h-schema}wiki set content_vector = (to_tsvector(title) || to_tsvector(content)) where id=" + wiki.getId()).executeUpdate();
    }

    @Transactional
    public void updateAll() {
        entityManager.createNativeQuery("update {h-schema}wiki set content_vector = (to_tsvector(title) || to_tsvector(content))").executeUpdate();
    }
}
