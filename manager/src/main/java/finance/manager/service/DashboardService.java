package finance.manager.service;

import finance.manager.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private MongoTemplate mongoTemplate;

    // Helper class for mapping aggregation results
    private static class AggregationResult {
        public String _id;
        public double total;
    }

    // Helper class for Monthly mapping
    private static class MonthlyAggregationResult {
        public int month;
        public double total;
    }

    // ✅ Total Income
    public double getTotalIncome(String userId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(userId).and("type").is("INCOME")),
                Aggregation.group("userId").sum("amount").as("total")
        );

        AggregationResults<AggregationResult> results = mongoTemplate.aggregate(aggregation, "transactions", AggregationResult.class);
        return results.getUniqueMappedResult() != null ? results.getUniqueMappedResult().total : 0.0;
    }

    // ✅ Total Expense
    public double getTotalExpense(String userId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(userId).and("type").is("EXPENSE")),
                Aggregation.group("userId").sum("amount").as("total")
        );

        AggregationResults<AggregationResult> results = mongoTemplate.aggregate(aggregation, "transactions", AggregationResult.class);
        return results.getUniqueMappedResult() != null ? results.getUniqueMappedResult().total : 0.0;
    }

    // ✅ Balance
    public double getBalance(String userId) {
        return getTotalIncome(userId) - getTotalExpense(userId);
    }

    // ✅ GET MONTHLY REPORT
    public Map<String, Double> getMonthlyReport(String userId) {
        // Group by the month of 'createdAt' property natively in MongoDB
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(userId)),
                Aggregation.project("amount")
                        .andExpression("month(createdAt)").as("month"),
                Aggregation.group("month").sum("amount").as("total")
        );

        AggregationResults<MonthlyAggregationResult> results = mongoTemplate.aggregate(aggregation, "transactions", MonthlyAggregationResult.class);
        List<MonthlyAggregationResult> mappedResults = results.getMappedResults();

        // Convert the string month representation for the frontend
        String[] monthNames = {"", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        Map<String, Double> monthlyData = new HashMap<>();

        for (MonthlyAggregationResult res : mappedResults) {
            if (res.month > 0 && res.month <= 12) {
                monthlyData.put(monthNames[res.month], res.total);
            }
        }
        return monthlyData;
    }

    // ✅ GET CATEGORY WISE REPORT
    public Map<String, Double> getCategoryReport(String userId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(userId)),
                Aggregation.group("category").sum("amount").as("total")
        );

        AggregationResults<AggregationResult> results = mongoTemplate.aggregate(aggregation, "transactions", AggregationResult.class);
        List<AggregationResult> mappedResults = results.getMappedResults();

        Map<String, Double> categoryData = new HashMap<>();
        for (AggregationResult res : mappedResults) {
            categoryData.put(res._id, res.total);
        }

        return categoryData;
    }
}