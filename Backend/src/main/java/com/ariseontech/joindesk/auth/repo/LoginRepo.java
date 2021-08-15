package com.ariseontech.joindesk.auth.repo;


import com.ariseontech.joindesk.auth.domain.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface LoginRepo extends JpaRepository<Login, Long> {

    Set<Login> findAllByOrderByEmailAsc();

    Login findByEmailIgnoreCase(String email);

    @Query(value = "select email from jd_user", nativeQuery = true)
    Set<String> getEmails();

    Set<Login> findByActiveTrue();

    Set<Login> findBySuperAdminTrue();

    Login findByUserNameIgnoreCase(String userName);

    Login findByApiToken(String token);

    Login findBySlackID(String slackID);
}
