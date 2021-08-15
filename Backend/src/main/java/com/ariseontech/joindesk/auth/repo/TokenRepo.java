package com.ariseontech.joindesk.auth.repo;

import com.ariseontech.joindesk.auth.domain.Login;
import com.ariseontech.joindesk.auth.domain.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface TokenRepo extends JpaRepository<Token, String> {

    Token findByToken(String token);

    Set<Token> findAllByUser(Login l);
}