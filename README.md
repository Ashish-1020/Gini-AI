# Gemma Chat

An Android app that runs Google's **Gemma 4** AI model entirely on your phone. Chat by text or talk to it hands-free, with no internet needed after the first setup.

## Features

- **Offline AI chat.** Gemma 4 E2B runs on-device, so your messages never leave the phone.
- **One-tap model download.** Downloads the 2.6 GB model inside the app, in the background, and resumes after network drops.
- **Voice mode.** Talk naturally: it listens, answers out loud, then listens again. Tap the mic to interrupt.
- **Shared conversation.** Voice and text use the same chat history.
- **Streaming replies.** Answers appear word by word, with Markdown formatting.
- **Model control.** See whether the model is running, and stop it to free memory. It also stops when you close the app.

## Requirements

- Android 7.0+ (API 24)
- About 4 GB of RAM or more recommended
- About 3 GB of free storage
- Internet only for the first model download

## Getting started

1. Open the `OfflineChatBot` folder in Android Studio.
2. Set the Gradle JDK to **17**: *Settings → Build Tools → Gradle → Gradle JDK*.
3. Run the app on a real device. Emulators are usually too slow for the model.
4. Tap **Get Started**, then **Download model**, then **Start chatting**.

For offline voice recognition, install your language's offline speech pack in the phone's settings.

## Tech stack

| Area | Tools |
| --- | --- |
| AI runtime | LiteRT-LM, Gemma 4 E2B |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, Hilt |
| Async | Kotlin Coroutines, Flow |
| Voice | Android SpeechRecognizer, TextToSpeech |
| Download | Android DownloadManager |

## Project structure

```
app/src/main/java/com/aashu/offlinechatbot/
├── data/
│   ├── chat/        Shared conversation (ChatSession)
│   ├── llm/         Gemma engine wrapper (LiteRT-LM)
│   ├── voice/       Speech-to-text and text-to-speech
│   ├── repository/  Model download
│   └── model/       Model info and download states
├── di/              Hilt modules
└── ui/
    ├── onboarding/  Welcome screen
    ├── setup/       Model download screen
    ├── chat/        Text chat
    ├── voice/       Voice mode
    └── components/  Shared UI pieces
```

## How it works

```
<img width="808" height="361" alt="Screenshot 2026-10-23 084929" src="https://github.com/user-attachments/assets/99837b48-aa95-4c3c-b1d7-14e78959750f" />


```

The model file comes from [litert-community/gemma-4-E2B-it-litert-lm](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) on Hugging Face.
