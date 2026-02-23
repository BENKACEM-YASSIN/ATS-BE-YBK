package com.ats.optimizer.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Media;
import com.microsoft.playwright.options.WaitUntilState;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;

@Service
@Slf4j
public class PdfService {

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public String extractText(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            if (document.isEncrypted()) {
                throw new RuntimeException("Encrypted PDFs are not supported.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("PDF Extraction Error:", e);
            throw new RuntimeException("Failed to extract text from PDF file. Ensure the file is a valid text-based PDF.");
        }
    }

    public byte[] renderCv(JsonNode cvData) {
        log.info("Starting PDF generation for CV data");

        try {
            java.net.URL url = new java.net.URL(frontendUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int responseCode = conn.getResponseCode();
            log.info("Frontend check ({}): {}", frontendUrl, responseCode);
        } catch (Exception e) {
            log.warn("Frontend check failed: {}. Continuing anyway but Playwright might fail.", e.getMessage());
        }

        try (Playwright playwright = Playwright.create()) {
            log.info("Playwright created, launching browser...");
            Browser browser = null;
            try {
                browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            } catch (Exception e) {
                log.error("Failed to launch browser. This usually means Playwright browsers are not installed. " +
                          "Try running: mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args=\"install\"", e);
                throw new RuntimeException("Browser engine not found. Please ensure Playwright browsers are installed on the server.");
            }

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1280, 1400));
            Page page = context.newPage();

            page.onConsoleMessage(msg -> {
                String text = msg.text();
                if ("error".equals(msg.type())) log.error("Browser console error: {}", text);
                else log.info("Browser console {}: {}", msg.type(), text);
            });
            page.onPageError(err -> log.error("Browser page crash: {}", err));

            String jsonStr = cvData.toString();
            String escapedJson = StringEscapeUtils.escapeEcmaScript(jsonStr);
            
            String initScript = "console.log('PDF: Injecting CV data...'); " +
                                "try { " +
                                "  localStorage.setItem('render_cv_data', \"" + escapedJson + "\"); " +
                                "  console.log('PDF: CV data injected successfully'); " +
                                "} catch(e) { console.error('PDF: Failed to inject CV data', e); }";
            page.addInitScript(initScript);

            log.info("Navigating to: {}/pdf-preview", frontendUrl);
            Response response = page.navigate(frontendUrl + "/pdf-preview", new Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD));
            
            if (response == null || !response.ok()) {
                String status = response != null ? String.valueOf(response.status()) : "null";
                log.error("Failed to navigate to frontend. Status: {}", status);
                throw new RuntimeException("Frontend unreachable at " + frontendUrl + " (Status: " + status + "). " +
                                         "Ensure the Angular app is running on this port.");
            }

            log.info("Waiting for CV container (.cv-page)...");
            try {
                page.waitForSelector(".cv-page", new Page.WaitForSelectorOptions().setTimeout(20000));
                log.info("CV container found!");
                page.waitForSelector("body.pdf-ready", new Page.WaitForSelectorOptions().setTimeout(20000));
                log.info("PDF pagination ready.");
            } catch (Exception e) {
                log.error("Timeout waiting for CV render readiness. Content snippet: {}", 
                         page.content().substring(0, Math.min(2000, page.content().length())));
                throw new RuntimeException("Timeout: CV preview failed to render. Selector '.cv-page' or 'body.pdf-ready' not found.");
            }

            page.emulateMedia(new Page.EmulateMediaOptions().setMedia(Media.PRINT));

            log.info("Printing to PDF...");
            Page.PdfOptions options = new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setMargin(new com.microsoft.playwright.options.Margin().setTop("0").setBottom("0").setLeft("0").setRight("0"));

            byte[] pdf = page.pdf(options);
            log.info("PDF generated successfully ({} bytes)", pdf.length);
            
            context.close();
            browser.close();
            return pdf;
        } catch (Exception e) {
            log.error("PDF Render Error: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
