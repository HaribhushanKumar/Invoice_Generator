package com.example.invoice.service;

import com.example.invoice.exception.ResourceNotFoundException;
import com.example.invoice.model.Invoice;
import com.example.invoice.model.LineItem;
import com.example.invoice.repository.InvoiceRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    private double calculateTotal(List<LineItem> lineItems) {
        return lineItems.stream()
                .mapToDouble(item -> item.getQuantity() * item.getPrice())
                .sum();
    }

    public Invoice createInvoice(Invoice invoice) {
        invoice.setTotalAmount(calculateTotal(invoice.getLineItems()));
        return invoiceRepository.save(invoice);
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }

    public Invoice updateInvoice(Long id, Invoice data) {
        Invoice existing = getInvoiceById(id);
        existing.setCustomerName(data.getCustomerName());
        existing.setLineItems(data.getLineItems());
        existing.setTotalAmount(calculateTotal(data.getLineItems()));
        return invoiceRepository.save(existing);
    }

    public void deleteInvoice(Long id) {
        invoiceRepository.delete(getInvoiceById(id));
    }

    public byte[] generateInvoicePdf(Long id) {
        Invoice invoice = getInvoiceById(id);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);

        document.open();

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 12);

        document.add(new Paragraph("Invoice #" + invoice.getId(), titleFont));
        document.add(new Paragraph("Customer: " + invoice.getCustomerName(), normalFont));
        document.add(new Paragraph("\n"));

        Table table = new Table(4);
        table.addCell("Description");
        table.addCell("Qty");
        table.addCell("Price");
        table.addCell("Total");

        for (LineItem item : invoice.getLineItems()) {
            table.addCell(item.getDescription());
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell(String.valueOf(item.getPrice()));
            table.addCell(String.valueOf(item.getQuantity() * item.getPrice()));
        }

        document.add(table);
        document.add(new Paragraph("\nGrand Total: ₹" + invoice.getTotalAmount(), titleFont));

        document.close();

        return out.toByteArray();
    }
}
