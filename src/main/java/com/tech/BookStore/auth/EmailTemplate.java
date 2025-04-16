package com.tech.BookStore.auth;

import lombok.Getter;

@Getter
public enum EmailTemplate {

    ACTIVATE_ACCOUNT("activate_account");

    private final String name;

    EmailTemplate(String name) {
        this.name = name;
    }
}
