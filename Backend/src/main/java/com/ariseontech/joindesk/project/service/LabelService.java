package com.ariseontech.joindesk.project.service;

import com.ariseontech.joindesk.issues.domain.Label;
import com.ariseontech.joindesk.issues.repo.LabelRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LabelService {

    @Autowired
    private LabelRepo labelRepo;

    @Cacheable(value = "label")
    public Optional<Label> get(Long id) {
        return labelRepo.findById(id);
    }
}
