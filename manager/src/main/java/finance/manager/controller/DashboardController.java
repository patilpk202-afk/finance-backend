package finance.manager.controller;

import finance.manager.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DashboardService service;

    // ✅ Summary API
    @GetMapping("/summary")
    public Map<String, Object> getSummary(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        double income = service.getTotalIncome(userId);
        double expense = service.getTotalExpense(userId);
        double balance = service.getBalance(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("totalIncome", income);
        response.put("totalExpense", expense);
        response.put("balance", balance);

        return response;
    }

    @GetMapping("/monthly")
    public Map<String, Double> getMonthly(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return service.getMonthlyReport(userId);
    }

    @GetMapping("/category")
    public Map<String, Double> getCategory(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return service.getCategoryReport(userId);
    }
}