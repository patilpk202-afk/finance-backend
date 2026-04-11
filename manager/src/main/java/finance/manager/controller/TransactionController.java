package finance.manager.controller;

import finance.manager.model.Transaction;
import finance.manager.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private TransactionService service;

    @PostMapping
    public Transaction create(@Valid @RequestBody Transaction transaction, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return service.create(transaction, userId);
    }

    // ✅ GET ALL FOR LOGGED IN USER
    @GetMapping
    public List<Transaction> getAll(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return service.getByUserId(userId);
    }

    // ✅ GET BY USER (Mapped to /all for frontend backwards compatibility if needed, but ignores URL inputs)
    @GetMapping("/all")
    public List<Transaction> getByUser(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return service.getByUserId(userId);
    }

    // ✅ UPDATE BY ID
    @PutMapping("/{id}")
    public Transaction update(@PathVariable String id,
                              @RequestBody Transaction transaction, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return service.updateTransaction(id, transaction, userId);
    }

    // ✅ DELETE BY ID
    @DeleteMapping("/{id}")
    public String delete(@PathVariable String id, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        service.deleteTransaction(id, userId);
        return "Transaction deleted successfully";
    }

    // ✅ PAGINATION
    @GetMapping("/user")
    public Page<Transaction> getByUser(
            HttpServletRequest request,
            @RequestParam int page,
            @RequestParam int size) {
        String userId = (String) request.getAttribute("userId");
        return service.getTransactionsByUser(userId, page, size);
    }

    // ✅ DOWNLOAD PDF STATEMENT
    @GetMapping("/report")
    public ResponseEntity<byte[]> getTransactionReport(
            HttpServletRequest request,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date to) {

        String userId = (String) request.getAttribute("userId");
        byte[] pdfBytes = service.generateTransactionReport(userId, from, to);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "statement.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}