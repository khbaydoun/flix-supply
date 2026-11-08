package com.khalil.supply.line;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lines")
@Transactional(readOnly = true)
public class LineController {

    private final LineRepository repository;

    public LineController(LineRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<LineDto> findAll() {
        return repository.findAllByOrderByCodeAsc().stream().map(LineDto::from).toList();
    }

    @GetMapping("/{code}")
    public LineDto findByCode(@PathVariable String code) {
        return repository.findByCode(code).map(LineDto::from)
                .orElseThrow(() -> new LineNotFoundException(code));
    }
}
