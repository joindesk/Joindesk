package com.ariseontech.joindesk.page.domain;

import com.ariseontech.joindesk.wiki.domain.WikiFolder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
public class PagePath {
    private String title, key;
    @JsonProperty(value = "isLeaf")
    private boolean leaf;
    private String icon = "anticon anticon-folder";
    private boolean selected, expanded;
    private List<PagePath> children = new ArrayList<>();

    @JsonIgnore
    private Page parent;

    public PagePath(String title, String key) {
        this.title = title;
        this.key = key;
    }

    public PagePath(String title, String key, boolean leaf) {
        this.title = title;
        this.key = key;
        this.leaf = leaf;
    }

    public String getTitle() {
        return title;
    }

    public String getKey() {
        return key;
    }

    @JsonProperty("isLeaf")
    public boolean isLeaf() {
        return leaf;
    }

    @JsonProperty("isLeaf")
    public boolean getLeaf() {
        return leaf;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public List<PagePath> getChildren() {
        return children;
    }
}
