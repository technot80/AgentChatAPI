package dev.agentchat.api;

public class ChatResponse {

    private final String content;
    private final boolean success;
    private final String errorMessage;
    private final int tokenCount;

    public ChatResponse(String content, boolean success, String errorMessage, int tokenCount) {
        this.content = content;
        this.success = success;
        this.errorMessage = errorMessage;
        this.tokenCount = tokenCount;
    }

    public static ChatResponse success(String content, int tokenCount) {
        return new ChatResponse(content, true, null, tokenCount);
    }

    public static ChatResponse error(String errorMessage) {
        return new ChatResponse(null, false, errorMessage, 0);
    }

    public String getContent() {
        return content;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getTokenCount() {
        return tokenCount;
    }
}
