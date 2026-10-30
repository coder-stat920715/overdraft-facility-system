package com.bank.overdraft.service;

import com.bank.overdraft.dto.ComparisonRequest;
import com.bank.overdraft.dto.ComparisonResponse;

public interface ComparisonService {

    ComparisonResponse compare(ComparisonRequest request);
}
