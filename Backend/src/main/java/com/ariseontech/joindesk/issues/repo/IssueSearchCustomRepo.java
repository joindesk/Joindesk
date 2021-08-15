package com.ariseontech.joindesk.issues.repo;

import com.ariseontech.joindesk.issues.domain.Issue;
import lombok.extern.java.Log;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.util.List;

@Repository
@Log
public class IssueSearchCustomRepo {
    @PersistenceContext
    private EntityManager entityManager;

    /*issueSearchCustomRepo.filter("").forEach(System.out::println);
        System.out.println(issueSearchCustomRepo.count(""));*/

    public List findUnIndexed() {
        String sql = "select * from issue where data is null OR content_vector is null";
        return entityManager.createNativeQuery(sql, Issue.class).getResultList();
    }

    public List<Issue> filter(String finalQuery, String query) {
        String sql = "select * from issue where " + finalQuery + query;
        log.info(sql);
        return entityManager.createNativeQuery(sql, Issue.class).getResultList();
    }

    public List<Object> filterOnlyKeys(String finalQuery, String query) {
        String sql = "select key, updated from issue where " + finalQuery + query;
        log.info(sql);
        return entityManager.createNativeQuery(sql).getResultList();
    }

    public BigInteger count(String query) {
        String sql = "select count(*) from issue where " + query;
        return (BigInteger) entityManager.createNativeQuery(sql).getSingleResult();
    }

    @Transactional
    public void update(String jsonData, Issue issue) {
        entityManager.createNativeQuery("update issue set data = '" + jsonData + "' where id=" + issue.getId()).executeUpdate();
        entityManager.createNativeQuery("update issue set content_vector = (to_tsvector('" + issue.getKeyPair()
                + "') || to_tsvector(summary)) where id=" + issue.getId()).executeUpdate();
    }
}
