package com.ariseontech.joindesk.issues.repo;

import lombok.extern.java.Log;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

@Repository
@Log
public class IssueCustomRepo {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void createSeq(String seq, Long start) {
        String createSeqQuery = "CREATE SEQUENCE " + seq + ((null != start && start > 0) ? " START " + start : "");
        entityManager.createNativeQuery(createSeqQuery).executeUpdate();
        log.warning("creating sequence for " + seq + " => " + createSeqQuery);
    }

    @Transactional
    public void updateCustomDataByIssueType(Long id, String key, String data) {
        String query = "update issue SET custom_data = jsonb_set(custom_data, '{" + key + "}', '" + data + "') where issue_type =" + id;
        System.out.println(query);
        entityManager.createNativeQuery(query).executeUpdate();
    }

    @Transactional
    public void updateCustomDataByIssue(Long id, String key, String data) {
        String query = "update issue SET custom_data = jsonb_set(custom_data, " + key + ", '" + data + "') where id =" + id;
        System.out.println(query);
        entityManager.createNativeQuery(query).executeUpdate();
    }

    @Transactional
    public void deleteCustomFieldByKey(String key) {
        String query = "update issue SET custom_data = custom_data - '" + key + "'";
        System.out.println(query);
        entityManager.createNativeQuery(query).executeUpdate();
    }
}
