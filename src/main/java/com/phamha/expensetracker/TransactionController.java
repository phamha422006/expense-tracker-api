package com.phamha.expensetracker;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository repo;

    public TransactionController(TransactionRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Transaction> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return filter(type, category, month);
    }

    @GetMapping("/summary")
    public Summary summary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        List<Transaction> list = filter(null, null, month);
        double income = sum(list, "THU");
        double expense = sum(list, "CHI");
        return new Summary(income, expense, income - expense);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getOne(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Transaction create(@Valid @RequestBody Transaction t) {
        t.setId(null);
        return repo.save(t);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> update(@PathVariable Long id, @Valid @RequestBody Transaction t) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        t.setId(id);
        return ResponseEntity.ok(repo.save(t));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private List<Transaction> filter(String type, String category, YearMonth month) {
        return repo.findAll().stream()
                .filter(t -> type == null || type.equalsIgnoreCase(t.getType()))
                .filter(t -> category == null || category.equalsIgnoreCase(t.getCategory()))
                .filter(t -> month == null
                        || (t.getDate() != null && YearMonth.from(t.getDate()).equals(month)))
                .toList();
    }

    private double sum(List<Transaction> list, String type) {
        return list.stream()
                .filter(t -> type.equalsIgnoreCase(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public record Summary(double totalIncome, double totalExpense, double balance) {}
}