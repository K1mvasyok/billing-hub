package io.github.k1mvasyok.billing.invoicing.app;

import io.github.k1mvasyok.billing.invoicing.domain.Invoice;
import io.github.k1mvasyok.billing.invoicing.domain.InvoiceStatus;
import io.github.k1mvasyok.billing.invoicing.infra.InvoiceRepository;
import io.github.k1mvasyok.billing.invoicing.infra.InvoiceSpecifications;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Выгрузка реестра счетов в XLSX (BR-18). Строки читаются страницами и пишутся в SXSSF,
 * который держит в памяти только окно последних строк, а остальное сбрасывает во временный файл.
 */
@Service
public class InvoiceExportService {

    static final int PAGE_SIZE = 1000;

    /** Размер окна SXSSF: сколько строк держится в памяти до сброса на диск. */
    static final int ROW_WINDOW = 200;

    private static final String[] HEADER = {
            "Номер", "Договор", "Клиент", "Период", "Вид", "Дата выставления", "Срок оплаты", "Статус", "Сумма"
    };

    private final InvoiceRepository invoices;

    public InvoiceExportService(InvoiceRepository invoices) {
        this.invoices = invoices;
    }

    /** Пишет реестр в out. Транзакции нет: каждая страница — отдельный короткий запрос. */
    public void export(UUID customerId, UUID contractId, InvoiceStatus status, LocalDate period, OutputStream out)
            throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_WINDOW)) {
            Sheet sheet = workbook.createSheet("Счета");
            writeRow(sheet.createRow(0), HEADER);
            int rowIndex = 1;
            for (int page = 0; ; page++) {
                Page<Invoice> chunk = invoices.findAll(
                        InvoiceSpecifications.matching(customerId, contractId, status, period),
                        PageRequest.of(page, PAGE_SIZE, Sort.by("number")));
                for (Invoice invoice : chunk.getContent()) {
                    writeInvoice(sheet.createRow(rowIndex++), invoice);
                }
                if (!chunk.hasNext()) {
                    break;
                }
            }
            workbook.write(out);
        }
    }

    private static void writeInvoice(Row row, Invoice invoice) {
        writeRow(row,
                invoice.getNumber(),
                invoice.getContractId().toString(),
                invoice.getCustomerId().toString(),
                invoice.getPeriod().toString(),
                invoice.getKind().name(),
                invoice.getIssueDate().toString(),
                invoice.getDueDate().toString(),
                invoice.getStatus().name());
        // Сумма записывается числом, чтобы её можно было суммировать в Excel.
        row.createCell(HEADER.length - 1, CellType.NUMERIC)
                .setCellValue(invoice.getTotal().doubleValue());
    }

    private static void writeRow(Row row, String... values) {
        for (int column = 0; column < values.length; column++) {
            Cell cell = row.createCell(column);
            cell.setCellValue(values[column]);
        }
    }
}
