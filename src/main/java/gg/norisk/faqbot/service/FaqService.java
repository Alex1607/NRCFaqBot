package gg.norisk.faqbot.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

@Setter
@Data
@AllArgsConstructor
public class FaqService {
    private String faqText;

    public boolean invalidFAQ() {
        return faqText == null || faqText.isEmpty();
    }
}
