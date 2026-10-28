package com.transferwise.idempotence4j.web;

import java.util.Objects;

public class TestItem {
    private String name;
    private Integer id;

    public TestItem() {
    }

    public TestItem(String name, Integer id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TestItem other)) {
            return false;
        }
        return Objects.equals(name, other.name) && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }
}
