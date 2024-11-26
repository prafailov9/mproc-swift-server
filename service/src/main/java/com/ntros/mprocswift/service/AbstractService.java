package com.ntros.mprocswift.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.Executor;

public abstract class AbstractService {

    @Autowired
    @Qualifier("taskExecutor")
    protected Executor executor;

}
