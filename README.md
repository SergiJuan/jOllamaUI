<div align="center">
  <img src="https://raw.githubusercontent.com/SergiJuan/jOllamaUI/main/jollamaui-logo.png" width="200" alt="jOllamaUI logo">

# jOllamaUI — Ollama Client UI

A simple Java desktop chat client for interacting with Ollama and local LLMs.

![GitHub stars](https://img.shields.io/github/stars/SergiJuan/jOllamaUI)
![GitHub forks](https://img.shields.io/github/forks/SergiJuan/jOllamaUI)
![Contributors](https://img.shields.io/github/contributors/SergiJuan/jOllamaUI?style=social)

![Java](https://img.shields.io/badge/Java-17+-orange)
![GitHub last commit](https://img.shields.io/github/last-commit/SergiJuan/jOllamaUI?color=green)
![GitHub License](https://img.shields.io/github/license/SergiJuan/jOllamaUI)

</div>

---


---

## Overview

**jOllamaUI** is a clean, fast, and intuitive web interface for [Ollama](https://ollama.ai) — the popular tool for running large language models locally. Built with Java and modern web technologies, it provides a seamless chat experience with your favorite LLMs without requiring complex setups or external dependencies.

## Features

- **Clean, Modern UI** — Minimalist design with a focus on readability and usability

<div align="center">
  <img src="https://raw.githubusercontent.com/SergiJuan/jOllamaUI/main/jollamaui-showcase.png" alt="jOllamaUI Chat Interface" width="90%">
</div>

- **Real-time Streaming** — See responses generate token-by-token in real-time
- **Model Management** — Browse, download, and delete models directly from the interface

<div align="center">
  <img src="https://raw.githubusercontent.com/SergiJuan/jOllamaUI/main/jollamaui-showcase3.png" alt="jOllamaUI Models View" width="90%">
</div>

- **Thinking Visualization** — Toggle to see model reasoning steps (for supported models)

<div align="center">
  <img src="https://raw.githubusercontent.com/SergiJuan/jOllamaUI/main/jollamaui-showcase3.png" alt="jOllamaUI Models View" width="90%">
</div>

- **Configurable Host** — Connect to local or remote Ollama instances

<div align="center">
  <img src="https://raw.githubusercontent.com/SergiJuan/jOllamaUI/main/jollamaui-showcase2.png" alt="jOllamaUI Models View" width="90%">
</div>

- **Conversation History** — Maintains context throughout your chat session
- **Lightweight** — Single JAR file, no external dependencies required
- **Cross-Platform** — Works on any system with Java 17+



## Quick Start

### Prerequisites

- Java 17 or higher
- [Ollama](https://ollama.ai) installed and running (default: `http://localhost:11434`)

### Installation

1. **Download** the latest release JAR from the [Releases](https://github.com/SergiJuan/jOllamaUI/releases) page

2. **Run** the application:
   ```bash
   java -jar jOllamaUI.jar
   ```

3. **Open** your browser at `http://localhost:8080`

### Building from Source

```bash
# Clone the repository
git clone https://github.com/SergiJuan/jOllamaUI.git
cd jOllamaUI

# Build with Maven
mvn clean package

# Run the generated JAR
java -jar target/jOllamaUI.jar
```

## Usage

1. **Select a Model** — Choose from your installed models in the dropdown
2. **Start Chatting** — Type your message and press Enter or click the send button
3. **Manage Models** — Navigate to the Models tab to pull new models or remove unused ones
4. **Configure Settings** — Change the Ollama host in Settings if needed

## Architecture

```
jOllamaUI/
├── src/main/java/jua/sergi/jollamaui/
│   ├── Application.java          # Entry point
│   ├── config/
│   │   └── AppConfig.java        # Configuration & shared state
│   ├── server/
│   │   └── HttpServerManager.java # HTTP server lifecycle
│   └── handlers/                  # API endpoints
│       ├── BaseHandler.java
│       ├── ChatHandler.java
│       ├── ModelsHandler.java
│       ├── PullHandler.java
│       ├── ModelDeleteHandler.java
│       ├── SettingsHandler.java
│       └── StaticFileHandler.java
└── src/main/resources/
    ├── index.html                 # Main HTML
    ├── css/
    │   └── style.css             # Stylesheet
    └── js/
        └── app.js                 # Frontend application
```


## Technologies

- **Backend**: Java 17, embedded HTTP server (`com.sun.net.httpserver`)
- **Frontend**: Vanilla JavaScript, CSS3, HTML5
- **Build Tool**: Maven
- **Dependencies**:
    - [jOllama](https://github.com/SergiJuan/jOllama) — Java client for Ollama
    - [Gson](https://github.com/google/gson) — JSON serialization

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built with [jOllama](https://github.com/SergiJuan/jOllama), a Java client for Ollama
- Inspired by the simplicity of modern chat interfaces
- Thanks to the Ollama team for making local LLMs accessible

---