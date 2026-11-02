package ru.novikov.finance.core;

public record User(String login, String salt, String passwordHash) {}
