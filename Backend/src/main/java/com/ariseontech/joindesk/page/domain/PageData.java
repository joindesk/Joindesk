package com.ariseontech.joindesk.page.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class PageData {
    private Page pageParent;
    private Set<Page> pageParents;
    private Set<Page> pages;
    private List<Page> path;
    private List<PagePath> tree;

    public PageData() {
        pageParents = new HashSet<>();
        pages = new HashSet<>();
        path = new ArrayList<>();
        tree = new ArrayList<>();
    }
}
