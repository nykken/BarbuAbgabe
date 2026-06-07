package com.barbu.catalog;

import org.engine.contract.Contract;

public record ContractDefinition(String id, String displayName, Contract contract) {}