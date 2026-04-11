package finance.manager.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Service
public class FinanceService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Exchange Rate Mock Fallback
    private static final double STATIC_USD_TO_INR = 83.20;

    public Map<String, Object> convertCurrency(String from, String to, double amount) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Using a truly open free API (open.er-api.com) as exchangerate.host now strictly requires API keys
            String url = "https://open.er-api.com/v6/latest/" + from.toUpperCase();
            Map<String, Object> apiResponse = restTemplate.getForObject(url, Map.class);
            
            if (apiResponse != null && "success".equals(apiResponse.get("result"))) {
                Map<String, Double> rates = (Map<String, Double>) apiResponse.get("rates");
                if (rates != null && rates.containsKey(to.toUpperCase())) {
                    // Extract the specific rate relative to the base 'from' currency
                    Object rateObj = rates.get(to.toUpperCase());
                    double rate = Double.parseDouble(rateObj.toString());
                    
                    double result = amount * rate;
                    
                    response.put("success", true);
                    response.put("result", formatDecimal(result));
                    return response;
                }
            }
        } catch (Exception e) {
            System.out.println("Live Currency API Failed: " + e.getMessage());
        }

        // Extensive Fallback Logic if completely offline (mapped against USD base)
        Map<String, Double> fallbackRates = new HashMap<>();
        fallbackRates.put("USD", 1.0);
        fallbackRates.put("INR", STATIC_USD_TO_INR);
        fallbackRates.put("EUR", 0.92);
        fallbackRates.put("GBP", 0.79);
        fallbackRates.put("AUD", 1.52);
        fallbackRates.put("CAD", 1.36);
        fallbackRates.put("SGD", 1.35);
        fallbackRates.put("AED", 3.67);

        double baseAmount = amount;
        double fromRate = fallbackRates.getOrDefault(from.toUpperCase(), 1.0);
        double toRate = fallbackRates.getOrDefault(to.toUpperCase(), 1.0);

        // Convert the input amount to base USD first, then multiply out to target
        double resultInUsd = amount / fromRate;
        double finalResult = resultInUsd * toRate;
        
        response.put("success", true);
        response.put("result", formatDecimal(finalResult));
        response.put("isMocked", true);
        return response;
    }

    public Map<String, Object> getLiveMetals() {
        Map<String, Object> response = new HashMap<>();
        try {
            // Attempt live call to metals API
            String url = "https://api.metals.live/v1/spot";
            // The API returns an array object layout
            Object[] apiResponseArr = restTemplate.getForObject(url, Object[].class);
            
            if (apiResponseArr != null && apiResponseArr.length > 0) {
                // If the live API somehow works despite TLS errors, parse it
                Map<String, Object> innerObj = (Map<String, Object>) apiResponseArr[0];
                double goldUsdOz = Double.parseDouble(innerObj.get("gold").toString());
                double silverUsdOz = Double.parseDouble(innerObj.get("silver").toString());

                // Exact math for 1 Gram across INR
                double goldInrGram = (goldUsdOz * STATIC_USD_TO_INR) / 31.1035;
                double silverInrGram = (silverUsdOz * STATIC_USD_TO_INR) / 31.1035;

                response.put("gold", formatDecimal(goldInrGram));
                response.put("silver", formatDecimal(silverInrGram));
                return response;
            }
        } catch (Exception e) {
            System.out.println("Live Metals API Failed, using requested fallback: " + e.getMessage());
        }

        // Rigid Fallback exact to user requirements for India in INR
        response.put("gold", 6000.0);
        response.put("silver", 75.0);
        response.put("isMocked", true);
        return response;
    }

    public Map<String, Object> getStockChart(String symbol) {
        Map<String, Object> response = new HashMap<>();
        // Auto-append .NS to target the NSE Indian market explicitly unless otherwise overridden
        String formattedSymbol = symbol.toUpperCase().endsWith(".NS") || symbol.toUpperCase().endsWith(".BO") 
                                 ? symbol.toUpperCase() 
                                 : symbol.toUpperCase() + ".NS";
        try {
            
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + formattedSymbol + "?range=1mo&interval=1d";
            
            // Yahoo Finance mandates explicit Browser User-Agents to prevent 429 limit drop-offs
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = apiResponse.getBody();

            if (body != null && body.containsKey("chart")) {
                Map<String, Object> chart = (Map<String, Object>) body.get("chart");
                if (chart != null && chart.containsKey("result") && chart.get("result") != null) {
                    java.util.List<Map<String, Object>> resultList = (java.util.List<Map<String, Object>>) chart.get("result");
                    if (!resultList.isEmpty()) {
                        Map<String, Object> result = resultList.get(0);
                        
                        Map<String, Object> meta = (Map<String, Object>) result.get("meta");
                        double currentPrice = Double.parseDouble(meta.get("regularMarketPrice").toString());
                        
                        java.util.List<Integer> timestamps = (java.util.List<Integer>) result.get("timestamp");
                        
                        Map<String, Object> indicators = (Map<String, Object>) result.get("indicators");
                        java.util.List<Map<String, Object>> quoteList = (java.util.List<Map<String, Object>>) indicators.get("quote");
                        java.util.List<Double> closes = (java.util.List<Double>) quoteList.get(0).get("close");

                        response.put("success", true);
                        response.put("symbol", meta.get("symbol"));
                        response.put("currentPrice", currentPrice);
                        response.put("timestamps", timestamps);
                        response.put("prices", closes);
                        return response;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Yahoo Finance API Proxy Failed (429/Crumb Block): " + e.getMessage());
            
            // Proceed to Synthetic Fallback Generation protecting the Frontend App from crashing
            response.put("success", true);
            response.put("symbol", formattedSymbol);
            response.put("isMocked", true);
            
            double basePrice = 1500.0;
            if (formattedSymbol.contains("RELIANCE")) basePrice = 2950.50;
            else if (formattedSymbol.contains("TCS")) basePrice = 4120.00;
            else if (formattedSymbol.contains("INFY")) basePrice = 1680.75;
            else if (formattedSymbol.contains("HDFC")) basePrice = 1455.20;
            
            java.util.List<Integer> timestamps = new java.util.ArrayList<>();
            java.util.List<Double> prices = new java.util.ArrayList<>();
            
            long currentTime = System.currentTimeMillis() / 1000;
            double volatilePrice = basePrice * 0.95; // start month slightly lower typically
            
            // Build exactly 30 days of synthetic but realistic trading slope data
            for (int i = 30; i >= 0; i--) {
                timestamps.add((int) (currentTime - (i * 86400)));
                
                // Add minor random fluctuation between -1.5% and +1.5%
                double change = (Math.random() * 0.03) - 0.015;
                volatilePrice = volatilePrice * (1 + change);
                prices.add(formatDecimal(volatilePrice));
            }
            
            response.put("currentPrice", formatDecimal(volatilePrice));
            response.put("timestamps", timestamps);
            response.put("prices", prices);
            
            return response;
        }
        
        // Final catch-all if Yahoo returns 200 OK but structurally invalid JSON
        response.put("success", false);
        response.put("error", "Stock not found or structured data invalid");
        return response;
    }

    private double formatDecimal(double value) {
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
