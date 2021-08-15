package com.ariseontech.joindesk.auth.repo;


import com.ariseontech.joindesk.auth.domain.AuthorityCode;
import com.ariseontech.joindesk.auth.domain.AuthorityGlobal;
import com.ariseontech.joindesk.auth.domain.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import javax.transaction.Transactional;
import java.util.List;

public interface AuthorityGlobalRepo extends JpaRepository<AuthorityGlobal, Long> {

    List<AuthorityGlobal> findByLogin(Login l);

    List<AuthorityGlobal> findByAuthorityCode(AuthorityCode code);

    @Transactional
    @Modifying
    void deleteByLogin(Login l);

}
