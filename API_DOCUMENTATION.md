# AgentChatAPI

A Minecraft plugin API that provides multi-session AI chat capabilities with OpenAI-compatible backends.

---

## Overview

AgentChatAPI provides a simple interface for any Minecraft plugin to add AI-powered chat functionality to bots, NPCs, fake players, or any other chat-capable entity.

### Features

- **Multi-session support**: Create unlimited independent chat sessions with separate contexts
- **OpenAI-compatible**: Works with OpenAI, Ollama, LM Studio, LocalAI, OpenRouter, and any OpenAI-compatible API
- **Provider support**: Lock to specific providers (useful for OpenRouter cost management)
- **Automatic session management**: Sessions auto-expire after idle time or token limit
- **Async operations**: All API calls are non-blocking
- **Folia & Paper compatible**: Works on both server types

---

## Configuration

### config.yml

```yaml
api:
  # OpenAI-compatible API endpoint
  url: "https://api.openai.com/v1"
  # Your API key (or token for self-hosted models)
  key: "your-api-key-here"
  # Model identifier
  model: "gpt-3.5-turbo"
  # Provider (optional, mainly for OpenRouter to lock to a specific provider)
  # Example: "openai", "anthropic", "azure"
  provider: ""

session:
  # Minutes of inactivity before session auto-expires
  max-idle-minutes: 15
  # Approximate max tokens per session (chars / 4)
  max-tokens: 10000

chat:
  # LLM temperature (0.0 - 2.0)
  temperature: 1.0
```

### Alternative Backends

**Ollama (local):**
```yaml
api:
  url: "http://localhost:11434/v1"
  key: "ollama"
  model: "llama3"
  provider: ""
```

**LM Studio:**
```yaml
api:
  url: "http://localhost:1234/v1"
  key: "lm-studio"
  model: "lmstudio-community/Mistral-7B-Instruct-v0.2"
  provider: ""
```

**OpenAI Official:**
```yaml
api:
  url: "https://api.openai.com/v1"
  key: "sk-..."
  model: "gpt-4o"
  provider: ""
```

**OpenRouter (with provider):**
```yaml
api:
  url: "https://openrouter.ai/api/v1"
  key: "your-openrouter-key"
  model: "gpt-3.5-turbo"
  provider: "openai"  # Lock to OpenAI provider (cheaper than auto-select)
```

---

## API Reference

### Getting the API

```java
import dev.agentchat.api.ChatAPI;
import dev.agentchat.api.ChatSession;
import dev.agentchat.api.ChatResponse;

// Get the API instance (available after AgentChatAPI loads)
ChatAPI api = ChatAPI.get();
```

### Creating a Session

```java
// sessionName: unique identifier for this session (e.g., "npc_1", "bot_alice")
// systemPrompt: instructions that define the agent's personality/behavior
ChatSession session = api.createSession("my_session_id", systemPrompt);
```

### Sending Messages

```java
// Send a message and get async response
CompletableFuture<ChatResponse> future = session.sendMessage("Hello!");

future.thenAccept(response -> {
    if (response.isSuccess()) {
        String reply = response.getContent();
        // Use the response...
    } else {
        String error = response.getErrorMessage();
        // Handle error...
    }
});
```

Or block for result (avoid on main thread):
```java
ChatResponse response = session.sendMessage("What's up?").join();
```

### Ending a Session

```java
// Permanently delete session and its context
api.endSession("my_session_id");
```

### Session Utilities

```java
// Check if session exists
boolean exists = api.hasSession("my_session_id");

// Get existing session (null if not found)
ChatSession session = api.getSession("my_session_id");

// Get all active sessions
Collection<ChatSession> allSessions = api.getAllSessions();

// Get session count
int count = api.getSessionCount();
```

### Session Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getName()` | `String` | Session identifier |
| `sendMessage(String)` | `CompletableFuture<ChatResponse>` | Send message, get response |
| `getApproximateTokenCount()` | `int` | Approximate tokens in context |
| `clearContext()` | `void` | Clear conversation history |
| `isExpired()` | `boolean` | Check if session expired |
| `getLastActivityTime()` | `long` | Timestamp of last activity |

### ChatResponse Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `isSuccess()` | `boolean` | Request succeeded |
| `getContent()` | `String` | AI response text (null if failed) |
| `getErrorMessage()` | `String` | Error details (null if success) |
| `getTokenCount()` | `int` | Approximate tokens used |

---

## Session Lifecycle

1. **Create**: `api.createSession(id, prompt)` - new session with context
2. **Interact**: `session.sendMessage()` - add to context, get AI response
3. **Auto-expire**: After `max-idle-minutes` of inactivity OR `max-tokens` exceeded
4. **Manual end**: `api.endSession(id)` - permanently delete session and context

---

## Soft-Dependency Setup

In your plugin's `plugin.yml`:

```yaml
name: YourPlugin
version: "1.0.0"
main: com.yourplugin.YourPlugin
api-version: "1.21"
softdepend: [AgentChatAPI]
```

### Checking for API Availability

```java
import org.bukkit.plugin.Plugin;

public class YourPlugin extends JavaPlugin {

    private ChatAPI chatApi = null;

    @Override
    public void onEnable() {
        Plugin apiPlugin = getServer().getPluginManager().getPlugin("AgentChatAPI");
        
        if (apiPlugin != null && apiPlugin.isEnabled()) {
            chatApi = ChatAPI.get();
            getLogger().info("AgentChatAPI connected");
        } else {
            getLogger().warning("AgentChatAPI not found");
        }
    }
}
```

---

## Thread Safety

- All API methods are thread-safe
- Sessions can be accessed from any thread
- Callbacks execute on server's async pool
- Safe to use in async events (AsyncPlayerChatEvent, etc.)

---

## Usage Examples

### Basic Pattern

```java
// 1. Get or create session for your entity
String sessionId = "entity_" + entity.getUniqueId();
ChatSession session = api.getSession(sessionId);
if (session == sessionId) {
    session = api.createSession(sessionId, loadPersonality(entity));
}

// 2. Send message
ChatResponse response = session.sendMessage(playerMessage).join();

// 3. Use response
if (response.isSuccess()) {
    String reply = response.getContent();
    entity.sendMessage(reply);
}
```

### Loading Personality

```java
private String loadPersonality(String entityId) {
    File file = new File(getDataFolder(), "personalities/" + entityId + ".md");
    if (file.exists()) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            // handle error
        }
    }
    return "You are a helpful assistant."; // fallback
}
```

---

## Notes

- Sessions are identified by strings - use consistent naming (e.g., `npc_1`, `bot_name`, `guardian_xyz`)
- Always check `response.isSuccess()` before using content
- Use `.thenAccept()` or async tasks instead of `.join()` on main thread
- Call `api.endSession()` when your entity is removed/despawned
- The `provider` field is OpenRouter-specific - it will be ignored by other APIs
