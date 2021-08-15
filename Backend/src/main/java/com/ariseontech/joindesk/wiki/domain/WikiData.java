package com.ariseontech.joindesk.wiki.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class WikiData {
    private WikiFolder wikiFolder;
    private Set<WikiFolder> wikiFolders;
    private Set<Wiki> wikis;
    private List<WikiFolder> path;
    private List<WikiPath> tree;

    public WikiData() {
        wikiFolders = new HashSet<>();
        wikis = new HashSet<>();
        path = new ArrayList<>();
        tree = new ArrayList<>();
    }
}
