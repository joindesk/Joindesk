package com.ariseontech.joindesk.wiki.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
public class WikiPath {
    private String title, key;
    @JsonProperty(value = "isLeaf")
    private boolean leaf;
    private String icon = "anticon anticon-folder";
    private boolean selected, expanded;
    private List<WikiPath> children = new ArrayList<>();

    @JsonIgnore
    private WikiFolder folder;

    public WikiPath(String title, String key) {
        this.title = title;
        this.key = key;
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

    public List<WikiPath> getChildren() {
        return children;
    }
}
