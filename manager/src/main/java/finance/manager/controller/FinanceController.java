package finance.manager.controller;

import finance.manager.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/finance")
@CrossOrigin(origins = "*")
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    @GetMapping("/convert")
    public Map<String, Object> convert(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam double amount) {
        return financeService.convertCurrency(from, to, amount);
    }

    @GetMapping("/metals")
    public Map<String, Object> getMetals() {
        return financeService.getLiveMetals();
    }

    @GetMapping("/stocks")
    public Map<String, Object> getStocks(@RequestParam String symbol) {
        return financeService.getStockChart(symbol);
    }
}
