package com.khalil.supply.line;

import com.khalil.supply.web.NotFoundException;

public class LineNotFoundException extends NotFoundException {
    public LineNotFoundException(String code) {
        super("Line not found: " + code);
    }
}
