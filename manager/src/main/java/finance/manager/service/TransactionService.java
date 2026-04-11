package finance.manager.service;

import finance.manager.exception.CustomException;
import finance.manager.model.Transaction;
import finance.manager.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.awt.Color;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository repository;

    // ✅ Add Transaction
    public Transaction create(Transaction transaction, String userId) {
        transaction.setUserId(userId);
        transaction.setCreatedAt(new Date());
        return repository.save(transaction);
    }

    // ✅ Get All (Unused practically, but if called internally should probably be removed. Keeping here but should not be exposed safely)
    public List<Transaction> getAll() {
        return repository.findAll();
    }

    // ✅ Get by User
    public List<Transaction> getByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    // ✅ Update Transaction
    public Transaction updateTransaction(String id, Transaction updated, String userId) {

        Transaction existing = repository.findById(id)
                .orElseThrow(() -> new CustomException("Transaction not found"));

        if (!existing.getUserId().equals(userId)) {
            throw new CustomException("Access Denied: You cannot modify this transaction");
        }

        existing.setAmount(updated.getAmount());
        existing.setType(updated.getType());
        existing.setCategory(updated.getCategory());
        existing.setDescription(updated.getDescription());
        existing.setTransactionDate(updated.getTransactionDate());

        return repository.save(existing);
    }

    // ✅ Delete Transaction
    public void deleteTransaction(String id, String userId) {

        Transaction existing = repository.findById(id)
                .orElseThrow(() -> new CustomException("Transaction not found"));

        if (!existing.getUserId().equals(userId)) {
            throw new CustomException("Access Denied: You cannot delete this transaction");
        }

        repository.delete(existing);
    }

    public Page<Transaction> getTransactionsByUser(String userId, int page, int size) {
        return repository.findByUserId(userId, PageRequest.of(page, size));
    }

    public byte[] generateTransactionReport(String userId, Date from, Date to) {
        
        // Pad the 'to' parameter pushing bounds to end-of-day safely capturing any transactions on explicitly selected dates
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(to);
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
        Date adjustedTo = cal.getTime();

        // Fetch custom arrays within exactly parsed bounding dates
        List<Transaction> transactions = repository.findByUserIdAndCreatedAtBetween(userId, from, adjustedTo);

        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Structure Branding Mapping
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Transaction Statement", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);

            // Structure Date Ranges Mapping
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Paragraph dateRange = new Paragraph("Period: " + sdf.format(from) + " to " + sdf.format(to), dateFont);
            dateRange.setAlignment(Paragraph.ALIGN_CENTER);
            dateRange.setSpacingAfter(20);
            document.add(dateRange);

            // Map standard Table arrays over five defined bounds dynamically 
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Map Column structures
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            String[] headers = {"Date", "Category", "Type", "Amount", "Description"};
            for (String header : headers) {
                PdfPCell hcell = new PdfPCell(new Phrase(header, headFont));
                hcell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                hcell.setBackgroundColor(Color.LIGHT_GRAY);
                hcell.setPadding(5);
                table.addCell(hcell);
            }

            // Iterate fetched mappings
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA);
            for (Transaction transaction : transactions) {
                // Column 1: Date mapping securely to createdAt constraints
                Date outputDate = transaction.getCreatedAt() != null ? transaction.getCreatedAt() : new Date();
                PdfPCell cell = new PdfPCell(new Phrase(sdf.format(outputDate), cellFont));
                cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                table.addCell(cell);

                // Column 2: Category
                cell = new PdfPCell(new Phrase(transaction.getCategory(), cellFont));
                cell.setPaddingLeft(5);
                cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                table.addCell(cell);

                // Column 3: Type
                cell = new PdfPCell(new Phrase(transaction.getType(), cellFont));
                cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                table.addCell(cell);

                // Column 4: Amount
                cell = new PdfPCell(new Phrase(String.format("$%.2f", transaction.getAmount()), cellFont));
                cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
                cell.setPaddingRight(5);
                table.addCell(cell);

                // Column 5: Description
                String desc = transaction.getDescription() != null ? transaction.getDescription() : "-";
                cell = new PdfPCell(new Phrase(desc, cellFont));
                cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
                cell.setPaddingLeft(5);
                table.addCell(cell);
            }

            document.add(table);
            document.close();
            
        } catch (DocumentException ex) {
            throw new CustomException("Error formatting PDF document dynamically");
        }

        return out.toByteArray();
    }
}