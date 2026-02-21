package com.ats.optimizer.tools;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.apache.commons.text.StringEscapeUtils;

import java.nio.file.Files;
import java.nio.file.Path;

public class PdfPreviewScreenshot {
    public static void main(String[] args) throws Exception {
        String frontendUrl = "http://localhost:4200";
        Path jsonPath = Path.of("c:/Users/Yassin Benkacem/Desktop/Personel/Projects/ATS/_tmp_cv.json");
        Path outPath = Path.of("c:/Users/Yassin Benkacem/Desktop/Personel/Projects/ATS/_pdf_preview.png");

        String json = Files.readString(jsonPath);
        if (!json.isEmpty() && json.charAt(0) == '\uFEFF') {
            json = json.substring(1);
        }
        String escaped = StringEscapeUtils.escapeEcmaScript(json);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1280, 1400));
            Page page = context.newPage();

            page.onConsoleMessage(msg -> System.out.println("BROWSER " + msg.type() + ": " + msg.text()));
            page.onPageError(err -> System.out.println("BROWSER PAGE ERROR: " + err));

            page.addInitScript("localStorage.setItem('render_cv_data', \"" + escaped + "\");");
            page.navigate(frontendUrl + "/pdf-preview", new Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD));

            // Quick DOM diagnostics
            Object diag = page.evaluate("() => {\n" +
                    "  const left = document.querySelector('.md\\\\:w-1\\\\/3');\n" +
                    "  const right = document.querySelector('.md\\\\:w-2\\\\/3');\n" +
                    "  const blocks = document.querySelectorAll('.section-block').length;\n" +
                    "  const page = document.querySelector('.cv-page');\n" +
                    "  const pageTop = page ? page.getBoundingClientRect().top : 0;\n" +
                    "  const leftSections = left ? Array.from(left.querySelectorAll(':scope > .section-block')) : [];\n" +
                    "  const leftChunkCounts = leftSections.map(sec => sec.querySelectorAll('.section-chunk').length);\n" +
                    "  const leftChunkBottoms = leftSections.map(sec => Array.from(sec.querySelectorAll('.section-chunk')).map(s => {\n" +
                    "    const r = s.getBoundingClientRect();\n" +
                    "    const mb = parseFloat(getComputedStyle(s).marginBottom || '0') || 0;\n" +
                    "    return Math.round(r.bottom - pageTop + mb);\n" +
                    "  }));\n" +
                    "  const leftAll = left ? left.querySelectorAll('.section-block').length : 0;\n" +
                    "  const rightAll = right ? right.querySelectorAll('.section-block').length : 0;\n" +
                    "  const leftDirect = left ? left.querySelectorAll(':scope > .section-block').length : 0;\n" +
                    "  const rightDirect = right ? right.querySelectorAll(':scope > .section-block').length : 0;\n" +
                    "  const bottoms = (el) => Array.from(el.querySelectorAll(':scope > .section-block')).map(s => {\n" +
                    "    const r = s.getBoundingClientRect();\n" +
                    "    const mb = parseFloat(getComputedStyle(s).marginBottom || '0') || 0;\n" +
                    "    return Math.round(r.bottom - pageTop + mb);\n" +
                    "  });\n" +
                    "  const leftBottoms = left ? bottoms(left) : [];\n" +
                    "  const rightBottoms = right ? bottoms(right) : [];\n" +
                    "  return { left: !!left, right: !!right, blocks, leftAll, rightAll, leftDirect, rightDirect, leftBottoms, rightBottoms, leftChunkCounts, leftChunkBottoms, pageTop: Math.round(pageTop) };\n" +
                    "}");
            System.out.println("DOM DIAG: " + diag);

            try {
                page.waitForSelector(".cv-page", new Page.WaitForSelectorOptions().setTimeout(20000));
                page.waitForSelector("body.pdf-ready", new Page.WaitForSelectorOptions().setTimeout(20000));
            } catch (Exception e) {
                System.out.println("WAIT ERROR: " + e.getMessage());
                String content = page.content();
                System.out.println("CONTENT SNIPPET: " + content.substring(0, Math.min(1500, content.length())));
            }

            page.screenshot(new Page.ScreenshotOptions().setPath(outPath).setFullPage(true));

            context.close();
            browser.close();
        }
    }
}
