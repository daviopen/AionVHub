package com.aionvhub.app;

interface IPrivilegedBridge {
    String execute(String command);
    int getUid();
}
