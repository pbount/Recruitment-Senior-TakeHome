package com.automwrite.assessment;

import com.automwrite.assessment.service.llm.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApplicationTest {

    @Autowired
    private LlmService llmService;

//    This is a STRICT text transformation task, not a conversational task.
//
//    Rewrite ONLY the text between [brackets] in the specified tone. Use the surrounding text to understand the context but don't use it in the output.
//
//    Tone: 'CASUAL'
//    Text: ' [Mr. B. Builder] 21 Paved driveway'
//
//    STRICT OUTPUT RULES:
//            1. Output first character must be first character replacing bracketed content
//     2. Output last character must be last character replacing bracketed content
//     3. No greetings, context, or other text permitted
//     4. No explanations
//     5. Direct replacement only
//     6. No Guesswork. If context is unclear, return the original text.
//     7. If no text present in brackets, return a single space character.
//            8. Don't modify abbreviations.
//
//    Example input: "This is a test. [The test is hard]. The test has concluded."
//    Example output: The test is difficult
//
//    FAILURE CONDITIONS:
//            - Any output starting before bracket content
//     - Any output continuing after bracket content
//     - Any explanatory text
//     - Transforming ambiguous text

    @Test
    void contextLoads() {
        // FORMAT: Output must begin and end with the exact content that replaces the [bracketed text]. No other characters allowed.
        String result = llmService.generateText("""        
STRICT text transformation task

Rewrite ONLY the text between [brackets] in the specified tone. Use the surrounding text to understand the context but don't include it in the output.

Tone: 'CASUAL'
Text: '[Mr. B. Builder] 21 Paved driveway'

STRICT OUTPUT RULES:
    Only modify the text if its tone significantly differs from the requested tone; otherwise, leave it unchanged.
    Output's first character must match the first character of the replacement text in brackets.
    Output's last character must match the last character of the replacement text in brackets.
    No greetings, context, or extra text permitted.
    No explanations or reasoning.
    All abbreviations must remain EXACTLY as they appear. Never expand abbreviations.
    Direct replacement only.
    No guesswork. If the context is unclear, return the original text in brackets.
    If no text is present in brackets, return a single space character.
       \s""");
        System.out.println(result);
    }

}
