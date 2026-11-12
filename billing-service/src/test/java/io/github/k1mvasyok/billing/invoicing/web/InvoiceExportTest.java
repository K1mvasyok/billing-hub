package io.github.k1mvasyok.billing.invoicing.web;

import io.github.k1mvasyok.billing.TestClockConfiguration;
import io.github.k1mvasyok.billing.TestcontainersConfiguration;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Реестр счетов в XLSX (BR-18): поток, фильтры, читаемый файл. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
class InvoiceExportTest {

    @Autowired
    private MockMvc mockMvc;

    private BillingFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new BillingFixtures(mockMvc);
    }

    @Test
    @DisplayName("BR-18: реестр отдаётся потоком и читается обратно в XLSX с теми же счетами")
    void exportStreamsFilteredInvoicesAsReadableWorkbook() throws Exception {
        String contractId = fixtures.contract("2042-01-01", "2042-12-31");
        fixtures.correction(contractId, "2042-08", "100.00");
        fixtures.correction(contractId, "2042-08", "-50.00");

        MvcResult started = mockMvc.perform(get("/api/v1/invoices/export.xlsx")
                        .param("contractId", contractId)
                        .param("period", "2042-08"))
                .andExpect(request().asyncStarted())
                .andReturn();
        byte[] file = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("invoices.xlsx")))
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook workbook = read(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Номер");
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
            double total = 0;
            for (int i = 1; i <= 2; i++) {
                Row row = sheet.getRow(i);
                assertThat(row.getCell(1).getStringCellValue()).isEqualTo(contractId);
                total += row.getCell(8).getNumericCellValue();
            }
            assertThat(total).isEqualTo(50.0);
        }
    }

    @Test
    @DisplayName("Неверный период отвечает 400 до начала потока, а не обрывает файл")
    void badPeriodFailsBeforeStreaming() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/export.xlsx").param("period", "2042-13"))
                .andExpect(status().isBadRequest());
    }

    private static XSSFWorkbook read(byte[] file) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(file));
    }
}
