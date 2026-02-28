# AgentChatAPI

A Minecraft server plugin that provides a multi-session AI chat API for other plugins.

![License](https://img.shields.io/badge/license-MIT-green)
![Version](https://img.shields.io/badge/version-1.0.0-blue)
![API](https://img.shields.io/badge/api-1.21-orange)

---

## What is AgentChatAPI?

AgentChatAPI is a **plugin-for-plugins**. It provides a simple API that lets developers add AI-powered chat to their own plugins - whether that's fake players, NPCs, guild bots, or anything else that needs to chat.

Instead of each plugin developer building their own AI integration, they can simply use this API to create chat sessions where each session maintains its own conversation context.

### What can you build with it?

- **AI-powered fake players** that actually chat with real players
- **Interactive NPCs** with custom personalities
- **Guild assistants** that help players
- **Quest bots** that hold conversations
- Really... anything that needs to talk and remember context

---

## Features

- **Multi-session support** - Create unlimited independent chat sessions
- **OpenAI-compatible** - Works with OpenAI, Ollama, LM Studio, LocalAI, and more
- **Automatic cleanup** - Sessions expire after idle time or token limits
- **Async everything** - Non-blocking API calls, safe for any thread
- **Folia & Paper** - Works on both server types
- **Lightweight** - Minimal dependencies, focused on one job

---

## For Server Owners

### Installation

1. Download the latest release
2. Drop it into your `plugins/` folder
3. Configure `config.yml` with your API key and preferred settings
4. Restart or reload the server

### Configuration

```yaml
api:
  url: "https://api.openai.com/v1"
  key: "your-api-key-here"
  model: "gpt-3.5-turbo"

session:
  max-idle-minutes: 15
  max-tokens: 10000

chat:
  temperature: 1.0
```

### Supported Backends

| Service | URL Example |
|---------|-------------|
| OpenAI | `https://api.openai.com/v1` |
| Ollama | `http://localhost:11434/v1` |
| LM Studio | `http://localhost:1234/v1` |
| LocalAI | `http://localhost:8080/v1` |

---

## For Developers

If you're a plugin developer and want to add AI chat to your plugin, check out the [API Documentation](API_DOCUMENTATION.md).

### Quick Example

```java
// Create a chat session with a personality
ChatSession session = ChatAPI.get().createSession("my_bot", 
    "You are a friendly village merchant who sells rare items.");

// Send a message and get response
ChatResponse response = session.sendMessage("What do you sell?").join();

if (response.isSuccess()) {
    String reply = response.getContent();
    // reply = "Welcome, traveler! I have many rare treasures to offer..."
}
```

### Integration

Add as a soft-dependency in your `plugin.yml`:

```yaml
softdepend: [AgentChatAPI]
```

Then check for it at runtime:

```java
Plugin apiPlugin = getServer().getPluginManager().getPlugin("AgentChatAPI");
if (apiPlugin != null && apiPlugin.isEnabled()) {
    ChatAPI api = ChatAPI.get();
    // Use the API...
}
```

---

## Real-World Example

This API is being used in **FakePlayersFolia** to give fake players AI-powered chat capabilities. Each fake player can have its own personality defined in a `.md` file, and they can actually hold conversations with real players.

Check out the [FakePlayersFolia](https://github.com/your-repo/fakeplayersfolia) repository for a complete implementation example.

---

## License

AgentChatAPI is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

---

## Support

- For bugs and issues: Open an issue on GitHub
- For discussions: Join our discord community at https://discord.gg/VHwDt2yrdC

---

## Credits

Built for the Minecraft server community.
